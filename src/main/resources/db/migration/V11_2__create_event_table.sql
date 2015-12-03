CREATE TABLE "payment_event"
(
  id serial PRIMARY KEY,
  payment_id integer NOT NULL REFERENCES "payment"(id),
  created timestamp NOT NULL,
  timestamp timestamp NULL,
  payment_status character varying(255) NOT NULL,
  check_succeeded boolean NOT NULL
)
WITH (
  OIDS=FALSE
);
ALTER TABLE "payment_event" OWNER TO oph;

CREATE INDEX ON "payment_event"(payment_id);
