package io.temporal.migration.interceptor;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface MigrationSupport {
    @ActivityMethod
    PullLegacyExecutionResponse pullLegacyExecution(PullLegacyExecutionRequest req);
}
