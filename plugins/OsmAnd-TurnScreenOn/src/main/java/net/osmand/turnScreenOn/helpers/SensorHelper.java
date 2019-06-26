package net.osmand.turnScreenOn.helpers;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import net.osmand.turnScreenOn.app.TurnScreenApp;
import net.osmand.turnScreenOn.listener.MessageObservable;
import net.osmand.turnScreenOn.listener.OnMessageListener;
import net.osmand.turnScreenOn.log.PlatformUtil;

import java.util.ArrayList;
import java.util.List;

public class SensorHelper implements SensorEventListener, MessageObservable {
    private static final org.apache.commons.logging.Log LOG = PlatformUtil.getLog(SensorHelper.class);

    private static final int SENSOR_SENSITIVITY = 4;

    private SensorManager mSensorManager;
    private Sensor mProximity;
    private TurnScreenApp app;
    private List<OnMessageListener> messageListeners;

    public SensorHelper(TurnScreenApp app) {
        this.app = app;
        mSensorManager = (SensorManager) app.getSystemService(Context.SENSOR_SERVICE);
        mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        messageListeners = new ArrayList<>();
    }

    public void switchOnSensor() {
        mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void switchOffSensor() {
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            if (event.values[0] >= -SENSOR_SENSITIVITY && event.values[0] <= SENSOR_SENSITIVITY) {
                notifyListeners();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void addListener(OnMessageListener listener) {
        if (listener != null && !messageListeners.contains(listener)) {
            messageListeners.add(listener);
        }
    }

    @Override
    public void removeListener(OnMessageListener listener) {
        if (listener != null) {
            messageListeners.remove(listener);
        }
    }

    @Override
    public void notifyListeners() {
        if (messageListeners != null) {
            for(OnMessageListener listener : messageListeners){
                listener.onMessageReceive();
            }
        }
    }
}
