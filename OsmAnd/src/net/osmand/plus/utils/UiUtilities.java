package net.osmand.plus.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.core.widget.TintableCompoundButton;

import com.google.android.material.slider.LabelFormatter;
import com.google.android.material.slider.RangeSlider;
import com.google.android.material.slider.Slider;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.snackbar.SnackbarContentLayout;

import net.osmand.CallbackWithObject;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;
import net.osmand.plus.widgets.style.CustomURLSpan;

import org.apache.commons.logging.Log;

import gnu.trove.map.hash.TLongObjectHashMap;

public class UiUtilities {

	private static final Log LOG = PlatformUtil.getLog(UiUtilities.class);

	private static final int ORIENTATION_0 = 0;
	private static final int ORIENTATION_90 = 3;
	private static final int ORIENTATION_270 = 1;
	private static final int ORIENTATION_180 = 2;
	private final TLongObjectHashMap<Drawable> drawableCache = new TLongObjectHashMap<>();
	private final OsmandApplication app;
	private static final int INVALID_ID = -1;

	public enum CompoundButtonType {
		GLOBAL,
		PROFILE_DEPENDENT,
		TOOLBAR
	}

	public enum CustomRadioButtonType {
		START,
		CENTER,
		END,
	}

	public UiUtilities(@NonNull OsmandApplication app) {
		this.app = app;
	}

	private synchronized Drawable getDrawable(@DrawableRes int resId, @ColorRes int clrId) {
		long key = (((long) resId) << 32L) + clrId;
		Drawable drawable = drawableCache.get(key);
		if (drawable == null) {
			drawable = AppCompatResources.getDrawable(app, resId);
			if (drawable != null) {
				drawable = DrawableCompat.wrap(drawable);
				drawable.mutate();
				if (clrId != 0) {
					DrawableCompat.setTint(drawable, ContextCompat.getColor(app, clrId));
				}
				drawableCache.put(key, drawable);
			}
		}
		return drawable;
	}

	@Nullable
	private synchronized Drawable getPaintedDrawable(@DrawableRes int resId, @ColorInt int color) {
		Drawable drawable = null;
		if (resId != 0) {
			long key = ((long) resId << 32L) + color;
			drawable = drawableCache.get(key);
			if (drawable == null) {
				drawable = AppCompatResources.getDrawable(app, resId);
				drawable = tintDrawable(drawable, color);

				drawableCache.put(key, drawable);
			}
		} else {
			LOG.warn("Invalid icon identifier");
		}
		return drawable;
	}

	@Nullable
	public Drawable getPaintedIcon(@DrawableRes int id, @ColorInt int color) {
		return getPaintedDrawable(id, color);
	}

	public Drawable getIcon(@DrawableRes int id, @ColorRes int colorId) {
		return getDrawable(id, colorId);
	}

	public Drawable getLayeredIcon(@DrawableRes int bgIconId, @DrawableRes int foregroundIconId) {
		return getLayeredIcon(bgIconId, foregroundIconId, 0, 0);
	}

	public Drawable getLayeredIcon(@DrawableRes int bgIconId, @DrawableRes int foregroundIconId,
	                               @ColorRes int bgColorId, @ColorRes int foregroundColorId) {
		Drawable background = getDrawable(bgIconId, bgColorId);
		Drawable foreground = getDrawable(foregroundIconId, foregroundColorId);
		return getLayeredIcon(background, foreground);
	}

	public static Drawable getLayeredIcon(Drawable... icons) {
		return new LayerDrawable(icons);
	}

	public Drawable getThemedIcon(@DrawableRes int id) {
		return getDrawable(id, R.color.icon_color_default_light);
	}

	public Drawable getActiveIcon(@DrawableRes int id, boolean nightMode) {
		return getDrawable(id, ColorUtilities.getActiveIconColorId(nightMode));
	}

	public Drawable getIcon(@DrawableRes int id) {
		return getDrawable(id, 0);
	}

	public Drawable getIcon(@DrawableRes int id, boolean light) {
		return getDrawable(id, ColorUtilities.getDefaultIconColorId(!light));
	}

	public static Drawable getColoredSelectableDrawable(Context ctx, int color, float alpha) {
		int colorWithAlpha = ColorUtilities.getColorWithAlpha(color, alpha);
		return getColoredSelectableDrawable(ctx, colorWithAlpha);
	}

	public static Drawable getColoredSelectableDrawable(Context ctx, int color) {
		Drawable drawable = null;
		Drawable bg = getSelectableDrawable(ctx);
		if (bg != null) {
			drawable = tintDrawable(bg, color);
		}
		return drawable;
	}

	@Nullable
	public static Drawable getSelectableDrawable(Context ctx) {
		int bgResId = AndroidUtils.resolveAttribute(ctx, R.attr.selectableItemBackground);
		if (bgResId != 0) {
			return AppCompatResources.getDrawable(ctx, bgResId);
		}
		return null;
	}

	public static Drawable createTintedDrawable(Context context, @DrawableRes int resId, @ColorInt int color) {
		return tintDrawable(AppCompatResources.getDrawable(context, resId), color);
	}

	public static Drawable tintDrawable(Drawable drawable, @ColorInt int color) {
		Drawable coloredDrawable = null;
		if (drawable != null) {
			coloredDrawable = DrawableCompat.wrap(drawable);
			if (coloredDrawable.getConstantState() != null) {
				coloredDrawable = coloredDrawable.getConstantState().newDrawable();
			}
			coloredDrawable.mutate();
			if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP && coloredDrawable instanceof RippleDrawable) {
				((RippleDrawable) coloredDrawable).setColor(ColorStateList.valueOf(color));
			} else {
				DrawableCompat.setTint(coloredDrawable, color);
			}
		}

		return coloredDrawable;
	}

	public int getScreenOrientation() {
		return getScreenOrientation(app);
	}

	public int getScreenOrientation(@NonNull Context context) {
		int screenOrientation = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
		switch (screenOrientation) {
			case ORIENTATION_0:   // Device default (normally portrait)
				screenOrientation = 0;
				break;
			case ORIENTATION_90:  // Landscape right
				screenOrientation = 90;
				break;
			case ORIENTATION_270: // Landscape left
				screenOrientation = 270;
				break;
			case ORIENTATION_180: // Upside down
				screenOrientation = 180;
				break;
		}
		//Looks like screenOrientation correction must not be applied for devices without compass?
		PackageManager manager = app.getPackageManager();
		boolean hasCompass = manager.hasSystemFeature(PackageManager.FEATURE_SENSOR_COMPASS);
		if (!hasCompass) {
			screenOrientation = 0;
		}
		return screenOrientation;
	}

	public static void setupSnackbar(Snackbar snackbar, boolean nightMode) {
		setupSnackbar(snackbar, nightMode, null, null, null, null);
	}

	public static void setupSnackbar(Snackbar snackbar, boolean nightMode, Integer maxLines) {
		setupSnackbar(snackbar, nightMode, null, null, null, maxLines);
	}

	public static void setupSnackbar(Snackbar snackbar, boolean nightMode, @ColorRes Integer backgroundColor,
	                                 @ColorRes Integer messageColor, @ColorRes Integer actionColor, Integer maxLines) {
		if (snackbar == null) {
			return;
		}
		View view = snackbar.getView();
		Context ctx = view.getContext();
		TextView tvMessage = view.findViewById(com.google.android.material.R.id.snackbar_text);
		TextView tvAction = view.findViewById(com.google.android.material.R.id.snackbar_action);
		if (messageColor == null) {
			messageColor = ColorUtilities.getActiveButtonsAndLinksTextColorId(nightMode);
		}
		tvMessage.setTextColor(ContextCompat.getColor(ctx, messageColor));
		if (actionColor == null) {
			actionColor = ColorUtilities.getActiveColorId(nightMode);
		}
		tvAction.setTextColor(ContextCompat.getColor(ctx, actionColor));
		if (maxLines != null) {
			tvMessage.setMaxLines(maxLines);
		}
		if (backgroundColor == null) {
			backgroundColor = nightMode ? R.color.list_background_color_dark : R.color.color_black;
		}
		view.setBackgroundColor(ContextCompat.getColor(ctx, backgroundColor));
	}

	public static void setupSnackbarVerticalLayout(Snackbar snackbar) {
		View view = snackbar.getView();
		Context ctx = view.getContext();
		TextView messageView = view.findViewById(com.google.android.material.R.id.snackbar_text);
		TextView actionView = view.findViewById(com.google.android.material.R.id.snackbar_action);
		ViewParent parent = actionView.getParent();
		if (parent instanceof SnackbarContentLayout) {
			((SnackbarContentLayout) parent).removeView(actionView);
			((SnackbarContentLayout) parent).removeView(messageView);
			LinearLayout container = new LinearLayout(ctx);
			container.setOrientation(LinearLayout.VERTICAL);
			container.addView(messageView);
			container.addView(actionView);
			((SnackbarContentLayout) parent).addView(container);
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT);
			actionView.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
			container.setLayoutParams(params);
		}
		try {
			snackbar.setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_FADE);
		} catch (Throwable e) {
		}
	}

	public static void rotateImageByLayoutDirection(ImageView image) {
		if (image == null) {
			return;
		}
		int rotation = AndroidUtils.getLayoutDirection(image.getContext()) == ViewCompat.LAYOUT_DIRECTION_RTL ? 180 : 0;
		image.setRotationY(rotation);
	}

	public static void updateCustomRadioButtons(Context app, View buttonsView, boolean nightMode,
	                                            CustomRadioButtonType buttonType) {
		int activeColor = ColorUtilities.getActiveColor(app, nightMode);
		int textColor = ColorUtilities.getPrimaryTextColor(app, nightMode);
		int radius = AndroidUtils.dpToPx(app, 4);
		boolean isLayoutRtl = AndroidUtils.isLayoutRtl(app);

		View startButtonContainer = buttonsView.findViewById(R.id.left_button_container);
		View centerButtonContainer = buttonsView.findViewById(R.id.center_button_container);
		View endButtonContainer = buttonsView.findViewById(R.id.right_button_container);

		GradientDrawable background = new GradientDrawable();
		background.setColor(ColorUtilities.getColorWithAlpha(activeColor, 0.1f));
		background.setStroke(AndroidUtils.dpToPx(app, 1.5f), ColorUtilities.getColorWithAlpha(activeColor, 0.5f));
		if (buttonType == CustomRadioButtonType.START) {
			if (isLayoutRtl) {
				background.setCornerRadii(new float[]{0, 0, radius, radius, radius, radius, 0, 0});
			} else {
				background.setCornerRadii(new float[]{radius, radius, 0, 0, 0, 0, radius, radius});
			}
			TextView startButtonText = startButtonContainer.findViewById(R.id.left_button);
			TextView endButtonText = endButtonContainer.findViewById(R.id.right_button);

			endButtonContainer.setBackgroundColor(Color.TRANSPARENT);
			endButtonText.setTextColor(activeColor);
			startButtonContainer.setBackground(background);
			startButtonText.setTextColor(textColor);

			if (centerButtonContainer != null) {
				TextView centerButtonText = centerButtonContainer.findViewById(R.id.center_button);
				centerButtonText.setTextColor(activeColor);
				centerButtonContainer.setBackgroundColor(Color.TRANSPARENT);
			}
		} else if (buttonType == CustomRadioButtonType.CENTER) {
			background.setCornerRadii(new float[]{0, 0, 0, 0, 0, 0, 0, 0});
			centerButtonContainer.setBackground(background);
			AndroidUiHelper.updateVisibility(centerButtonContainer, true);

			TextView centerButtonText = centerButtonContainer.findViewById(R.id.center_button);
			centerButtonText.setTextColor(textColor);

			if (endButtonContainer != null) {
				TextView endButtonText = endButtonContainer.findViewById(R.id.right_button);
				endButtonText.setTextColor(activeColor);
				endButtonContainer.setBackgroundColor(Color.TRANSPARENT);
			}
			if (startButtonContainer != null) {
				TextView startButtonText = startButtonContainer.findViewById(R.id.left_button);
				startButtonText.setTextColor(activeColor);
				startButtonContainer.setBackgroundColor(Color.TRANSPARENT);
			}
		} else {
			if (isLayoutRtl) {
				background.setCornerRadii(new float[]{radius, radius, 0, 0, 0, 0, radius, radius});
			} else {
				background.setCornerRadii(new float[]{0, 0, radius, radius, radius, radius, 0, 0});
			}
			TextView startButtonText = startButtonContainer.findViewById(R.id.left_button);
			TextView endButtonText = endButtonContainer.findViewById(R.id.right_button);

			endButtonContainer.setBackground(background);
			endButtonText.setTextColor(textColor);
			startButtonContainer.setBackgroundColor(Color.TRANSPARENT);
			startButtonText.setTextColor(activeColor);

			if (centerButtonContainer != null) {
				TextView centerButtonText = centerButtonContainer.findViewById(R.id.center_button);
				centerButtonText.setTextColor(activeColor);
				centerButtonContainer.setBackgroundColor(Color.TRANSPARENT);
			}
		}
	}

	public static void setupCompoundButtonDrawable(Context ctx, boolean nightMode, @ColorInt int activeColor, Drawable drawable) {
		int inactiveColor = ColorUtilities.getDefaultIconColor(ctx, nightMode);
		int[][] states = {
				new int[]{-android.R.attr.state_checked},
				new int[]{android.R.attr.state_checked}
		};
		ColorStateList csl = new ColorStateList(states, new int[]{inactiveColor, activeColor});
		DrawableCompat.setTintList(DrawableCompat.wrap(drawable), csl);
	}

	public static void setupCompoundButton(boolean nightMode, @ColorInt int activeColor, CompoundButton compoundButton) {
		if (compoundButton == null) {
			return;
		}
		Context ctx = compoundButton.getContext();
		int inactiveColorPrimary = ContextCompat.getColor(ctx, nightMode ? R.color.icon_color_default_dark : R.color.icon_color_secondary_light);
		int inactiveColorSecondary = ColorUtilities.getColorWithAlpha(inactiveColorPrimary, 0.45f);
		setupCompoundButton(compoundButton, activeColor, inactiveColorPrimary, inactiveColorSecondary);
	}

	public static void setupCompoundButton(CompoundButton compoundButton, boolean nightMode, CompoundButtonType type) {
		if (compoundButton == null) {
			return;
		}
		OsmandApplication app = (OsmandApplication) compoundButton.getContext().getApplicationContext();
		int activeColor = ColorUtilities.getActiveColor(app, nightMode);
		@ColorInt int inactiveColorPrimary = ContextCompat.getColor(app, nightMode ? R.color.icon_color_default_dark : R.color.icon_color_secondary_light);
		@ColorInt int inactiveColorSecondary = ColorUtilities.getColorWithAlpha(inactiveColorPrimary, 0.45f);
		switch (type) {
			case PROFILE_DEPENDENT:
				ApplicationMode appMode = app.getSettings().getApplicationMode();
				activeColor = appMode.getProfileColor(nightMode);
				break;
			case TOOLBAR:
				activeColor = Color.WHITE;
				inactiveColorPrimary = activeColor;
				inactiveColorSecondary = ColorUtilities.getColorWithAlpha(Color.BLACK, 0.25f);
				break;
		}
		setupCompoundButton(compoundButton, activeColor, inactiveColorPrimary, inactiveColorSecondary);
	}

	public static Drawable getStrokedBackgroundForCompoundButton(@NonNull OsmandApplication app, int highlightColorDay, int highlightColorNight, boolean checked, boolean nightMode) {
		GradientDrawable background = (GradientDrawable) AppCompatResources.getDrawable(app,
				R.drawable.bg_select_group_button_outline);
		if (background != null) {
			int highlightColor = ContextCompat.getColor(app, nightMode ?
					highlightColorNight : highlightColorDay);
			int strokedColor = AndroidUtils.getColorFromAttr(getThemedContext(app, nightMode),
					R.attr.stroked_buttons_and_links_outline);
			background = (GradientDrawable) background.mutate();
			if (checked) {
				background.setStroke(0, Color.TRANSPARENT);
				background.setColor(highlightColor);
			} else {
				background.setStroke(app.getResources().getDimensionPixelSize(R.dimen.map_button_stroke), strokedColor);
			}
		}
		return background;
	}

	public static void setupCompoundButton(CompoundButton compoundButton,
	                                       @ColorInt int activeColor,
	                                       @ColorInt int inactiveColorPrimary,
	                                       @ColorInt int inactiveColorSecondary) {
		if (compoundButton == null) {
			return;
		}
		int[][] states = {
				new int[]{-android.R.attr.state_enabled},
				new int[]{-android.R.attr.state_checked},
				new int[]{android.R.attr.state_checked}
		};
		if (compoundButton instanceof SwitchCompat) {
			int[] thumbColors = {inactiveColorPrimary, inactiveColorPrimary, activeColor};
			int[] trackColors = {inactiveColorSecondary, inactiveColorSecondary, inactiveColorSecondary};

			SwitchCompat sc = (SwitchCompat) compoundButton;
			DrawableCompat.setTintList(DrawableCompat.wrap(sc.getThumbDrawable()), new ColorStateList(states, thumbColors));
			DrawableCompat.setTintList(DrawableCompat.wrap(sc.getTrackDrawable()), new ColorStateList(states, trackColors));
		} else if (compoundButton instanceof TintableCompoundButton) {
			int[] colors = {inactiveColorPrimary, inactiveColorPrimary, activeColor};
			ColorStateList csl = new ColorStateList(states, colors);
			((TintableCompoundButton) compoundButton).setSupportButtonTintList(csl);
		}
		compoundButton.setBackgroundColor(Color.TRANSPARENT);
	}

	public static void setupToolbarOverflowIcon(Toolbar toolbar, @DrawableRes int iconId, @ColorRes int colorId) {
		Context ctx = toolbar.getContext();
		if (ctx != null) {
			Drawable icon = ContextCompat.getDrawable(ctx, iconId);
			toolbar.setOverflowIcon(icon);
			if (icon != null) {
				int color = ContextCompat.getColor(ctx, colorId);
				DrawableCompat.setTint(icon.mutate(), color);
				toolbar.setOverflowIcon(icon);
			}
		}
	}

	public static ViewGroup createSliderView(@NonNull Context ctx, boolean nightMode) {
		return (ViewGroup) getInflater(ctx, nightMode).inflate(R.layout.slider, null, false);
	}

	public static void setupSlider(Slider slider, boolean nightMode, @ColorInt Integer activeColor) {
		setupSlider(slider, nightMode, activeColor, false);
	}

	public static void setupSlider(Slider slider, boolean nightMode,
	                               @ColorInt Integer activeColor, boolean showTicks) {
		Context ctx = slider.getContext();
		if (ctx == null) {
			return;
		}
		int themeId = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		ctx = new ContextThemeWrapper(ctx, themeId);

		// colors
		int[][] states = {
				new int[]{android.R.attr.state_enabled},
				new int[]{-android.R.attr.state_enabled}
		};
		if (activeColor == null) {
			activeColor = AndroidUtils.getColorFromAttr(ctx, R.attr.active_color_basic);
		}
		int activeDisableColor = ColorUtilities.getColorWithAlpha(activeColor, 0.25f);
		ColorStateList activeCsl = new ColorStateList(states, new int[]{activeColor, activeDisableColor});
		int inactiveColor = ColorUtilities.getColorWithAlpha(activeColor, 0.5f);
		int inactiveDisableColor = ContextCompat.getColor(ctx, nightMode ? R.color.icon_color_default_dark : R.color.icon_color_secondary_light);
		ColorStateList inactiveCsl = new ColorStateList(states, new int[]{inactiveColor, inactiveDisableColor});
		slider.setTrackActiveTintList(activeCsl);
		slider.setTrackInactiveTintList(inactiveCsl);
		slider.setHaloTintList(activeCsl);
		slider.setThumbTintList(activeCsl);
		int colorBlack = ContextCompat.getColor(ctx, R.color.color_black);
		int ticksColor = showTicks ?
				(nightMode ? colorBlack : ColorUtilities.getColorWithAlpha(colorBlack, 0.5f)) :
				Color.TRANSPARENT;
		slider.setTickTintList(new ColorStateList(states, new int[]{ticksColor, ticksColor}));

		// sizes
		slider.setThumbRadius(ctx.getResources().getDimensionPixelSize(R.dimen.slider_thumb_size));
		slider.setHaloRadius(ctx.getResources().getDimensionPixelSize(R.dimen.slider_thumb_halo_size));
		slider.setTrackHeight(ctx.getResources().getDimensionPixelSize(R.dimen.slider_track_height));

		// label behavior
		slider.setLabelBehavior(LabelFormatter.LABEL_GONE);
	}

	public static void setupSlider(RangeSlider slider, boolean nightMode,
	                               @ColorInt Integer activeColor, boolean showTicks) {
		Context ctx = slider.getContext();
		if (ctx == null) {
			return;
		}
		int themeId = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		ctx = new ContextThemeWrapper(ctx, themeId);

		// colors
		int[][] states = {
				new int[]{android.R.attr.state_enabled},
				new int[]{-android.R.attr.state_enabled}
		};
		if (activeColor == null) {
			activeColor = AndroidUtils.getColorFromAttr(ctx, R.attr.active_color_basic);
		}
		int activeDisableColor = ColorUtilities.getColorWithAlpha(activeColor, 0.25f);
		ColorStateList activeCsl = new ColorStateList(states, new int[]{activeColor, activeDisableColor});
		int inactiveColor = ContextCompat.getColor(ctx, nightMode ? R.color.icon_color_default_dark : R.color.icon_color_secondary_light);
		ColorStateList inactiveCsl = new ColorStateList(states, new int[]{activeDisableColor, inactiveColor});
		slider.setTrackActiveTintList(activeCsl);
		slider.setTrackInactiveTintList(inactiveCsl);
		slider.setHaloTintList(activeCsl);
		slider.setThumbTintList(activeCsl);
		int colorBlack = ContextCompat.getColor(ctx, R.color.color_black);
		int ticksColor = showTicks ?
				(nightMode ? colorBlack : ColorUtilities.getColorWithAlpha(colorBlack, 0.5f)) :
				Color.TRANSPARENT;
		slider.setTickTintList(new ColorStateList(states, new int[]{ticksColor, ticksColor}));

		// sizes
		slider.setThumbRadius(ctx.getResources().getDimensionPixelSize(R.dimen.slider_thumb_size));
		slider.setHaloRadius(ctx.getResources().getDimensionPixelSize(R.dimen.slider_thumb_halo_size));
		slider.setTrackHeight(ctx.getResources().getDimensionPixelSize(R.dimen.slider_track_height));

		// label behavior
		slider.setLabelBehavior(LabelFormatter.LABEL_GONE);
	}

	public static void setupDialogButton(boolean nightMode, View buttonView, DialogButtonType buttonType, @StringRes int buttonTextId) {
		setupDialogButton(nightMode, buttonView, buttonType, buttonView.getContext().getString(buttonTextId));
	}

	public static void setupDialogButton(boolean nightMode, View buttonView, DialogButtonType buttonType, CharSequence buttonText) {
		setupDialogButton(nightMode, buttonView, buttonType, buttonText, INVALID_ID);
	}

	public static void setupDialogButton(boolean nightMode, View buttonView, DialogButtonType buttonType, CharSequence buttonText, int iconResId) {
		// Base background
		Context ctx = buttonView.getContext();
		int backgroundAttr = buttonType.getBackgroundAttr();
		if (backgroundAttr != INVALID_ID) {
			int backgroundResId = AndroidUtils.resolveAttribute(ctx, buttonType.getBackgroundAttr());
			AndroidUtils.setBackground(ctx, buttonView, backgroundResId);
		}

		// Ripple background
		View buttonContainer = buttonView.findViewById(R.id.button_container);
		int rippleResId = AndroidUtils.resolveAttribute(ctx, buttonType.getRippleAttr());
		AndroidUtils.setBackground(ctx, buttonContainer, rippleResId);

		// Content colors
		ColorStateList colorStateList;
		int contentColorId = AndroidUtils.resolveAttribute(ctx, buttonType.getContentColorAttr());
		if (buttonType == DialogButtonType.TERTIARY) {
			int disabledColor = ColorUtilities.getSecondaryTextColorId(nightMode);
			colorStateList = AndroidUtils.createEnabledColorStateList(ctx, disabledColor, contentColorId);
		} else {
			colorStateList = ContextCompat.getColorStateList(ctx, contentColorId);
		}

		// Button title
		TextViewEx tvTitle = buttonView.findViewById(R.id.button_text);
		tvTitle.setText(buttonText);
		tvTitle.setTextColor(colorStateList);
		tvTitle.setEnabled(buttonView.isEnabled());

		// Button icon
		if (iconResId != INVALID_ID) {
			int contentColor = ColorUtilities.getColor(ctx, contentColorId);
			Drawable icon = AppCompatResources.getDrawable(ctx, iconResId);
			icon = tintDrawable(icon, contentColor);
			tvTitle.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
			tvTitle.setCompoundDrawablePadding(AndroidUtils.dpToPx(ctx, ctx.getResources().getDimension(R.dimen.content_padding_half)));
		}
	}

	public static LayoutInflater getInflater(Context ctx, boolean nightMode) {
		return LayoutInflater.from(getThemedContext(ctx, nightMode));
	}

	public static Context getThemedContext(Context context, boolean nightMode) {
		return getThemedContext(context, nightMode, R.style.OsmandLightTheme, R.style.OsmandDarkTheme);
	}

	public static Context getThemedContext(Context context, boolean nightMode, int lightStyle, int darkStyle) {
		return new ContextThemeWrapper(context, nightMode ? darkStyle : lightStyle);
	}

	public static void setMargins(View v, int s, int t, int e, int b) {
		if (v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
			ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
			AndroidUtils.setMargins(p, s, t, e, b);
			v.requestLayout();
		}
	}

	public static SpannableString createSpannableString(@NonNull String text, int style, @NonNull String... textToStyle) {
		SpannableString spannable = new SpannableString(text);
		for (String t : textToStyle) {
			setSpan(spannable, new StyleSpan(style), text, t);
		}
		return spannable;
	}

	public static SpannableString createCustomFontSpannable(@NonNull Typeface typeface, @NonNull String text, @NonNull String... textToStyle) {
		SpannableString spannable = new SpannableString(text);
		for (String s : textToStyle) {
			setSpan(spannable, new CustomTypefaceSpan(typeface), text, s);
		}
		return spannable;
	}

	public static SpannableString createUrlSpannable(@NonNull String text, @NonNull String url) {
		SpannableString spannable = new SpannableString(text);
		setSpan(spannable, new CustomURLSpan(url), text, url);
		return spannable;
	}

	private static void setSpan(@NonNull SpannableString spannable,
	                            @NonNull Object styleSpan,
	                            @NonNull String text, @NonNull String textToSpan) {
		try {
			int start = text.indexOf(textToSpan);
			int end = start + textToSpan.length();
			spannable.setSpan(styleSpan, start, end, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
		} catch (RuntimeException e) {
			LOG.error("Error trying to find index of " + textToSpan + " " + e);
		}
	}

	public static void setupClickableText(OsmandApplication app,
	                                      TextView textView,
	                                      String text,
	                                      String clickableText,
	                                      boolean isNightMode,
	                                      @NonNull CallbackWithObject<Void> onClickedText) {
		SpannableString spannableString = new SpannableString(text);
		ClickableSpan clickableSpan = new ClickableSpan() {
			@Override
			public void onClick(@NonNull View view) {
				onClickedText.processResult(null);
			}

			@Override
			public void updateDrawState(@NonNull TextPaint ds) {
				super.updateDrawState(ds);
				ds.setUnderlineText(false);
			}
		};
		try {
			int startIndex = text.indexOf(clickableText);
			spannableString.setSpan(clickableSpan, startIndex, startIndex + clickableText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			textView.setText(spannableString);
			textView.setMovementMethod(LinkMovementMethod.getInstance());
			textView.setHighlightColor(ColorUtilities.getActiveColor(app, isNightMode));
		} catch (RuntimeException e) {
			LOG.error("Error trying to find index of " + clickableText + " " + e);
		}
	}
}
