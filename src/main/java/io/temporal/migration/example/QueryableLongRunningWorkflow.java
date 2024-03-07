package io.temporal.migration.example;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.WorkflowInterface;

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
 * @see io.temporal.workflow.WorkflowInterface
 * @see io.temporal.workflow.WorkflowMethod
 */
@WorkflowInterface
public interface QueryableLongRunningWorkflow {
    @QueryMethod
    LongRunningWorkflowParams getMigrationState();
}
