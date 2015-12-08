ALTER TABLE "payment_event" ADD old_status character varying(255) NULL;
ALTER TABLE "payment_event" RENAME status TO new_status;
