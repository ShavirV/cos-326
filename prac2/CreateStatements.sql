/*
   Shavir Vallabh - u23718146
   
   This file builds the entire schema for the system,
   including tables, domains, enums, and sequences
*/

-- Everything is run as one transaction so no weird artefacts are left over
BEGIN;

-- Drop everything so we can start from a clean state. Can rerun without issues.
DROP TABLE IF EXISTS DeletedIncidentAudit CASCADE;
DROP TABLE IF EXISTS NodeBreachIncident CASCADE;
DROP TABLE IF EXISTS NetworkIncident CASCADE;
DROP TABLE IF EXISTS Incident CASCADE;
DROP TABLE IF EXISTS EvidenceCatalog CASCADE;

DROP FUNCTION IF EXISTS audit_deleted_incident() CASCADE;
DROP FUNCTION IF EXISTS check_evidence_integrity() CASCADE;
DROP FUNCTION IF EXISTS validate_evidence_array(TEXT[]) CASCADE;
DROP FUNCTION IF EXISTS has_duplicate_hashes(TEXT[]) CASCADE;
DROP FUNCTION IF EXISTS is_hash_registered(TEXT) CASCADE;
DROP FUNCTION IF EXISTS format_investigator_name(InvestigatorName) CASCADE;

DROP TYPE IF EXISTS InvestigatorName CASCADE;
DROP TYPE IF EXISTS SeverityType CASCADE;
DROP DOMAIN IF EXISTS NodeIPType CASCADE;

DROP SEQUENCE IF EXISTS incident_seq;
DROP SEQUENCE IF EXISTS evidence_seq;


-- 1. DOMAINS, ENUMS & SEQUENCES


-- Enforce IPv4 address format 0.0.0.0 to 255.255.255.255
CREATE DOMAIN NodeIPType AS VARCHAR(15)
    CHECK (
        VALUE IS NULL
        -- regex that i stole for IPv4 address formatting
        OR VALUE ~ '^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$'
    );
COMMENT ON DOMAIN NodeIPType IS 'IPv4 address stored as text, max 15 chars, validated by regex.';

CREATE TYPE SeverityType AS ENUM ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL');

-- ROW type bundling three varchar fields together to be used in NodeBreachIncident
CREATE TYPE InvestigatorName AS (
    title       VARCHAR(10),
    first_name  VARCHAR(50),
    last_name   VARCHAR(50)
);

-- Sequences used to generate PKs
-- Lets a base table and child tables draw from the same ID space
-- Thus a NetworkIncident and NodeBreachIncident cannot share the same ID, which is important
-- However Incident may collide with any of these since it is the parent class... not important here
CREATE SEQUENCE incident_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE evidence_seq START WITH 1 INCREMENT BY 1;



-- 2. TABLE INHERITANCE & AUXILIARY TABLES
 

-- Incident base table and NetworkIncident and NodeBreachIncident subtables
-- Children automatically have parent attributes by inherence 
CREATE TABLE Incident (
    incident_id     INTEGER PRIMARY KEY DEFAULT nextval('incident_seq'),
    incident_code   VARCHAR(10) UNIQUE NOT NULL,
    target_ip       NodeIPType,
    severity        SeverityType,
    detected_at     TIMESTAMP NOT NULL DEFAULT now()
);
COMMENT ON TABLE Incident IS 'Base class / table for the incident hierarchy.';

-- NetworkIncident (Inherits Incident)
-- evidence_hashes is a native PostgreSQL array (TEXT[]) 
-- single thing can ref multiple without any JOINS
CREATE TABLE NetworkIncident (
    slice_id         VARCHAR(20),
    evidence_hashes  TEXT[]
) INHERITS (Incident);
COMMENT ON TABLE NetworkIncident IS 'Incident subtype: a captured 5G network-slice event with linked PCAP evidence hashes.';

-- NodeBreachIncident (Inherits Incident)
-- compromised edge-computing node. Adds node_id, the composite UDT and a bool flag
CREATE TABLE NodeBreachIncident (
    node_id                 VARCHAR(20),
    assigned_investigator   InvestigatorName,
    is_contained             BOOLEAN NOT NULL DEFAULT FALSE
) INHERITS (Incident);
COMMENT ON TABLE NodeBreachIncident IS 'Incident subtype: a breached edge node, tracked with its assigned investigator.';

-- EvidenceCatalog, registered hashes, checked for validity below
CREATE TABLE EvidenceCatalog (
    hash_id         INTEGER PRIMARY KEY DEFAULT nextval('evidence_seq'),
    evidence_hash   TEXT UNIQUE NOT NULL,
    file_type       VARCHAR(20)
);
COMMENT ON TABLE EvidenceCatalog IS 'Registry of valid SHA-256 evidence hashes that may be referenced by NetworkIncident.';

-- Since this needs to track both NetworkIncident and NodeBreachIncident,
-- we add the special attributes of both here, such that one table can hold
-- data from both tables. No FK since its only created when the original table
-- entry is deleted. 
CREATE TABLE DeletedIncidentAudit (
    audit_id                SERIAL PRIMARY KEY,
    incident_type           VARCHAR(20) NOT NULL,   -- 'NetworkIncident' | 'NodeBreachIncident'
    incident_id             INTEGER NOT NULL,
    incident_code           VARCHAR(10),
    target_ip               NodeIPType,
    severity                SeverityType,
    detected_at             TIMESTAMP,
    -- NetworkIncident-specific columns, NULL for NodeBreachIncident rows
    slice_id                VARCHAR(20),
    evidence_hashes          TEXT[],
    -- NodeBreachIncident-specific columns, NULL for NetworkIncident rows
    node_id                  VARCHAR(20),
    assigned_investigator    InvestigatorName,
    is_contained              BOOLEAN,
    -- Audit specific data
    deleted_at                TIMESTAMP NOT NULL DEFAULT now(),
    deleted_by                VARCHAR(50) NOT NULL
);
COMMENT ON TABLE DeletedIncidentAudit IS 'Immutable log of every row deleted from NetworkIncident or NodeBreachIncident.';

--  3. PL/pgSQL UTILITY & VALIDATION FUNCTIONS 

-- format_investigator_name(InvestigatorName): takes the composite UDT
-- and returns a single display string, e.g. 'Dr. Jo Nasi'.
CREATE OR REPLACE FUNCTION format_investigator_name(p_investigator InvestigatorName)
RETURNS TEXT AS $$
BEGIN
    IF p_investigator IS NULL THEN
        RETURN NULL;
    END IF;

    RETURN TRIM(
        COALESCE(p_investigator.title, '') || ' ' ||
        COALESCE(p_investigator.first_name, '') || ' ' ||
        COALESCE(p_investigator.last_name, '')
    );
END;
$$ LANGUAGE plpgsql IMMUTABLE;
COMMENT ON FUNCTION format_investigator_name(InvestigatorName) IS
    'Formats an InvestigatorName composite value as "Title First Last"';

-- is_hash_registered(TEXT): a simple existence check against
-- EvidenceCatalog. Kept as its own function (rather than inlined) so it
-- can be reused both by validate_evidence_array() below and directly in
-- SELECT queries
CREATE OR REPLACE FUNCTION is_hash_registered(p_hash TEXT)
RETURNS BOOLEAN AS $$
DECLARE
    v_exists BOOLEAN;
BEGIN
    SELECT EXISTS (
        SELECT 1 FROM EvidenceCatalog WHERE evidence_hash = p_hash
    ) INTO v_exists;

    RETURN v_exists;
END;
$$ LANGUAGE plpgsql STABLE;
COMMENT ON FUNCTION is_hash_registered(TEXT) IS
    'Returns TRUE if the given hash exists in EvidenceCatalog.';

-- simple nested loop to check if duplicates appear. O(n^2) ahh implementation
CREATE OR REPLACE FUNCTION has_duplicate_hashes(p_hashes TEXT[])
RETURNS BOOLEAN AS $$
DECLARE
    v_len INTEGER;
    i INTEGER;
    j INTEGER;
BEGIN
    IF p_hashes IS NULL THEN
        RETURN FALSE;
    END IF;

    v_len := array_length(p_hashes, 1);

    -- base case, an empty or single-element or NULL len array cant have dupes
    IF v_len IS NULL OR v_len < 2 THEN
        RETURN FALSE;
    END IF;

    -- Nested loop: outer index i, inner index j > i, so every index [i, j] is checked exactly once
    -- also note 1 based indexing... :(
    FOR i IN 1 .. v_len - 1 LOOP
        FOR j IN i + 1 .. v_len LOOP
            IF p_hashes[i] = p_hashes[j] THEN
                RETURN TRUE;
            END IF;
        END LOOP;
    END LOOP;

    RETURN FALSE;
END;
$$ LANGUAGE plpgsql IMMUTABLE;
COMMENT ON FUNCTION has_duplicate_hashes(TEXT[]) IS
    'Check for duplicate values inside a hash array.';

-- validate_evidence_array(TEXT[]): composes the two functions above
-- every hash in the array must be registered in EvidenceCatalog AND the
-- array must contain no duplicates
-- called by the check_evidence_integrity() trigger
-- gatekeep gaslight girlboss
CREATE OR REPLACE FUNCTION validate_evidence_array(p_hashes TEXT[])
RETURNS BOOLEAN AS $$
DECLARE
    v_hash TEXT;
BEGIN
    -- NULL or empty evidence array is treated as vacuously valid
    IF p_hashes IS NULL OR array_length(p_hashes, 1) IS NULL THEN
        RETURN TRUE;
    END IF;

    -- Reject on duplicates
    IF has_duplicate_hashes(p_hashes) THEN
        RETURN FALSE;
    END IF;

    -- Every hash must already be registered in EvidenceCatalog
    FOREACH v_hash IN ARRAY p_hashes LOOP
        IF NOT is_hash_registered(v_hash) THEN
            RETURN FALSE;
        END IF;
    END LOOP;

    RETURN TRUE;
END;
$$ LANGUAGE plpgsql STABLE;
COMMENT ON FUNCTION validate_evidence_array(TEXT[]) IS
    'TRUE only if every hash in the array is registered in EvidenceCatalog and no duplicates exist.';


-- 4. TRIGGER FUNCTIONS & TRIGGERS

-- check_evidence_integrity() BEFORE INSERT OR UPDATE on NetworkIncident
-- raises exception so we never have invalid data in the table
CREATE OR REPLACE FUNCTION check_evidence_integrity()
RETURNS TRIGGER AS $$
BEGIN
    IF NOT validate_evidence_array(NEW.evidence_hashes) THEN
        RAISE EXCEPTION
            'evidence_hashes for incident_code % contains an unregistered and/or duplicate hash: %',
            NEW.incident_code, NEW.evidence_hashes;
    END IF;

    -- Must return NEW for a BEFORE ROW trigger to proceed
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_check_evidence_integrity
    BEFORE INSERT OR UPDATE ON NetworkIncident
    FOR EACH ROW
    EXECUTE FUNCTION check_evidence_integrity();

-- audit_deleted_incident(): AFTER DELETE on NetworkIncident AND NodeBreachIncident
-- TG_TABLE_NAME tells us which of the two tables fired the trigger, so
-- we branch and populate the type-specific columns accordingly, leaving other cols a null
--  Runs after delete because we are recording history, not gating the
-- delete itself, and OLD.* is only guaranteed populated for AFTER
-- triggers on the just-deleted row.
CREATE OR REPLACE FUNCTION audit_deleted_incident()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_TABLE_NAME = 'networkincident' THEN
        INSERT INTO DeletedIncidentAudit (
            incident_type, incident_id, incident_code, target_ip, severity, detected_at,
            slice_id, evidence_hashes,
            deleted_at, deleted_by
        ) VALUES (
            'NetworkIncident', OLD.incident_id, OLD.incident_code, OLD.target_ip, OLD.severity, OLD.detected_at,
            OLD.slice_id, OLD.evidence_hashes,
            now(), CURRENT_USER
        );

    ELSIF TG_TABLE_NAME = 'nodebreachincident' THEN
        INSERT INTO DeletedIncidentAudit (
            incident_type, incident_id, incident_code, target_ip, severity, detected_at,
            node_id, assigned_investigator, is_contained,
            deleted_at, deleted_by
        ) VALUES (
            'NodeBreachIncident', OLD.incident_id, OLD.incident_code, OLD.target_ip, OLD.severity, OLD.detected_at,
            OLD.node_id, OLD.assigned_investigator, OLD.is_contained,
            now(), CURRENT_USER
        );
    END IF;

    -- Return value is ignored for AFTER triggers, but PL/pgSQL still
    -- requires a RETURN statement.
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_deleted_network_incident
    AFTER DELETE ON NetworkIncident
    FOR EACH ROW
    EXECUTE FUNCTION audit_deleted_incident();

CREATE TRIGGER trg_audit_deleted_node_breach_incident
    AFTER DELETE ON NodeBreachIncident
    FOR EACH ROW
    EXECUTE FUNCTION audit_deleted_incident();

COMMIT;
