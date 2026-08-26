package net.osmand.plus.plugins.aistracker;

import androidx.annotation.NonNull;

import net.osmand.shared.aistracker.AisDataListener;
import net.osmand.shared.aistracker.AisMessageListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import org.apache.commons.logging.Log;
import net.osmand.PlatformUtil;

public class AisMessageSimulationListener extends AisMessageListener {

    private static final Log LOG = PlatformUtil.getLog(AisMessageSimulationListener.class);

    private final int simulatedMessageLatencyTime;

    public AisMessageSimulationListener(@NonNull AisDataListener dataListener, @NonNull File file,
                                        int simulatedLatencyTime) {
        super(dataListener);
        simulatedMessageLatencyTime = simulatedLatencyTime;
        startReading(file);
    }

    private void startReading(File file) {
        new Thread(() -> {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    processLine(line);
                }
            } catch (Exception e) {
                LOG.error("Error reading simulated AIS messages", e);
            }
        }).start();
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
