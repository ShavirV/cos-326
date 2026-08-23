/* =====================================================================
   COS326 - Database Systems - Practical 2 2026
   SelectQueries.sql  (Task 4: Analytical SQL Reports, 8 Marks)
   ===================================================================== */

-- ---------------------------------------------------------------------
-- 1. Investigator Assignment Summary (2 Marks)
-- Lists incident_code, target IP, containment status, and the formatted
-- investigator name for all breach incidents. We select directly from
-- NodeBreachIncident (rather than the parent Incident table) because
-- is_contained and assigned_investigator only exist on that subtype,
-- and format_investigator_name() is called on the composite column to
-- turn it into the requested single display string.
-- ---------------------------------------------------------------------
SELECT
    incident_code,
    target_ip                                          AS target_ip,
    is_contained,
    format_investigator_name(assigned_investigator)     AS investigator_name
FROM NodeBreachIncident
ORDER BY incident_code;


-- ---------------------------------------------------------------------
-- 2. Evidence Hash Lookup Query (3 Marks)
-- Given a specific hash (example: 'hash_101'), list every
-- NetworkIncident row that references it inside evidence_hashes. The
-- ANY() construct tests array membership: "value = ANY(array_column)"
-- is the standard, index-friendly way to search inside a PostgreSQL
-- array column.
-- ---------------------------------------------------------------------
SELECT
    incident_id,
    incident_code,
    slice_id,
    evidence_hashes
FROM NetworkIncident
WHERE 'hash_101' = ANY (evidence_hashes)
ORDER BY incident_code;


-- ---------------------------------------------------------------------
-- 3. High-Risk Network Slices Report (3 Marks)
-- Group by slice_id and count incidents whose severity is CRITICAL or
-- HIGH. FILTER (WHERE ...) is used instead of a WHERE clause on the
-- whole query so that slices are still included even if, hypothetically,
-- every one of their incidents were LOW/MEDIUM (COUNT would just come
-- back as 0 rather than the slice disappearing) - a defensive touch for
-- an "operational report" that ops staff will rely on.
-- ---------------------------------------------------------------------
SELECT
    slice_id,
    COUNT(*) FILTER (WHERE severity IN ('CRITICAL', 'HIGH')) AS high_risk_incident_count
FROM NetworkIncident
GROUP BY slice_id
ORDER BY high_risk_incident_count DESC;
