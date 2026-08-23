/* =====================================================================
   COS326 - Database Systems - Practical 2 2026
   TestTriggerQueries.sql  (Task 5: Trigger & Constraint Testing, 10 Marks)

   General approach: TC-1 and TC-2 are EXPECTED to fail (the trigger
   must abort the transaction with an error). Rather than letting a
   RAISE EXCEPTION poison the whole script (PostgreSQL aborts the entire
   surrounding transaction once an error occurs), each of those two
   tests is wrapped in its own SAVEPOINT. If the statement raises the
   expected exception we ROLLBACK TO SAVEPOINT, which discards only that
   failed attempt and lets the script continue cleanly into the next
   test. TC-3 and TC-4 are expected to SUCCEED, so no savepoint/rollback
   is needed for them - we just DELETE and then SELECT the audit table
   to prove the trigger fired.
   ===================================================================== */

BEGIN;

-- =====================================================================
-- TC-1: INSERT INTO NetworkIncident with an unregistered evidence hash
-- Tested condition : evidence_hashes contains 'hash_999', which was
--                     never inserted into EvidenceCatalog.
-- Expected result   : trg_check_evidence_integrity aborts the INSERT
--                     with an exception (caught below, then rolled
--                     back to the savepoint so the script can continue).
-- =====================================================================
SAVEPOINT tc1_savepoint;

DO $$
BEGIN
    INSERT INTO NetworkIncident (incident_code, target_ip, severity, detected_at, slice_id, evidence_hashes)
    VALUES ('NI-BAD1', '10.0.0.99', 'HIGH', now(), 'SLICE-A1', ARRAY['hash_101', 'hash_999']);

    RAISE NOTICE 'TC-1 FAILED: insert with unregistered hash was NOT rejected.';
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'TC-1 PASSED: insert correctly rejected -> %', SQLERRM;
END;
$$;

ROLLBACK TO SAVEPOINT tc1_savepoint;


-- =====================================================================
-- TC-2: INSERT / UPDATE on NetworkIncident with duplicate evidence hashes
-- Tested condition : evidence_hashes contains 'hash_101' twice.
-- Expected result   : trg_check_evidence_integrity aborts the statement
--                     with an exception.
-- =====================================================================
SAVEPOINT tc2_savepoint;

DO $$
BEGIN
    INSERT INTO NetworkIncident (incident_code, target_ip, severity, detected_at, slice_id, evidence_hashes)
    VALUES ('NI-BAD2', '10.0.0.98', 'HIGH', now(), 'SLICE-B2', ARRAY['hash_101', 'hash_101']);

    RAISE NOTICE 'TC-2 FAILED: insert with duplicate hashes was NOT rejected.';
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'TC-2 PASSED: insert correctly rejected -> %', SQLERRM;
END;
$$;

ROLLBACK TO SAVEPOINT tc2_savepoint;

-- Also demonstrate the UPDATE path, since the spec says "INSERT / UPDATE":
-- take the existing valid row NI-0002 (evidence_hashes = {hash_103}) and
-- try to update it to contain a duplicate.
SAVEPOINT tc2b_savepoint;

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


-- =====================================================================
-- TC-3: DELETE FROM NetworkIncident
-- Tested condition : delete an existing, valid network incident record
--                     (NI-0003, inserted in InsertQueries.sql).
-- Expected result   : row is removed from NetworkIncident AND a
--                     matching row appears in DeletedIncidentAudit with
--                     incident_type = 'NetworkIncident'.
-- =====================================================================
DELETE FROM NetworkIncident WHERE incident_code = 'NI-0003';

-- Verification query for the marker: confirms (a) the live row is gone
-- and (b) the audit row is present with the correct captured attributes.
SELECT 'TC-3 live row remaining (expect 0 rows)' AS check_label, *
FROM NetworkIncident WHERE incident_code = 'NI-0003';

SELECT 'TC-3 audit row (expect 1 row)' AS check_label,
       incident_type, incident_code, slice_id, evidence_hashes, deleted_at, deleted_by
FROM DeletedIncidentAudit
WHERE incident_code = 'NI-0003' AND incident_type = 'NetworkIncident';


-- =====================================================================
-- TC-4: DELETE FROM NodeBreachIncident
-- Tested condition : delete an existing node breach incident record
--                     (NB-0002, inserted in InsertQueries.sql).
-- Expected result   : row is removed from NodeBreachIncident AND a
--                     matching row appears in DeletedIncidentAudit with
--                     incident_type = 'NodeBreachIncident'.
-- =====================================================================
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
