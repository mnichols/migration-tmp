package io.temporal.migration.example;
/*
 *  Copyright (c) 2020 Temporal Technologies, Inc. All Rights Reserved
 *
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. A copy of the License is
 *  located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

import io.temporal.common.interceptors.WorkerInterceptor;
import io.temporal.migration.interceptor.MigrationWorkerInterceptor;
import io.temporal.migration.interceptor.Migrator;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerFactoryOptions;
import io.temporal.worker.WorkerOptions;

import java.time.Duration;

public class LegacyWorker {

  /**
   * With the Workflow and Activities defined, we can now start execution. The
   * main method starts
   * the worker and then the workflow.
   */
  public static void main(String[] args) throws Exception {

    Clients clients = new Clients();
    Migration migration = new MigrationImpl(clients.getTargetClient(), clients.getLegacyClient());


    /*
     * Define the workflow factory. It is used to create workflow workers for a
     * specific task queue.
     */
    final WorkerInterceptor workerInterceptor = new MigrationWorkerInterceptor((Migrator) migration);
    WorkerFactoryOptions wfo;
    if(System.getenv("SUPPORT_INTERCEPTION") != null) {
      wfo = WorkerFactoryOptions.newBuilder().setWorkerInterceptors(workerInterceptor).setMaxWorkflowThreadCount(2).build();
    } else {
      wfo = WorkerFactoryOptions.newBuilder().setMaxWorkflowThreadCount(2).build();
    }

    WorkerFactory factory = WorkerFactory.newInstance(clients.getLegacyClient(), wfo);    /*
     * Define the workflow worker. Workflow workers listen to a defined task queue
     * and process
     * workflows and activities.
     */
    Worker worker = factory.newWorker(AppConfig.TASK_QUEUE);
  
    /*
     * Register the workflow implementation with the worker.
     * Workflow implementations must be known to the worker at runtime in
     * order to dispatch workflow tasks.
     */


    if(System.getenv("SUPPORT_MIGRATION") == null) {
      worker.registerActivitiesImplementations(new ActivitiesImpl(),
              migration,
              new Simulation.ExecutionActivitiesImpl(clients.getLegacyClient()),
              new Simulation.SignalActivitiesImpl(clients.getLegacyClient(), clients.getTargetClient()));
      worker.registerWorkflowImplementationTypes(
              LongRunningWorkflowImpl.class,
              Simulation.SimulationWorkflowImpl.class
              );
    } else {
      System.out.println("running worker with migration extensions");
      worker.registerActivitiesImplementations(
              new ActivitiesImpl(),
              migration,
              new Simulation.ExecutionActivitiesImpl(clients.getLegacyClient()),
              new Simulation.SignalActivitiesImpl(clients.getLegacyClient(), clients.getTargetClient()));
      worker.registerWorkflowImplementationTypes(
              MigrateableLongRunningWorkflowImpl.class,
              Simulation.SimulationWorkflowImpl.class
              );

    }


    /*
     * Start all the workers registered for a specific task queue.
     * The started workers then start polling for workflows and activities.
     */
    factory.start();

  }
  public static WorkerFactoryOptions createWorkerFactoryOptions() {
    WorkerFactoryOptions.Builder builder = WorkerFactoryOptions.newBuilder();
    builder = builder
            // set thread limits and (implicitly) cache
            .setMaxWorkflowThreadCount(600)
            // OR set the cacheSize directly
            .setWorkflowCacheSize(600); // this
    return builder.build();
  }

  public static WorkerOptions createWorkerOptions() {
      WorkerOptions.Builder builder = WorkerOptions.newBuilder();
      // all set to defaults
      builder = builder
              // poller
              .setMaxConcurrentActivityTaskPollers(5)
              .setMaxConcurrentWorkflowTaskPollers(5)

              // executor
              .setMaxConcurrentActivityExecutionSize(200)
              .setMaxConcurrentWorkflowTaskExecutionSize(200)
              .setMaxConcurrentLocalActivityExecutionSize(200)

              // rate limiting
              .setMaxWorkerActivitiesPerSecond(0 /* unlimited */)
              .setMaxTaskQueueActivitiesPerSecond(0 /* unlimited */)

              // behavior
              .setDefaultDeadlockDetectionTimeout(1000 /* ms */)

              // heartbeat
              .setMaxHeartbeatThrottleInterval(Duration.ofSeconds(30))

              // sticky queue
              .setStickyQueueScheduleToStartTimeout(Duration.ofSeconds(5))

              // eager activities
              .setDisableEagerExecution(false)

              // versioning
              .setBuildId("42")
              .setUseBuildIdForVersioning(false);

      return builder.build();
  }
}


