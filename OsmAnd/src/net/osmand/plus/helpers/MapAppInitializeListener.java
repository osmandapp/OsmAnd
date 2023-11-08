package net.osmand.plus.helpers;

import static net.osmand.plus.AppInitializer.InitEvents.FAVORITES_INITIALIZED;
import static net.osmand.plus.AppInitializer.InitEvents.MAPS_INITIALIZED;
import static net.osmand.plus.AppInitializer.InitEvents.NATIVE_INITIALIZED;
import static net.osmand.plus.AppInitializer.InitEvents.NATIVE_OPEN_GL_INITIALIZED;
import static net.osmand.plus.AppInitializer.InitEvents.ROUTING_CONFIG_INITIALIZED;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.AppInitializer;
import net.osmand.plus.AppInitializer.AppInitializeListener;
import net.osmand.plus.AppInitializer.InitEvents;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.corenative.NativeCoreContext;

public class MapAppInitializeListener implements AppInitializeListener {

	private final OsmandApplication app;
	private final MapActivity activity;

	private boolean renderingViewSetup;

	public MapAppInitializeListener(@NonNull MapActivity activity) {
		this.activity = activity;
		this.app = activity.getMyApplication();
	}

	@Override
	public void onProgress(@NonNull AppInitializer init, @NonNull InitEvents event) {
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
		if (event == ROUTING_CONFIG_INITIALIZED) {
			activity.getRestoreNavigationHelper().checkRestoreRoutingMode();
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