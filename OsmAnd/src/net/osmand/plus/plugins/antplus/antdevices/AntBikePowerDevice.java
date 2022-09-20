package net.osmand.plus.plugins.antplus.antdevices;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc;
import com.dsi.ant.plugins.antplus.pccbase.PccReleaseHandle;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class AntBikePowerDevice extends AntCommonDevice<AntPlusBikePowerPcc> {

	private BikePowerData lastBikePowerData;

	private final List<AntBikePowerDataListener> dataListeners = new ArrayList<>();

	public static class BikePowerData {

		// The estimated timestamp of when this event was triggered.
		private final long timestamp;

		// The average power calculated from sensor data. Units: W.
		private final BigDecimal calculatedPower;

		private final boolean powerOnlyData;
		private final boolean initialPowerOnlyData;

		BikePowerData(long timestamp,
		              BigDecimal calculatedPower,
		              boolean powerOnlyData,
		              boolean initialPowerOnlyData) {
			this.timestamp = timestamp;
			this.calculatedPower = calculatedPower;
			this.powerOnlyData = powerOnlyData;
			this.initialPowerOnlyData = initialPowerOnlyData;
		}

		public long getTimestamp() {
			return timestamp;
		}

		public BigDecimal getCalculatedPower() {
			return calculatedPower;
		}

		public boolean isPowerOnlyData() {
			return powerOnlyData;
		}

		public boolean isInitialPowerOnlyData() {
			return initialPowerOnlyData;
		}

		@Override
		public String toString() {
			return "BikePowerData {" +
					"timestamp=" + timestamp +
					", calculatedPower=" + calculatedPower +
					", powerOnlyData=" + powerOnlyData +
					", initialPowerOnlyData=" + initialPowerOnlyData +
					'}';
		}
	}

	public interface AntBikePowerDataListener {

		void onAntBikePowerData(@NonNull BikePowerData data);
	}

	public AntBikePowerDevice() {
	}

	public AntBikePowerDevice(int antDeviceNumber) {
		super(antDeviceNumber);
	}

	@Nullable
	public BikePowerData getLastHeartRateData() {
		return lastBikePowerData;
	}

	public void addDataListener(@NonNull AntBikePowerDataListener listener) {
		if (!dataListeners.contains(listener)) {
			dataListeners.add(listener);
		}
	}

	public void removeDataListener(@NonNull AntBikePowerDataListener listener) {
		dataListeners.remove(listener);
	}

	@Override
	protected PccReleaseHandle<AntPlusBikePowerPcc> requestAccess(@NonNull Context context, int antDeviceNumber) {
		return AntPlusBikePowerPcc.requestAccess(context, antDeviceNumber, 0,
				new PluginAccessResultReceiver(), deviceStateChangeReceiver);
	}

	@Override
	protected PccReleaseHandle<AntPlusBikePowerPcc> requestAccess(@Nullable Activity activity, @NonNull Context context) {
		return AntPlusBikePowerPcc.requestAccess(activity, context, new PluginAccessResultReceiver(), deviceStateChangeReceiver);
	}

	protected void subscribeToEvents() {
		pcc.subscribeCalculatedPowerEvent((estTimestamp, eventFlags, dataSource, calculatedPower) -> {
			boolean powerOnlyData = AntPlusBikePowerPcc.DataSource.POWER_ONLY_DATA.equals(dataSource);
			boolean initialPowerOnlyData = AntPlusBikePowerPcc.DataSource.INITIAL_VALUE_POWER_ONLY_DATA.equals(dataSource);

			lastBikePowerData = new BikePowerData(estTimestamp, calculatedPower, powerOnlyData, initialPowerOnlyData);

			for (AntBikePowerDataListener listener : dataListeners) {
				listener.onAntBikePowerData(lastBikePowerData);
			}
		});
	}
}