# Java Migration Example
A Java project using Signals to show how to migrate from OSS to Cloud

## Overview

This project simulates migrating _n_ workflows to a new Namespace while each workflow receives a signal that mutates internal workflow
state every **500 milliseconds** with a timestamp.


### Get the project
```
$ git clone https://github.com/temporalio/migration-example.git
```

Configure the following  variables (for Cloud) in [AppConfig.java](/src/main/java/io.temporal.migration.signaled/AppConfig.java).
```
# the "target" namespace to which you will be sending workflow state
TARGET_NAMESPACE="{namespace}.{accountid}"
# connection info for Temporal Cloud namespace
TLS_CERT_PATH="/path/to/ca.pem"
TLS_KEY_PATH="/path/to/ca.key"
# How many workflows do you want to execute and simulate signaling with?
WORKFLOW_COUNT=6 # how many workflows to kickoff/migrate
# What is the prefix to use for the executed workflow IDs?
ID_SEED="foo" 
```

#### Build the project

```
./gradlew build
```

#### Steps
1. Run a Temporal dev server with `temporal server start-dev`
1. `./gradlew setupWorkflows` : this executes `{WORKFLOW_COUNT}` workflows and starts a [Simulator](/src/main/java/io.temporal.migration.signaled/Simulation) that sends a timestamp to each workflow every 500ms.
1. `./gradlew startLegacyWorker` : this represents your code as it is being executed _today_ in your own Temporal cluster.
1. Open `localhost:8233/namespaces/default`.
    1. Observe created workflows are suspended and observe `callThat` signals being sent to those workflows every 500ms.
1. Shut down the `startLegacyWorker` process
1. `SUPPORT_MIGRATION=true SUPPORT_INTERCEPTION=true ./gradlew startLegacyWorker`: This represents executing the workflow extended to support the required Query in your legacy environment.
1. Run `./gradlew startInterceptingLegacyWorker`: This represents you starting the legacy worker with the interceptor registered, ready to receive the batch signal.
2. In a separate terminal: `./gradlew startTargetWorker`: This represents the updated code executed in the [target Namespace](/src/main/java/io.temporal.migration.signaled/TargetWorker).
1. ```
   # Send a batch signal job to all workflows of this Type to perform the migration!!
   temporal workflow signal \
    --query 'WorkflowType="LongRunningWorkflow" AND ExecutionStatus="Running"' \
    --name migrateIt \
    --reason 'migration' \
    --input \"finalvalue\" 
   ```
1. Open `localhost:8322/namespaces/default`.
    1. Observe workflows are `Completed`.
    1. Alternately use `WorkflowType="LongRunningWorkflow" AND ExecutionStatus="Running"` to ensure you have zero results.
1. Open `/path/to/target/namespace` in target UI. Observe the same workflows (by WorkflowID) running in the new namespace.
1. The input value for the new workflow should be identical to the final value in the legacy workflow. Use the `getMigrationState` query of the workflow to determine this.
    1. The [VerifyingLongRunningWorkflowImpl](/src/main/java/io.temporal.migration.signaled/VerifyingLongRunningWorkflowImpl) is register to perform this check for you. It will fail if the old and new executions do not have the correct state.
1. Find workflows that failed verification in target UI with `WorkflowType="LongRunningWorkflow" AND ExecutionStatus="Completed"`

#### Handy Commands

_Terminating all the things_
```
temporal workflow terminate --query 'ExecutionStatus="Running"' --reason term --env yourtarget
```

_Deleting all the things_
```
temporal workflow delete --query 'ExecutionStatus="Running"' --reason term --env yourtarget
```
