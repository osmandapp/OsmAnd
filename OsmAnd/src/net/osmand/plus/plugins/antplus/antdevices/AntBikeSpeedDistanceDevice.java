package net.osmand.plus.plugins.antplus.antdevices;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dsi.ant.plugins.antplus.pcc.AntPlusBikeSpeedDistancePcc;
import com.dsi.ant.plugins.antplus.pccbase.PccReleaseHandle;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class AntBikeSpeedDistanceDevice extends AntCommonDevice<AntPlusBikeSpeedDistancePcc> {

	private BikeSpeedDistanceData lastBikeCadenceData;

	private final List<AntBikeSpeedDistanceDataListener> dataListeners = new ArrayList<>();

	public static class BikeSpeedDistanceData {

		// The estimated timestamp of when this event was triggered.
		// Useful for correlating multiple events and determining when data was sent for more accurate data records.
		private final long timestamp;

		// Sensor reported time counter value of last distance or speed computation (up to 1/200s accuracy). Units: s.
		private final BigDecimal timestampOfLastEvent;

		//Total number of revolutions since the sensor was first connected.
		// Note: If the subscriber is not the first PCC connected to the device the accumulation will
		// probably already be at a value greater than 0 and the subscriber should save the first
		// received value as a relative zero for itself. Units: revolutions.
		private final long cumulativeRevolutions;

		public BikeSpeedDistanceData(long timestamp, BigDecimal timestampOfLastEvent, long cumulativeRevolutions) {
			this.timestamp = timestamp;
			this.timestampOfLastEvent = timestampOfLastEvent;
			this.cumulativeRevolutions = cumulativeRevolutions;
		}

		public long getTimestamp() {
			return timestamp;
		}

		@Override
		public String toString() {
			return "BikeSpeedDistanceData {" +
					"timestamp=" + timestamp +
					", timestampOfLastEvent=" + timestampOfLastEvent +
					", cumulativeRevolutions=" + cumulativeRevolutions +
					'}';
		}
	}

	public interface AntBikeSpeedDistanceDataListener {

		void onAntBikeSpeedDistanceData(@NonNull BikeSpeedDistanceData data);
	}

	public AntBikeSpeedDistanceDevice() {
	}

	public AntBikeSpeedDistanceDevice(int antDeviceNumber) {
		super(antDeviceNumber);
	}

	@Nullable
	public BikeSpeedDistanceData getLastHeartRateData() {
		return lastBikeCadenceData;
	}

	public void addDataListener(@NonNull AntBikeSpeedDistanceDataListener listener) {
		if (!dataListeners.contains(listener)) {
			dataListeners.add(listener);
		}
	}

	public void removeDataListener(@NonNull AntBikeSpeedDistanceDataListener listener) {
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
		pcc.subscribeRawSpeedAndDistanceDataEvent((estTimestamp, eventFlags, timestampOfLastEvent, cumulativeRevolutions) -> {
			lastBikeCadenceData = new BikeSpeedDistanceData(estTimestamp, timestampOfLastEvent, cumulativeRevolutions);

			for (AntBikeSpeedDistanceDataListener listener : dataListeners) {
				listener.onAntBikeSpeedDistanceData(lastBikeCadenceData);
			}
		});
	}
}
