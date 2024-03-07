package io.temporal.migration.interceptor;

import io.micrometer.core.lang.NonNull;
import io.temporal.common.interceptors.ActivityInboundCallsInterceptor;
import io.temporal.common.interceptors.WorkerInterceptor;
import io.temporal.common.interceptors.WorkflowInboundCallsInterceptor;
import io.temporal.workflow.Workflow;

public class MigrationWorkerInterceptor implements WorkerInterceptor {


    private final Migrator migrator;

    public MigrationWorkerInterceptor(@NonNull Migrator migrator) {
        this.migrator = migrator;
    }

    @Override
    public WorkflowInboundCallsInterceptor interceptWorkflow(WorkflowInboundCallsInterceptor next) {
        if(!this.migrator.isMigrateable(Workflow.getInfo())) {
            return next;
        }

        return new MigrationWorkflowInboundCallsInterceptor(next, migrator);
    }

    @Override
    public ActivityInboundCallsInterceptor interceptActivity(ActivityInboundCallsInterceptor next) {
        return next;
    }
}
