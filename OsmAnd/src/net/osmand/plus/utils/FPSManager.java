package net.osmand.plus.utils;

import androidx.annotation.NonNull;

import net.osmand.core.android.MapRendererView;

import java.util.Timer;
import java.util.TimerTask;

public class FPSManager {
    
    public enum Interaction {
        IDLE,
        IMPRECISE_ANIMATION,
        ANIMATION,
        UI,
    }

    private static volatile FPSManager instance;
        
    private int currentMaxFPS = 1;
    private final int[] interactionCounters = {0, 0, 0, 0};
    private MapRendererView mapRenderer = null;
    private Timer idleTimer = null;
    private TimerTask idleTimerTask = null;

    public static FPSManager getInstance() {
        if (instance == null) {
            synchronized (FPSManager.class) {
                if (instance == null) {
                    instance = new FPSManager();
                }
            }
        }

        return instance;
    }

    public void initialize(@NonNull MapRendererView mapRenderer) {
        this.mapRenderer = mapRenderer;
    }

    public void setInteraction(@NonNull Interaction interaction) {
        synchronized (interactionCounters) {
            ++interactionCounters[interaction.ordinal()];
            updateFPS();
        }
    }

    public void endInteraction(@NonNull Interaction state) {
        synchronized (interactionCounters) {
            --interactionCounters[state.ordinal()];
            updateFPS();
        }
    }

    private void updateFPS() {
        if (mapRenderer == null) {
            return;
        }

        if (idleTimer != null) {
            idleTimer.cancel();
            idleTimer = null;
            idleTimerTask = null;
        }

        Interaction activeInteraction = Interaction.IDLE;
        for (int i = Interaction.IDLE.ordinal() + 1; i < Interaction.values().length; i++) {
            if (interactionCounters[i] > 0) {
                activeInteraction = Interaction.values()[i];
            }
        }

        if (activeInteraction != Interaction.IDLE) {
            int fps = getFPSForInteraction(activeInteraction, mapRenderer);
            setRendererFPS(fps, mapRenderer);
        } else { // if interaction == IDLE set it after idleStateTimerDelay to avoid animation lag
            idleTimer = new Timer();
            idleTimerTask = new TimerTask() {
                @Override
                public void run() {
                    synchronized (interactionCounters) {
                        int fps = getFPSForInteraction(Interaction.IDLE, mapRenderer);
                        setRendererFPS(fps, mapRenderer);

                        idleTimer = null;
                        idleTimerTask = null;
                    }
                }
            };

            int idleInteractionTimerDelay = mapRenderer.getMaxHardwareFrameRate() / 1000;
            idleTimer.schedule(idleTimerTask, idleInteractionTimerDelay);
        }
    }

    private void setRendererFPS(int fps, @NonNull MapRendererView mapRenderer) {
        if (fps != currentMaxFPS) {
            mapRenderer.setMaximumFrameRate(fps);
            currentMaxFPS = fps;
        }
    }

    private int getFPSForInteraction(@NonNull Interaction state, @NonNull MapRendererView mapRenderer) {
        return switch (state) {
            case IMPRECISE_ANIMATION -> mapRenderer.getMaxHardwareFrameRate() / 4;
            case ANIMATION -> mapRenderer.getMaxHardwareFrameRate() / 2;
            case UI -> mapRenderer.getMaxHardwareFrameRate();
            default -> 1;
        };
    }
}
