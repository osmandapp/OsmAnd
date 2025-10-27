package net.osmand.plus.views.controls.maphudbuttons;

import static android.graphics.Region.Op.DIFFERENCE;
import static android.graphics.drawable.GradientDrawable.RECTANGLE;
import static android.widget.ImageView.ScaleType.CENTER;
import static net.osmand.plus.quickaction.ButtonAppearanceParams.BIG_SIZE_DP;
import static net.osmand.plus.quickaction.ButtonAppearanceParams.ROUND_RADIUS_DP;
import static net.osmand.plus.quickaction.ButtonAppearanceParams.TRANSPARENT_ALPHA;
import static net.osmand.plus.views.layers.ContextMenuLayer.VIBRATE_SHORT;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnAttachStateChangeListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.quickaction.ButtonAppearanceParams;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.controls.MapHudLayout.SizeChangeListener;
import net.osmand.plus.views.controls.MapHudLayout.ViewChangeProvider;
import net.osmand.plus.views.controls.MapHudLayout.VisibilityChangeListener;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.mapwidgets.WidgetsVisibilityHelper;
import net.osmand.plus.views.mapwidgets.configure.buttons.MapButtonState;
import net.osmand.util.Algorithms;

import org.jetbrains.annotations.NotNull;

public abstract class MapButton extends FrameLayout implements OnAttachStateChangeListener, ViewChangeProvider {

	protected final OsmandApplication app;
	protected final OsmandSettings settings;
	protected final UiUtilities uiUtilities;

	protected final ImageView imageView;
	protected final Path clipPath = new Path();
	protected LayerDrawable shadowDrawable;

	protected MapActivity mapActivity;
	protected WidgetsVisibilityHelper visibilityHelper;

	protected ButtonAppearanceParams appearanceParams;
	protected ButtonAppearanceParams customAppearanceParams;

	protected int strokeWidth;
	protected float shadowRadius;
	protected float shadowPadding;

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

	private SizeChangeListener sizeListener;
	private VisibilityChangeListener visibilityListener;

	public MapButton(@NonNull Context context) {
		this(context, null);
	}

	public MapButton(@NonNull Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public MapButton(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public MapButton(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);

		this.app = (OsmandApplication) context.getApplicationContext();
		this.settings = app.getSettings();
		this.uiUtilities = app.getUIUtilities();
		this.strokeWidth = AndroidUtils.dpToPx(context, 1);
		this.shadowRadius = AndroidUtils.dpToPxF(context, 2);
		this.shadowPadding = AndroidUtils.dpToPxF(context, 4);
		this.imageView = setupImageView(context, attrs, defStyleAttr, defStyleRes);

		init();
	}

	@NonNull
	protected ImageView setupImageView(@NonNull Context context, @Nullable AttributeSet attrs,
	                                   @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
		int imageSize = (int) getImageSize();
		ImageView imageView = new ImageView(context, attrs, defStyleAttr, defStyleRes);
		imageView.setClickable(false);
		imageView.setFocusable(false);
		addView(imageView, new FrameLayout.LayoutParams(imageSize, imageSize, Gravity.CENTER));

		return imageView;
	}

	protected void init() {
		setClipToPadding(false);
		addOnAttachStateChangeListener(this);
		setBackgroundColor(Color.TRANSPARENT);
		setPadding((int) shadowPadding, (int) shadowPadding, (int) shadowPadding, (int) shadowPadding);
		setNightMode(app.getDaynightHelper().isNightMode());
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

		MapButtonState buttonState = getButtonState();
		if (buttonState != null) {
			buttonState.updatePositions(mapActivity);
		}
	}

	@NonNull
	public ImageView getImageView() {
		return imageView;
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

	@Nullable
	public ButtonPositionSize getDefaultPositionSize() {
		MapButtonState buttonState = getButtonState();
		if (buttonState != null) {
			return buttonState.getDefaultPositionSize();
		}
		return null;
	}

	public void setCustomAppearanceParams(@Nullable ButtonAppearanceParams customAppearanceParams) {
		this.customAppearanceParams = customAppearanceParams;
	}

	public void setInvalidated(boolean invalidated) {
		if (!this.invalidated) {
			this.invalidated = invalidated;
		}
	}

	public void setAlwaysVisible(boolean alwaysVisible) {
		this.alwaysVisible = alwaysVisible;
	}

	public boolean isUseCustomPosition() {
		return useCustomPosition;
	}

	public void setUseCustomPosition(boolean useCustomPosition) {
		this.useCustomPosition = useCustomPosition;

		MapButtonState buttonState = getButtonState();
		if (useCustomPosition && buttonState != null) {
			setOnLongClickListener(v -> {
				Vibrator vibrator = (Vibrator) mapActivity.getSystemService(Context.VIBRATOR_SERVICE);
				vibrator.vibrate(VIBRATE_SHORT);
				setScaleX(1.5f);
				setScaleY(1.5f);
				setAlpha(0.95f);
				setOnTouchListener(new MapButtonTouchListener(buttonState, mapActivity));
				return true;
			});
		}
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
			updateContent();
		}
	}

	protected void updateColors(boolean nightMode) {
		setIconColor(ColorUtilities.getMapButtonIconColor(getContext(), nightMode));
		setBackgroundColors(ColorUtilities.getMapButtonBackgroundColor(getContext(), nightMode),
				ColorUtilities.getMapButtonBackgroundPressedColor(getContext(), nightMode));
	}

	protected void updateContent() {
		updateIcon();
		updateSize();
		updateBackground();
		updateShadow();
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
			OsmandMapLayer.setMapButtonIcon(imageView, drawable, CENTER);
		}
	}

	protected void updateSize() {
		updateSize(this, (int) getFrameSize());
		updateSize(imageView, (int) getImageSize());
	}

	protected void updateSize(@NonNull View view, int size) {
		ViewGroup.LayoutParams params = view.getLayoutParams();
		params.height = size;
		params.width = size;
		view.setLayoutParams(params);
	}

	protected void updateBackground() {
		Context context = getContext();
		float opacity = appearanceParams.getOpacity();
		int cornerRadius = AndroidUtils.dpToPx(context, appearanceParams.getCornerRadius());

		GradientDrawable normal = new GradientDrawable();
		normal.setShape(RECTANGLE);
		normal.setColor(ColorUtilities.getColorWithAlpha(backgroundColor, opacity));
		normal.setCornerRadius(cornerRadius);
		if (opacity == 0 || nightMode) {
			normal.setStroke(strokeWidth, ColorUtilities.getColor(context, nightMode ? R.color.map_widget_dark_stroke : R.color.map_widget_light_trans));
		}

		GradientDrawable pressed = new GradientDrawable();
		pressed.setShape(RECTANGLE);
		pressed.setColor(backgroundPressedColor);
		pressed.setCornerRadius(cornerRadius);
		pressed.setStroke(strokeWidth, ColorUtilities.getColor(context, nightMode ? R.color.map_widget_dark_stroke : R.color.map_widget_light_pressed));

		imageView.setBackground(AndroidUtils.createPressedStateListDrawable(normal, pressed));
	}

	protected void updateShadow() {
		int radius = AndroidUtils.dpToPx(getContext(), appearanceParams.getCornerRadius());
		float[] outerRadius = new float[] {radius, radius, radius, radius, radius, radius, radius, radius};

		ShapeDrawable drawable = new ShapeDrawable();
		drawable.getPaint().setAntiAlias(true);
		drawable.getPaint().setShadowLayer(shadowRadius, 0f, 0f, ColorUtilities.getColorWithAlpha(Color.BLACK, 0.5f));
		drawable.setShape(new RoundRectShape(outerRadius, null, null));

		shadowDrawable = new LayerDrawable(new ShapeDrawable[] {drawable});
		shadowDrawable.setLayerInset(0, (int) shadowPadding, (int) shadowPadding, (int) shadowPadding, (int) shadowPadding);
	}

	@Override
	protected void onDraw(@NotNull Canvas canvas) {
		super.onDraw(canvas);

		if (!nightMode && appearanceParams != null) {
			drawShadow(canvas);
		}
	}

	protected void drawShadow(@NotNull Canvas canvas) {
		canvas.save();

		int width = getWidth();
		int height = getHeight();
		int padding = (int) shadowPadding;
		int radius = AndroidUtils.dpToPx(getContext(), appearanceParams.getCornerRadius());

		clipPath.reset();
		clipPath.addRoundRect(padding, padding, width - padding, height - padding, radius, radius, Direction.CW);

		canvas.clipPath(clipPath, DIFFERENCE);
		shadowDrawable.setBounds(0, 0, width, height);
		shadowDrawable.draw(canvas);
		canvas.restore();
	}

	@Override
	public void onViewAttachedToWindow(@NonNull View v) {
		if (mapActivity != null) {
			setInvalidated(true);
			update();
		}
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

	public void savePosition() {
		MapButtonState buttonState = getButtonState();
		if (buttonState != null && useCustomPosition) {
			buttonState.savePosition();
		}
	}

	public float getFrameSize() {
		return (int) getImageSize() + ((int) shadowPadding) * 2;
	}

	public float getImageSize() {
		ButtonAppearanceParams params = appearanceParams != null ? appearanceParams : getAppearanceParams();
		return AndroidUtils.dpToPxF(getContext(), params.getSize());
	}

	@NonNull
	public ButtonAppearanceParams createDefaultAppearanceParams() {
		return new ButtonAppearanceParams("ic_quick_action", BIG_SIZE_DP, TRANSPARENT_ALPHA, ROUND_RADIUS_DP);
	}

	@Override
	public void setSizeListener(@Nullable SizeChangeListener listener) {
		this.sizeListener = listener;
	}

	@Override
	public void setVisibilityListener(@Nullable VisibilityChangeListener listener) {
		this.visibilityListener = listener;
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		if (sizeListener != null) {
			sizeListener.onSizeChanged(this, w, h, oldw, oldh);
		}
	}

	@Override
	protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
		super.onVisibilityChanged(changedView, visibility);
		if (visibilityListener != null) {
			visibilityListener.onVisibilityChanged(changedView, visibility);
		}
	}
}