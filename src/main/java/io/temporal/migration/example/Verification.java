package io.temporal.migration.example;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import io.temporal.migration.interceptor.PullLegacyExecutionRequest;
import io.temporal.migration.interceptor.PullLegacyExecutionResponse;

@ActivityInterface
public interface Verification {
    @ActivityMethod
    LongRunningWorkflowParams verify(LongRunningWorkflowParams sourceValue);

    @ActivityMethod
    PullLegacyExecutionResponse pull(PullLegacyExecutionRequest cmd);
}
