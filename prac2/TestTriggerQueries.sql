/*
   -- TC1 and TC2 must fail, aborting the transaction
   -- we create a savepoint and rollback after they screw up
   -- TC3 and TC4 are meant to succeed
*/

BEGIN;

-- TC-1: INSERT INTO NetworkIncident with an unregistered evidence hash
-- Expected result   : trg_check_evidence_integrity aborts the INSERT
--                     with an exception (caught, then rolled
--                     back to the savepoint so the script can continue).
SAVEPOINT tc1_savepoint;

DO $$
BEGIN
    INSERT INTO NetworkIncident (incident_code, target_ip, severity, detected_at, slice_id, evidence_hashes)
    VALUES ('NI-BAD1', '10.0.0.99', 'HIGH', now(), 'SLICE-A1', ARRAY['hash_101', 'i_made_it_up']);

    RAISE NOTICE 'TC-1 FAILED: insert with unregistered hash was not rejected.';
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'TC-1 PASSED: insert correctly rejected -> %', SQLERRM;
END;
$$;

ROLLBACK TO SAVEPOINT tc1_savepoint;


-- TC-2: INSERT / UPDATE on NetworkIncident with duplicate evidence hashes
-- Expected result   : trg_check_evidence_integrity aborts the statement
--                     with an exception.
SAVEPOINT tc2_savepoint;

DO $$
BEGIN
    INSERT INTO NetworkIncident (incident_code, target_ip, severity, detected_at, slice_id, evidence_hashes)
    VALUES ('NI-BAD2', '10.0.0.98', 'HIGH', now(), 'SLICE-B2', ARRAY['hash_101', 'hash_101']);

    RAISE NOTICE 'TC-2 FAILED: insert with duplicate hashes was not rejected.';
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'TC-2 PASSED: insert correctly rejected -> %', SQLERRM;
END;
$$;

ROLLBACK TO SAVEPOINT tc2_savepoint;

SAVEPOINT tc2b_savepoint;

-- the update action should work the exact same
DO $$
BEGIN
    UPDATE NetworkIncident
    SET evidence_hashes = ARRAY['hash_103', 'hash_103']
    WHERE incident_code = 'NI-0002';

    RAISE NOTICE 'TC-2b FAILED: update with duplicate hashes was NOT rejected.';
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'TC-2b PASSED: update correctly rejected -> %', SQLERRM;
END;
$$;

ROLLBACK TO SAVEPOINT tc2b_savepoint;


-- TC-3: DELETE FROM NetworkIncident
-- Expected result : row is removed from NetworkIncident AND a
-- matching row appears in DeletedIncidentAudit with incident_type = 'NetworkIncident'
DELETE FROM NetworkIncident WHERE incident_code = 'NI-0003';

-- Verification query confirms (a) the live row is gone (b) the audit row is present with the correct captured attributes
SELECT 'TC-3 live row remaining (expect 0 rows)' AS check_label, *
FROM NetworkIncident WHERE incident_code = 'NI-0003';

SELECT 'TC-3 audit row (expect 1 row)' AS check_label,
       incident_type, incident_code, slice_id, evidence_hashes, deleted_at, deleted_by
FROM DeletedIncidentAudit
WHERE incident_code = 'NI-0003' AND incident_type = 'NetworkIncident';


-- TC-4: DELETE FROM NodeBreachIncident
-- Expected result : same as case 3
DELETE FROM NodeBreachIncident WHERE incident_code = 'NB-0002';

SELECT 'TC-4 live row remaining (expect 0 rows)' AS check_label, *
FROM NodeBreachIncident WHERE incident_code = 'NB-0002';

SELECT 'TC-4 audit row (expect 1 row)' AS check_label,
       incident_type, incident_code, node_id,
       format_investigator_name(assigned_investigator) AS investigator,
       is_contained, deleted_at, deleted_by
FROM DeletedIncidentAudit
WHERE incident_code = 'NB-0002' AND incident_type = 'NodeBreachIncident';

COMMIT;
