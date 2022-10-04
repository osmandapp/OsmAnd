package net.osmand.plus.plugins.antplus.devices;

import androidx.annotation.NonNull;

import net.osmand.StateChangedListener;
import net.osmand.plus.plugins.antplus.antdevices.AntCommonDevice;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.views.mapwidgets.WidgetType;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public abstract class CommonDevice<T extends AntCommonDevice<?>> {

	private final T antDevice;
	private final IPreferenceFactory preferenceFactory;
	private CommonPreference<Boolean> deviceEnabledPref;
	private CommonPreference<Integer> deviceNumberPref;
	private CommonPreference<Boolean> writeGpxPref;

	private final List<DeviceListener> listeners = new ArrayList<>();

	private final StateChangedListener<Boolean> deviceEnabledPrefListener = change -> {
		for (DeviceListener listener : listeners) {
			if (change) {
				listener.onDeviceConnected(this);
			} else {
				listener.onDeviceDisconnected(this);
			}
		}
	};

	public interface DeviceListener {
		void onDeviceConnected(@NonNull CommonDevice<?> device);

		void onDeviceDisconnected(@NonNull CommonDevice<?> device);
	}

	public interface IPreferenceFactory {
		CommonPreference<Boolean> registerBooleanPref(@NonNull String prefId, boolean defValue);

		CommonPreference<Integer> registerIntPref(@NonNull String prefId, int defValue);
	}

	public CommonDevice(@NonNull IPreferenceFactory preferenceFactory) {
		this.preferenceFactory = preferenceFactory;
		this.antDevice = createAntDevice();
		getDeviceEnabledPref().addListener(deviceEnabledPrefListener);
	}

	public T getAntDevice() {
		return antDevice;
	}

	protected CommonPreference<Boolean> registerBooleanPref(@NonNull String prefId, boolean defValue) {
		return preferenceFactory.registerBooleanPref(prefId, defValue);
	}

	protected CommonPreference<Integer> registerIntPref(@NonNull String prefId, int defValue) {
		return preferenceFactory.registerIntPref(prefId, defValue);
	}

	public void addListener(@NonNull DeviceListener listener) {
		if (!listeners.contains(listener)) {
			listeners.add(listener);
		}
	}

	public void removeListener(@NonNull DeviceListener listener) {
		listeners.remove(listener);
	}

	public boolean isEnabled() {
		return getDeviceEnabledPref().get();
	}

	public void setEnabled(boolean enabled) {
		getDeviceEnabledPref().set(enabled);
	}

	public int getDeviceNumber() {
		return getDeviceNumberPref().get();
	}

	public void setDeviceNumber(int deviceNumber) {
		getDeviceNumberPref().set(deviceNumber);
	}

	public boolean shouldWriteGpx() {
		return getWriteGpxPref().get();
	}

	public void setWriteGpx(boolean write) {
		getWriteGpxPref().set(write);
	}

	@NonNull
	public abstract T createAntDevice();

	@NonNull
	protected abstract String getDeviceEnabledPrefId();

	@NonNull
	protected abstract String getDeviceNumberPrefId();

	@NonNull
	protected abstract String getWriteGpxPrefId();

	@NonNull
	public abstract WidgetType getDeviceWidgetType();

	@NonNull
	public abstract void writeDataToJson(@NonNull JSONObject json) throws JSONException;

	@NonNull
	public CommonPreference<Boolean> getDeviceEnabledPref() {
		if (deviceEnabledPref == null) {
			deviceEnabledPref = registerBooleanPref(getDeviceEnabledPrefId(), true);
		}
		return deviceEnabledPref;
	}

	@NonNull
	public CommonPreference<Integer> getDeviceNumberPref() {
		if (deviceNumberPref == null) {
			deviceNumberPref = registerIntPref(getDeviceNumberPrefId(), -1);
		}
		return deviceNumberPref;
	}

	@NonNull
	public CommonPreference<Boolean> getWriteGpxPref() {
		if (writeGpxPref == null) {
			writeGpxPref = registerBooleanPref(getWriteGpxPrefId(), false);
		}
		return writeGpxPref;
	}
}
