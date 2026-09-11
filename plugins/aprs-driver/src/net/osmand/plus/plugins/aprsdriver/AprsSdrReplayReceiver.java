package net.osmand.plus.plugins.aprsdriver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.aprs.AprsPlugin;

import org.apache.commons.logging.Log;

import java.io.File;

/**
 * Debug hook: {@code adb shell am broadcast -a net.osmand.aprs.REPLAY_IQ -p net.osmand.dev}
 */
public class AprsSdrReplayReceiver extends BroadcastReceiver {

	public static final String ACTION_REPLAY_IQ = "net.osmand.aprs.REPLAY_IQ";
	public static final String ACTION_TEST_STATIONS = "net.osmand.aprs.TEST_STATIONS";

	private static final Log LOG = PlatformUtil.getLog(AprsSdrReplayReceiver.class);

	@Override
	public void onReceive(@NonNull Context context, @NonNull Intent intent) {
		if (!ACTION_REPLAY_IQ.equals(intent.getAction())
				&& !ACTION_TEST_STATIONS.equals(intent.getAction())) {
			return;
		}
		final PendingResult pendingResult = goAsync();
		OsmandApplication app = (OsmandApplication) context.getApplicationContext();
		AprsPlugin core = PluginsHelper.getPlugin(AprsPlugin.class);
		if (core != null) {
			PluginsHelper.enablePlugin(null, app, core, true);
			core.updateLayers(app, null);
		}
		if (ACTION_TEST_STATIONS.equals(intent.getAction())) {
			injectTestStations(core);
			if (core != null) {
				LOG.info("Injected test APRS stations: " + core.getDataManager().getStations().size());
				app.getOsmandMap().getMapView().refreshMap();
			}
			pendingResult.finish();
			return;
		}
		String path = intent.getStringExtra("path");
		File replay = resolveReplayFile(context, path);
		if (!replay.isFile()) {
			LOG.error("IQ replay file not found: " + replay.getAbsolutePath());
			pendingResult.finish();
			return;
		}
		AprsSdrPlugin sdr = PluginsHelper.getPlugin(AprsSdrPlugin.class);
		if (sdr != null) {
			sdr.startIqReplay(replay);
			LOG.info("Started IQ replay: " + replay.getAbsolutePath());
		}
		pendingResult.finish();
		if (core != null) {
			core.updateLayers(app, null);
			LOG.info("APRS stations in store: " + core.getDataManager().getStations().size());
		}
	}

	@NonNull
	private static File resolveReplayFile(@NonNull Context context, @Nullable String path) {
		if (path != null) {
			File explicit = new File(path);
			if (explicit.isFile()) {
				return explicit;
			}
		}
		File inFiles = new File(context.getFilesDir(), AprsSdrPlugin.DEFAULT_IQ_REPLAY_NAME);
		if (inFiles.isFile()) {
			return inFiles;
		}
		return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
				AprsSdrPlugin.DEFAULT_IQ_REPLAY_NAME);
	}

	private static void injectTestStations(@Nullable AprsPlugin core) {
		if (core == null) {
			return;
		}
		net.osmand.plus.plugins.aprs.AprsPacketParser parser = new net.osmand.plus.plugins.aprs.AprsPacketParser();
		String[][] samples = {
				{"LB1XX-9", "!6041.92N/01043.52E/>045/029/Portable QRV 144.800"},
				{"LA2YY-7", "!6038.66N/01044.24E/>120/037"},
				{"LG3ZZ-1", "!6041.17N/01039.85E/_c045s012t057r000p000P000h52b10139"},
				{"LA4AA-2", "!6040.19N/01041.17E/>330/043"},
				{"LB5BB-3", "!6042.97N/01044.72E/k090/018"},
		};
		for (String[] sample : samples) {
			net.osmand.plus.plugins.aprs.AprsStation station = parser.parseAprsPayload(sample[0], sample[1]);
			if (station != null) {
				core.ingestParsedStation(station);
			}
		}
	}
}
