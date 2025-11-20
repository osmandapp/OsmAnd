package net.osmand.plus;

import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.srtm.SRTMPlugin;
import net.osmand.plus.plugins.weather.WeatherPlugin;
import net.osmand.plus.settings.backend.OsmandSettings;

import org.apache.commons.logging.Log;

public class OsmAndDiagnosticThread extends Thread {

	private static final Log LOG = PlatformUtil.getLog(OsmAndDiagnosticThread.class);

	private static final long POLL_INTERVAL_MS = 500L;
	private final OsmandApplication app;
	private long lastDiagnosticThreadFailedTime = 0;
	private long lastCallChainBuiltTime = 0;

	@FunctionalInterface
	private interface ChainedAction {
		void run() throws InterruptedException;
	}

	public OsmAndDiagnosticThread(@NonNull OsmandApplication app) {
		super("OsmAndDiagnosticThread");
		this.app = app;
	}

	@Override
	public void run() {
		LOG.info("Diagnostic thread started. Building call chain...");
		while (!isInterrupted()) {
			long time = System.currentTimeMillis();
			try {
				OsmandSettings settings = app.getSettings();
				ChainedAction callChain = this::awaitNextCheck;
				if (settings == null) {
					LOG.info("Call chain empty. Skipping...");
					callChain.run();
					continue;
				}
				if (app.useOpenGlRenderer()) {
					if (app.getOsmandMap().getMapView().is3DMode()) {
						final ChainedAction previousChain = callChain;
						callChain = () -> feature_3DMode_ON(previousChain);
					}
					WeatherPlugin weatherPlugin = PluginsHelper.getActivePlugin(WeatherPlugin.class);
					if (weatherPlugin != null && weatherPlugin.isAnyDataVisible()) {
						final ChainedAction previousChain = callChain;
						callChain = () -> feature_WeatherMaps_ON(previousChain);
					}
					if (settings.SPHERICAL_MAP.get()) {
						final ChainedAction previousChain = callChain;
						callChain = () -> feature_SphericalMap_ON(previousChain);
					}
					SRTMPlugin srtmPlugin = PluginsHelper.getActivePlugin(SRTMPlugin.class);
					if (srtmPlugin != null && srtmPlugin.is3DMapsEnabled()) {
						final ChainedAction previousChain = callChain;
						callChain = () -> feature_Relief3D_ON(previousChain);
					}
					final ChainedAction previousChain = callChain;
					callChain = () -> feature_RenderingV2_ON(previousChain);
				} else {
					final ChainedAction previousChain = callChain;
					callChain = () -> feature_RenderingV1_ON(previousChain);
				}
				if (time - lastCallChainBuiltTime > 1000L * 60L) {
					LOG.info("Call chain built. Running...");
					lastCallChainBuiltTime = time;
				}
				callChain.run();
			} catch (InterruptedException e) {
				LOG.info("Diagnostic thread interrupted.");
				break;
			} catch (Exception e) {
				if (time - lastDiagnosticThreadFailedTime > 1000L * 60L) {
					LOG.error("Diagnostic thread failed.", e);
					lastDiagnosticThreadFailedTime = time;
				}
			}
		}
	}

	private void feature_RenderingV1_ON(ChainedAction nextAction) throws InterruptedException {
		nextAction.run();
	}

	private void feature_RenderingV2_ON(ChainedAction nextAction) throws InterruptedException {
		nextAction.run();
	}

	private void feature_Relief3D_ON(ChainedAction nextAction) throws InterruptedException {
		nextAction.run();
	}

	private void feature_SphericalMap_ON(ChainedAction nextAction) throws InterruptedException {
		nextAction.run();
	}

	private void feature_WeatherMaps_ON(ChainedAction nextAction) throws InterruptedException {
		nextAction.run();
	}

	private void feature_3DMode_ON(ChainedAction nextAction) throws InterruptedException {
		nextAction.run();
	}

	private void awaitNextCheck() throws InterruptedException {
		Thread.sleep(POLL_INTERVAL_MS);
	}
}
