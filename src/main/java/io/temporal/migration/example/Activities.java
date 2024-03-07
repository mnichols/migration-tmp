package io.temporal.migration.example;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface Activities {
    @ActivityMethod
    void execNonIdempotentStep1(String value);
    @ActivityMethod
    void execStep2(String value);
}
