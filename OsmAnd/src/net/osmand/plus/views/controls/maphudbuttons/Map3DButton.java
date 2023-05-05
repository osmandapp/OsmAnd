package net.osmand.plus.views.controls.maphudbuttons;

import static net.osmand.plus.views.OsmandMapTileView.*;
import static net.osmand.plus.views.layers.ContextMenuLayer.VIBRATE_SHORT;
import static net.osmand.plus.views.layers.MapQuickActionLayer.calculateTotalSizePx;
import static net.osmand.plus.views.layers.MapQuickActionLayer.getActionOnTouchListener;
import static net.osmand.plus.views.layers.MapQuickActionLayer.setFabButtonMargin;
import static net.osmand.plus.views.mapwidgets.configure.Map3DModeBottomSheet.*;

import android.content.Context;
import android.os.Vibrator;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.views.AnimateDraggingMapThread;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.MapQuickActionLayer;

public class Map3DButton extends MapButton {
	public Map3DButton(@NonNull MapActivity mapActivity, @NonNull ImageView fabButton, @NonNull String id) {
		super(mapActivity, fabButton, id);
		OsmandMapTileView mapView = mapActivity.getMapView();

		updateButton(mapView.getElevationAngle() != DEFAULT_ELEVATION_ANGLE);
		setRoundTransparentBackground();
		setIconColorId(R.color.map_button_icon_color_light, R.color.map_button_icon_color_dark);
		setMap3DButtonMargin(fabButton);
		setOnClickListener(getOnCLickListener(mapView));
		setOnLongClickListener(getLongClickListener(fabButton));
		setElevationListener(mapView);
	}

	private void setElevationListener(OsmandMapTileView mapView) {
		mapView.addElevationListener(angle -> updateButton(angle != DEFAULT_ELEVATION_ANGLE));
	}

	private View.OnClickListener getOnCLickListener(OsmandMapTileView mapView) {
		AnimateDraggingMapThread animateDraggingMapThread = mapView.getAnimatedDraggingThread();
		return view -> {
			boolean isDefaultAngle = mapView.getElevationAngle() == DEFAULT_ELEVATION_ANGLE;
			if (isDefaultAngle) {
				int zoom = mapView.getZoom();
				if (zoom < 10) {
					animateDraggingMapThread.startTilting(55);
				} else if (zoom < 12) {
					animateDraggingMapThread.startTilting(50);
				} else if (zoom < 14) {
					animateDraggingMapThread.startTilting(45);
				} else if (zoom < 16) {
					animateDraggingMapThread.startTilting(40);
				} else if (zoom < 17) {
					animateDraggingMapThread.startTilting(35);
				} else {
					animateDraggingMapThread.startTilting(30);
				}
				mapView.refreshMap();
			} else {
				animateDraggingMapThread.startTilting(DEFAULT_ELEVATION_ANGLE);
				mapView.refreshMap();
			}
		};
	}

	private View.OnLongClickListener getLongClickListener(ImageView fabButton) {
		return view -> {
			Vibrator vibrator = (Vibrator) mapActivity.getSystemService(Context.VIBRATOR_SERVICE);
			vibrator.vibrate(VIBRATE_SHORT);
			view.setScaleX(1.5f);
			view.setScaleY(1.5f);
			view.setAlpha(0.95f);
			view.setOnTouchListener(getActionOnTouchListener(app, mapActivity, fabButton, new MapQuickActionLayer.SetFabMarginCallback() {
				@Override
				public void onSetPortraitMargin(int rightMargin, int bottomMargin) {
					app.getSettings().setPortraitFab3DModeMargin(rightMargin, bottomMargin);
				}

				@Override
				public void onSetLandscapeMargin(int rightMargin, int bottomMargin) {
					app.getSettings().setLandscapeFab3DModeMargin(rightMargin, bottomMargin);
				}
			}));
			return true;
		};
	}

	private void updateButton(boolean is3DMode) {
		setIconId(is3DMode ? R.drawable.ic_action_2d : R.drawable.ic_action_3d);
		setContentDesc(is3DMode ? R.string.map_2d_mode_action : R.string.map_3d_mode_action);
	}

	private void setMap3DButtonMargin(ImageView fabButton) {
		if (mapActivity != null) {
			int defMarginPortrait = calculateTotalSizePx(app, R.dimen.map_button_size, R.dimen.map_button_spacing);
			int defMarginLandscape = calculateTotalSizePx(app, R.dimen.map_button_size, R.dimen.map_button_spacing_land);
			FrameLayout.LayoutParams param = (FrameLayout.LayoutParams) fabButton.getLayoutParams();
			if (AndroidUiHelper.isOrientationPortrait(mapActivity)) {
				Pair<Integer, Integer> fabMargin = settings.getPortraitMap3DModeFabMargin();
				setFabButtonMargin(mapActivity, fabButton, param, fabMargin, defMarginPortrait, defMarginPortrait);
			} else {
				Pair<Integer, Integer> fabMargin = settings.getLandscapeMap3DModeFabMargin();
				setFabButtonMargin(mapActivity, fabButton, param, fabMargin, defMarginLandscape, defMarginLandscape);
			}
		}
	}

	@Override
	protected boolean shouldShow() {
		return app.getSettings().MAP_3D_MODE_VISIBILITY.get() == Map3DModeVisibility.VISIBLE
				|| (app.getSettings().MAP_3D_MODE_VISIBILITY.get() == Map3DModeVisibility.VISIBLE_IN_3D_MODE
				&& app.getOsmandMap().getMapView().getElevationAngle() != DEFAULT_ELEVATION_ANGLE);
	}
}
