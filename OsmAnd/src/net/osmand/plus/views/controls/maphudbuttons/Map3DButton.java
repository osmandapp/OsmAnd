package net.osmand.plus.views.controls.maphudbuttons;

import static net.osmand.plus.views.OsmandMapTileView.DEFAULT_ELEVATION_ANGLE;
import static net.osmand.plus.views.OsmandMapTileView.ElevationListener;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.settings.enums.Map3DModeVisibility;
import net.osmand.plus.views.AnimateDraggingMapThread;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.mapwidgets.configure.buttons.Map3DButtonState;
import net.osmand.plus.views.mapwidgets.configure.buttons.MapButtonState;

public class Map3DButton extends MapButton {

	private final Map3DButtonState buttonState;
	private final ElevationListener elevationListener;
	private final AnimateDraggingMapThread animateDraggingMapThread;

	public Map3DButton(@NonNull Context context) {
		this(context, null);
	}

	public Map3DButton(@NonNull Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public Map3DButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		buttonState = app.getMapButtonsHelper().getMap3DButtonState();

		elevationListener = getElevationListener();
		animateDraggingMapThread = getMapView().getAnimatedDraggingThread();

		setOnClickListener(getOnCLickListener());
	}

	@Nullable
	@Override
	public MapButtonState getButtonState() {
		return buttonState;
	}

	@Override
	public void update() {
		super.update();

		boolean flatMode = buttonState.isFlatMapMode();
		setContentDescription(app.getString(flatMode ? R.string.map_3d_mode_action : R.string.map_2d_mode_action));
	}

	@NonNull
	private View.OnClickListener getOnCLickListener() {
		OsmandMapTileView mapView = getMapView();
		return view -> {
			boolean flatMode = buttonState.isFlatMapMode();
			float tiltAngle = flatMode ? getElevationAngle(mapView.getZoom()) : DEFAULT_ELEVATION_ANGLE;
			animateDraggingMapThread.startTilting(tiltAngle, 0.0f);
			mapView.refreshMap();
		};
	}

	private float getElevationAngle(int zoom) {
		float map3DModeElevationAngle = buttonState.getElevationAngle();
		if (map3DModeElevationAngle != DEFAULT_ELEVATION_ANGLE) {
			return map3DModeElevationAngle;
		} else {
			return getMapView().getAdjustedTiltAngle(zoom, true);
		}
	}

	@NonNull
	private ElevationListener getElevationListener() {
		return new ElevationListener() {
			@Override
			public void onElevationChanging(float angle) {
				setInvalidated(true);
			}

			@Override
			public void onStopChangingElevation(float angle) {
				buttonState.setElevationAngle(angle);
			}
		};
	}

	@Override
	protected boolean shouldShow() {
		boolean shouldShowFabButton = mapActivity.getWidgetsVisibilityHelper().shouldShowMap3DButton();
		Map3DModeVisibility visibility = buttonState.getVisibility();

		return app.useOpenGlRenderer() && shouldShowFabButton
				&& (visibility == Map3DModeVisibility.VISIBLE
				|| (visibility == Map3DModeVisibility.VISIBLE_IN_3D_MODE && !buttonState.isFlatMapMode()));
	}

	@Override
	public void onViewAttachedToWindow(@NonNull View view) {
		super.onViewAttachedToWindow(view);
		getMapView().addElevationListener(elevationListener);
	}

	@Override
	public void onViewDetachedFromWindow(@NonNull View view) {
		super.onViewDetachedFromWindow(view);
		getMapView().removeElevationListener(elevationListener);
	}
}
