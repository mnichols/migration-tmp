package io.temporal.migration.example;

public final class AppConfig {
    public static final String TASK_QUEUE = "MigrationTaskQueue";
    public static final String TARGET_NAMESPACE="migration-demo.sdvdw";

    public static final String TLS_CERT_PATH="/Users/mnichols/certs/migration-demo.sdvdw.pem";
    public static final String TLS_KEY_PATH="/Users/mnichols/certs/migration-demo.sdvdw.key";
    public static final int WORKFLOW_COUNT =50;
    public static final String ID_SEED="cici";

    public static final Integer SIGNAL_FREQUENCY_MILLIS=250;

    public static final boolean SIMULATOR_FAILOVER = false;

    private AppConfig() {
    }
}
