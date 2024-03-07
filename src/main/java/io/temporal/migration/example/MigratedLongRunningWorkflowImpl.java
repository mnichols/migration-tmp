package io.temporal.migration.example;

import io.temporal.activity.ActivityOptions;
import io.temporal.failure.ApplicationFailure;
import io.temporal.workflow.Workflow;

import java.time.Duration;

// MigratedLongRunningWorkflowImpl runs in the target namespace
public class MigratedLongRunningWorkflowImpl implements LongRunningWorkflow{
    private final Verification verification;
    private final Activities acts;
    private final LongRunningWorkflowParams currentValue;

    public MigratedLongRunningWorkflowImpl() {
        this.currentValue = new LongRunningWorkflowParams();

        this.acts =  Workflow.newActivityStub(
                Activities.class,
                ActivityOptions.
                        newBuilder().
                        setStartToCloseTimeout(Duration.ofSeconds(10)).
                        build());
        this.verification = Workflow.newActivityStub(
                Verification.class,
                ActivityOptions.
                        newBuilder().
                        setStartToCloseTimeout(Duration.ofSeconds(10)).
                        build());
    }


    @Override
    public String execute(LongRunningWorkflowParams params)  {
        this.setCurrentValue(params.value);

        //This just verifies the input value against the legacy workflow, failing if it didn't receive the correct one
        // This block of verification can be removed
        LongRunningWorkflowParams expect = this.verification.verify(params);
        if(this.currentValue.value != expect.value && !expect.value.equals(params.value)) {
            // this might fail since now we are resuming in target NS but ContinueAsNew might have race with a inbound signal
            String message = String.format("workflow '%s' expected '%s' but got input params of '%s'. Current value is '%s'",
                    Workflow.getInfo().getWorkflowId(),
                    expect.value,
                    params.value,
                    this.currentValue.value);
            throw ApplicationFailure.newFailure(message, "verificationFailure");
        }

        // Now let's do the "migrated" implementation that skips things or works with elapsed timing, etc
        if(!params.executionState.skipStep1) {
            acts.execNonIdempotentStep1(params.value);
        }
        // sleep for five minutes, change this to support a flag that allows CAN
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
