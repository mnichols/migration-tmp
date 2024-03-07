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

import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

public class TargetWorker {

  /**
   * With the Workflow and Activities defined, we can now start execution. The
   * main method starts
   * the worker and then the workflow.
   */
  public static void main(String[] args) throws Exception {

    Clients clients = new Clients();


    /*
     * Define the workflow factory. It is used to create workflow workers for a
     * specific task queue.
     */
    WorkerFactory factory = WorkerFactory.newInstance(clients.getTargetClient());

    /*
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


    worker.registerActivitiesImplementations(new ActivitiesImpl(), new VerificationImpl(clients.getLegacyClient()));

    worker.registerWorkflowImplementationTypes(MigratedLongRunningWorkflowImpl.class);

//    worker.registerWorkflowImplementationTypes(LongRunningWorkflowImpl.class);
    /*
     * Start all the workers registered for a specific task queue.
     * The started workers then start polling for workflows and activities.
     */
    factory.start();

  }
}
