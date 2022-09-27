package net.osmand.plus.plugins.antplus.antdevices;

import static com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult.SUCCESS;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc.IDeviceStateChangeReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc.IPluginAccessResultReceiver;
import com.dsi.ant.plugins.antplus.pccbase.PccReleaseHandle;

import java.util.ArrayList;
import java.util.List;

public abstract class AntCommonDevice<T extends AntPluginPcc> {

	protected T pcc;
	protected PccReleaseHandle<T> releaseHandle;

	protected int antDeviceNumber = -1;
	protected AntDeviceConnectionState state = AntDeviceConnectionState.DISCONNECTED;

	private final List<AntDeviceListener> listeners = new ArrayList<>();

	public interface AntDeviceListener {

		@MainThread
		void onAntDeviceConnect(@NonNull AntCommonDevice<?> device, @NonNull AntDeviceConnectionResult result,
		                        int antDeviceNumber, @Nullable String error);

		@MainThread
		void onAntDeviceDisconnect(@NonNull AntCommonDevice<?> device);
	}

	//Receives state changes and shows it on the status display line
	protected final IDeviceStateChangeReceiver deviceStateChangeReceiver =
			newDeviceState -> {
				if (newDeviceState == DeviceState.DEAD || newDeviceState == DeviceState.CLOSED) {
					state = AntDeviceConnectionState.DISCONNECTED;
					for (AntDeviceListener listener : listeners) {
						listener.onAntDeviceDisconnect(this);
					}
				}
			};

	public AntCommonDevice() {
	}

	public AntCommonDevice(int antDeviceNumber) {
		this.antDeviceNumber = antDeviceNumber;
	}

	public int getAntDeviceNumber() {
		return antDeviceNumber;
	}

	public boolean hasAntDeviceNumber() {
		return antDeviceNumber != -1;
	}

	public boolean isConnected() {
		return state == AntDeviceConnectionState.CONNECTED;
	}

	public boolean isConnecting() {
		return state == AntDeviceConnectionState.CONNECTING;
	}

	public boolean isDisconnected() {
		return state == AntDeviceConnectionState.DISCONNECTED;
	}

	public void addListener(@NonNull AntDeviceListener listener) {
		if (!listeners.contains(listener)) {
			listeners.add(listener);
		}
	}

	public void removeListener(@NonNull AntDeviceListener listener) {
		listeners.remove(listener);
	}

	public String getDeviceName() {
		return pcc != null ? pcc.getDeviceName() : this.getClass().getSimpleName();
	}

	/**
	 * Resets the PCC connection to request access again.
	 */
	public void resetConnection(@Nullable Activity activity, @NonNull Context context) {
		//Release the old access if it exists
		closeConnection();

		requestAccessToPcc(activity, context);
	}

	public void closeConnection() {
		if (releaseHandle != null) {
			state = AntDeviceConnectionState.DISCONNECTED;
			releaseHandle.close();
			releaseHandle = null;
		}
	}

	protected void requestAccessToPcc(@Nullable Activity activity, @NonNull Context context) {
		// starts the plugins UI search
		if (antDeviceNumber != -1) {
			state = AntDeviceConnectionState.CONNECTING;
			releaseHandle = requestAccess(context, antDeviceNumber);
		} else {
			state = AntDeviceConnectionState.CONNECTING;
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
		AntCommonDevice<?> that = (AntCommonDevice<?>) o;
		return antDeviceNumber == that.antDeviceNumber;
	}

	@Override
	public int hashCode() {
		return getClass().getSimpleName().hashCode();
	}

	protected abstract PccReleaseHandle<T> requestAccess(@NonNull Context context, int antDeviceNumber);

	protected abstract PccReleaseHandle<T> requestAccess(@Nullable Activity activity, @NonNull Context context);

	protected abstract void subscribeToEvents();

	protected class PluginAccessResultReceiver implements IPluginAccessResultReceiver<T> {

		public PluginAccessResultReceiver() {
		}

		@Override
		public void onResultReceived(T result, RequestAccessResult resultCode,
		                             DeviceState initialDeviceState) {
			AntDeviceConnectionResult connectionResult;
			int antDeviceNumber = -1;
			String error = null;
			state = resultCode == SUCCESS
					? AntDeviceConnectionState.CONNECTED
					: AntDeviceConnectionState.DISCONNECTED;
			switch (resultCode) {
				case SUCCESS:
					pcc = result;
					connectionResult = AntDeviceConnectionResult.SUCCESS;
					antDeviceNumber = result.getAntDeviceNumber();
					AntCommonDevice.this.antDeviceNumber = antDeviceNumber;
					subscribeToEvents();
					break;
				case CHANNEL_NOT_AVAILABLE:
					connectionResult = AntDeviceConnectionResult.CHANNEL_NOT_AVAILABLE;
					error = "Channel Not Available";
					break;
				case ADAPTER_NOT_DETECTED:
					connectionResult = AntDeviceConnectionResult.ADAPTER_NOT_DETECTED;
					error = "ANT Adapter Not Available. Built-in ANT hardware or external adapter required.";
					break;
				case BAD_PARAMS:
					//Note: Since we compose all the params ourself, we should never see this result
					connectionResult = AntDeviceConnectionResult.BAD_PARAMS;
					error = "Bad request parameters.";
					break;
				case OTHER_FAILURE:
					connectionResult = AntDeviceConnectionResult.OTHER_FAILURE;
					error = "RequestAccess failed. See logcat for details.";
					break;
				case DEPENDENCY_NOT_INSTALLED:
					connectionResult = AntDeviceConnectionResult.DEPENDENCY_NOT_INSTALLED;
					error = "";
					break;
				case SEARCH_TIMEOUT:
					connectionResult = AntDeviceConnectionResult.SEARCH_TIMEOUT;
					break;
				case USER_CANCELLED:
					connectionResult = AntDeviceConnectionResult.USER_CANCELLED;
					break;
				case UNRECOGNIZED:
					connectionResult = AntDeviceConnectionResult.UNRECOGNIZED;
					error = "Failed: UNRECOGNIZED. PluginLib Upgrade Required?";
					break;
				default:
					connectionResult = AntDeviceConnectionResult.UNRECOGNIZED;
					error = "Unrecognized result: " + resultCode;
					break;
			}
			for (AntDeviceListener listener : listeners) {
				listener.onAntDeviceConnect(AntCommonDevice.this, connectionResult, antDeviceNumber, error);
			}
		}
	}
}
