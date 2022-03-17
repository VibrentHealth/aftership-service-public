-- --------------------------------------------------------------------------------------------------------------------
-- Date          : Sept 08, 2021            Added By  : Ritesh Khaire
-- JIRA ID       : AC-102878                Comments  : Added  script to create audit table for error tracking request
-- --------------------------------------------------------------------------------------------------------------------

create table aftership.tracking_request_error_aud
(
  id                      bigint(20) auto_increment ,
  rev                     int(11)     not null,
  revtype                 tinyint     null,
  tracking_id             varchar(45) null,
  error_code              int         null,
  retry_count             int         null,
  track_delivery_request  json        null,
  header                  json        null,
  created_on              bigint(20)  null,
  updated_on              bigint(20)  null,
  primary key (id, rev)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;