/* =====================================================================
   COS326 - Database Systems - Practical 2 2026
   InsertQueries.sql  (Task 3: Data Population, 4 Marks)

   Order matters: EvidenceCatalog rows must exist BEFORE we insert any
   NetworkIncident row that references those hashes, because
   trg_check_evidence_integrity (BEFORE INSERT on NetworkIncident) will
   reject any hash that isn't already registered.
   ===================================================================== */

-- ---------------------------------------------------------------------
-- EvidenceCatalog: at least 4 valid, registered hashes.
-- Using short, readable placeholder strings ('hash_101', ...) rather
-- than real 64-char SHA-256 hex digests purely so the sample data stays
-- easy to read/trace through the SelectQueries.sql and
-- TestTriggerQueries.sql demos; the column is TEXT so either works.
-- ---------------------------------------------------------------------
INSERT INTO EvidenceCatalog (evidence_hash, file_type) VALUES
    ('hash_101', 'PCAP'),
    ('hash_102', 'PCAP'),
    ('hash_103', 'LOG'),
    ('hash_104', 'MEMDUMP');

-- ---------------------------------------------------------------------
-- NetworkIncident: at least 3 valid records.
-- Every hash referenced in evidence_hashes below already exists in
-- EvidenceCatalog above, and none of the arrays contain internal
-- duplicates, so all three rows will pass check_evidence_integrity().
-- incident_id is left to its DEFAULT nextval('incident_seq') so it
-- shares the same id-space as NodeBreachIncident (see CreateStatements.sql
-- comment on the Incident table for why this matters).
-- ---------------------------------------------------------------------
INSERT INTO NetworkIncident (incident_code, target_ip, severity, detected_at, slice_id, evidence_hashes) VALUES
    ('NI-0001', '10.20.30.1',  'CRITICAL', '2026-08-01 09:15:00', 'SLICE-A1', ARRAY['hash_101', 'hash_102']),
    ('NI-0002', '10.20.30.5',  'HIGH',     '2026-08-02 14:42:00', 'SLICE-A1', ARRAY['hash_103']),
    ('NI-0003', '172.16.5.20', 'MEDIUM',   '2026-08-03 03:05:00', 'SLICE-B2', ARRAY['hash_104']);

-- ---------------------------------------------------------------------
-- NodeBreachIncident: at least 2 valid records.
-- assigned_investigator is populated using PostgreSQL's ROW(...)::type
-- composite-literal syntax to construct an InvestigatorName value
-- inline.
-- ---------------------------------------------------------------------
INSERT INTO NodeBreachIncident (incident_code, target_ip, severity, detected_at, node_id, assigned_investigator, is_contained) VALUES
    ('NB-0001', '192.168.1.10', 'CRITICAL', '2026-08-04 11:00:00', 'EDGE-NODE-07',
        ROW('Dr.', 'Alex', 'Mercer')::InvestigatorName, FALSE),
    ('NB-0002', '192.168.1.44', 'HIGH', '2026-08-05 20:30:00', 'EDGE-NODE-12',
        ROW('Ms.', 'Naledi', 'Khumalo')::InvestigatorName, TRUE);
