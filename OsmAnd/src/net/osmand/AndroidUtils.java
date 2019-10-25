package net.osmand;


import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.StatFs;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.text.TextUtilsCompat;
import android.support.v4.view.ViewCompat;
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
import android.view.ViewParent;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import net.osmand.plus.R;
import net.osmand.plus.download.DownloadActivity;

import java.io.File;
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

	public static final String STRING_PLACEHOLDER = "%s";
	
	/**
	 * @param context
	 * @return true if Hardware keyboard is available
	 */
	
	public static boolean isHardwareKeyboardAvailable(Context context) {
		return context.getResources().getConfiguration().keyboard != Configuration.KEYBOARD_NOKEYS;
	}

	public static void softKeyboardDelayed(final View view) {
		view.post(new Runnable() {
			@Override
			public void run() {
				if (!isHardwareKeyboardAvailable(view.getContext())) {
					showSoftKeyboard(view);
				}
			}
		});
	}

	public static void showSoftKeyboard(final View view) {
		InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null) {
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

	public static ColorStateList createBottomNavColorStateList(Context ctx, boolean nightMode) {
		return AndroidUtils.createCheckedColorStateList(ctx, nightMode,
				R.color.icon_color_default_light, R.color.wikivoyage_active_light,
				R.color.icon_color_default_light, R.color.wikivoyage_active_dark);
	}

	public static String trimExtension(String src) {
		if (src != null) {
			int index = src.lastIndexOf('.');
			if (index != -1) {
				return src.substring(0, index);
			}
		}
		return src;
	}

	public static Uri getUriForFile(Context context, File file) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
			return Uri.fromFile(file);
		} else {
			return FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
		}
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

	public static String formatSize(long sizeBytes) {
		int sizeKb = (int) ((sizeBytes + 512) >> 10);
		if (sizeKb > 0) {
			if (sizeKb > 1 << 20) {
				return DownloadActivity.formatGb.format(new Object[]{(float) sizeKb / (1 << 20)});
			} else if (sizeBytes > (100 * (1 << 10))) {
				return DownloadActivity.formatMb.format(new Object[]{(float) sizeBytes / (1 << 20)});
			} else {
				return DownloadActivity.formatKb.format(new Object[]{(float) sizeBytes / (1 << 10)});
			}
		}
		return "";
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

	public static StateListDrawable createCheckedStateListDrawable(Drawable normal, Drawable checked) {
		return createStateListDrawable(normal, checked, android.R.attr.state_checked);
	}

	public static StateListDrawable createPressedStateListDrawable(Drawable normal, Drawable pressed) {
		return createStateListDrawable(normal, pressed, android.R.attr.state_pressed);
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

	public static void setSnackbarTextColor(Snackbar snackbar, @ColorRes int colorId) {
		View view = snackbar.getView();
		TextView tv = (TextView) view.findViewById(android.support.design.R.id.snackbar_action);
		tv.setTextColor(ContextCompat.getColor(view.getContext(), colorId));
	}

	public static void setSnackbarTextMaxLines(Snackbar snackbar, int maxLines) {
		View view = snackbar.getView();
		TextView tv = (TextView) view.findViewById(android.support.design.R.id.snackbar_text);
		tv.setMaxLines(maxLines);
	}

	public static void setBackground(Context ctx, View view, boolean night, int lightResId, int darkResId) {
		Drawable drawable;
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
			drawable = ctx.getResources().getDrawable(night ? darkResId : lightResId, ctx.getTheme());
		} else {
			drawable = ctx.getResources().getDrawable(night ? darkResId : lightResId);
		}
		setBackground(view, drawable);
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
			view.setForeground(ctx.getResources().getDrawable(night ? darkResId : lightResId,
					ctx.getTheme()));
		} else if (view instanceof FrameLayout) {
			((FrameLayout) view).setForeground(ctx.getResources().getDrawable(night ? darkResId : lightResId));
		}
	}

	public static void updateImageButton(Context ctx, ImageButton button, int iconLightId, int iconDarkId, int bgLightId, int bgDarkId, boolean night) {
		button.setImageDrawable(ctx.getResources().getDrawable(night ? iconDarkId : iconLightId));
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
			button.setBackground(ctx.getResources().getDrawable(night ? bgDarkId : bgLightId, ctx.getTheme()));
		} else {
			button.setBackgroundDrawable(ctx.getResources().getDrawable(night ? bgDarkId : bgLightId));
		}
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

	public static void enterToFullScreen(Activity activity) {
		if (Build.VERSION.SDK_INT >= 21) {
			activity.getWindow().getDecorView()
					.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
		}
	}

	public static void exitFromFullScreen(Activity activity) {
		if (Build.VERSION.SDK_INT >= 21) {
			activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
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

	public static float getFreeSpaceGb(File dir) {
		if (dir.canRead()) {
			StatFs fs = new StatFs(dir.getAbsolutePath());
			return (float) (fs.getBlockSize()) * fs.getAvailableBlocks() / (1 << 30);
		}
		return -1;
	}

	public static float getTotalSpaceGb(File dir) {
		if (dir.canRead()) {
			return (float) (dir.getTotalSpace()) / (1 << 30);
		}
		return -1;
	}

	public static float getUsedSpaceGb(File dir) {
		if (dir.canRead()) {
			return getTotalSpaceGb(dir) - getFreeSpaceGb(dir);
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
}
