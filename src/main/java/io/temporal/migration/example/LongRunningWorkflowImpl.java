package io.temporal.migration.example;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;

import java.time.Duration;

public class LongRunningWorkflowImpl implements LongRunningWorkflow {
    private Activities acts;
    public LongRunningWorkflowImpl() {
        this.currentValue = new LongRunningWorkflowParams();
        this.acts =  Workflow.newActivityStub(
                Activities.class,
                ActivityOptions.
                        newBuilder().
                        setStartToCloseTimeout(Duration.ofSeconds(10)).
                        build());
    }
    private LongRunningWorkflowParams currentValue;
    @Override
    public String execute(LongRunningWorkflowParams params) {
        this.setCurrentValue(params.value);
        acts.execNonIdempotentStep1(params.value);
        // sleep for five minutes
        Workflow.sleep(Duration.ofSeconds(300));
        return this.currentValue.value;

    }

    @Override
    public void callThat(String newValue) {
        this.setCurrentValue(newValue);
        // TODO move into workflow thread
        acts.execStep2(newValue);
    }

    public void setCurrentValue(String value) {
        this.currentValue.value = value;
    }
}
