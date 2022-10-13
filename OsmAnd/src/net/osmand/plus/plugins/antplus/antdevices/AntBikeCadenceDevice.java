package net.osmand.plus.plugins.antplus.antdevices;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dsi.ant.plugins.antplus.pcc.AntPlusBikeCadencePcc;
import com.dsi.ant.plugins.antplus.pccbase.PccReleaseHandle;

import java.util.ArrayList;
import java.util.List;

public class AntBikeCadenceDevice extends AntCommonDevice<AntPlusBikeCadencePcc> {

	private BikeCadenceData lastBikeCadenceData;

	private final List<AntBikeCadenceDataListener> dataListeners = new ArrayList<>();

	public static class BikeCadenceData {

		// The estimated timestamp of when this event was triggered.
		// Useful for correlating multiple events and determining when data was sent for more accurate data records.
		private final long timestamp;

		// The cadence calculated from the raw values in the sensor broadcast. Units: rpm.
		private final int calculatedCadence;

		BikeCadenceData(long timestamp, int calculatedCadence) {
			this.timestamp = timestamp;
			this.calculatedCadence = calculatedCadence;
		}

		public long getTimestamp() {
			return timestamp;
		}

		public int getCalculatedCadence() {
			return calculatedCadence;
		}

		@Override
		public String toString() {
			return "BikeCadenceData {" +
					"timestamp=" + timestamp +
					", calculatedCadence=" + calculatedCadence +
					'}';
		}
	}

	public interface AntBikeCadenceDataListener {

		void onAntBikePowerData(@NonNull BikeCadenceData data);
	}

	public AntBikeCadenceDevice() {
	}

	public AntBikeCadenceDevice(int antDeviceNumber) {
		super(antDeviceNumber);
	}

	@Nullable
	public BikeCadenceData getLastBikeCadenceData() {
		return lastBikeCadenceData;
	}

	public void addDataListener(@NonNull AntBikeCadenceDataListener listener) {
		if (!dataListeners.contains(listener)) {
			dataListeners.add(listener);
		}
	}

	public void removeDataListener(@NonNull AntBikeCadenceDataListener listener) {
		dataListeners.remove(listener);
	}

	@Override
	protected PccReleaseHandle<AntPlusBikeCadencePcc> requestAccess(@NonNull Context context, int antDeviceNumber) {
		return AntPlusBikeCadencePcc.requestAccess(context, antDeviceNumber, 0,
				false, new PluginAccessResultReceiver(), deviceStateChangeReceiver);
	}

	@Override
	protected PccReleaseHandle<AntPlusBikeCadencePcc> requestAccess(@Nullable Activity activity, @NonNull Context context) {
		return AntPlusBikeCadencePcc.requestAccess(activity, context, new PluginAccessResultReceiver(), deviceStateChangeReceiver);
	}

	protected void subscribeToEvents() {
		pcc.subscribeCalculatedCadenceEvent((estTimestamp, eventFlags, calculatedCadence) -> {
			lastBikeCadenceData = new BikeCadenceData(estTimestamp, calculatedCadence.intValue());

			for (AntBikeCadenceDataListener listener : dataListeners) {
				listener.onAntBikePowerData(lastBikeCadenceData);
			}
		});
	}
}
