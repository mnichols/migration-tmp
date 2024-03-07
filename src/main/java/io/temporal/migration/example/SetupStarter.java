package io.temporal.migration.example;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class SetupStarter {
    private static final Logger logger = LoggerFactory.getLogger(Simulation.SimulationWorkflow.class);

    public static void main(String[] args) {
        Clients clients = new Clients();

        String idSeed = AppConfig.ID_SEED;
        int count = AppConfig.WORKFLOW_COUNT;

        WorkflowClient legacy = clients.getLegacyClient();
        try {
            for (int i = 0; i < count; i++) {
                String wid = String.format("%s-%d", idSeed, i);
                WorkflowOptions workflowOptions = WorkflowOptions.newBuilder().
                        setWorkflowId(wid).
                        setTaskQueue(AppConfig.TASK_QUEUE).build();
                LongRunningWorkflow workflow = legacy.newWorkflowStub(LongRunningWorkflow.class, workflowOptions);

                LongRunningWorkflowParams params = new LongRunningWorkflowParams();
                params.value = String.format("initial-%d", i);
                WorkflowClient.start(workflow::execute, params);
            }
        } catch( Exception e) {
            logger.error("an error was encountered trying to start workflows...aborting: " + e);
            System.exit(1);
        }

        Simulation.SimulationWorkflow simulationWorkflow = legacy.
                newWorkflowStub(Simulation.SimulationWorkflow.class, WorkflowOptions.newBuilder().
                setWorkflowId(String.format("%s-simulator", idSeed)).setTaskQueue(AppConfig.TASK_QUEUE).build());
        try {
            Simulation.SimulationWorkflowParams params = new Simulation.SimulationWorkflowParams();
            params.setFailover(AppConfig.SIMULATOR_FAILOVER);
            CompletableFuture<Void> execution = WorkflowClient.execute(simulationWorkflow::simulate, params);
            logger.info("simulation started: " + execution);
        } catch( Exception e) {
            logger.error("simulation failed to start: " + e);
            System.exit(1);
        }
    }
}
