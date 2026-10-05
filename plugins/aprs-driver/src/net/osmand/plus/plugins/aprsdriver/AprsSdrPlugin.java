package net.osmand.plus.plugins.aprsdriver;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.aprs.AprsPlugin;

import org.apache.commons.logging.Log;

import java.io.File;

public class AprsSdrPlugin extends OsmandPlugin {

	private static final Log LOG = PlatformUtil.getLog(AprsSdrPlugin.class);
	public static final String APRS_SDR_ID = "osmand.aprs.sdr";
	public static final String DEFAULT_IQ_REPLAY_NAME = "synthetic_gjoevik_5stations_120s.wav";

	private AprsSdrDriver driver;
	private AprsSdrThread sdrThread;

	public AprsSdrPlugin(@NonNull OsmandApplication app) {
		super(app);
	}

	@Override
	public String getId() {
		return APRS_SDR_ID;
	}

	@Override
	public String getName() {
		return app.getString(R.string.plugin_aprs_sdr_name);
	}

	@Override
	public CharSequence getDescription(boolean linksEnabled) {
		return app.getString(R.string.plugin_aprs_sdr_description);
	}

	@Override
	public int getLogoResourceId() {
		return R.drawable.ic_plugin_aprs;
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		if (enabled) {
			startSdr();
		} else {
			stopSdr();
		}
	}

	private void startSdr() {
		AprsPlugin core = PluginsHelper.getPlugin(AprsPlugin.class);
		if (core == null) {
			LOG.warn("aprs-core not found; aprs-driver disabled");
			return;
		}
		driver = new AprsSdrDriver(app);
		sdrThread = new AprsSdrThread();
		driver.setListener(new AprsSdrDriver.DeviceListener() {
			@Override
			public void onDeviceReady(@NonNull android.hardware.usb.UsbDeviceConnection connection,
			                          @NonNull android.hardware.usb.UsbEndpoint bulkIn) {
				sdrThread.startUsbCapture(connection, bulkIn);
			}

			@Override
			public void onDeviceDenied(@NonNull android.hardware.usb.UsbDevice device) {
				LOG.warn("RTL-SDR USB permission denied");
			}

			@Override
			public void onNoDevice() {
				LOG.info("No RTL-SDR USB device attached");
			}
		});
		driver.requestDeviceAccess();
	}

	private void stopSdr() {
		if (sdrThread != null) {
			sdrThread.stopCapture();
			try {
				sdrThread.awaitFinish(3000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			sdrThread = null;
		}
		if (driver != null) {
			driver.shutdown();
			driver = null;
		}
	}

	public void startIqReplay(@NonNull File wavFile) {
		AprsPlugin core = PluginsHelper.getPlugin(AprsPlugin.class);
		if (core == null) {
			LOG.warn("aprs-core not found; cannot replay IQ");
			return;
		}
		if (!core.isEnabled()) {
			PluginsHelper.enablePlugin(null, app, core, true);
		}
		if (!isEnabled()) {
			PluginsHelper.enablePlugin(null, app, this, true);
		}
		stopSdr();
		driver = new AprsSdrDriver(app);
		driver.setListener(new AprsSdrDriver.DeviceListener() {
			@Override
			public void onDeviceReady(@NonNull android.hardware.usb.UsbDeviceConnection connection,
			                          @NonNull android.hardware.usb.UsbEndpoint bulkIn) {
				sdrThread.startUsbCapture(connection, bulkIn);
			}

			@Override
			public void onDeviceDenied(@NonNull android.hardware.usb.UsbDevice device) {
				LOG.warn("RTL-SDR USB permission denied");
			}

			@Override
			public void onNoDevice() {
				LOG.info("No RTL-SDR USB device attached");
			}
		});
		sdrThread = new AprsSdrThread();
		sdrThread.startFileReplay(wavFile, AprsSdrDsp.IF_OFFSET_HZ);
	}

	@Nullable
	public AprsSdrThread getSdrThread() {
		return sdrThread;
	}

	@Override
	public void disable(@NonNull OsmandApplication app) {
		stopSdr();
		super.disable(app);
	}
}
