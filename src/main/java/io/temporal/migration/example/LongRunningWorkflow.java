package io.temporal.migration.example;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * The Workflow Definition's Interface must contain one method annotated
 * with @WorkflowMethod.
 *
 * <p>
 * Workflow Definitions should not contain any heavyweight computations,
 * non-deterministic
 * code, network calls, database operations, etc. Those things should be handled
 * by the
 * Activities.
 *
 * @see WorkflowInterface
 * @see WorkflowMethod
 */
@WorkflowInterface
public interface LongRunningWorkflow extends ValueSettable {
    /**
     * We are using an object for input params here but if user used a primitive then
     * it will be difficult to communicate that executionState over at the target site
     */
    @WorkflowMethod
    String execute(LongRunningWorkflowParams params);
    @SignalMethod
    void callThat(String newValue);

}
