package io.temporal.migration.example;

import io.temporal.client.*;
import io.temporal.serviceclient.SimpleSslContextBuilder;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class Clients {
    public WorkflowClient getLegacyClient() {
        return legacyClient;
    }

    public WorkflowClient getTargetClient() {
        return targetClient;
    }

    private WorkflowClient legacyClient;
    private WorkflowClient targetClient;

    private WorkflowClient composedClient;
    public Clients() {
        WorkflowServiceStubs legacyService;
        legacyService = WorkflowServiceStubs.newServiceStubs(
                WorkflowServiceStubsOptions.newBuilder().build());

        WorkflowServiceStubs targetService = null;
        String targetNamespace = AppConfig.TARGET_NAMESPACE;
        if(targetNamespace==null) {
            throw new IllegalArgumentException("missing TARGET_NAMESPACE");
        }
        String targetEndpoint = String.format("%s.tmprl.cloud:7233", targetNamespace);

        try {
            System.out.println(String.format("loading %s", AppConfig.TLS_CERT_PATH));
            InputStream clientCert = new FileInputStream(AppConfig.TLS_CERT_PATH);
            InputStream clientKey = new FileInputStream(AppConfig.TLS_KEY_PATH);

            targetService = WorkflowServiceStubs.newServiceStubs(
                    WorkflowServiceStubsOptions.newBuilder()
                            .setSslContext(SimpleSslContextBuilder.forPKCS8(clientCert, clientKey).build())
                            .setTarget(targetEndpoint)
                            .build());

        } catch (IOException e) {
            System.err.println("Error loading certificates: " + e.getMessage());
        }
        /*
         * Get a Workflow service client which can be used to start, Signal, and Query
         * Workflow Executions.
         */
        legacyClient = WorkflowClient.newInstance(legacyService,
                WorkflowClientOptions.newBuilder()
                        .build());

        targetClient = WorkflowClient.newInstance(targetService,
                WorkflowClientOptions.newBuilder()
                        .setNamespace(targetNamespace)
                        .build());
    }

}
