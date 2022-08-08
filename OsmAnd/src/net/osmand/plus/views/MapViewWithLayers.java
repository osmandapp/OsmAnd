package net.osmand.plus.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewStub;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.core.android.AtlasMapRendererView;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.auto.NavigationSession;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.corenative.NativeCoreContext;

public class MapViewWithLayers extends FrameLayout {

	private AtlasMapRendererView atlasMapRendererView;

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

		boolean nightMode = getMyApplication().getDaynightHelper().isNightMode();
		inflate(UiUtilities.getThemedContext(context, nightMode), R.layout.map_view_with_layers, this);
	}

	public void setupOpenGLView(boolean init) {
		OsmandApplication app = getMyApplication();
		OsmandSettings settings = app.getSettings();
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		NavigationSession carNavigationSession = app.getCarNavigationSession();
		View androidAutoPlaceholder = findViewById(R.id.AndroidAutoPlaceholder);
		boolean useAndroidAuto = carNavigationSession != null && carNavigationSession.hasStarted()
				&& InAppPurchaseHelper.isAndroidAutoAvailable(app);
		if (settings.USE_OPENGL_RENDER.get() && NativeCoreContext.isInit()) {
			ViewStub stub = findViewById(R.id.atlasMapRendererViewStub);
			if (atlasMapRendererView == null) {
				atlasMapRendererView = (AtlasMapRendererView) stub.inflate();
				atlasMapRendererView.setAzimuth(0);
				float elevationAngle = mapView.normalizeElevationAngle(app.getSettings().LAST_KNOWN_MAP_ELEVATION.get());
				atlasMapRendererView.setElevationAngle(elevationAngle);
				NativeCoreContext.getMapRendererContext().setMapRendererView(atlasMapRendererView);
			}
			mapView.setMapRenderer(atlasMapRendererView);
			OsmAndMapLayersView ml = findViewById(R.id.MapLayersView);
			if (useAndroidAuto) {
				ml.setVisibility(View.GONE);
				ml.setMapView(null);
				androidAutoPlaceholder.setVisibility(View.VISIBLE);
			} else {
				ml.setVisibility(View.VISIBLE);
				ml.setMapView(mapView);
				androidAutoPlaceholder.setVisibility(View.GONE);
			}
			app.getMapViewTrackingUtilities().setMapView(mapView);
			OsmAndMapSurfaceView surf = findViewById(R.id.MapView);
			surf.setVisibility(View.GONE);
		} else {
			OsmAndMapSurfaceView surf = findViewById(R.id.MapView);
			if (useAndroidAuto) {
				surf.setVisibility(View.GONE);
				surf.setMapView(null);
				androidAutoPlaceholder.setVisibility(View.VISIBLE);
			} else {
				surf.setVisibility(View.VISIBLE);
				surf.setMapView(mapView);
				androidAutoPlaceholder.setVisibility(View.GONE);
			}
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

	public void onDestroy() {
		if (atlasMapRendererView != null) {
			atlasMapRendererView.handleOnDestroy();
		}
	}

	public void onSetMapElevation(float angle) {
		if (atlasMapRendererView != null) {
			atlasMapRendererView.setElevationAngle(angle);
		}
	}

	@NonNull
	private OsmandApplication getMyApplication() {
		return ((OsmandApplication) getContext().getApplicationContext());
	}
}
