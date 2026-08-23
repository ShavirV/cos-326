-- Lists incident_code, target IP, containment status, and the formatted
-- investigator name for all breach incidents. We select directly from
-- NodeBreachIncident because is_contained and assigned_investigator only exist on that subtype,
-- and format_investigator_name() is called on the composite column to
-- turn it into the requested single display string.
SELECT
    incident_code,
    target_ip AS target_ip,
    is_contained,
    format_investigator_name(assigned_investigator) AS investigator_name -- inline sort of format
FROM NodeBreachIncident
ORDER BY incident_code;


-- Given a specific hash ('hash_101'), list every
-- NetworkIncident row that references it inside evidence_hashes. The
-- ANY() construct tests array membership: "value = ANY(array_column)" to search inside a column
SELECT
    incident_id,
    incident_code,
    slice_id,
    evidence_hashes
FROM NetworkIncident
WHERE 'hash_101' = ANY (evidence_hashes)
ORDER BY incident_code;

-- Group by slice_id and count incidents whose severity is CRITICAL or
-- HIGH. FILTER (WHERE ...) is used instead of a WHERE clause on the
-- whole query so that slices are still included even if every incident is LOW/MEDIUM
SELECT
    slice_id,
    COUNT(*) FILTER (WHERE severity IN ('CRITICAL', 'HIGH')) AS high_risk_incident_count
FROM NetworkIncident
GROUP BY slice_id
ORDER BY high_risk_incident_count DESC;
