package net.osmand.plus.plugins.antplus.antdevices;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc;
import com.dsi.ant.plugins.antplus.pccbase.PccReleaseHandle;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class AntHeartRateDevice extends AntCommonDevice<AntPlusHeartRatePcc> {

	private HeartRateData lastHeartRateData;

	private final List<HeartRateDataListener> dataListeners = new ArrayList<>();

	public static class HeartRateData {

		// The estimated timestamp of when this event was triggered.
		private final long timestamp;

		// Current heart rate valid for display, computed by sensor. Units: BPM
		private final int computedHeartRate;
		private final boolean computedHeartRateInitial;

		// Heart beat count. Units: beats
		private final long heartBeatCount;
		private final boolean heartBeatCountInitial;

		// Sensor reported time counter value of last distance or speed computation (up to 1/1024s accuracy). Units: s
		private final BigDecimal heartBeatEventTime;
		private final boolean heartBeatEventTimeInitial;

		HeartRateData(long timestamp,
		              int computedHeartRate, boolean computedHeartRateInitial,
		              long heartBeatCount, boolean heartBeatCountInitial,
		              BigDecimal heartBeatEventTime, boolean heartBeatEventTimeInitial) {
			this.timestamp = timestamp;
			this.computedHeartRate = computedHeartRate;
			this.computedHeartRateInitial = computedHeartRateInitial;
			this.heartBeatCount = heartBeatCount;
			this.heartBeatCountInitial = heartBeatCountInitial;
			this.heartBeatEventTime = heartBeatEventTime;
			this.heartBeatEventTimeInitial = heartBeatEventTimeInitial;
		}

		public long getTimestamp() {
			return timestamp;
		}

		public int getComputedHeartRate() {
			return computedHeartRate;
		}

		public boolean isComputedHeartRateInitial() {
			return computedHeartRateInitial;
		}

		public long getHeartBeatCount() {
			return heartBeatCount;
		}

		public boolean isHeartBeatCountInitial() {
			return heartBeatCountInitial;
		}

		public BigDecimal getHeartBeatEventTime() {
			return heartBeatEventTime;
		}

		public boolean isHeartBeatEventTimeInitial() {
			return heartBeatEventTimeInitial;
		}

		@Override
		public String toString() {
			return "HeartRateData {" +
					"timestamp=" + timestamp +
					", computedHeartRate=" + computedHeartRate +
					", computedHeartRateInitial=" + computedHeartRateInitial +
					", heartBeatCount=" + heartBeatCount +
					", heartBeatCountInitial=" + heartBeatCountInitial +
					", heartBeatEventTime=" + heartBeatEventTime +
					", heartBeatEventTimeInitial=" + heartBeatEventTimeInitial +
					'}';
		}
	}

	public interface HeartRateDataListener {

		void onHeartRateData(@NonNull HeartRateData data);
	}

	public AntHeartRateDevice() {
	}

	public AntHeartRateDevice(int antDeviceNumber) {
		super(antDeviceNumber);
	}

	@Nullable
	public HeartRateData getLastHeartRateData() {
		return lastHeartRateData;
	}

	public void addDataListener(@NonNull HeartRateDataListener listener) {
		if (!dataListeners.contains(listener)) {
			dataListeners.add(listener);
		}
	}

	public void removeDataListener(@NonNull HeartRateDataListener listener) {
		dataListeners.remove(listener);
	}

	@Override
	protected PccReleaseHandle<AntPlusHeartRatePcc> requestAccess(@NonNull Context context,
	                                                              int antDeviceNumber) {
		return AntPlusHeartRatePcc.requestAccess(context, antDeviceNumber, 0,
				new PluginAccessResultReceiver(), deviceStateChangeReceiver);
	}

	@Override
	protected PccReleaseHandle<AntPlusHeartRatePcc> requestAccess(@Nullable Activity activity,
	                                                              @NonNull Context context) {
		return AntPlusHeartRatePcc.requestAccess(activity, context,
				new PluginAccessResultReceiver(), deviceStateChangeReceiver);
	}

	protected void subscribeToEvents() {
		pcc.subscribeHeartRateDataEvent((estTimestamp, eventFlags, computedHeartRate, heartBeatCount, heartBeatEventTime, dataState) -> {
			boolean zeroState = AntPlusHeartRatePcc.DataState.ZERO_DETECTED.equals(dataState);
			boolean initialState = AntPlusHeartRatePcc.DataState.INITIAL_VALUE.equals(dataState);

			lastHeartRateData = new HeartRateData(estTimestamp, computedHeartRate, zeroState,
					heartBeatCount, initialState, heartBeatEventTime, initialState);

			for (HeartRateDataListener listener : dataListeners) {
				listener.onHeartRateData(lastHeartRateData);
			}
		});
	}
}
