package net.osmand.plus.views.controls.maphudbuttons;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_3D_HUD_ID;
import static net.osmand.plus.settings.backend.preferences.FabMarginPreference.setFabButtonMargin;
import static net.osmand.plus.utils.AndroidUtils.calculateTotalSizePx;
import static net.osmand.plus.utils.AndroidUtils.getMoveFabOnTouchListener;
import static net.osmand.plus.views.OsmandMapTileView.DEFAULT_ELEVATION_ANGLE;
import static net.osmand.plus.views.OsmandMapTileView.ElevationListener;
import static net.osmand.plus.views.layers.ContextMenuLayer.VIBRATE_SHORT;

import android.content.Context;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.preferences.FabMarginPreference;
import net.osmand.plus.settings.enums.Map3DModeVisibility;
import net.osmand.plus.views.AnimateDraggingMapThread;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.mapwidgets.configure.buttons.Map3DButtonState;

public class Map3DButton extends MapButton {

	private final Map3DButtonState buttonState;
	private final ElevationListener elevationListener;
	private final AnimateDraggingMapThread animateDraggingMapThread;
	private final ViewGroup defaultContainer;
	private boolean specialPosition;

	public Map3DButton(@NonNull MapActivity mapActivity, @NonNull ImageView fabButton) {
		super(mapActivity, fabButton, MAP_3D_HUD_ID, false);
		buttonState = app.getMapButtonsHelper().getMap3DButtonState();

		OsmandMapTileView mapView = mapActivity.getMapView();
		elevationListener = getElevationListener();
		animateDraggingMapThread = mapView.getAnimatedDraggingThread();
		defaultContainer = (ViewGroup) fabButton.getParent();

		updateButton(!isDefaultElevationAngle());
		setRoundTransparentBackground();
		setIconColorId(R.color.map_button_icon_color_light, R.color.map_button_icon_color_dark);
		setMap3DButtonMargin(fabButton);
		view.setOnClickListener(getOnCLickListener(mapView));
		view.setOnLongClickListener(getLongClickListener(fabButton));
		setElevationListener(mapView);
	}

	private void setElevationListener(OsmandMapTileView mapView) {
		mapView.addElevationListener(elevationListener);
	}

	private View.OnClickListener getOnCLickListener(OsmandMapTileView mapView) {
		return view -> {
			boolean defaultElevationAngle = isDefaultElevationAngle();
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
			return mapActivity.getMapView().getAdjustedTiltAngle(zoom, true);
		}
	}

	@NonNull
	private ElevationListener getElevationListener() {
		return new ElevationListener() {
			@Override
			public void onElevationChanging(float angle) {
				updateButton(angle != DEFAULT_ELEVATION_ANGLE);
			}

			@Override
			public void onStopChangingElevation(float angle) {
				buttonState.setElevationAngle(angle);
			}
		};
	}

	@NonNull
	private View.OnLongClickListener getLongClickListener(@NonNull ImageView fabButton) {
		return view -> {
			if (!specialPosition) {
				Vibrator vibrator = (Vibrator) mapActivity.getSystemService(Context.VIBRATOR_SERVICE);
				vibrator.vibrate(VIBRATE_SHORT);
				view.setScaleX(1.5f);
				view.setScaleY(1.5f);
				view.setAlpha(0.95f);
				view.setOnTouchListener(getMoveFabOnTouchListener(app, mapActivity, fabButton, buttonState.getFabMarginPref()));
				return true;
			}
			return false;
		};
	}

	@Override
	protected void updateState(boolean nightMode) {
		boolean is3DMode = !isDefaultElevationAngle();
		updateButton(is3DMode);
	}

	private void updateButton(boolean is3DMode) {
		setIconId(is3DMode ? R.drawable.ic_action_2d : R.drawable.ic_action_3d);
		view.setContentDescription(app.getString(is3DMode ? R.string.map_2d_mode_action : R.string.map_3d_mode_action));
	}

	public void moveToSpecialPosition(@NonNull ViewGroup container, @NonNull ViewGroup.LayoutParams params) {
		ViewGroup parent = (ViewGroup) view.getParent();
		if (parent != null) {
			specialPosition = true;
			parent.removeView(view);
			view.setLayoutParams(params);
			container.addView(view);
		}
	}

	public void restoreSavedPosition() {
		ViewGroup parent = (ViewGroup) view.getParent();
		if (parent != null) {
			specialPosition = false;
			parent.removeView(view);
			if (defaultContainer != null) {
				int btnSizePx = app.getResources().getDimensionPixelSize(R.dimen.map_button_size);
				FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(btnSizePx, btnSizePx);
				params.gravity = Gravity.BOTTOM | Gravity.END;
				view.setLayoutParams(params);
				defaultContainer.addView(view);
				setMap3DButtonMargin(view);
			}
		}
	}

	private void setMap3DButtonMargin(@NonNull ImageView fabButton) {
		if (mapActivity != null && !specialPosition) {
			int defMarginPortrait = calculateTotalSizePx(app, R.dimen.map_button_size, R.dimen.map_button_spacing);
			int defMarginLandscape = calculateTotalSizePx(app, R.dimen.map_button_size, R.dimen.map_button_spacing_land);
			FrameLayout.LayoutParams param = (FrameLayout.LayoutParams) fabButton.getLayoutParams();
			FabMarginPreference preference = buttonState.getFabMarginPref();

			if (AndroidUiHelper.isOrientationPortrait(mapActivity)) {
				Pair<Integer, Integer> fabMargin = preference.getPortraitFabMargin();
				setFabButtonMargin(mapActivity, fabButton, param, fabMargin, defMarginPortrait, defMarginPortrait);
			} else {
				Pair<Integer, Integer> fabMargin = preference.getLandscapeFabMargin();
				setFabButtonMargin(mapActivity, fabButton, param, fabMargin, defMarginLandscape, defMarginLandscape);
			}
		}
	}

	@Override
	public void refresh() {
		updateVisibility(shouldShow());
		setMap3DButtonMargin(view);
	}

	@Override
	protected boolean shouldShow() {
		boolean shouldShowFabButton = mapActivity.getWidgetsVisibilityHelper().shouldShowMap3DButton();
		Map3DModeVisibility visibility = buttonState.getVisibility();

		return app.useOpenGlRenderer() &&
				shouldShowFabButton &&
				(visibility == Map3DModeVisibility.VISIBLE
						|| (visibility == Map3DModeVisibility.VISIBLE_IN_3D_MODE
						&& !isDefaultElevationAngle()));
	}

	private boolean isDefaultElevationAngle() {
		return app.getOsmandMap().getMapView().getElevationAngle() == DEFAULT_ELEVATION_ANGLE;
	}

	public void onDestroyButton() {
		mapActivity.getMapView().removeElevationListener(elevationListener);
	}
}
