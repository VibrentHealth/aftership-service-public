-- --------------------------------------------------------------------------------------------------------------------
-- Date          : Sept 2, 2021             Added By  : John Shaik
-- JIRA ID       : AC-102848                Comments  : Added table to store track delivery request
-- --------------------------------------------------------------------------------------------------------------------
-- -----------------------------------------------------
-- Table `aftership`.`tracking_request`
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS aftership;

CREATE TABLE IF NOT EXISTS `aftership`.`revinfo` (
  `rev` int(11) NOT NULL AUTO_INCREMENT,
  `revtstmp` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`rev`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;


create table aftership.tracking_request
(
  id                      bigint(20) auto_increment primary key,
  operation               varchar(45) not null,
  provider                varchar(45) not null,
  tracking_id             varchar(45) not null,
  participant             json        null,
  status                  varchar(45) null,
  sub_status_code         varchar(45) null,
  sub_status_description  text        null,
  carrier_response        json        null,
  header                  json        null,
  created_on              bigint(20)  null,
  updated_on              bigint(20)  null,
  constraint tracking_request_tracking_id_uindex unique (tracking_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

create table aftership.tracking_request_aud
(
  id                      bigint(20)      not null,
  rev                     int(11)         not null,
  revtype                 tinyint         null,
  operation               varchar(45)     null,
  provider                varchar(45)     null,
  tracking_id             varchar(45)     null,
  participant             json            null,
  status                  varchar(45)     null,
  sub_status_code         varchar(45)     null,
  sub_status_description  text            null,
  carrier_response        json            null,
  header                  json            null,
  created_on              bigint(20)      null,
  updated_on              bigint(20)      null,
  primary key (id, rev),
  constraint `fk_tracking_request_audit_id` foreign key (`id`) references `tracking_request` (`id`),
  constraint `fk_tracking_request_audit_revinfo` foreign key (`rev`) references `revinfo` (`rev`)
) ENGINE = InnoDB
  DEFAULT charset = utf8;

create index index_tracking_request_aud_rev on aftership.tracking_request_aud (rev);