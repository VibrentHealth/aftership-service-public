-- --------------------------------------------------------------------------------------------------------------------
-- Date          : Sept 6, 2021             Added By  : John Shaik
-- JIRA ID       : AC-102203                Comments  : Added table to store track delivery error request
-- --------------------------------------------------------------------------------------------------------------------
-- -----------------------------------------------------
-- Table `aftership`.`tracking_request_error`
-- -----------------------------------------------------
create table aftership.tracking_request_error
(
  id                      bigint(20) auto_increment primary key,
  tracking_id             varchar(45) not null,
  error_code              int         null,
  retry_count             int         not null,
  track_delivery_request  json        null,
  header                  json        null,
  created_on              bigint(20)  null,
  updated_on              bigint(20)  null,
  constraint tracking_request_error_tracking_id_uindex unique (tracking_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;