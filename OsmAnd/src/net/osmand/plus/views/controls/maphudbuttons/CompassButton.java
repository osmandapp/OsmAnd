package net.osmand.plus.views.controls.maphudbuttons;

import static androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK;
import static androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_LONG_CLICK;
import static net.osmand.plus.settings.enums.CompassMode.MANUALLY_ROTATED;
import static net.osmand.plus.settings.enums.CompassMode.NORTH_IS_UP;
import static net.osmand.plus.settings.enums.CompassVisibility.ALWAYS_VISIBLE;
import static net.osmand.plus.settings.enums.CompassVisibility.VISIBLE_IF_MAP_ROTATED;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.ViewPropertyAnimatorCompat;
import androidx.core.view.ViewPropertyAnimatorListener;
import androidx.fragment.app.Fragment;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.configmap.ConfigureMapFragment;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.settings.controllers.CompassModeWidgetDialogController;
import net.osmand.plus.settings.enums.CompassMode;
import net.osmand.plus.settings.enums.CompassVisibility;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.mapwidgets.configure.buttons.CompassButtonState;
import net.osmand.plus.views.mapwidgets.configure.buttons.MapButtonState;

import org.jetbrains.annotations.NotNull;

public class CompassButton extends MapButton {

	private static final int HIDE_DELAY_MS = 5000;

	private final CompassButtonState buttonState;
	private ViewPropertyAnimatorCompat hideAnimator;

	private boolean forceHideCompass;
	private boolean specialPosition;
	private float mapRotation;

	public CompassButton(@NonNull Context context) {
		this(context, null);
	}

	public CompassButton(@NonNull Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public CompassButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		buttonState = app.getMapButtonsHelper().getCompassButtonState();
	}

	@Nullable
	@Override
	public MapButtonState getButtonState() {
		return buttonState;
	}

	@Override
	public void setMapActivity(@NonNull @NotNull MapActivity mapActivity) {
		super.setMapActivity(mapActivity);
		setupTouchListener();
		setupAccessibilityActions();
	}

	@Override
	public void update() {
		super.update();

		float mapRotation = mapActivity.getMapRotate();
		if (this.mapRotation != mapRotation) {
			this.mapRotation = mapRotation;

			if (imageView.getDrawable() instanceof CompassDrawable drawable) {
				drawable.setMapRotation(mapRotation);
			}
			imageView.invalidate();
		}
		CompassMode compassMode = settings.getCompassMode();
		setContentDescription(app.getString(compassMode.getTitleId()));
	}

	@Override
	protected void updateColors(boolean nightMode) {
		setBackgroundColors(ColorUtilities.getMapButtonBackgroundColor(getContext(), nightMode),
				ColorUtilities.getMapButtonBackgroundPressedColor(getContext(), nightMode));
	}

	@Override
	protected void updateIcon() {
		String iconName = appearanceParams.getIconName();
		int iconId = AndroidUtils.getDrawableId(app, iconName);
		if (iconId == 0) {
			iconId = RenderingIcons.getBigIconResourceId(iconName);
		}
		boolean customIcon = !CompassMode.isCompassIconId(iconId);
		setIconColor(customIcon ? ColorUtilities.getMapButtonIconColor(getContext(), nightMode) : 0);

		super.updateIcon();
	}

	@SuppressLint("ClickableViewAccessibility")
	private void setupTouchListener() {
		setOnTouchListener(new View.OnTouchListener() {

			private final GestureDetector gestureDetector = new GestureDetector(getContext(), new SimpleOnGestureListener() {
				@Override
				public boolean onDoubleTap(@NonNull MotionEvent e) {
					app.getMapViewTrackingUtilities().requestSwitchCompassToNextMode();
					return true;
				}

				@Override
				public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
					Fragment fragment = mapActivity.getSupportFragmentManager().findFragmentByTag(ConfigureMapFragment.TAG);
					if (fragment != null) {
						showCompassModeWidgetDialog();
						return true;
					}
					if (settings.getCompassMode() == NORTH_IS_UP) {
						app.showShortToastMessage(R.string.compass_click_north_is_up);
					} else {
						rotateMapToNorth();
					}
					return true;
				}

				@Override
				public void onLongPress(@NonNull MotionEvent e) {
					showCompassModeWidgetDialog();
				}
			});

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return gestureDetector.onTouchEvent(event);
			}
		});
	}

	private void setupAccessibilityActions() {
		ViewCompat.replaceAccessibilityAction(this, ACTION_CLICK,
				app.getString(NORTH_IS_UP.getTitleId()), (view, arguments) -> {
					rotateMapToNorth();
					return true;
				});
		ViewCompat.replaceAccessibilityAction(this, ACTION_LONG_CLICK,
				app.getString(R.string.choose_map_orientation), (view, arguments) -> {
					showCompassModeWidgetDialog();
					return true;
				});
	}

	private void rotateMapToNorth() {
		getMapView().resetRotation();
		app.getMapViewTrackingUtilities().setLastResetRotationToNorth(System.currentTimeMillis());
		if (settings.getCompassMode() == MANUALLY_ROTATED) {
			settings.setManuallyMapRotation(0);
		}
	}

	private void showCompassModeWidgetDialog() {
		CompassModeWidgetDialogController.showDialog(mapActivity);
	}

	@Nullable
	public View moveToSpecialPosition(@NonNull ViewGroup container, @NonNull ViewGroup.LayoutParams params) {
		ViewGroup parent = (ViewGroup) getParent();
		if (parent != null) {
			cancelHideAnimation();
			specialPosition = true;
			parent.removeView(this);
			setLayoutParams(params);
			container.addView(this);
			return this;
		}
		return null;
	}

	public void moveToDefaultPosition() {
		ViewGroup parent = (ViewGroup) getParent();
		if (parent != null) {
			specialPosition = false;
			parent.removeView(this);
			ViewGroup defaultContainer = mapActivity.findViewById(R.id.layers_compass_layout);
			if (defaultContainer != null) {
				int buttonSizePx = mapActivity.getResources().getDimensionPixelSize(R.dimen.map_small_button_size);
				int topMarginPx = mapActivity.getResources().getDimensionPixelSize(R.dimen.map_small_button_margin);
				LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(buttonSizePx, buttonSizePx);
				params.topMargin = topMarginPx;
				setLayoutParams(params);
				defaultContainer.addView(this);
			}
		}
	}

	@Override
	protected boolean shouldShow() {
		forceHideCompass = routeDialogOpened || visibilityHelper.shouldHideCompass();
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
			visible = app.getAppCustomization().isFeatureEnabled(getButtonId());
		}
		if (!specialPosition && visible != (getVisibility() == View.VISIBLE)) {
			if (visible) {
				if (hideAnimator != null) {
					hideAnimator.cancel();
				}
				setVisibility(VISIBLE);
				invalidate();
			} else if (hideAnimator == null) {
				if (!forceHideCompass) {
					hideDelayed(HIDE_DELAY_MS);
				} else {
					forceHideCompass = false;
					setVisibility(GONE);
					invalidate();
				}
			}
			return true;
		} else if (visible && hideAnimator != null) {
			hideAnimator.cancel();
			setVisibility(VISIBLE);
			invalidate();
			return true;
		}
		return false;
	}

	public void hideDelayed(long msec) {
		if (!specialPosition && getVisibility() == VISIBLE) {
			if (hideAnimator != null) {
				hideAnimator.cancel();
			}
			hideAnimator = ViewCompat.animate(this)
					.alpha(0f)
					.setDuration(250)
					.setStartDelay(msec)
					.setListener(new ViewPropertyAnimatorListener() {
						@Override
						public void onAnimationStart(@NotNull View view) {
						}

						@Override
						public void onAnimationEnd(@NotNull View view) {
							view.setVisibility(View.GONE);
							view.setAlpha(1f);
							hideAnimator = null;
						}

						@Override
						public void onAnimationCancel(@NotNull View view) {
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
}