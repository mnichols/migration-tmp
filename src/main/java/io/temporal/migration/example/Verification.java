package io.temporal.migration.example;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface Verification {
    @ActivityMethod
    LongRunningWorkflowParams verify(LongRunningWorkflowParams sourceValue);
}
