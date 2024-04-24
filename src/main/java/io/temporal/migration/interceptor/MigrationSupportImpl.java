package io.temporal.migration.interceptor;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.temporal.activity.Activity;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionRequest;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionResponse;
import io.temporal.api.workflowservice.v1.WorkflowServiceGrpc;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowStub;
import io.temporal.failure.ApplicationFailure;
import io.temporal.serviceclient.WorkflowServiceStubs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class MigrationSupportImpl implements MigrationSupport{
    private static Logger logger = LoggerFactory.getLogger(MigrationSupportImpl.class);
    private final WorkflowClient legacyClient;

    public MigrationSupportImpl(WorkflowClient legacyClient) {
        this.legacyClient = legacyClient;
    }

    private void sleep(int seconds) {
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(seconds));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @Override
    public PullLegacyExecutionResponse pullLegacyExecution(PullLegacyExecutionRequest cmd) {

        if (Objects.equals(cmd.getNamespace(), "")) {
            cmd.setNamespace(this.legacyClient.getOptions().getNamespace());
        }
        if (Objects.equals(cmd.getWorkflowId(), "")) {
            cmd.setWorkflowId(Activity.getExecutionContext().getInfo().getWorkflowId());
        }

        PullLegacyExecutionResponse resp = new PullLegacyExecutionResponse();
        WorkflowStub legacyWF = this.legacyClient.newUntypedWorkflowStub(cmd.getWorkflowId());
        WorkflowServiceStubs svc = this.legacyClient.getWorkflowServiceStubs();
        WorkflowServiceGrpc.WorkflowServiceBlockingStub stub = svc.blockingStub();
        DescribeWorkflowExecutionRequest req = DescribeWorkflowExecutionRequest.newBuilder().
                setNamespace(cmd.getNamespace()).
                setExecution(legacyWF.getExecution()).build();

        while (true) {
            try {
                DescribeWorkflowExecutionResponse wfResp = stub.describeWorkflowExecution(req);
                WorkflowExecutionStatus stat = wfResp.getWorkflowExecutionInfo().getStatus();
                if (wfResp.getWorkflowExecutionInfo().getStatus() == WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_COMPLETED) {
                    Object out = legacyWF.query(Constants.MIGRATION_STATE_QUERY_NAME, Object.class);
                    resp.setMigrationState(out);
                    resp.setStatus(stat);
                    resp.setResumable(true);
                    return resp;
                }
                sleep(3);
            } catch (StatusRuntimeException e) {
                Status.Code rstat = e.getStatus().getCode();
                if (Objects.requireNonNull(rstat) == Status.Code.NOT_FOUND) {
                    throw ApplicationFailure.newNonRetryableFailure("workflow execution not found", "not found");
                }
            }
        }
    }
}
