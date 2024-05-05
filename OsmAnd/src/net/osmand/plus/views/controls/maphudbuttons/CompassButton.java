package net.osmand.plus.views.controls.maphudbuttons;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.COMPASS_HUD_ID;
import static net.osmand.plus.settings.enums.CompassVisibility.ALWAYS_VISIBLE;
import static net.osmand.plus.settings.enums.CompassVisibility.VISIBLE_IF_MAP_ROTATED;
import static net.osmand.plus.views.layers.base.OsmandMapLayer.setMapButtonIcon;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.ViewPropertyAnimatorCompat;
import androidx.core.view.ViewPropertyAnimatorListener;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.controllers.CompassModeWidgetDialogController;
import net.osmand.plus.settings.enums.CompassMode;
import net.osmand.plus.settings.enums.CompassVisibility;
import net.osmand.plus.views.mapwidgets.configure.buttons.CompassButtonState;

public class CompassButton extends MapButton {

	private static final int HIDE_DELAY_MS = 5000;

	private final CompassButtonState buttonState;
	private ViewPropertyAnimatorCompat hideAnimator;

	private boolean forceHideCompass;
	private boolean specialPosition;
	private float mapRotation;

	public CompassButton(@NonNull MapActivity mapActivity) {
		super(mapActivity, mapActivity.findViewById(R.id.map_compass_button), COMPASS_HUD_ID, false);
		buttonState = app.getMapButtonsHelper().getCompassButtonState();

		setIconColorId(0);
		setBackground(R.drawable.btn_inset_circle_trans, R.drawable.btn_inset_circle_night);
		setOnClickListener(v -> app.getMapViewTrackingUtilities().requestSwitchCompassToNextMode());
		setOnLongClickListener(v -> {
			CompassModeWidgetDialogController.showDialog(mapActivity);
			return true;
		});
	}

	@Nullable
	public ImageView moveToSpecialPosition(@NonNull ViewGroup container, @NonNull LayoutParams params) {
		ViewGroup parent = (ViewGroup) view.getParent();
		if (parent != null) {
			cancelHideAnimation();
			specialPosition = true;
			parent.removeView(view);
			view.setLayoutParams(params);
			container.addView(view);
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
		CompassMode compassMode = settings.getCompassMode();
		setIconId(compassMode.getIconId());
		setContentDesc(compassMode.getTitleId());
	}

	@Override
	protected boolean shouldShow() {
		forceHideCompass = isRouteDialogOpened() || visibilityHelper.shouldHideCompass();
		if (forceHideCompass) {
			return false;
		} else if (!specialPosition) {
			CompassVisibility visibility = buttonState.getVisibility();
			return visibility == VISIBLE_IF_MAP_ROTATED ? mapActivity.getMapRotate() != 0 : visibility == ALWAYS_VISIBLE;
		}
		return true;
	}

	@Override
	public boolean updateVisibility(boolean visible) {
		if (visible) {
			visible = app.getAppCustomization().isFeatureEnabled(id);
		}
		if (!specialPosition && visible != (view.getVisibility() == View.VISIBLE)) {
			if (visible) {
				if (hideAnimator != null) {
					hideAnimator.cancel();
				}
				view.setVisibility(View.VISIBLE);
				view.invalidate();
			} else if (hideAnimator == null) {
				if (!forceHideCompass) {
					hideDelayed(HIDE_DELAY_MS);
				} else {
					forceHideCompass = false;
					view.setVisibility(View.GONE);
					view.invalidate();
				}
			}
			return true;
		} else if (visible && hideAnimator != null) {
			hideAnimator.cancel();
			view.setVisibility(View.VISIBLE);
			view.invalidate();
			return true;
		}
		return false;
	}

	public void hideDelayed(long msec) {
		if (!specialPosition && view.getVisibility() == View.VISIBLE) {
			if (hideAnimator != null) {
				hideAnimator.cancel();
			}
			hideAnimator = ViewCompat.animate(view)
					.alpha(0f)
					.setDuration(250)
					.setStartDelay(msec)
					.setListener(new ViewPropertyAnimatorListener() {
						@Override
						public void onAnimationStart(View view) {
						}

						@Override
						public void onAnimationEnd(View view) {
							view.setVisibility(View.GONE);
							view.setAlpha(1f);
							hideAnimator = null;
						}

						@Override
						public void onAnimationCancel(View view) {
							view.setVisibility(View.GONE);
							view.setAlpha(1f);
							hideAnimator = null;
						}
					});
			hideAnimator.start();
		}
	}

	public void cancelHideAnimation() {
		if (hideAnimator != null) {
			hideAnimator.cancel();
		}
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