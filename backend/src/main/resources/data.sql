-- Seeded demo users. Ids are predictable (H2 identity starts at 1) so the
-- frontend's mock-login dropdown and this file stay in sync.
INSERT INTO app_user (name, email, phone, role) VALUES ('Amira Hassan', 'amira.hassan@example.com', '+60 12-653 4501', 'CLAIMANT');
INSERT INTO app_user (name, email, phone, role) VALUES ('Wei Lin Tan', 'weilin.tan@example.com', '+60 17-567 4502', 'CLAIMANT');
INSERT INTO app_user (name, email, phone, role) VALUES ('Priya Nair', 'priya.nair@chubb.example', '+60 19-456 4503', 'OFFICER');
INSERT INTO app_user (name, email, phone, role) VALUES ('Marcus Ong', 'marcus.ong@chubb.example', '+60 16-364 4504', 'OFFICER');
INSERT INTO app_user (name, email, phone, role) VALUES ('Sarah Lim', 'sarah.lim@chubb.example', '+60 16-555 4505', 'MANAGER');

-- A few sample claims so the queue/workload/dashboard aren't empty on first run.
INSERT INTO claim (claimant_id, assigned_officer_id, type, status, incident_date, incident_description, estimated_liability, created_at, updated_at)
VALUES (1, NULL, 'MOTOR', 'SUBMITTED', '2026-08-01', 'Rear-ended at a traffic light on the PLUS highway, moderate bumper damage.', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO claim (claimant_id, assigned_officer_id, type, status, incident_date, incident_description, estimated_liability, created_at, updated_at)
VALUES (2, NULL, 'PROPERTY', 'SUBMITTED', '2026-07-28', 'Roof leak during heavy rain caused water damage to the living room ceiling.', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO claim (claimant_id, assigned_officer_id, type, status, incident_date, incident_description, estimated_liability, created_at, updated_at)
VALUES (1, 3, 'MOTOR', 'UNDER_REVIEW', '2026-07-15', 'Side mirror clipped by another vehicle while parked.', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO claim (claimant_id, assigned_officer_id, type, status, incident_date, incident_description, estimated_liability, created_at, updated_at)
VALUES (2, 3, 'PROPERTY', 'ASSESSED', '2026-07-10', 'Kitchen fire, smoke damage to cabinetry and one wall.', 8500.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO claim_status_history (claim_id, from_status, to_status, changed_by_id, changed_at)
VALUES (3, 'SUBMITTED', 'UNDER_REVIEW', 3, CURRENT_TIMESTAMP);

INSERT INTO claim_status_history (claim_id, from_status, to_status, changed_by_id, changed_at)
VALUES (4, 'SUBMITTED', 'UNDER_REVIEW', 3, CURRENT_TIMESTAMP);

INSERT INTO claim_status_history (claim_id, from_status, to_status, changed_by_id, changed_at)
VALUES (4, 'UNDER_REVIEW', 'ASSESSED', 3, CURRENT_TIMESTAMP);

-- Seed notifications matching the seeded status history above, so the bell
-- isn't empty on first login (claims created via this SQL file bypass
-- ClaimService, which is what normally creates these - see NotificationService).
INSERT INTO notification (recipient_id, claim_id, message, is_read, created_at)
VALUES (1, 3, 'Your claim #3 moved from SUBMITTED to UNDER_REVIEW', false, CURRENT_TIMESTAMP);

INSERT INTO notification (recipient_id, claim_id, message, is_read, created_at)
VALUES (2, 4, 'Your claim #4 moved from SUBMITTED to UNDER_REVIEW', true, CURRENT_TIMESTAMP);

INSERT INTO notification (recipient_id, claim_id, message, is_read, created_at)
VALUES (2, 4, 'Your claim #4 moved from UNDER_REVIEW to ASSESSED', false, CURRENT_TIMESTAMP);

INSERT INTO notification_dispatch (channel, recipient_address, subject, message, status, provider_message_id, triggered_by, dispatched_at)
VALUES ('EMAIL', 'amira.hassan@example.com', 'Chubb Claims Update — Claim #3', 'Your claim #3 moved from SUBMITTED to UNDER_REVIEW', 'SENT', 'sim-seed-0001', 'IN_APP', CURRENT_TIMESTAMP);

INSERT INTO notification_dispatch (channel, recipient_address, subject, message, status, provider_message_id, triggered_by, dispatched_at)
VALUES ('EMAIL', 'weilin.tan@example.com', 'Chubb Claims Update — Claim #4', 'Your claim #4 moved from UNDER_REVIEW to ASSESSED', 'SENT', 'sim-seed-0002', 'IN_APP', CURRENT_TIMESTAMP);
