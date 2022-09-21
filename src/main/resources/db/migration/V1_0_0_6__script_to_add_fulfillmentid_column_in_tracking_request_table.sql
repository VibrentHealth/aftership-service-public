-- --------------------------------------------------------------------------------------------------------------------
-- Date          : Aug 10, 2022             Added By  : Jigar Patel
-- JIRA ID       : AC-121210                Comments  : Alter script to add new column for fulfillment id
-- --------------------------------------------------------------------------------------------------------------------

ALTER TABLE `tracking_request`
	ADD COLUMN `fulfillment_order_id` bigint(20) NULL DEFAULT NULL;

ALTER TABLE `tracking_request_aud`
	ADD COLUMN `fulfillment_order_id` bigint(20) NULL DEFAULT NULL;