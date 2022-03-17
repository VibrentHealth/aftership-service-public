package com.vibrent.aftership.constants;

public class KafkaConstants {

    public static final Long POLL_TIMEOUT = 3000L;

    public static final String VXP_HEADER_VERSION = "VXP-Header-Version";
    public static final String VXP_ORIGINATOR = "VXP-Originator";
    public static final String VXP_PATTERN = "VXP-Pattern";
    public static final String VXP_MESSAGE_SPEC = "VXP-Message-Spec";
    public static final String VXP_MESSAGE_SPEC_VERSION = "VXP-Message-Spec-Version";
    public static final String VXP_TENANT_ID = "VXP-Tenant-ID";
    public static final String VXP_PROGRAM_ID = "VXP-Program-ID";
    public static final String VXP_TRIGGER = "VXP-Trigger";
    public static final String VXP_WORKFLOW_NAME = "VXP-Workflow-Name";
    public static final String VXP_WORKFLOW_INSTANCE_ID = "VXP-Workflow-Instance-ID";
    public static final String VXP_MESSAGE_ID = "VXP-Message-ID";
    public static final String VXP_IN_REPLY_TO_ID = "VXP-In-Reply-To-ID";
    public static final String VXP_MESSAGE_TIMESTAMP = "VXP-Message-Timestamp";
    public static final String VIBRENT_ID = "Vibrent-ID";
    public static final String EXTERNAL_ID = "External-ID";
    public static final String TRACKING_ID = "Tracking-ID";

    private KafkaConstants() {
        // static class...
    }
}
