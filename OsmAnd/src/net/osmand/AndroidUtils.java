package net.osmand;


import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.StatFs;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.style.CharacterStyle;
import android.text.style.ImageSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.text.TextUtilsCompat;
import androidx.core.view.ViewCompat;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static android.content.Context.POWER_SERVICE;
import static android.util.TypedValue.COMPLEX_UNIT_DIP;
import static android.util.TypedValue.COMPLEX_UNIT_SP;

public class AndroidUtils {
	private static final Log LOG = PlatformUtil.getLog(AndroidUtils.class);

	public static final String STRING_PLACEHOLDER = "%s";
	public static final MessageFormat formatKb = new MessageFormat("{0, number,##.#}", Locale.US);
	public static final MessageFormat formatGb = new MessageFormat("{0, number,#.##}", Locale.US);
	public static final MessageFormat formatMb = new MessageFormat("{0, number,##.#}", Locale.US);

	/**
	 * @param context
	 * @return true if Hardware keyboard is available
	 */

	public static boolean isHardwareKeyboardAvailable(Context context) {
		return context.getResources().getConfiguration().keyboard != Configuration.KEYBOARD_NOKEYS;
	}

	public static void softKeyboardDelayed(final Activity activity, final View view) {
		view.post(new Runnable() {
			@Override
			public void run() {
				if (!isHardwareKeyboardAvailable(view.getContext())) {
					showSoftKeyboard(activity,view);
				}
			}
		});
	}

	public static void showSoftKeyboard(final Activity activity, final View view) {
		InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				KeyguardManager keyguardManager = (KeyguardManager) view.getContext().getSystemService(Context.KEYGUARD_SERVICE);
				keyguardManager.requestDismissKeyguard(activity,null);
			}
			imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
		}
	}

	public static void hideSoftKeyboard(final Activity activity, final View input) {
		InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
		if (inputMethodManager != null) {
			if (input != null) {
				IBinder windowToken = input.getWindowToken();
				if (windowToken != null) {
					inputMethodManager.hideSoftInputFromWindow(windowToken, 0);
				}
			}
		}

	}

	public static Bitmap scaleBitmap(Bitmap bm, int newWidth, int newHeight, boolean keepOriginalBitmap) {
		int width = bm.getWidth();
		int height = bm.getHeight();
		float scaleWidth = ((float) newWidth) / width;
		float scaleHeight = ((float) newHeight) / height;
		Matrix matrix = new Matrix();
		matrix.postScale(scaleWidth, scaleHeight);
		Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
		if (!keepOriginalBitmap) {
			bm.recycle();
		}
		return resizedBitmap;
	}

	public static Bitmap createScaledBitmap(Drawable drawable, int width, int height) {
		return scaleBitmap(drawableToBitmap(drawable), width, height, false);
	}

	public static ColorStateList createBottomNavColorStateList(Context ctx, boolean nightMode) {
		return AndroidUtils.createCheckedColorStateList(ctx, nightMode,
				R.color.icon_color_default_light, R.color.wikivoyage_active_light,
				R.color.icon_color_default_light, R.color.wikivoyage_active_dark);
	}

	public static String addColon(OsmandApplication app, @StringRes int stringRes) {
		return app.getString(R.string.ltr_or_rtl_combine_via_colon, app.getString(stringRes), "").trim();
	}

	public static Uri getUriForFile(Context context, File file) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
			return Uri.fromFile(file);
		} else {
			return FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
		}
	}

	public static boolean isIntentSafe(Context context, Intent intent) {
		return intent.resolveActivity(context.getPackageManager()) != null;
	}

	public static boolean isActivityNotDestroyed(@Nullable Activity activity) {
		if (Build.VERSION.SDK_INT >= 17) {
			return activity != null && !activity.isFinishing() && !activity.isDestroyed();
		}
		return activity != null && !activity.isFinishing();
	}

	public static Spannable replaceCharsWithIcon(String text, Drawable icon, String[] chars) {
		Spannable spannable = new SpannableString(text);
		for (String entry : chars) {
			int i = text.indexOf(entry, 0);
			while (i < text.length() && i != -1) {
				ImageSpan span = new ImageSpan(icon) {
					public void draw(Canvas canvas, CharSequence text, int start, int end,
									 float x, int top, int y, int bottom, Paint paint) {
						Drawable drawable = getDrawable();
						canvas.save();
						int transY = bottom - drawable.getBounds().bottom;
						transY -= paint.getFontMetricsInt().descent / 2;
						canvas.translate(x, transY);
						drawable.draw(canvas);
						canvas.restore();
					}
				};
				spannable.setSpan(span, i, i + entry.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				i = text.indexOf(entry, i + entry.length());
			}
		}
		return spannable;
	}

	public static void removeLinkUnderline(TextView textView) {
		Spannable s = new SpannableString(textView.getText());
		for (URLSpan span : s.getSpans(0, s.length(), URLSpan.class)) {
			int start = s.getSpanStart(span);
			int end = s.getSpanEnd(span);
			s.removeSpan(span);
			span = new URLSpan(span.getURL()) {
				@Override
				public void updateDrawState(TextPaint ds) {
					super.updateDrawState(ds);
					ds.setUnderlineText(false);
				}
			};
			s.setSpan(span, start, end, 0);
		}
		textView.setText(s);
	}

	public static String formatDate(Context ctx, long time) {
		return DateFormat.getDateFormat(ctx).format(new Date(time));
	}

	public static String formatDateTime(Context ctx, long time) {
		Date d = new Date(time);
		return DateFormat.getDateFormat(ctx).format(d) +
				" " + DateFormat.getTimeFormat(ctx).format(d);
	}

	public static String formatTime(Context ctx, long time) {
		return DateFormat.getTimeFormat(ctx).format(new Date(time));
	}


	public static String formatSize(Context ctx, long sizeBytes) {
		int sizeKb = (int) ((sizeBytes + 512) >> 10);
		if (sizeKb > 0) {

			String size = "";
			String numSuffix = "MB";
			if (sizeKb > 1 << 20) {
				size = formatGb.format(new Object[]{(float) sizeKb / (1 << 20)});
				numSuffix = "GB";
			} else if (sizeBytes > (100 * (1 << 10))) {
				size = formatMb.format(new Object[]{(float) sizeBytes / (1 << 20)});
			} else {
				size = formatKb.format(new Object[]{(float) sizeBytes / (1 << 10)});
				numSuffix = "kB";
			}
			if(ctx == null) {
				return size + " " + numSuffix;
			}
			return ctx.getString(R.string.ltr_or_rtl_combine_via_space, size, numSuffix);
		}
		return "";
	}

	public static String getFreeSpace(Context ctx, File dir) {
		long size = AndroidUtils.getAvailableSpace(dir);
		return AndroidUtils.formatSize(ctx, size);
	}

	public static View findParentViewById(View view, int id) {
		ViewParent viewParent = view.getParent();

		while (viewParent != null && viewParent instanceof View) {
			View parentView = (View) viewParent;
			if (parentView.getId() == id)
				return parentView;

			viewParent = parentView.getParent();
		}

		return null;
	}

	public static ColorStateList createCheckedColorStateList(Context ctx, @ColorRes int normal, @ColorRes int checked) {
		return createCheckedColorStateList(ctx, false, normal, checked, 0, 0);
	}

	public static ColorStateList createCheckedColorStateList(Context ctx, boolean night,
															 @ColorRes int lightNormal, @ColorRes int lightChecked,
															 @ColorRes int darkNormal, @ColorRes int darkChecked) {
		return createColorStateList(ctx, night, android.R.attr.state_checked,
				lightNormal, lightChecked, darkNormal, darkChecked);
	}

	public static ColorStateList createEnabledColorStateList(Context ctx, @ColorRes int normal, @ColorRes int pressed) {
		return createEnabledColorStateList(ctx, false, normal, pressed, 0, 0);
	}

	public static ColorStateList createEnabledColorStateList(Context ctx, boolean night,
	                                                         @ColorRes int lightNormal, @ColorRes int lightPressed,
	                                                         @ColorRes int darkNormal, @ColorRes int darkPressed) {
		return createColorStateList(ctx, night, android.R.attr.state_enabled,
				lightNormal, lightPressed, darkNormal, darkPressed);
	}

	public static ColorStateList createPressedColorStateList(Context ctx, @ColorRes int normal, @ColorRes int pressed) {
		return createPressedColorStateList(ctx, false, normal, pressed, 0, 0);
	}

	public static ColorStateList createPressedColorStateList(Context ctx, boolean night,
															 @ColorRes int lightNormal, @ColorRes int lightPressed,
															 @ColorRes int darkNormal, @ColorRes int darkPressed) {
		return createColorStateList(ctx, night, android.R.attr.state_pressed,
				lightNormal, lightPressed, darkNormal, darkPressed);
	}

	private static ColorStateList createColorStateList(Context ctx, boolean night, int state,
													   @ColorRes int lightNormal, @ColorRes int lightState,
													   @ColorRes int darkNormal, @ColorRes int darkState) {
		return new ColorStateList(
				new int[][]{
						new int[]{state},
						new int[]{}
				},
				new int[]{
						ContextCompat.getColor(ctx, night ? darkState : lightState),
						ContextCompat.getColor(ctx, night ? darkNormal : lightNormal)
				}
		);
	}

	public static ColorStateList createColorStateList(Context ctx, boolean night) {
		return new ColorStateList(
				new int[][] {
						new int[] {-android.R.attr.state_enabled}, // disabled
						new int[] {android.R.attr.state_checked},
						new int[] {}
				},
				new int[] {
						ContextCompat.getColor(ctx, night? R.color.text_color_secondary_dark : R.color.text_color_secondary_light),
						ContextCompat.getColor(ctx, night? R.color.active_color_primary_dark : R.color.active_color_primary_light),
						ContextCompat.getColor(ctx, night? R.color.text_color_secondary_dark : R.color.text_color_secondary_light)}
		);
	}

	public static ColorStateList createCheckedColorIntStateList(@ColorInt int normal, @ColorInt int checked) {
		return createCheckedColorIntStateList(false, normal, checked, 0, 0);
	}

	public static ColorStateList createCheckedColorIntStateList(boolean night,
															 @ColorInt int lightNormal, @ColorInt int lightChecked,
															 @ColorInt int darkNormal, @ColorInt int darkChecked) {
		return createColorIntStateList(night, android.R.attr.state_checked,
				lightNormal, lightChecked, darkNormal, darkChecked);
	}

	public static ColorStateList createEnabledColorIntStateList(@ColorInt int normal, @ColorInt int pressed) {
		return createEnabledColorIntStateList(false, normal, pressed, 0, 0);
	}

	public static ColorStateList createEnabledColorIntStateList(boolean night,
															 @ColorInt int lightNormal, @ColorInt int lightPressed,
															 @ColorInt int darkNormal, @ColorInt int darkPressed) {
		return createColorIntStateList(night, android.R.attr.state_enabled,
				lightNormal, lightPressed, darkNormal, darkPressed);
	}

	private static ColorStateList createColorIntStateList(boolean night, int state,
													   @ColorInt int lightNormal, @ColorInt int lightState,
													   @ColorInt int darkNormal, @ColorInt int darkState) {
		return new ColorStateList(
				new int[][]{
						new int[]{state},
						new int[]{}
				},
				new int[]{
						night ? darkState : lightState,
						night ? darkNormal : lightNormal
				}
		);
	}

	public static StateListDrawable createCheckedStateListDrawable(Drawable normal, Drawable checked) {
		return createStateListDrawable(normal, checked, android.R.attr.state_checked);
	}

	public static StateListDrawable createPressedStateListDrawable(Drawable normal, Drawable pressed) {
		return createStateListDrawable(normal, pressed, android.R.attr.state_pressed);
	}

	public static StateListDrawable createEnabledStateListDrawable(Drawable disabled, Drawable enabled) {
		return createStateListDrawable(disabled, enabled, android.R.attr.state_enabled);
	}

	private static StateListDrawable createStateListDrawable(Drawable normal, Drawable stateDrawable, int state) {
		StateListDrawable res = new StateListDrawable();
		res.addState(new int[]{state}, stateDrawable);
		res.addState(new int[]{}, normal);
		return res;
	}

	public static LayerDrawable createProgressDrawable(@ColorInt int bgColor, @ColorInt int progressColor) {
		ShapeDrawable bg = new ShapeDrawable();
		bg.getPaint().setColor(bgColor);

		ShapeDrawable progress = new ShapeDrawable();
		progress.getPaint().setColor(progressColor);

		LayerDrawable res = new LayerDrawable(new Drawable[]{
				bg,
				new ClipDrawable(progress, Gravity.START, ClipDrawable.HORIZONTAL)
		});

		res.setId(0, android.R.id.background);
		res.setId(1, android.R.id.progress);

		return res;
	}

	public static void setBackground(Context ctx, View view, boolean night, int lightResId, int darkResId) {
		setBackground(view, AppCompatResources.getDrawable(ctx, night ? darkResId : lightResId));
	}

	public static void setBackground(View view, Drawable drawable) {
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
			view.setBackground(drawable);
		} else {
			view.setBackgroundDrawable(drawable);
		}
	}

	public static void setForeground(Context ctx, View view, boolean night, int lightResId, int darkResId) {
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
			view.setForeground(AppCompatResources.getDrawable(ctx, night ? darkResId : lightResId));
		} else if (view instanceof FrameLayout) {
			((FrameLayout) view).setForeground(AppCompatResources.getDrawable(ctx, night ? darkResId : lightResId));
		}
	}

	public static void updateImageButton(OsmandApplication ctx, ImageButton button,
	                                     @DrawableRes int iconLightId, @DrawableRes int iconDarkId,
	                                     @DrawableRes int bgLightId, @DrawableRes int bgDarkId, boolean night) {
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
			button.setBackground(ctx.getUIUtilities().getIcon(night ? bgDarkId : bgLightId));
		} else {
			button.setBackgroundDrawable(ctx.getUIUtilities().getIcon(night ? bgDarkId : bgLightId));
		}
		int btnSizePx = button.getLayoutParams().height;
		int iconSizePx = ctx.getResources().getDimensionPixelSize(R.dimen.map_widget_icon);
		int iconPadding = (btnSizePx - iconSizePx) / 2;
		button.setPadding(iconPadding, iconPadding, iconPadding, iconPadding);
		button.setScaleType(ImageView.ScaleType.FIT_CENTER);
		button.setImageDrawable(ctx.getUIUtilities().getMapIcon(night ? iconDarkId : iconLightId, !night));
	}

	public static void setDashButtonBackground(Context ctx, View view, boolean night) {
		setBackground(ctx, view, night, R.drawable.dashboard_button_light, R.drawable.dashboard_button_dark);
	}

	public static void setBackgroundColor(Context ctx, View view, boolean night, int lightResId, int darkResId) {
		view.setBackgroundColor(ctx.getResources().getColor(night ? darkResId : lightResId));
	}

	public static void setListItemBackground(Context ctx, View view, boolean night) {
		setBackgroundColor(ctx, view, night, R.color.list_background_color_light, R.color.list_background_color_dark);
	}

	public static void setListBackground(Context ctx, View view, boolean night) {
		setBackgroundColor(ctx, view, night, R.color.activity_background_color_light, R.color.activity_background_color_dark);
	}

	public static void setTextPrimaryColor(Context ctx, TextView textView, boolean night) {
		textView.setTextColor(night ?
				ctx.getResources().getColor(R.color.text_color_primary_dark)
				: ctx.getResources().getColor(R.color.text_color_primary_light));
	}

	public static void setTextSecondaryColor(Context ctx, TextView textView, boolean night) {
		textView.setTextColor(night ?
				ctx.getResources().getColor(R.color.text_color_secondary_dark)
				: ctx.getResources().getColor(R.color.text_color_secondary_light));
	}

	public static void setHintTextSecondaryColor(Context ctx, TextView textView, boolean night) {
		textView.setHintTextColor(night ?
				ctx.getResources().getColor(R.color.text_color_secondary_dark)
				: ctx.getResources().getColor(R.color.text_color_secondary_light));
	}

	@ColorRes
	public static int getPrimaryTextColorId(boolean nightMode) {
		return nightMode ? R.color.text_color_primary_dark : R.color.text_color_primary_light;
	}

	@ColorRes
	public static int getSecondaryTextColorId(boolean nightMode) {
		return nightMode ? R.color.text_color_secondary_dark : R.color.text_color_secondary_light;
	}

	public static int getTextMaxWidth(float textSize, List<String> titles) {
		int width = 0;
		for (String title : titles) {
			int titleWidth = getTextWidth(textSize, title);
			if (titleWidth > width) {
				width = titleWidth;
			}
		}
		return width;
	}

	public static int getTextWidth(float textSize, String text) {
		Paint paint = new Paint();
		paint.setTextSize(textSize);
		return (int) paint.measureText(text);
	}

	public static int getTextHeight(Paint paint) {
		Paint.FontMetrics fm = paint.getFontMetrics();
		float height = fm.bottom - fm.top;
		return (int) height;
	}

	public static int dpToPx(Context ctx, float dp) {
		Resources r = ctx.getResources();
		return (int) TypedValue.applyDimension(
				COMPLEX_UNIT_DIP,
				dp,
				r.getDisplayMetrics()
		);
	}

	public static int spToPx(Context ctx, float sp) {
		Resources r = ctx.getResources();
		return (int) TypedValue.applyDimension(
				COMPLEX_UNIT_SP,
				sp,
				r.getDisplayMetrics()
		);
	}

	@ColorInt
	public static int getColorFromAttr(@NonNull Context ctx, @AttrRes int colorAttribute) {
		TypedValue typedValue = new TypedValue();
		ctx.getTheme().resolveAttribute(colorAttribute, typedValue, true);
		return typedValue.data;
	}

	public static int resolveAttribute(Context ctx, int attribute) {
		TypedValue outValue = new TypedValue();
		ctx.getTheme().resolveAttribute(attribute, outValue, true);
		return outValue.resourceId;
	}

	public static float getFloatValueFromRes(Context ctx, int resId) {
		TypedValue outValue = new TypedValue();
		ctx.getResources().getValue(resId, outValue, true);
		return outValue.getFloat();
	}

	public static int getDrawableId(OsmandApplication app, String id) {
		if (!Algorithms.isEmpty(id)) {
			return app.getResources().getIdentifier(id, "drawable", app.getPackageName());
		}
		return 0;
	}

	public static int getStatusBarHeight(Context ctx) {
		int result = 0;
		int resourceId = ctx.getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0) {
			result = ctx.getResources().getDimensionPixelSize(resourceId);
		}
		return result;
	}

	public static void addStatusBarPadding21v(Context ctx, View view) {
		if (Build.VERSION.SDK_INT >= 21) {
			int paddingLeft = view.getPaddingLeft();
			int paddingTop = view.getPaddingTop();
			int paddingRight = view.getPaddingRight();
			int paddingBottom = view.getPaddingBottom();
			view.setPadding(paddingLeft, paddingTop + getStatusBarHeight(ctx), paddingRight, paddingBottom);
		}
	}

	public static int getNavBarHeight(Context ctx) {
		if (!hasNavBar(ctx)) {
			return 0;
		}
		boolean landscape = ctx.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
		boolean isSmartphone = ctx.getResources().getConfiguration().smallestScreenWidthDp < 600;
		if (isSmartphone && landscape) {
			return 0;
		}
		int id = ctx.getResources().getIdentifier(landscape ? "navigation_bar_height_landscape" : "navigation_bar_height", "dimen", "android");
		if (id > 0) {
			return ctx.getResources().getDimensionPixelSize(id);
		}
		return 0;
	}

	public static boolean hasNavBar(Context ctx) {
		int id = ctx.getResources().getIdentifier("config_showNavigationBar", "bool", "android");
		return id > 0 && ctx.getResources().getBoolean(id);
	}

	public static int getScreenHeight(Activity activity) {
		DisplayMetrics dm = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
		return dm.heightPixels;
	}

	public static int getScreenWidth(Activity activity) {
		DisplayMetrics dm = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
		return dm.widthPixels;
	}

	public static boolean isValidEmail(CharSequence target) {
		return !TextUtils.isEmpty(target) && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
	}

	public static PointF centroidForPoly(PointF[] points) {
		float centroidX = 0, centroidY = 0;

		for (PointF point : points) {
			centroidX += point.x / points.length;
			centroidY += point.y / points.length;
		}
		return new PointF(centroidX, centroidY);
	}

	public static void showNavBar(Activity activity) {
		if (Build.VERSION.SDK_INT >= 19 && !isNavBarVisible(activity)) {
			switchNavBarVisibility(activity);
		}
	}

	public static void hideNavBar(Activity activity) {
		if (Build.VERSION.SDK_INT >= 19 && isNavBarVisible(activity)) {
			switchNavBarVisibility(activity);
		}
	}

	public static boolean isNavBarVisible(Activity activity) {
		if (Build.VERSION.SDK_INT >= 19) {
			int uiOptions = activity.getWindow().getDecorView().getSystemUiVisibility();
			return !((uiOptions | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == uiOptions);
		}
		return true;
	}

	public static void switchNavBarVisibility(Activity activity) {
		if (Build.VERSION.SDK_INT < 19) {
			return;
		}
		View decorView = activity.getWindow().getDecorView();
		int uiOptions = decorView.getSystemUiVisibility();
		uiOptions ^= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
		uiOptions ^= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
		decorView.setSystemUiVisibility(uiOptions);
	}

	public static int[] getCenterViewCoordinates(View view) {
		int[] coordinates = new int[2];
		view.getLocationOnScreen(coordinates);
		coordinates[0] += view.getWidth() / 2;
		coordinates[1] += view.getHeight() / 2;
		return coordinates;
	}

	public static void enterToFullScreen(Activity activity, View view) {
		if (Build.VERSION.SDK_INT >= 21) {
			requestLayout(view);
			activity.getWindow().getDecorView()
					.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
		}
	}

	public static void exitFromFullScreen(Activity activity, View view) {
		if (Build.VERSION.SDK_INT >= 21) {
			requestLayout(view);
			activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
		}
	}

	private static void requestLayout(final View view) {
		if (view != null) {
			ViewTreeObserver vto = view.getViewTreeObserver();
			vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

				@Override
				public void onGlobalLayout() {
					ViewTreeObserver obs = view.getViewTreeObserver();
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
						obs.removeOnGlobalLayoutListener(this);
					} else {
						obs.removeGlobalOnLayoutListener(this);
					}
					view.requestLayout();
				}
			});
		}
	}

	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
		List<Map.Entry<K, V>> list = new LinkedList<>(map.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
			public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
				return (o1.getValue()).compareTo(o2.getValue());
			}
		});

		Map<K, V> result = new LinkedHashMap<>();
		for (Map.Entry<K, V> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}

	public static boolean isScreenOn(Context context) {
		PowerManager powerManager = (PowerManager) context.getSystemService(POWER_SERVICE);
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH && powerManager.isInteractive()
				|| Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT_WATCH && powerManager.isScreenOn();
	}

	public static boolean isScreenLocked(Context context) {
		KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
		return keyguardManager.inKeyguardRestrictedInputMode();
	}

	public static CharSequence getStyledString(CharSequence baseString, CharSequence stringToInsertAndStyle, int typefaceStyle) {

		if (typefaceStyle == Typeface.NORMAL || typefaceStyle == Typeface.BOLD
				|| typefaceStyle == Typeface.ITALIC || typefaceStyle == Typeface.BOLD_ITALIC
				|| baseString.toString().contains(STRING_PLACEHOLDER)) {

			return getStyledString(baseString, stringToInsertAndStyle, null, new StyleSpan(typefaceStyle));
		} else {
			return baseString;
		}
	}

	public static void setCompoundDrawablesWithIntrinsicBounds(@NonNull TextView tv, Drawable start, Drawable top, Drawable end, Drawable bottom){
		if (isSupportRTL()) {
			tv.setCompoundDrawablesRelativeWithIntrinsicBounds(start, top, end, bottom);
		} else {
			tv.setCompoundDrawablesWithIntrinsicBounds(start, top, end, bottom);
		}
	}

	public static Drawable[] getCompoundDrawables(@NonNull TextView tv){
		if (isSupportRTL()) {
			return tv.getCompoundDrawablesRelative();
		}
		return tv.getCompoundDrawables();
	}

	public static void setPadding(View view, int start, int top, int end, int bottom) {
		if (isSupportRTL()) {
			view.setPaddingRelative(start, top, end, bottom);
		} else {
			view.setPadding(start, top, end, bottom);
		}
	}

	public static void setMargins(ViewGroup.MarginLayoutParams layoutParams, int start, int top, int end, int bottom) {
		layoutParams.setMargins(start, top, end, bottom);
		if (isSupportRTL()) {
			layoutParams.setMarginStart(start);
			layoutParams.setMarginEnd(end);
		}
	}

	public static void setTextDirection(@NonNull TextView tv, boolean rtl) {
		if (isSupportRTL()) {
			int textDirection = rtl ? View.TEXT_DIRECTION_RTL : View.TEXT_DIRECTION_LTR;
			tv.setTextDirection(textDirection);
		}
	}

	public static int getLayoutDirection(@NonNull Context ctx) {
		Locale currentLocale = ctx.getResources().getConfiguration().locale;
		return TextUtilsCompat.getLayoutDirectionFromLocale(currentLocale);
	}

	public static int getNavigationIconResId(@NonNull Context ctx) {
		return isLayoutRtl(ctx) ? R.drawable.ic_arrow_forward : R.drawable.ic_arrow_back;
	}

	public static Drawable getDrawableForDirection(@NonNull Context ctx,
	                                               @NonNull Drawable drawable) {
		return isLayoutRtl(ctx) ? getMirroredDrawable(ctx, drawable) : drawable;
	}

	public static Drawable getMirroredDrawable(@NonNull Context ctx,
	                                           @NonNull Drawable drawable) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			drawable.setAutoMirrored(true);
			return drawable;
		}
		Bitmap bitmap = drawableToBitmap(drawable);
		return new BitmapDrawable(ctx.getResources(), flipBitmapHorizontally(bitmap));
	}

	public static Bitmap drawableToBitmap(Drawable drawable) {
		if (drawable instanceof BitmapDrawable) {
			BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
			if(bitmapDrawable.getBitmap() != null) {
				return bitmapDrawable.getBitmap();
			}
		}

		Bitmap bitmap = null;
		if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
			bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
		} else {
			bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
		}

		Canvas canvas = new Canvas(bitmap);
		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		drawable.draw(canvas);
		return bitmap;
	}

	public static Bitmap flipBitmapHorizontally(Bitmap source) {
		Matrix matrix = new Matrix();
		matrix.preScale(-1.0f, 1.0f, source.getWidth() / 2f, source.getHeight() / 2f);
		return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
	}

	public static void setTextHorizontalGravity(@NonNull TextView tv, int hGravity) {
		if (tv.getContext() != null) {
			boolean isLayoutRtl = AndroidUtils.isLayoutRtl(tv.getContext());
			int gravity = Gravity.LEFT;
			if (isLayoutRtl && (hGravity == Gravity.START)
					|| !isLayoutRtl && hGravity == Gravity.END) {
				gravity = Gravity.RIGHT;
			}
			tv.setGravity(gravity);
		}
	}

	public static boolean isSupportRTL() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1;
	}

	public static boolean isLayoutRtl(Context ctx) {
		return isSupportRTL() && getLayoutDirection(ctx) == ViewCompat.LAYOUT_DIRECTION_RTL;
	}

	public static ArrayList<View> getChildrenViews(ViewGroup vg) {
		ArrayList<View> result = new ArrayList<>();
		for (int i = 0; i < vg.getChildCount(); i++) {
			View child = vg.getChildAt(i);
			result.add(child);
		}
		return result;
	}

	public static long getAvailableSpace(@NonNull OsmandApplication app) {
		return getAvailableSpace(app.getAppPath(null));
	}

	public static long getTotalSpace(@NonNull OsmandApplication app) {
		return getTotalSpace(app.getAppPath(null));
	}

	public static long getAvailableSpace(@Nullable File dir) {
		if (dir != null && dir.canRead()) {
			try {
				StatFs fs = new StatFs(dir.getAbsolutePath());
				if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR2) {
					return fs.getAvailableBlocksLong() * fs.getBlockSizeLong();
				} else {
					return fs.getAvailableBlocks() * fs.getBlockSize();
				}
			} catch (IllegalArgumentException e) {
				LOG.error(e);
			}
		}
		return -1;
	}

	public static long getTotalSpace(@Nullable File dir) {
		if (dir != null && dir.canRead()) {
			try {
				StatFs fs = new StatFs(dir.getAbsolutePath());
				if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR2) {
					return fs.getBlockCountLong() * fs.getBlockSizeLong();
				} else {
					return fs.getBlockCount() * fs.getBlockSize();
				}
			} catch (IllegalArgumentException e) {
				LOG.error(e);
			}
		}
		return -1;
	}

	public static float getFreeSpaceGb(File dir) {
		if (dir.canRead()) {
			try {
				StatFs fs = new StatFs(dir.getAbsolutePath());
				return (float) (fs.getBlockSize()) * fs.getAvailableBlocks() / (1 << 30);
			} catch (IllegalArgumentException e) {
				LOG.error(e);
			}
		}
		return -1;
	}

	public static float getTotalSpaceGb(File dir) {
		if (dir.canRead()) {
			return (float) (dir.getTotalSpace()) / (1 << 30);
		}
		return -1;
	}

	public static CharSequence getStyledString(CharSequence baseString, CharSequence stringToInsertAndStyle,
											   CharacterStyle baseStyle, CharacterStyle replaceStyle) {
		int indexOfPlaceholder = baseString.toString().indexOf(STRING_PLACEHOLDER);
		if (replaceStyle != null || baseStyle != null || indexOfPlaceholder != -1) {
			String nStr = baseString.toString().replace(STRING_PLACEHOLDER, stringToInsertAndStyle);
			SpannableStringBuilder ssb = new SpannableStringBuilder(nStr);
			if(baseStyle != null) {
				if(indexOfPlaceholder > 0) {
					ssb.setSpan(baseStyle, 0, indexOfPlaceholder, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
				if(indexOfPlaceholder + stringToInsertAndStyle.length() < nStr.length()) {
					ssb.setSpan(baseStyle,
							indexOfPlaceholder + stringToInsertAndStyle.length(),
							nStr.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
			}
			if(replaceStyle != null) {
				ssb.setSpan(replaceStyle, indexOfPlaceholder,
						indexOfPlaceholder + stringToInsertAndStyle.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			return ssb;
		} else {
			return baseString;
		}
	}

	public static boolean isRTL() {
		return TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault()) == ViewCompat.LAYOUT_DIRECTION_RTL;
	}

	public static String createNewFileName(String oldName) {
		int firstDotIndex = oldName.indexOf('.');
		String nameWithoutExt = oldName.substring(0, firstDotIndex);
		String ext = oldName.substring(firstDotIndex);

		StringBuilder numberSection = new StringBuilder();
		int i = nameWithoutExt.length() - 1;
		boolean hasNameNumberSection = false;
		do {
			char c = nameWithoutExt.charAt(i);
			if (Character.isDigit(c)) {
				numberSection.insert(0, c);
			} else if(Character.isSpaceChar(c) && numberSection.length() > 0) {
				hasNameNumberSection = true;
				break;
			} else {
				break;
			}
			i--;
		} while (i >= 0);
		int newNumberValue = Integer.parseInt(hasNameNumberSection ? numberSection.toString() : "0") + 1;

		String newName;
		if (newNumberValue == 1) {
			newName = nameWithoutExt + " " + newNumberValue + ext;
		} else {
			newName = nameWithoutExt.substring(0, i) + " " + newNumberValue + ext;
		}

		return newName;
	}

	public static StringBuilder formatWarnings(List<String> warnings) {
		StringBuilder builder = new StringBuilder();
		boolean f = true;
		for (String w : warnings) {
			if (f) {
				f = false;
			} else {
				builder.append('\n');
			}
			builder.append(w);
		}
		return builder;
	}

	public static String getRoutingStringPropertyName(Context ctx, String propertyName, String defValue) {
		String value = getStringByProperty(ctx, "routing_attr_" + propertyName + "_name");
		return value != null ? value : defValue;
	}

	public static String getRoutingStringPropertyDescription(Context ctx, String propertyName, String defValue) {
		String value = getStringByProperty(ctx, "routing_attr_" + propertyName + "_description");
		return value != null ? value : defValue;
	}

	public static String getRenderingStringPropertyName(Context ctx, String propertyName, String defValue) {
		String value = getStringByProperty(ctx, "rendering_attr_" + propertyName + "_name");
		return value != null ? value : defValue;
	}

	public static String getRenderingStringPropertyDescription(Context ctx, String propertyName, String defValue) {
		String value = getStringByProperty(ctx, "rendering_attr_" + propertyName + "_description");
		return value != null ? value : defValue;
	}

	public static String getIconStringPropertyName(Context ctx, String propertyName) {
		String value = getStringByProperty(ctx, "icon_group_" + propertyName);
		return value != null ? value : propertyName;
	}

	public static String getRenderingStringPropertyValue(Context ctx, String propertyValue) {
		if (propertyValue == null) {
			return "";
		}
		String propertyValueReplaced = propertyValue.replaceAll("\\s+", "_");
		String value = getStringByProperty(ctx, "rendering_value_" + propertyValueReplaced + "_name");
		return value != null ? value : propertyValue;
	}

	public static String getStringRouteInfoPropertyValue(Context ctx, String propertyValue) {
		if (propertyValue == null) {
			return "";
		}
		String propertyValueReplaced = propertyValue.replaceAll("\\s+", "_");
		String value = getStringByProperty(ctx, "routeInfo_" + propertyValueReplaced + "_name");
		return value != null ? value : propertyValue;
	}


	public static String getActivityTypeStringPropertyName(Context ctx, String propertyName, String defValue) {
		String value = getStringByProperty(ctx, "activity_type_" + propertyName + "_name");
		return value != null ? value : defValue;
	}

	private static String getStringByProperty(@NonNull Context ctx, @NonNull String property) {
		try {
			Field field = R.string.class.getField(property);
			return getStringForField(ctx, field);
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
		return null;
	}

	private static String getStringForField(@NonNull Context ctx, @Nullable Field field) throws IllegalAccessException {
		if (field != null) {
			Integer in = (Integer) field.get(null);
			return ctx.getString(in);
		}
		return null;
	}
}