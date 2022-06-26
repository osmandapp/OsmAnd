package net.osmand.plus.views.controls.maphudbuttons;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.COMPASS_HUD_ID;
import static net.osmand.plus.views.layers.base.OsmandMapLayer.setMapButtonIcon;

public class CompassButton extends MapButton {

	private boolean specialPosition;
	private float mapRotation;

	public CompassButton(@NonNull MapActivity mapActivity) {
		super(mapActivity, mapActivity.findViewById(R.id.map_compass_button), COMPASS_HUD_ID);
		setIconColorId(0);
		setBackground(R.drawable.btn_inset_circle_trans, R.drawable.btn_inset_circle_night);
		setOnClickListener(v -> app.getMapViewTrackingUtilities().switchRotateMapMode());
	}

	@Nullable
	public ImageView moveToSpecialPosition(@NonNull ViewGroup specialContainer,
	                                       @NonNull ViewGroup.LayoutParams layoutParams) {
		ViewGroup parent = (ViewGroup) view.getParent();
		if (parent != null) {
			specialPosition = true;
			parent.removeView(view);
			view.setLayoutParams(layoutParams);
			specialContainer.addView(view);
			return view;
		}
		return null;
	}

	public void moveToDefaultPosition() {
		ViewGroup parent = (ViewGroup) view.getParent();
		if (parent != null) {
			specialPosition = false;
			parent.removeView(view);
			ViewGroup defaultContainer = mapActivity.findViewById(R.id.layers_compass_layout);
			if (defaultContainer != null) {
				int buttonSizePx = mapActivity.getResources().getDimensionPixelSize(R.dimen.map_small_button_size);
				int topMarginPx = mapActivity.getResources().getDimensionPixelSize(R.dimen.map_small_button_margin);
				LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(buttonSizePx, buttonSizePx);
				params.topMargin = topMarginPx;
				view.setLayoutParams(params);
				defaultContainer.addView(view);
			}
		}
	}

	public void updateState(boolean nightMode) {
		float mapRotation = mapActivity.getMapRotate();
		if (this.mapRotation != mapRotation) {
			this.mapRotation = mapRotation;
			view.invalidate();
		}

		if (settings.ROTATE_MAP.get() == OsmandSettings.ROTATE_MAP_NONE) {
			setIconId(R.drawable.ic_compass_niu, R.drawable.ic_compass_niu_white);
			setContentDesc(R.string.rotate_map_none_opt);
		} else if (settings.ROTATE_MAP.get() == OsmandSettings.ROTATE_MAP_BEARING) {
			setIconId(R.drawable.ic_compass_bearing, R.drawable.ic_compass_bearing_white);
			setContentDesc(R.string.rotate_map_bearing_opt);
		} else {
			setIconId(R.drawable.ic_compass, R.drawable.ic_compass_white);
			setContentDesc(R.string.rotate_map_compass_opt);
		}
	}

	@Override
	protected boolean shouldShow() {
		if (isRouteDialogOpened() || widgetsVisibilityHelper.shouldHideCompass()) {
			return false;
		} else if (!specialPosition) {
			ApplicationMode appMode = settings.getApplicationMode();
			return settings.SHOW_COMPASS_ALWAYS.getModeValue(appMode);
		}
		return true;
	}

	@Override
	public boolean updateVisibility(boolean visible) {
		if (visible) {
			visible = app.getAppCustomization().isFeatureEnabled(id);
		}
		if (!specialPosition && AndroidUiHelper.updateVisibility(view, visible)) {
			view.invalidate();
			return true;
		}
		return false;
	}

	@Override
	protected void setDrawable(@NonNull Drawable drawable) {
		setMapButtonIcon(view, new CompassDrawable(drawable));
	}

	private class CompassDrawable extends Drawable {

		private final Drawable original;

		public CompassDrawable(@NonNull Drawable original) {
			this.original = original;
		}

		@Override
		public void draw(Canvas canvas) {
			canvas.save();
			canvas.rotate(mapRotation, getIntrinsicWidth() / 2f, getIntrinsicHeight() / 2f);
			original.draw(canvas);
			canvas.restore();
		}

		@Override
		public int getMinimumHeight() {
			return original.getMinimumHeight();
		}

		@Override
		public int getMinimumWidth() {
			return original.getMinimumWidth();
		}

		@Override
		public int getIntrinsicHeight() {
			return original.getIntrinsicHeight();
		}

		@Override
		public int getIntrinsicWidth() {
			return original.getIntrinsicWidth();
		}

		@Override
		public void setChangingConfigurations(int configs) {
			super.setChangingConfigurations(configs);
			original.setChangingConfigurations(configs);
		}

		@Override
		public void setBounds(int left, int top, int right, int bottom) {
			super.setBounds(left, top, right, bottom);
			original.setBounds(left, top, right, bottom);
		}

		@Override
		public void setAlpha(int alpha) {
			original.setAlpha(alpha);
		}

		@Override
		public void setColorFilter(ColorFilter cf) {
			original.setColorFilter(cf);
		}

		@Override
		public int getOpacity() {
			return original.getOpacity();
		}
	}
}