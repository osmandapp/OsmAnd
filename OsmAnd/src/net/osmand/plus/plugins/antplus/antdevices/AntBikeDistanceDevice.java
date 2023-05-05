package net.osmand.plus.plugins.antplus.antdevices;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dsi.ant.plugins.antplus.pcc.AntPlusBikeSpeedDistancePcc;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikeSpeedDistancePcc.CalculatedAccumulatedDistanceReceiver;
import com.dsi.ant.plugins.antplus.pcc.defines.EventFlag;
import com.dsi.ant.plugins.antplus.pccbase.PccReleaseHandle;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class AntBikeDistanceDevice extends AntCommonDevice<AntPlusBikeSpeedDistancePcc> {

	private static final double WHEEL_CIRCUMFERENCE = 2.1; //The wheel circumference in meters, used to calculate distance

	private BikeDistanceData lastBikeDistanceData;

	private final List<AntBikeDistanceDataListener> dataListeners = new ArrayList<>();

	public static class BikeDistanceData {

		// The estimated timestamp of when this event was triggered.
		// Useful for correlating multiple events and determining when data was sent for more accurate data records.
		private final long timestamp;

		// The accumulated distance calculated from the raw values in the sensor broadcast since the sensor was first connected, based on this classes' set wheel circumference passed to the constructor.
		private final double accumulatedDistance;

		public BikeDistanceData(long timestamp, double accumulatedDistance) {
			this.timestamp = timestamp;
			this.accumulatedDistance = accumulatedDistance;
		}

		public long getTimestamp() {
			return timestamp;
		}

		public double getAccumulatedDistance() {
			return accumulatedDistance;
		}

		@Override
		public String toString() {
			return "BikeDistanceData {" +
					"timestamp=" + timestamp +
					", calculatedAccumulatedDistance=" + accumulatedDistance +
					'}';
		}
	}

	public interface AntBikeDistanceDataListener {

		void onAntBikeDistanceData(@NonNull BikeDistanceData data);
	}

	public AntBikeDistanceDevice() {
	}

	public AntBikeDistanceDevice(int antDeviceNumber) {
		super(antDeviceNumber);
	}

	@Nullable
	public BikeDistanceData getLastBikeDistanceData() {
		return lastBikeDistanceData;
	}

	public void addDataListener(@NonNull AntBikeDistanceDataListener listener) {
		if (!dataListeners.contains(listener)) {
			dataListeners.add(listener);
		}
	}

	public void removeDataListener(@NonNull AntBikeDistanceDataListener listener) {
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
		pcc.subscribeCalculatedAccumulatedDistanceEvent(new CalculatedAccumulatedDistanceReceiver(new BigDecimal(WHEEL_CIRCUMFERENCE)) {
			@Override
			public void onNewCalculatedAccumulatedDistance(long estTimestamp, EnumSet<EventFlag> eventFlags, BigDecimal accumulatedDistance) {
				lastBikeDistanceData = new BikeDistanceData(estTimestamp, accumulatedDistance.doubleValue());

				for (AntBikeDistanceDataListener listener : dataListeners) {
					listener.onAntBikeDistanceData(lastBikeDistanceData);
				}
			}
		});
	}
}
