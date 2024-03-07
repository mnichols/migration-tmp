package io.temporal.migration.example;

public class LongRunningWorkflowState {
    public String targetNamespace;
    public Boolean skipStep1;

    public LongRunningWorkflowState() {}
    public LongRunningWorkflowState( Boolean skipStep1) {
        this.skipStep1 = skipStep1;
    }
}
