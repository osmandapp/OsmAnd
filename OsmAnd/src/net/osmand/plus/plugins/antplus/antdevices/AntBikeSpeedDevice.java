package net.osmand.plus.plugins.antplus.antdevices;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dsi.ant.plugins.antplus.pcc.AntPlusBikeSpeedDistancePcc;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikeSpeedDistancePcc.CalculatedSpeedReceiver;
import com.dsi.ant.plugins.antplus.pcc.defines.EventFlag;
import com.dsi.ant.plugins.antplus.pccbase.PccReleaseHandle;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class AntBikeSpeedDevice extends AntCommonDevice<AntPlusBikeSpeedDistancePcc> {

	private static final double WHEEL_CIRCUMFERENCE = 2.1; //The wheel circumference in meters, used to calculate speed

	private BikeSpeedData lastBikeSpeedData;

	private final List<AntBikeSpeedDataListener> dataListeners = new ArrayList<>();

	public static class BikeSpeedData {

		// The estimated timestamp of when this event was triggered.
		// Useful for correlating multiple events and determining when data was sent for more accurate data records.
		private final long timestamp;

		// The speed calculated from the raw values in the sensor broadcast, based on this classes' set wheel circumference passed to the constructor. Units: m/s.
		private final double calculatedSpeed;

		public BikeSpeedData(long timestamp, double calculatedSpeed) {
			this.timestamp = timestamp;
			this.calculatedSpeed = calculatedSpeed;
		}

		public long getTimestamp() {
			return timestamp;
		}

		public double getCalculatedSpeed() {
			return calculatedSpeed;
		}

		@Override
		public String toString() {
			return "BikeSpeedData {" +
					"timestamp=" + timestamp +
					", calculatedSpeed=" + calculatedSpeed +
					'}';
		}
	}

	public interface AntBikeSpeedDataListener {

		void onAntBikeSpeedData(@NonNull BikeSpeedData data);
	}

	public AntBikeSpeedDevice() {
	}

	public AntBikeSpeedDevice(int antDeviceNumber) {
		super(antDeviceNumber);
	}

	@Nullable
	public BikeSpeedData getLastBikeSpeedData() {
		return lastBikeSpeedData;
	}

	public void addDataListener(@NonNull AntBikeSpeedDataListener listener) {
		if (!dataListeners.contains(listener)) {
			dataListeners.add(listener);
		}
	}

	public void removeDataListener(@NonNull AntBikeSpeedDataListener listener) {
		dataListeners.remove(listener);
	}

	@Override
	protected PccReleaseHandle<AntPlusBikeSpeedDistancePcc> requestAccess(@NonNull Context context, int antDeviceNumber) {
		return AntPlusBikeSpeedDistancePcc.requestAccess(context, antDeviceNumber, 0,
				false, new PluginAccessResultReceiver(), deviceStateChangeReceiver);
	}

	@Override
	protected PccReleaseHandle<AntPlusBikeSpeedDistancePcc> requestAccess(@Nullable Activity activity, @NonNull Context context) {
		return AntPlusBikeSpeedDistancePcc.requestAccess(activity, context, new PluginAccessResultReceiver(), deviceStateChangeReceiver);
	}

	protected void subscribeToEvents() {
		pcc.subscribeCalculatedSpeedEvent(new CalculatedSpeedReceiver(new BigDecimal(WHEEL_CIRCUMFERENCE)) {
			@Override
			public void onNewCalculatedSpeed(long estTimestamp, EnumSet<EventFlag> enumSet, BigDecimal calculatedSpeed) {
				lastBikeSpeedData = new BikeSpeedData(estTimestamp, calculatedSpeed.doubleValue());

				for (AntBikeSpeedDataListener listener : dataListeners) {
					listener.onAntBikeSpeedData(lastBikeSpeedData);
				}
			}
		});
	}
}
