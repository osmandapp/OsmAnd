package net.osmand.plus.plugins.aistracker;

import androidx.annotation.NonNull;

import java.io.File;

public class AisMessageSimulationListener extends AisMessageListener  {

    private int simulatedMessageLatencyTime = 0;

    public AisMessageSimulationListener(@NonNull AisTrackerLayer layer, @NonNull File file,
                                        int simulatedLatencyTime) {
        super(layer, file);
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
