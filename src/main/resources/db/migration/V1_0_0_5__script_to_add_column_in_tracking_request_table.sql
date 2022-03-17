-- --------------------------------------------------------------------------------------------------------------------
-- Date          : Dec 29, 2021             Added By  : Ritesh Kahire
-- JIRA ID       : AC-103152                Comments  : Alter script to add new column to identify carrier response type
-- --------------------------------------------------------------------------------------------------------------------

ALTER TABLE `tracking_request`
	ADD COLUMN `carrier_response_type` VARCHAR(50) NULL DEFAULT 'NOTIFICATION';

ALTER TABLE `tracking_request_aud`
	ADD COLUMN `carrier_response_type` VARCHAR(50) NULL DEFAULT NULL;