package io.temporal.migration.example;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.temporal.activity.LocalActivityOptions;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionRequest;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionResponse;
import io.temporal.api.workflowservice.v1.WorkflowServiceGrpc;
import io.temporal.client.*;
import io.temporal.migration.interceptor.Constants;
import io.temporal.migration.interceptor.ForwardSignalCommand;
import io.temporal.migration.interceptor.MigrateCommand;
import io.temporal.migration.interceptor.Migrator;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInfo;
import org.slf4j.Logger;

import java.time.Duration;

/*
    Example implementation of Migrator interface to start workflow in target Namespace
 */
public class MigrationImpl implements Migration, Migrator {
    private static final Logger logger = Workflow.getLogger(MigrationImpl.class);

    private final WorkflowClient legacyNamespaceClient;
    private WorkflowClient targetNamespaceClient;
    public MigrationImpl(WorkflowClient targetNamespaceClient, WorkflowClient legacyNamespaceClient) {
        this.targetNamespaceClient = targetNamespaceClient;
        this.legacyNamespaceClient = legacyNamespaceClient;
    }
    @Override
    public void migrate(MigrateCommand cmd) {
        Migration stub = Workflow.newLocalActivityStub(
                Migration.class,
                LocalActivityOptions.newBuilder().
                        setStartToCloseTimeout(Duration.ofSeconds(5)).
                        build());
        stub.resumeInTarget(cmd);
    }

    @Override
    public void forwardSignal(ForwardSignalCommand cmd) {
        Migration stub = Workflow.newLocalActivityStub(
                Migration.class,
                LocalActivityOptions.newBuilder().
                        setStartToCloseTimeout(Duration.ofSeconds(5)).
                        build());
        stub.signalTarget(cmd);
    }

    @Override
    public void signalTarget(ForwardSignalCommand input) {
        // check that execution in target exists
        WorkflowStub targetWF = this.
                targetNamespaceClient.
                newUntypedWorkflowStub(input.workflowId);

        try {
            WorkflowStub workflow = this.targetNamespaceClient.newUntypedWorkflowStub(input.workflowId);
            workflow.signal(input.signalName, input.arguments);
        } catch(StatusRuntimeException e) {
            if(e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                logger.debug("workflow {} not found...signal not forwarded in target", input.workflowId);
                // swallow this error
            }
        } catch(Exception e) {
            logger.error("failed to forwardSignal", e);
            throw e;
        }
    }

    @Override
    public void resumeInTarget(MigrateCommand cmd) {
        // check that execution in target exists
        WorkflowStub targetWF = this.
                targetNamespaceClient.
                newUntypedWorkflowStub(cmd.workflowId);
        WorkflowServiceStubs svc = this.targetNamespaceClient.getWorkflowServiceStubs();
        WorkflowServiceGrpc.WorkflowServiceBlockingStub stub = svc.blockingStub();
        DescribeWorkflowExecutionRequest req = DescribeWorkflowExecutionRequest.newBuilder().
                setNamespace(this.targetNamespaceClient.getOptions().getNamespace()).
                setExecution(targetWF.getExecution()).build();
        try {
            DescribeWorkflowExecutionResponse resp = stub.describeWorkflowExecution(req);
        } catch(StatusRuntimeException e) {
            if(e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                //logger.info("workflow {} not found...starting in target", cmd.workflowId);
                WorkflowStub workflow = this.targetNamespaceClient.newUntypedWorkflowStub(cmd.workflowType,
                        WorkflowOptions.newBuilder().
                                setWorkflowId(cmd.workflowId).
                                setTaskQueue(AppConfig.TASK_QUEUE).
                                build());

                // getting the value at the last possible moment...use??
//                Object args = this.legacyNamespaceClient.
//                        newUntypedWorkflowStub(cmd.workflowId).query(Constants.MIGRATION_STATE_QUERY_NAME,Object.class);
                workflow.start(cmd.arguments);
                return;
            }
            throw e;
        } catch(Exception e) {
            logger.error("failed to resumeInRemote", e);
            throw e;
        }
    }

    @Override
    public boolean isMigrateable(WorkflowInfo info) {
        return info.getWorkflowType().equals("LongRunningWorkflow");
    }
}
