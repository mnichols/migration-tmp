package io.temporal.migration.example;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import io.temporal.migration.interceptor.ForwardSignalCommand;
import io.temporal.migration.interceptor.MigrateCommand;

@ActivityInterface
public interface Migration  {

    @ActivityMethod
    void resumeInTarget(MigrateCommand cmd);

    @ActivityMethod
    void signalTarget(ForwardSignalCommand cmd);
}
