package net.osmand.plus.views.controls.maphudbuttons;

import static net.osmand.plus.utils.AndroidUtils.getMoveFabOnTouchListener;
import static net.osmand.plus.views.OsmandMapTileView.DEFAULT_ELEVATION_ANGLE;
import static net.osmand.plus.views.OsmandMapTileView.ElevationListener;
import static net.osmand.plus.views.layers.ContextMenuLayer.VIBRATE_SHORT;

import android.content.Context;
import android.os.Vibrator;
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
		setOnLongClickListener(getLongClickListener());
	}

	@Nullable
	@Override
	public MapButtonState getButtonState() {
		return buttonState;
	}

	@Override
	public void update() {
		super.update();

		boolean is3DMode = !buttonState.isDefaultElevationAngle();
		setContentDescription(app.getString(is3DMode ? R.string.map_2d_mode_action : R.string.map_3d_mode_action));
	}

	@NonNull
	private View.OnClickListener getOnCLickListener() {
		OsmandMapTileView mapView = getMapView();
		return view -> {
			boolean defaultElevationAngle = buttonState.isDefaultElevationAngle();
			float tiltAngle = defaultElevationAngle ? getElevationAngle(mapView.getZoom()) : DEFAULT_ELEVATION_ANGLE;
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

	@NonNull
	private View.OnLongClickListener getLongClickListener() {
		return view -> {
			Vibrator vibrator = (Vibrator) mapActivity.getSystemService(Context.VIBRATOR_SERVICE);
			vibrator.vibrate(VIBRATE_SHORT);
			setScaleX(1.5f);
			setScaleY(1.5f);
			setAlpha(0.95f);
			setOnTouchListener(getMoveFabOnTouchListener(app, mapActivity, this, buttonState.getFabMarginPref()));
			return true;
		};
	}

	@Override
	protected boolean shouldShow() {
		boolean shouldShowFabButton = mapActivity.getWidgetsVisibilityHelper().shouldShowMap3DButton();
		Map3DModeVisibility visibility = buttonState.getVisibility();

		return app.useOpenGlRenderer() && shouldShowFabButton
				&& (visibility == Map3DModeVisibility.VISIBLE
				|| (visibility == Map3DModeVisibility.VISIBLE_IN_3D_MODE
				&& !buttonState.isDefaultElevationAngle()));
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
