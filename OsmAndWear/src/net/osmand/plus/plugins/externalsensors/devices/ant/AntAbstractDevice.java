package net.osmand.plus.plugins.externalsensors.devices.ant;

import static com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult.SUCCESS;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc.IDeviceStateChangeReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc.IPluginAccessResultReceiver;
import com.dsi.ant.plugins.antplus.pccbase.PccReleaseHandle;

import net.osmand.PlatformUtil;
import net.osmand.plus.plugins.externalsensors.devices.AbstractDevice;
import net.osmand.plus.plugins.externalsensors.devices.DeviceConnectionResult;
import net.osmand.plus.plugins.externalsensors.devices.DeviceConnectionState;
import net.osmand.plus.plugins.externalsensors.devices.sensors.ant.AntAbstractSensor;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

public abstract class AntAbstractDevice<T extends AntPluginPcc> extends AbstractDevice<AntAbstractSensor<T>> {

	protected static final Log LOG = PlatformUtil.getLog(AntAbstractDevice.class);
	protected static final String SEARCHING_ID_PREFIX = "Searching_";

	protected String foundDeviceId;
	protected T pcc;
	protected PccReleaseHandle<T> releaseHandle;

	protected boolean combined;
	protected int deviceNumber;

	//Receives state changes and shows it on the status display line
	protected final IDeviceStateChangeReceiver deviceStateChangeReceiver =
			newDeviceState -> {
				if (newDeviceState == DeviceState.DEAD || newDeviceState == DeviceState.CLOSED) {
					setCurrentState(DeviceConnectionState.DISCONNECTED);
					for (DeviceListener listener : listeners) {
						listener.onDeviceDisconnect(this);
					}
				}
			};

	public AntAbstractDevice(@NonNull String deviceId) {
		super(deviceId);
		if (!isSearching()) {
			int deviceNumber;
			try {
				deviceNumber = Integer.parseInt(deviceId);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Failed to create Ant+ device. DeviceId '" + deviceId + "' is not a number");
			}
			this.deviceNumber = deviceNumber;
		}
	}

	public int getDeviceNumber() {
		return deviceNumber;
	}

	public boolean hasDeviceNumber() {
		return deviceNumber != -1;
	}

	public boolean isCombined() {
		return combined;
	}

	public boolean isSearching() {
		return deviceId.startsWith(SEARCHING_ID_PREFIX) && Algorithms.isEmpty(foundDeviceId);
	}

	public boolean isSearchDone() {
		return !Algorithms.isEmpty(foundDeviceId);
	}

	@NonNull
	@Override
	public String getDeviceId() {
		return !Algorithms.isEmpty(foundDeviceId) ? foundDeviceId : super.getDeviceId();
	}

	public T getPcc() {
		return pcc;
	}

	/**
	 * Resets the PCC connection to request access again.
	 */
	public void resetConnection(@Nullable Activity activity, @NonNull Context context) {
		//Release the old access if it exists
		disconnect();

		requestAccessToPcc(activity, context);
	}

	@Override
	public boolean disconnect() {
		if (releaseHandle != null) {
			setCurrentState(DeviceConnectionState.DISCONNECTED);
			releaseHandle.close();
			releaseHandle = null;
			return true;
		}
		return false;
	}

	protected void requestAccessToPcc(@Nullable Activity activity, @NonNull Context context) {
		// starts the plugins UI search
		if (deviceNumber != -1) {
			setCurrentState(DeviceConnectionState.CONNECTING);
			releaseHandle = requestAccess(context, deviceNumber);
		} else {
			setCurrentState(DeviceConnectionState.CONNECTING);
			releaseHandle = requestAccess(activity, context);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		AntAbstractDevice<?> that = (AntAbstractDevice<?>) o;
		return deviceNumber == that.deviceNumber;
	}

	@Override
	public int hashCode() {
		return getClass().getSimpleName().hashCode();
	}

	protected abstract PccReleaseHandle<T> requestAccess(@NonNull Context context, int deviceNumber);

	protected abstract PccReleaseHandle<T> requestAccess(@Nullable Activity activity, @NonNull Context context);

	protected void subscribeToEvents() {
		for (AntAbstractSensor<T> sensor : sensors) {
			sensor.subscribeToEvents();
		}
	}

	@Override
	public boolean connect(@NonNull Context context, @Nullable Activity activity) {
		if (isDisconnected()) {
			if (hasDeviceNumber()) {
				LOG.debug(this + " connecting");
				resetConnection(null, context);
			} else if (activity != null) {
				LOG.debug(this + " connecting");
				resetConnection(activity, context);
			}
			for (DeviceListener listener : listeners) {
				listener.onDeviceConnecting(this);
			}
		}
		return true;
	}

	protected class PluginAccessResultReceiver implements IPluginAccessResultReceiver<T> {

		public PluginAccessResultReceiver() {
		}

		@Override
		public void onResultReceived(T result, RequestAccessResult resultCode,
		                             DeviceState initialDeviceState) {
			DeviceConnectionResult connectionResult;
			int deviceNumber;
			String error = null;
			setCurrentState(resultCode == SUCCESS
					? DeviceConnectionState.CONNECTED
					: DeviceConnectionState.DISCONNECTED);
			switch (resultCode) {
				case SUCCESS:
					pcc = result;
					if (Algorithms.isEmpty(deviceName)) {
						setDeviceName(pcc.getDeviceName());
					}
					connectionResult = DeviceConnectionResult.SUCCESS;
					deviceNumber = result.getAntDeviceNumber();
					AntAbstractDevice.this.deviceNumber = deviceNumber;
					if (isSearching()) {
						foundDeviceId = String.valueOf(deviceNumber);
					}
					subscribeToEvents();
					break;
				case CHANNEL_NOT_AVAILABLE:
					connectionResult = DeviceConnectionResult.CHANNEL_NOT_AVAILABLE;
					error = "Channel Not Available";
					break;
				case ADAPTER_NOT_DETECTED:
					connectionResult = DeviceConnectionResult.ADAPTER_NOT_DETECTED;
					error = "ANT Adapter Not Available. Built-in ANT hardware or external adapter required.";
					break;
				case BAD_PARAMS:
					//Note: Since we compose all the params ourself, we should never see this result
					connectionResult = DeviceConnectionResult.BAD_PARAMS;
					error = "Bad request parameters.";
					break;
				case OTHER_FAILURE:
					connectionResult = DeviceConnectionResult.OTHER_FAILURE;
					error = "RequestAccess failed. See logcat for details.";
					break;
				case DEPENDENCY_NOT_INSTALLED:
					connectionResult = DeviceConnectionResult.DEPENDENCY_NOT_INSTALLED;
					error = "";
					break;
				case SEARCH_TIMEOUT:
					connectionResult = DeviceConnectionResult.SEARCH_TIMEOUT;
					break;
				case USER_CANCELLED:
					connectionResult = DeviceConnectionResult.USER_CANCELLED;
					break;
				case UNRECOGNIZED:
					connectionResult = DeviceConnectionResult.UNRECOGNIZED;
					error = "Failed: UNRECOGNIZED. PluginLib Upgrade Required?";
					break;
				default:
					connectionResult = DeviceConnectionResult.UNRECOGNIZED;
					error = "Unrecognized result: " + resultCode;
					break;
			}
			if (!combined) {
				for (DeviceListener listener : AntAbstractDevice.this.listeners) {
					listener.onDeviceConnect(AntAbstractDevice.this, connectionResult, error);
				}
			}
		}
	}
}
