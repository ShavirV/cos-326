/*

   Order matters, EvidenceCatalog rows must exist BEFORE we insert any
   NetworkIncident row that references those hashes, because
   trg_check_evidence_integrity will
   reject any hash that isn't already registered.
*/

-- fake hashes but just let it slide
INSERT INTO EvidenceCatalog (evidence_hash, file_type) VALUES
    ('hash_101', 'PCAP'),
    ('hash_102', 'PCAP'),
    ('hash_103', 'LOG'),
    ('hash_104', 'MEMDUMP');

-- 3 records using hashes that alredy exist in the evidenceCatalog
INSERT INTO NetworkIncident (incident_code, target_ip, severity, detected_at, slice_id, evidence_hashes) VALUES
    ('NI-0001', '10.20.30.1',  'CRITICAL', '2026-08-01 09:15:00', 'SLICE-A1', ARRAY['hash_101', 'hash_102']),
    ('NI-0002', '10.20.30.5',  'HIGH',     '2026-08-02 14:42:00', 'SLICE-A1', ARRAY['hash_103']),
    ('NI-0003', '172.16.5.20', 'MEDIUM',   '2026-08-03 03:05:00', 'SLICE-B2', ARRAY['hash_104']);

-- assigned investigator is created inline using ROW(..)
INSERT INTO NodeBreachIncident (incident_code, target_ip, severity, detected_at, node_id, assigned_investigator, is_contained) VALUES
    ('NB-0001', '192.168.1.10', 'CRITICAL', '2026-08-04 11:00:00', 'EDGE-NODE-07',
        ROW('Dr.', 'Jo', 'Nasi')::InvestigatorName, FALSE),
    ('NB-0002', '192.168.1.44', 'HIGH', '2026-08-05 20:30:00', 'EDGE-NODE-12',
        ROW('Mr.', 'Bat', 'Mann')::InvestigatorName, TRUE);
