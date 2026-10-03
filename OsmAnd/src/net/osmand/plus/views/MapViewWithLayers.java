package net.osmand.plus.views;

import static net.osmand.plus.views.OsmandMapTileView.MIN_ALLOWED_ELEVATION_ANGLE;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewStub;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.core.android.AtlasMapRendererView;
import net.osmand.core.android.MapRendererContext;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.ZoomLevel;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.auto.NavigationSession;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.inapp.InAppPurchaseUtils;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.OsmandMap.RenderingViewSetupListener;
import net.osmand.plus.views.corenative.NativeCoreContext;

public class MapViewWithLayers extends FrameLayout {

	public static final int SYMBOLS_UPDATE_INTERVAL = 2000;

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final OsmandMapTileView mapView;

	private RenderingViewSetupListener renderingViewSetupListener;
	private AtlasMapRendererView atlasMapRendererView;
	@Nullable
	private MapRendererView retainedMapRendererView;

	/**
	 * Holds the initialized map renderer of a map view whose activity is being recreated.
	 * Android hands it over to the replacement activity only when that activity is really
	 * created again, so no configuration change has to be guessed
	 */
	public static class RetainedRenderer {
		@Nullable
		private MapRendererView mapRendererView;
	}

	public MapViewWithLayers(@NonNull Context context) {
		this(context, null);
	}

	public MapViewWithLayers(@NonNull Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public MapViewWithLayers(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public MapViewWithLayers(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);

		app = getMyApplication();
		settings = app.getSettings();

		OsmandMap osmandMap = app.getOsmandMap();
		osmandMap.addRenderingViewSetupListener(getRenderingViewSetupListener());

		mapView = osmandMap.getMapView();
		mapView.setupTouchDetectors(getContext());

		boolean nightMode = app.getDaynightHelper().isNightMode(ThemeUsageContext.MAP);
		inflate(UiUtilities.getThemedContext(context, nightMode), R.layout.map_view_with_layers, this);
	}

	public void setupRenderingView() {
		NavigationSession carNavigationSession = app.getCarNavigationSession();
		View androidAutoPlaceholder = findViewById(R.id.AndroidAutoPlaceholder);
		boolean useAndroidAuto = carNavigationSession != null && carNavigationSession.hasStarted()
				&& InAppPurchaseUtils.isAndroidAutoAvailable(app);

		OsmAndMapSurfaceView surfaceView = findViewById(R.id.MapView);
		OsmAndMapLayersView mapLayersView = findViewById(R.id.MapLayersView);

		boolean useOpenglRender = app.useOpenGlRenderer();
		surfaceView.setMapView(!useOpenglRender && !useAndroidAuto ? mapView : null);
		if (useOpenglRender && !useAndroidAuto) {
			mapView.setMinAllowedElevationAngle(MIN_ALLOWED_ELEVATION_ANGLE);
			setupAtlasMapRendererView();
			mapLayersView.setMapView(mapView);
			app.getMapViewTrackingUtilities().setMapView(mapView);
			mapView.setMapRenderer(atlasMapRendererView, false);
		} else if (!useAndroidAuto) {
			mapView.setMapRenderer(null, false);
			resetMapRendererView();
		}
		// Nothing took the renderer of the previous activity's view over, so it has to be released
		releaseRetainedRenderer();

		AndroidUiHelper.updateVisibility(surfaceView, !useAndroidAuto && !useOpenglRender);
		AndroidUiHelper.updateVisibility(mapLayersView, !useAndroidAuto && useOpenglRender);
		AndroidUiHelper.updateVisibility(atlasMapRendererView, !useAndroidAuto && useOpenglRender);
		AndroidUiHelper.updateVisibility(androidAutoPlaceholder, useAndroidAuto);
	}

	/**
	 * Takes the map renderer that the previous activity's map view kept for this one
	 */
	public void setRetainedRenderer(@NonNull RetainedRenderer retained) {
		retainedMapRendererView = retained.mapRendererView;
		retained.mapRendererView = null;
	}

	private void releaseRetainedRenderer() {
		MapRendererView mapRendererView = retainedMapRendererView;
		if (mapRendererView != null) {
			retainedMapRendererView = null;
			mapRendererView.stopRenderer();
		}
	}

	private void resetMapRendererView() {
		MapRendererContext mapRendererContext = NativeCoreContext.getMapRendererContext();
		if (mapRendererContext != null && atlasMapRendererView != null)
			mapRendererContext.releaseMapRendererView(atlasMapRendererView);
	}

	private void setupAtlasMapRendererView() {
		ViewStub stub = findViewById(R.id.atlasMapRendererViewStub);
		MapRendererView mapRendererView = null;
		MapRendererContext mapRendererContext = NativeCoreContext.getMapRendererContext();
		if (mapRendererContext != null) {
			if (atlasMapRendererView != null && mapRendererContext.getMapRendererView() == atlasMapRendererView)
				return;
			if (mapView.getMapRenderer() != null)
				mapView.setMapRenderer(null, true);
			if (mapRendererContext.getMapRendererView() != null) {
				mapRendererView = mapRendererContext.getMapRendererView();
				mapRendererContext.setMapRendererView(null);
			}
		}
		DisplayMetrics metrics = new DisplayMetrics();
		AndroidUtils.getDisplay(getContext()).getMetrics(metrics);
		NativeCoreContext.setMapRendererContext(app, metrics.density);
		mapRendererContext = NativeCoreContext.getMapRendererContext();
		if (mapRendererContext != null) {
			if (atlasMapRendererView == null) {
				atlasMapRendererView = (AtlasMapRendererView) stub.inflate();
			} else {
				atlasMapRendererView.handleOnCreate(null);
			}
			if (mapRendererView == null) {
				// Renderer of the previous activity's map view, suspended by its onDestroy().
				// It's taken right before the handover, so that it's released by
				// setupRenderingView() when there's no renderer context to hand it to
				mapRendererView = retainedMapRendererView;
				retainedMapRendererView = null;
			}
			// Get MSAA setting from development plugin
			boolean enableMSAA = settings.ENABLE_MSAA.get();
			mapRendererContext.presetMapRendererOptions(atlasMapRendererView, enableMSAA);
			atlasMapRendererView.setupRenderer(getContext(), 0, 0, mapRendererView);
			atlasMapRendererView.setMinZoomLevel(ZoomLevel.swigToEnum(mapView.getMinZoom()));
			atlasMapRendererView.setMaxZoomLevel(ZoomLevel.swigToEnum(mapView.getMaxZoom()));
			atlasMapRendererView.setAzimuth(0);
			atlasMapRendererView.setFlatEarth(!settings.SPHERICAL_MAP.get());
			atlasMapRendererView.removeAllSymbolsProviders();
			atlasMapRendererView.resumeSymbolsUpdate();
			float elevationAngle = mapView.normalizeElevationAngle(settings.getLastKnownMapElevation());
			atlasMapRendererView.setElevationAngle(elevationAngle);
			mapView.setElevationAngle(elevationAngle);
			atlasMapRendererView.setSymbolsUpdateInterval(SYMBOLS_UPDATE_INTERVAL);
			mapRendererContext.setMapRendererView(atlasMapRendererView);
			mapView.applyMaximumFrameRate(atlasMapRendererView);
			mapView.applyDebugSettings(atlasMapRendererView);
		}
	}

	public void onCreate(Bundle savedInstanceState) {
		if (atlasMapRendererView != null) {
			atlasMapRendererView.handleOnCreate(savedInstanceState);
		}
	}

	public void onResume() {
		if (atlasMapRendererView != null) {
			atlasMapRendererView.handleOnResume();
		}
	}

	public void onPause() {
		if (atlasMapRendererView != null) {
			atlasMapRendererView.handleOnPause();
		}
	}

	public void onDestroy(@Nullable RetainedRenderer retained) {
		if (atlasMapRendererView != null) {
			NavigationSession carNavigationSession = app.getCarNavigationSession();
			if (carNavigationSession == null || !carNavigationSession.hasStarted()) {
				mapView.setMapRenderer(null, true);
				MapRendererContext mapRendererContext = NativeCoreContext.getMapRendererContext();
				boolean retainRenderer = retained != null && mapRendererContext != null
						&& mapRendererContext.getMapRendererView() == atlasMapRendererView
						&& atlasMapRendererView.suspendRenderer();
				if (retainRenderer) {
					// The renderer and its EGL contexts stay alive until the map view of the
					// recreated activity takes them over in setupAtlasMapRendererView()
					retained.mapRendererView = atlasMapRendererView;
					mapRendererContext.setMapRendererView(null);
				} else {
					resetMapRendererView();
					atlasMapRendererView.handleOnDestroy();
				}
			}
		}
		releaseRetainedRenderer();
		mapView.clearTouchDetectors();
		app.getOsmandMap().removeRenderingViewSetupListener(getRenderingViewSetupListener());
	}

	@NonNull
	private OsmandApplication getMyApplication() {
		return ((OsmandApplication) getContext().getApplicationContext());
	}

	@NonNull
	private RenderingViewSetupListener getRenderingViewSetupListener() {
		if (renderingViewSetupListener == null) {
			renderingViewSetupListener = this::setupRenderingView;
		}
		return renderingViewSetupListener;
	}
}
