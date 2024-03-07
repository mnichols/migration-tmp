package io.temporal.migration.example;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;

import java.time.Duration;

// Definition that is identical to previous implementation but
// updated to expose the executionState
public class MigrateableLongRunningWorkflowImpl implements  LongRunningWorkflow, QueryableLongRunningWorkflow {

    private final LongRunningWorkflowParams currentValue;
    private final Activities acts;

    public MigrateableLongRunningWorkflowImpl() {
        this.currentValue = new LongRunningWorkflowParams();
        this.currentValue.executionState = new LongRunningWorkflowState();
        this.acts =  Workflow.newActivityStub(
                Activities.class,
                ActivityOptions.
                        newBuilder().
                        setStartToCloseTimeout(Duration.ofSeconds(10)).
                        build());
    }

    // Workflow Function
    @Override
    public String execute(LongRunningWorkflowParams params)  {
        // This is the original workflow logic
        this.setCurrentValue(params.value);
        acts.execNonIdempotentStep1(params.value);
        this.currentValue.executionState.skipStep1 = true;
        // sleep for five minutes
        Workflow.sleep(Duration.ofSeconds(300));
        return this.currentValue.value;
    }


    @Override
    public void callThat(String newValue) {
        this.setCurrentValue(newValue);
        // this could be used as the migration method that continues as new
        // the question is whether this would drain off soon enough
        acts.execStep2(newValue);
    }


    @Override
    public LongRunningWorkflowParams getMigrationState() {
        return this.currentValue;
    }

    @Override
    public void setCurrentValue(String value) {
        this.currentValue.value = value;
    }

}
