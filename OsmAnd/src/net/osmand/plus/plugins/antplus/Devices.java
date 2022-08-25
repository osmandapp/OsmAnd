package net.osmand.plus.plugins.antplus;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.antplus.devices.CommonDevice;
import net.osmand.plus.plugins.antplus.devices.CommonDevice.DeviceListener;
import net.osmand.plus.plugins.antplus.antdevices.AntCommonDevice;
import net.osmand.plus.plugins.antplus.antdevices.AntCommonDevice.AntDeviceListener;
import net.osmand.plus.plugins.antplus.antdevices.AntDeviceConnectionResult;
import net.osmand.plus.plugins.antplus.devices.HeartRateDevice;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

public class Devices implements AntDeviceListener, DeviceListener {

	private static final Log LOG = PlatformUtil.getLog(Devices.class);

	private final OsmandApplication app;
	private MapActivity mapActivity;
	private boolean installPluginAsked = false;

	private final List<CommonDevice<?>> devices = new ArrayList<>();

	Devices(@NonNull OsmandApplication app, @NonNull AntPlusPlugin antPlugin) {
		this.app = app;

		devices.add(new HeartRateDevice(antPlugin));

		for (CommonDevice<?> device : devices) {
			device.addListener(this);
		}
	}

	void setMapActivity(MapActivity mapActivity) {
		this.mapActivity = mapActivity;
	}

	void connectAntDevices(@Nullable Activity activity) {
		for (CommonDevice<?> device : devices) {
			if (device.isEnabled()) {
				AntCommonDevice<?> antDevice = device.getAntDevice();
				antDevice.addListener(this);
				connectAntDevice(antDevice, activity);
			}
		}
	}

	void disconnectAntDevices() {
		for (CommonDevice<?> device : devices) {
			AntCommonDevice<?> antDevice = device.getAntDevice();
			antDevice.closeConnection();
			antDevice.removeListener(this);
		}
	}

	void updateAntDevices(@Nullable Activity activity) {
		for (CommonDevice<?> device : devices) {
			AntCommonDevice<?> antDevice = device.getAntDevice();
			if (device.isEnabled() && antDevice.isDisconnected()) {
				antDevice.addListener(this);
				connectAntDevice(antDevice, activity);
			} else if (!device.isEnabled() && antDevice.isConnected()) {
				antDevice.closeConnection();
				antDevice.removeListener(this);
			}
		}
	}

	@Nullable
	<T extends CommonDevice<?>> T getDevice(@NonNull Class<T> clz) {
		for (CommonDevice<?> device : devices) {
			if (clz.isInstance(device)) {
				return (T) device;
			}
		}
		return null;
	}

	@Nullable
	<T extends AntCommonDevice<?>> T getAntDevice(@NonNull Class<T> clz) {
		for (CommonDevice<?> device : devices) {
			AntCommonDevice<?> antDevice = device.getAntDevice();
			if (clz.isInstance(antDevice)) {
				return (T) antDevice;
			}
		}
		return null;
	}

	@Nullable
	CommonDevice<?> getDeviceByAntDevice(@NonNull AntCommonDevice<?> antDevice) {
		for (CommonDevice<?> device : devices) {
			if (device.getAntDevice().equals(antDevice)) {
				return device;
			}
		}
		return null;
	}

	private void saveAntDeviceNumber(@NonNull AntCommonDevice<?> antDevice, int antDeviceNumber) {
		CommonDevice<?> device = getDeviceByAntDevice(antDevice);
		if (device != null) {
			device.setDeviceNumber(antDeviceNumber);
		}
	}

	private void connectAntDevice(@NonNull AntCommonDevice<?> antDevice, @Nullable Activity activity) {
		if (antDevice.isDisconnected()) {
			if (antDevice.hasAntDeviceNumber()) {
				LOG.debug("ANT+ " + antDevice.getDeviceName() + " device connecting with device number " + antDevice.getAntDeviceNumber());
				antDevice.resetConnection(null, app);
			} else if (activity != null) {
				LOG.debug("ANT+ " + antDevice.getDeviceName() + " device connecting without device number");
				antDevice.resetConnection(activity, app);
			}
		}
	}

	void askPluginInstall() {
		if (mapActivity == null || installPluginAsked) {
			return;
		}
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mapActivity);
		dialogBuilder.setTitle("Missing Dependency");
		dialogBuilder.setMessage("The required service\n\"" + AntPlusHeartRatePcc.getMissingDependencyName() + "\"\n was not found. " +
				"You need to install the ANT+ Plugins service or you may need to update your existing version if you already have it. " +
				"Do you want to launch the Play Store to get it?");
		dialogBuilder.setCancelable(true);
		dialogBuilder.setPositiveButton("Go to Store", (dialog, which) -> {
			Intent startStore = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id="
					+ AntPluginPcc.getMissingDependencyPackageName()));
			startStore.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			mapActivity.startActivity(startStore);
		});
		dialogBuilder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

		final AlertDialog waitDialog = dialogBuilder.create();
		waitDialog.show();
		installPluginAsked = true;
	}

	@Override
	public void onAntDeviceConnect(@NonNull AntCommonDevice<?> antDevice, @NonNull AntDeviceConnectionResult result, int antDeviceNumber, @Nullable String error) {
		if (!Algorithms.isEmpty(error)) {
			LOG.error("ANT+ " + antDevice.getDeviceName() + " device connection error: " + error);
		}
		CommonDevice<?> device = getDeviceByAntDevice(antDevice);
		switch (result) {
			case SUCCESS:
				LOG.debug("ANT+ " + antDevice.getDeviceName() + " device connected. Device number = " + antDeviceNumber);
				saveAntDeviceNumber(antDevice, antDeviceNumber);
				if (device != null && !device.isEnabled()) {
					updateAntDevices(mapActivity);
				}
				break;
			case DEPENDENCY_NOT_INSTALLED:
				LOG.debug("ANT+ plugin is not installed. Ask plugin install.");
				askPluginInstall();
				break;
			case SEARCH_TIMEOUT:
				if (device != null && !device.isEnabled()) {
					updateAntDevices(mapActivity);
				} else {
					LOG.debug("ANT+ Reconnect " + antDevice.getDeviceName() + " after timeout");
					connectAntDevice(antDevice, mapActivity);
				}
				break;
			default:
				break;
		}
	}

	@Override
	public void onAntDeviceDisconnect(@NonNull AntCommonDevice<?> antDevice) {
		LOG.debug("ANT+ " + antDevice.getDeviceName() + " (" + antDevice.getAntDeviceNumber() + ") disconnected");
	}

	@Override
	public void onDeviceConnected(@NonNull CommonDevice<?> device) {
		app.runInUIThread(() -> updateAntDevices(mapActivity));
	}

	@Override
	public void onDeviceDisconnected(@NonNull CommonDevice<?> device) {
		app.runInUIThread(() -> updateAntDevices(mapActivity));
	}
}
