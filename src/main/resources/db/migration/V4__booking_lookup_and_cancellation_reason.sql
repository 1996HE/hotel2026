-- Guest booking lookup and auditable whole-request cancellation.

ALTER TABLE public_booking_requests
  ADD COLUMN cancellation_reason varchar(1000);

-- Preserve compatibility if a deployment already contains cancellations created
-- before a reason became mandatory.
UPDATE public_booking_requests
SET cancellation_reason = '移行前のキャンセル（理由未記録）'
WHERE status = 'cancelled'
  AND NULLIF(BTRIM(cancellation_reason), '') IS NULL;

ALTER TABLE public_booking_requests
  ADD CONSTRAINT ck_public_booking_cancellation_reason CHECK (
    status != 'cancelled' OR NULLIF(BTRIM(cancellation_reason), '') IS NOT NULL
  );
