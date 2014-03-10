# Create UserSessions

# --- !Ups

DROP TABLE Films;

CREATE TABLE Films
(
  code character(5),
  title character varying(40),
  did integer,
  date_prod date,
  kind character varying(10),
  len interval hour to minute,
  CONSTRAINT production UNIQUE (date_prod)
);

INSERT INTO Films(
            code, title, did, date_prod, kind, len)
    VALUES ('CODE1', 'Title1', 1, now(), 'Kind1', '00:40:00');



DROP TABLE Event;

CREATE TABLE Event
(
  id SERIAL,
  event_id varchar(255) NOT NULL,
  lat double precision NOT NULL,
  lon double precision NOT NULL,
  event_timestamp bigint NOT NULL,
  icon varchar(255) NOT NULL,
  tags text NOT NULL,
  PRIMARY KEY (id)
);

DROP TABLE EventDescription;

CREATE TABLE EventDescription
(
  id SERIAL,
  event_id varchar(255) NOT NULL,
  lang character(2) NOT NULL,
  title varchar(255) NOT NULL,
  markup text,
  tags text,
  PRIMARY KEY (id)
);




# --- !Downs

DROP TABLE UserSessions;

DROP TABLE Films