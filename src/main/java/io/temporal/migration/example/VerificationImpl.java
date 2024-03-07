package io.temporal.migration.example;

import io.temporal.activity.Activity;
import io.temporal.client.WorkflowClient;

public class VerificationImpl implements Verification{
    private final WorkflowClient legacyClient;

    public VerificationImpl(WorkflowClient legacyClient) {
        this.legacyClient = legacyClient;
    }

    @Override
    public LongRunningWorkflowParams verify(LongRunningWorkflowParams sourceValue) {
        QueryableLongRunningWorkflow workflow = legacyClient.newWorkflowStub(QueryableLongRunningWorkflow.class, Activity.getExecutionContext().getInfo().getWorkflowId());
        LongRunningWorkflowParams lastKnownValue = workflow.getMigrationState();
        return lastKnownValue;
    }
}
