package net.osmand.plus;

import androidx.annotation.NonNull;

import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.srtm.SRTMPlugin;
import net.osmand.plus.plugins.weather.WeatherPlugin;
import net.osmand.plus.settings.backend.OsmandSettings;

public class OsmAndDiagnosticThread extends Thread {

	private static final long POLL_INTERVAL_MS = 500L;
	private final OsmandApplication app;
	private final OsmandSettings settings;

	@FunctionalInterface
	private interface ChainedAction {
		void run() throws InterruptedException;
	}

	public OsmAndDiagnosticThread(@NonNull OsmandApplication app) {
		super("OsmAndDiagnosticThread");
		this.app = app;
		this.settings = app.getSettings();
	}

	@Override
	public void run() {
		while (!isInterrupted()) {
			try {
				ChainedAction callChain = this::awaitNextCheck;
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
				callChain.run();
			} catch (InterruptedException e) {
				break;
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
