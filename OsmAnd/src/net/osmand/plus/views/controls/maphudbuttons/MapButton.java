package net.osmand.plus.views.controls.maphudbuttons;

import static android.graphics.drawable.GradientDrawable.RECTANGLE;
import static android.widget.ImageView.ScaleType.CENTER;
import static net.osmand.plus.quickaction.ButtonAppearanceParams.BIG_SIZE_DP;
import static net.osmand.plus.quickaction.ButtonAppearanceParams.ROUND_RADIUS_DP;
import static net.osmand.plus.quickaction.ButtonAppearanceParams.TRANSPARENT_ALPHA;
import static net.osmand.plus.settings.backend.preferences.FabMarginPreference.setFabButtonMargin;

import android.content.Context;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnAttachStateChangeListener;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.util.Pair;
import androidx.core.view.ViewCompat;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.quickaction.ButtonAppearanceParams;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.FabMarginPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.mapwidgets.WidgetsVisibilityHelper;
import net.osmand.plus.views.mapwidgets.configure.buttons.MapButtonState;
import net.osmand.util.Algorithms;

public abstract class MapButton extends AppCompatImageButton implements OnAttachStateChangeListener {

	protected final OsmandApplication app;
	protected final OsmandSettings settings;
	protected final UiUtilities uiUtilities;

	protected MapActivity mapActivity;
	protected WidgetsVisibilityHelper visibilityHelper;

	protected ButtonAppearanceParams appearanceParams;
	protected ButtonAppearanceParams customAppearanceParams;

	protected int strokeWidth;
	protected boolean nightMode;
	protected boolean invalidated = true;
	protected boolean alwaysVisible;
	protected boolean useCustomPosition;
	protected boolean useDefaultAppearance;

	protected boolean routeDialogOpened;
	protected boolean showBottomButtons;

	@ColorInt
	protected int iconColor;
	@ColorInt
	protected int backgroundColor;
	@ColorInt
	protected int backgroundPressedColor;

	public MapButton(@NonNull Context context) {
		this(context, null);
	}

	public MapButton(@NonNull Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public MapButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);

		this.app = (OsmandApplication) context.getApplicationContext();
		this.settings = app.getSettings();
		this.uiUtilities = app.getUIUtilities();
		this.strokeWidth = app.getResources().getDimensionPixelSize(R.dimen.map_button_stroke);

		setupShadow();
		addOnAttachStateChangeListener(this);
		setNightMode(app.getDaynightHelper().isNightModeForMapControls());
	}

	@NonNull
	public String getButtonId() {
		MapButtonState buttonState = getButtonState();
		return buttonState != null ? buttonState.getId() : "";
	}

	@NonNull
	public OsmandMapTileView getMapView() {
		return app.getOsmandMap().getMapView();
	}

	public void setMapActivity(@NonNull MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		this.visibilityHelper = mapActivity.getWidgetsVisibilityHelper();
	}

	@Nullable
	public abstract MapButtonState getButtonState();

	@NonNull
	public ButtonAppearanceParams getAppearanceParams() {
		MapButtonState buttonState = getButtonState();
		if (buttonState != null) {
			return useDefaultAppearance ? buttonState.createDefaultAppearanceParams() : buttonState.createAppearanceParams();
		}
		return createDefaultAppearanceParams();
	}

	public void setCustomAppearanceParams(@Nullable ButtonAppearanceParams customAppearanceParams) {
		this.customAppearanceParams = customAppearanceParams;
	}

	public void setInvalidated(boolean invalidated) {
		this.invalidated = invalidated;
	}

	public void setAlwaysVisible(boolean alwaysVisible) {
		this.alwaysVisible = alwaysVisible;
	}

	public void setUseCustomPosition(boolean useCustomPosition) {
		this.useCustomPosition = useCustomPosition;
	}

	public void setUseDefaultAppearance(boolean useDefaultAppearance) {
		this.useDefaultAppearance = useDefaultAppearance;
	}

	public void setNightMode(boolean nightMode) {
		if (this.nightMode != nightMode) {
			this.nightMode = nightMode;
			this.invalidated = true;
		}
	}

	public void setRouteDialogOpened(boolean routeDialogOpened) {
		if (this.routeDialogOpened != routeDialogOpened) {
			this.routeDialogOpened = routeDialogOpened;
			this.invalidated = true;
		}
	}

	public void setShowBottomButtons(boolean showBottomButtons) {
		if (this.showBottomButtons != showBottomButtons) {
			this.showBottomButtons = showBottomButtons;
			this.invalidated = true;
		}
	}

	public void update() {
		updateVisibility();
		if (getVisibility() != View.VISIBLE) {
			return;
		}
		updateColors(nightMode);

		ButtonAppearanceParams params = getAppearanceParams();
		if (invalidated || !Algorithms.objectEquals(appearanceParams, params) || customAppearanceParams != null) {
			this.appearanceParams = customAppearanceParams != null ? customAppearanceParams : params;
			this.invalidated = false;

			updateIcon();
			updateSize();
			updateBackground();
		}
	}

	protected void updateColors(boolean nightMode) {
		setIconColor(ColorUtilities.getMapButtonIconColor(getContext(), nightMode));
		setBackgroundColors(ColorUtilities.getMapButtonBackgroundColor(getContext(), nightMode),
				ColorUtilities.getMapButtonBackgroundPressedColor(getContext(), nightMode));
	}

	protected void setIconColor(@ColorInt int iconColor) {
		if (this.iconColor != iconColor) {
			this.iconColor = iconColor;
			this.invalidated = true;
		}
	}

	protected void setBackgroundColors(@ColorInt int backgroundColor, @ColorInt int backgroundPressedColor) {
		if (this.backgroundColor != backgroundColor || this.backgroundPressedColor != backgroundPressedColor) {
			this.backgroundColor = backgroundColor;
			this.backgroundPressedColor = backgroundPressedColor;
			this.invalidated = true;
		}
	}

	protected void updateIcon() {
		String iconName = appearanceParams.getIconName();
		int iconId = AndroidUtils.getDrawableId(app, iconName);
		if (iconId == 0) {
			iconId = RenderingIcons.getBigIconResourceId(iconName);
		}
		if (iconId != 0) {
			Drawable drawable;
			MapButtonState buttonState = getButtonState();
			if (buttonState != null) {
				drawable = getButtonState().getIcon(iconId, iconColor, nightMode, true);
			} else {
				drawable = iconColor != 0 ? uiUtilities.getPaintedIcon(iconId, iconColor) : uiUtilities.getIcon(iconId);
			}
			OsmandMapLayer.setMapButtonIcon(this, drawable, CENTER);
		}
	}

	protected void updateSize() {
		int size = AndroidUtils.dpToPx(getContext(), appearanceParams.getSize());
		ViewGroup.LayoutParams params = getLayoutParams();
		params.height = size;
		params.width = size;
		setLayoutParams(params);
	}

	protected void updateBackground() {
		Context context = getContext();
		int size = AndroidUtils.dpToPx(context, appearanceParams.getSize());
		int cornerRadius = AndroidUtils.dpToPx(context, appearanceParams.getCornerRadius());

		GradientDrawable normal = new GradientDrawable();
		normal.setSize(size, size);
		normal.setShape(RECTANGLE);
		normal.setColor(ColorUtilities.getColorWithAlpha(backgroundColor, appearanceParams.getOpacity()));
		normal.setCornerRadius(cornerRadius);
		normal.setStroke(strokeWidth, ColorUtilities.getColor(context, nightMode ? R.color.map_widget_dark_stroke : R.color.map_widget_light_trans));

		GradientDrawable pressed = new GradientDrawable();
		pressed.setSize(size, size);
		pressed.setShape(RECTANGLE);
		pressed.setColor(backgroundPressedColor);
		pressed.setCornerRadius(cornerRadius);
		pressed.setStroke(strokeWidth, ColorUtilities.getColor(context, nightMode ? R.color.map_widget_dark_stroke : R.color.map_widget_light_pressed));

		setBackground(AndroidUtils.createPressedStateListDrawable(normal, pressed));
	}

	private void setupShadow() {
		setOutlineProvider(new ViewOutlineProvider() {
			@Override
			public void getOutline(View view, Outline outline) {
				Drawable background = view.getBackground();
				if (background != null) {
					background.getOutline(outline);
				} else {
					outline.setRect(0, 0, view.getWidth(), view.getHeight());
				}
				outline.setAlpha(1);
			}
		});
		ViewCompat.setElevation(this, 5.0f);
	}

	@Override
	public void onViewAttachedToWindow(@NonNull View v) {
		updateMargins();
		setInvalidated(true);
		update();
	}

	@Override
	public void onViewDetachedFromWindow(@NonNull View view) {

	}

	protected abstract boolean shouldShow();

	public boolean updateVisibility() {
		MapButtonState buttonState = getButtonState();
		boolean enabled = buttonState != null && buttonState.isEnabled();
		return updateVisibility(alwaysVisible || enabled && shouldShow());
	}

	protected boolean updateVisibility(boolean visible) {
		if (visible) {
			visible = app.getAppCustomization().isFeatureEnabled(getButtonId());
		}
		return AndroidUiHelper.updateVisibility(this, visible);
	}

	public void updateMargins() {
		MapButtonState buttonState = getButtonState();
		FabMarginPreference preference = buttonState != null ? buttonState.getFabMarginPref() : null;
		if (mapActivity != null && useCustomPosition && preference != null) {
			FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) getLayoutParams();
			if (AndroidUiHelper.isOrientationPortrait(mapActivity)) {
				Pair<Integer, Integer> margins = preference.getPortraitFabMargins();
				Pair<Integer, Integer> defMargins = preference.getDefaultPortraitMargins();
				setFabButtonMargin(mapActivity, this, params, margins, defMargins.first, defMargins.second);
			} else {
				Pair<Integer, Integer> margins = preference.getLandscapeFabMargin();
				Pair<Integer, Integer> defMargins = preference.getDefaultLandscapeMargins();
				setFabButtonMargin(mapActivity, this, params, margins, defMargins.first, defMargins.second);
			}
		}
	}

	@NonNull
	public ButtonAppearanceParams createDefaultAppearanceParams() {
		return new ButtonAppearanceParams("ic_quick_action", BIG_SIZE_DP, TRANSPARENT_ALPHA, ROUND_RADIUS_DP);
	}
}