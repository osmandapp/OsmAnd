package net.osmand.plus.helpers;

import static net.osmand.plus.AppInitEvents.BROUTER_INITIALIZED;
import static net.osmand.plus.AppInitEvents.FAVORITES_INITIALIZED;
import static net.osmand.plus.AppInitEvents.INDEX_REGION_BOUNDARIES;
import static net.osmand.plus.AppInitEvents.MAPS_INITIALIZED;
import static net.osmand.plus.AppInitEvents.NATIVE_INITIALIZED;
import static net.osmand.plus.AppInitEvents.NATIVE_OPEN_GL_INITIALIZED;
import static net.osmand.plus.AppInitEvents.ROUTING_CONFIG_INITIALIZED;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;
import net.osmand.plus.AppInitEvents;
import net.osmand.plus.AppInitializeListener;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.corenative.NativeCoreContext;

import java.io.IOException;

public class MapAppInitializeListener implements AppInitializeListener {

	private final OsmandApplication app;
	private final MapActivity activity;

	private boolean renderingViewSetup;

	public MapAppInitializeListener(@NonNull MapActivity activity) {
		this.activity = activity;
		this.app = activity.getApp();
	}

	@Override
	public void onProgress(@NonNull AppInitializer init, @NonNull AppInitEvents event) {
		String tn = init.getCurrentInitTaskName();
		if (tn != null) {
			((TextView) activity.findViewById(R.id.ProgressMessage)).setText(tn);
		}
		boolean openGlInitialized = event == NATIVE_OPEN_GL_INITIALIZED && NativeCoreContext.isInit();
		if ((openGlInitialized || event == NATIVE_INITIALIZED) && !renderingViewSetup) {
			app.getOsmandMap().setupRenderingView();
			renderingViewSetup = true;
		}
		if (openGlInitialized) {
			app.getOsmandMap().getMapLayers().updateLayers(activity);
		}
		if (event == MAPS_INITIALIZED) {
			// TODO investigate if this false cause any issues!
			activity.getMapView().refreshMap(false);
			activity.getDashboard().updateLocation(true, true, false);
			app.getTargetPointsHelper().lookupAddressAll();
		}
		if (event == FAVORITES_INITIALIZED) {
			activity.refreshMap();
		}
		if (event == INDEX_REGION_BOUNDARIES) {
			if (app.getAppInitializer().isRoutingConfigInitialized()) {
				activity.getRestoreNavigationHelper().checkRestoreRoutingMode();
			}
		}
		if (event == ROUTING_CONFIG_INITIALIZED) {
			boolean hasRegions = false;
			try {
				hasRegions = PlatformUtil.getOsmandRegions() != null;
			} catch (IOException ignore) {
			}
			if (hasRegions) {
				activity.getRestoreNavigationHelper().checkRestoreRoutingMode();
			}
		}
		if (event == BROUTER_INITIALIZED) {
			activity.getMapActions().updateDrawerMenu();
		}
	}

	@Override
	public void onFinish(@NonNull AppInitializer init) {
		if (!renderingViewSetup) {
			app.getOsmandMap().setupRenderingView();
		}
		activity.getMapView().refreshMap(false);
		activity.getDashboard().updateLocation(true, true, false);
		activity.findViewById(R.id.init_progress).setVisibility(View.GONE);
		activity.findViewById(R.id.drawer_layout).invalidate();
	}
}