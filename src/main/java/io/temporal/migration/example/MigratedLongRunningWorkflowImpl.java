package io.temporal.migration.example;

import io.temporal.activity.ActivityOptions;
import io.temporal.failure.ApplicationFailure;
import io.temporal.workflow.ContinueAsNewOptions;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Objects;

// MigratedLongRunningWorkflowImpl runs in the target namespace
public class MigratedLongRunningWorkflowImpl implements LongRunningWorkflow{
    private final Verification verification;
    private final Activities acts;
    private final LongRunningWorkflowParams currentValue;
    private static Logger logger = LoggerFactory.getLogger(MigratedLongRunningWorkflowImpl.class);
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
        /*

         */

        this.setCurrentValue(params.value);

        //This just verifies the input value against the legacy workflow, failing if it didn't receive the correct one
        // This block of verification can be removed
        LongRunningWorkflowParams expect = this.verification.verify(params);
        if(Objects.equals(params.value, expect.value)) {
            // Now let's do the "migrated" implementation that skips things or works with elapsed timing, etc
            if(!params.executionState.skipStep1) {
                acts.execNonIdempotentStep1(params.value);
            }
            // sleep for five minutes, change this to support a flag that allows CAN
            Workflow.sleep(Duration.ofSeconds(30));
            // double check that a late signal did not come in
            if(Objects.equals(this.currentValue.value, params.value)) {
                return this.currentValue.value;
            }
            // this might fail since now we are resuming in target NS but ContinueAsNew or UnhandledCommand might have race with a inbound signal
            String message = String.format("forwarded signal scenario 1...workflow '%s' expected '%s' from legacy execution, but got input params of '%s'. \n Current value received by a late arriving signal is '%s'",
                    Workflow.getInfo().getWorkflowId(),
                    expect.value,
                    params.value,
                    this.currentValue.value);
            throw ApplicationFailure.newFailure(message,
                    "forwardedSignalFailure");

        }
        // wait a moment for a signal to arrive, this time we are comparing local value against the 'expect' value
        Workflow.sleep(Duration.ofSeconds(5));
        if(Objects.equals(this.currentValue.value, expect.value)) {

            // sleep for five minutes, change this to support a flag that allows CAN
            Workflow.sleep(Duration.ofSeconds(30));
            // double check that a late signal did not come in
            if(Objects.equals(this.currentValue.value, expect.value)) {
                return this.currentValue.value;
            }
            // this might fail since now we are resuming in target NS but UnhandledCommand might have race with a inbound signal
            String message = String.format("forwarded signal scenario 2...workflow '%s' expected '%s' from legacy execution, but got input params of '%s'. \n Current value (possibly impacted by forwarded signal) is '%s'",
                    Workflow.getInfo().getWorkflowId(),
                    expect.value,
                    params.value,
                    this.currentValue.value);
            throw ApplicationFailure.newFailure(message,
                    "forwardedSignalFailure");
        }
        String message = String.format("workflow '%s' expected '%s' from legacy execution, but got input params of '%s'. \n Current value  is '%s'",
                Workflow.getInfo().getWorkflowId(),
                expect.value,
                params.value,
                this.currentValue.value);

        throw ApplicationFailure.newFailure(message,
                "verificationFailure");
    }


    @Override
    public void callThat(String newValue) {
        this.setCurrentValue(newValue);
        // TODO move into workflow thread
        //acts.execStep2(newValue);
    }

    public void setCurrentValue(String value) {
        this.currentValue.value = value;
    }

}
