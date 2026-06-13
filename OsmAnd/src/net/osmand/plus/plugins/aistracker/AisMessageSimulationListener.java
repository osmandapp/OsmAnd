package net.osmand.plus.plugins.aistracker;

import androidx.annotation.NonNull;

import java.io.File;

public class AisMessageSimulationListener extends AisMessageListener  {

    private final int simulatedMessageLatencyTime;

    public AisMessageSimulationListener(@NonNull AisDataListener dataListener, @NonNull File file,
                                        int simulatedLatencyTime) {
        super(dataListener, file);
        simulatedMessageLatencyTime = simulatedLatencyTime;
    }

    @Override
    protected void handleAisMessage(int aisType, Object obj) {
        try {
            Thread.sleep(simulatedMessageLatencyTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        super.handleAisMessage(aisType, obj);
    }
}
