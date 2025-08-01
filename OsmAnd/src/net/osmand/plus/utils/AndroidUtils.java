package net.osmand.plus.utils;


import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.BLUETOOTH;
import static android.Manifest.permission.BLUETOOTH_ADMIN;
import static android.Manifest.permission.BLUETOOTH_CONNECT;
import static android.Manifest.permission.BLUETOOTH_SCAN;
import static android.graphics.Paint.ANTI_ALIAS_FLAG;
import static android.graphics.Paint.FILTER_BITMAP_FLAG;
import static android.util.TypedValue.COMPLEX_UNIT_DIP;
import static android.util.TypedValue.COMPLEX_UNIT_SP;
import static android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;

import android.Manifest;
import android.app.Activity;
import android.app.KeyguardManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.IBinder;
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
import android.text.style.URLSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.*;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.text.TextUtilsCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.osm.OsmRouteType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.mapwidgets.OutlinedTextContainer;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.views.OsmandMap;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.shared.gpx.primitives.RouteActivity;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.text.MessageFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

	public static void softKeyboardDelayed(@NonNull Activity activity, @NonNull View view) {
		view.post(() -> {
			if (!isHardwareKeyboardAvailable(view.getContext())) {
				showSoftKeyboard(activity, view);
			}
		});
	}

	public static void showSoftKeyboard(@NonNull Activity activity, @NonNull View view) {
		InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				KeyguardManager keyguardManager = (KeyguardManager) view.getContext().getSystemService(Context.KEYGUARD_SERVICE);
				keyguardManager.requestDismissKeyguard(activity, null);
			}
			imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
		}
	}

	public static void hideSoftKeyboard(@NonNull Activity activity, @Nullable View input) {
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
		if (newWidth == width && newHeight == height) {
			return bm;
		}
		float scaleWidth = ((float) newWidth) / width;
		float scaleHeight = ((float) newHeight) / height;
		Matrix matrix = new Matrix();
		matrix.postScale(scaleWidth, scaleHeight);
		Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
		if (!keepOriginalBitmap) {
			bm.recycle();
		}
		return resizedBitmap;
	}

	public static byte[] getByteArrayFromBitmap(@NonNull Bitmap bitmap) {
		int size = bitmap.getRowBytes() * bitmap.getHeight();
		ByteBuffer byteBuffer = ByteBuffer.allocate(size);
		bitmap.copyPixelsToBuffer(byteBuffer);
		return byteBuffer.array();
	}

	public static Bitmap createScaledBitmapWithTint(Context ctx, @DrawableRes int drawableId, float scale, int tint) {
		Drawable drawableIcon = AppCompatResources.getDrawable(ctx, drawableId);
		if (drawableIcon != null) {
			DrawableCompat.setTint(DrawableCompat.wrap(drawableIcon), tint);
		}
		Bitmap bitmap = drawableToBitmap(drawableIcon, true);
		if (bitmap != null && scale != 1f && scale > 0.0f) {
			bitmap = scaleBitmap(bitmap,
					(int) (bitmap.getWidth() * scale), (int) (bitmap.getHeight() * scale), false);
		}
		return bitmap;
	}

	public static Bitmap createScaledBitmap(@NonNull Drawable drawable, float scale) {
		int width = (int) (drawable.getIntrinsicWidth() * scale);
		int height = (int) (drawable.getIntrinsicHeight() * scale);
		width += width % 2 == 1 ? 1 : 0;
		height += height % 2 == 1 ? 1 : 0;
		return scaleBitmap(drawableToBitmap(drawable), width, height, true);
	}

	public static Bitmap createScaledBitmap(@NonNull Drawable drawable, int width, int height) {
		return scaleBitmap(drawableToBitmap(drawable), width, height, false);
	}

	public static ColorStateList createBottomNavColorStateList(Context ctx, boolean nightMode) {
		return createCheckedColorStateList(ctx, nightMode,
				R.color.icon_color_default_light, R.color.active_color_primary_light,
				R.color.icon_color_default_light, R.color.active_color_primary_dark);
	}

	public static void drawScaledLayerDrawable(@NonNull Canvas canvas, @NonNull LayerDrawable layerDrawable, int locationX, int locationY, float scale) {
		Paint bitmapPaint = new Paint(ANTI_ALIAS_FLAG | FILTER_BITMAP_FLAG);
		int layers = layerDrawable.getNumberOfLayers() - 1;
		int maxVectorLayerWidth = 0;
		int maxVectorLayerHeight = 0;
		for (int i = 0; i <= layers; i++) {
			Drawable drawable = layerDrawable.getDrawable(i);
			if (drawable instanceof VectorDrawable) {
				maxVectorLayerWidth = Math.max(maxVectorLayerWidth, drawable.getIntrinsicWidth());
				maxVectorLayerHeight = Math.max(maxVectorLayerHeight, drawable.getIntrinsicHeight());
			}
		}
		for (int i = 0; i <= layers; i++) {
			Drawable drawable = layerDrawable.getDrawable(i);
			if (drawable != null) {
				if (drawable instanceof VectorDrawable) {
					int width = (int) (drawable.getIntrinsicWidth() * scale);
					int height = (int) (drawable.getIntrinsicHeight() * scale);
					Rect boundsVector = new Rect(locationX - width / 2, locationY - height / 2,
							locationX + width / 2, locationY + height / 2);
					drawable.setBounds(boundsVector);
					drawable.draw(canvas);
				} else {
					Bitmap srcBitmap = ((BitmapDrawable) drawable).getBitmap();
					float scaleX = (float) maxVectorLayerWidth / srcBitmap.getWidth();
					float scaleY = (float) maxVectorLayerHeight / srcBitmap.getHeight();
					if (maxVectorLayerWidth == 0 || maxVectorLayerHeight == 0) {
						scaleX = 1;
						scaleY = 1;
					}
					int width = (int) (srcBitmap.getWidth() * scaleX * scale);
					int height = (int) (srcBitmap.getHeight() * scaleY * scale);
					Rect srcRect = new Rect(0, 0, srcBitmap.getWidth(), srcBitmap.getHeight());
					Rect dstRect = new Rect(locationX - width / 2,
							locationY - height / 2,
							locationX + width / 2,
							locationY + height / 2);
					canvas.drawBitmap(srcBitmap, srcRect, dstRect, bitmapPaint);
				}
			}
		}
	}

	public static String addColon(@NonNull OsmandApplication app, @StringRes int stringRes) {
		return app.getString(R.string.ltr_or_rtl_combine_via_colon, app.getString(stringRes), "").trim();
	}

	public static Uri getUriForFile(@NonNull Context context, @NonNull File file) {
		return FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
	}

	public static boolean startActivityIfSafe(@NonNull Context context, @NonNull Intent intent) {
		return startActivityIfSafe(context, intent, null);
	}

	public static boolean startActivityIfSafe(@NonNull Context context, @NonNull Intent intent, @Nullable Intent chooserIntent) {
		try {
			Intent selectedIntent = chooserIntent != null ? chooserIntent : intent;
			if (!(context instanceof Activity)) {
				selectedIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			}
			context.startActivity(selectedIntent);
			return true;
		} catch (ActivityNotFoundException e) {
			LOG.error(e);
			getApp(context).showToastMessage(R.string.no_activity_for_intent);
			return false;
		}
	}

	public static boolean startActivityForResultIfSafe(@NonNull Activity activity,
			@NonNull Intent intent, int requestCode) {
		try {
			activity.startActivityForResult(intent, requestCode);
			return true;
		} catch (ActivityNotFoundException e) {
			LOG.error(e);
			getApp(activity).showToastMessage(R.string.no_activity_for_intent);
			return false;
		}
	}

	public static void startActivityForResultIfSafe(@NonNull Fragment fragment, @NonNull Intent intent, int requestCode) {
		try {
			fragment.startActivityForResult(intent, requestCode);
		} catch (ActivityNotFoundException e) {
			LOG.error(e);
			Context context = fragment.getContext();
			if (context != null) {
				getApp(context).showToastMessage(R.string.no_activity_for_intent);
			}
		}
	}

	public static boolean isActivityNotDestroyed(@Nullable Activity activity) {
		return activity != null && !activity.isFinishing() && !activity.isDestroyed();
	}

	public static boolean isFragmentCanBeAdded(@NonNull FragmentManager manager, @Nullable String tag) {
		return isFragmentCanBeAdded(manager, tag, false);
	}

	public static boolean isFragmentCanBeAdded(@NonNull FragmentManager manager, @Nullable String tag, boolean preventFragmentDuplication) {
		boolean isStateSaved = manager.isStateSaved();
		return preventFragmentDuplication ? !isStateSaved && manager.findFragmentByTag(tag) == null : !isStateSaved;
	}

	public static Spannable replaceCharsWithIcon(String text, Drawable icon, String[] chars) {
		Spannable spannable = new SpannableString(text);
		for (String entry : chars) {
			int i = text.indexOf(entry);
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
				public void updateDrawState(@NonNull TextPaint ds) {
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

	@NonNull
	public static String formatSize(Context ctx, long sizeBytes) {
		return formatSize(ctx, sizeBytes, false);
	}

	@NonNull
	public static String formatSize(Context ctx, long sizeBytes, boolean round) {
		FormattedSize formattedSize = formatSize(sizeBytes, round);
		if (formattedSize != null) {
			String size = formattedSize.num;
			String numSuffix = formattedSize.numSuffix;
			if (ctx == null) {
				return size + " " + numSuffix;
			}
			return ctx.getString(R.string.ltr_or_rtl_combine_via_space, size, numSuffix);
		}
		return "";
	}

	@Nullable
	private static FormattedSize formatSize(long sizeBytes, boolean round) {
		if (sizeBytes <= 0) {
			return null;
		}
		FormattedSize result = new FormattedSize();
		int sizeKb = (int) ((sizeBytes + 512) >> 10);
		if (sizeKb > 1 << 20) {
			result.num = formatGb.format(new Object[] {roundIfNeeded((float) sizeKb / (1 << 20), round)});
			result.numSuffix = "GB";
		} else if (sizeBytes > (100 * (1 << 10))) {
			result.num = formatMb.format(new Object[] {roundIfNeeded((float) sizeBytes / (1 << 20), round)});
			result.numSuffix = "MB";
		} else {
			result.num = formatKb.format(new Object[] {roundIfNeeded((float) sizeBytes / (1 << 10), round)});
			result.numSuffix = "kB";
		}
		return result;
	}

	final static class FormattedSize {
		String num;
		String numSuffix;
	}

	private static float roundIfNeeded(float value, boolean round) {
		return round ? Math.round(value) : value;
	}

	public static String getFreeSpace(Context ctx, File dir) {
		long size = getAvailableSpace(dir);
		return formatSize(ctx, size);
	}

	public static View findParentViewById(View view, int id) {
		ViewParent viewParent = view.getParent();

		while (viewParent instanceof View) {
			View parentView = (View) viewParent;
			if (parentView.getId() == id) {
				return parentView;
			}
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

	@NonNull
	private static ColorStateList createColorStateList(Context ctx, boolean night, int state,
	                                                   @ColorRes int lightNormal, @ColorRes int lightState,
	                                                   @ColorRes int darkNormal, @ColorRes int darkState) {
		int normalColor = ContextCompat.getColor(ctx, night ? darkNormal : lightNormal);
		int stateColor = ContextCompat.getColor(ctx, night ? darkState : lightState);
		return createColorStateList(state, stateColor, normalColor);
	}

	@NonNull
	public static ColorStateList createColorStateList(int state, @ColorInt int stateColor, @ColorInt int normal) {
		return new ColorStateList(
				new int[][] {new int[] {state}, new int[] {}},
				new int[] {stateColor, normal}
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
						ColorUtilities.getSecondaryTextColor(ctx, night),
						ColorUtilities.getActiveColor(ctx, night),
						ColorUtilities.getSecondaryTextColor(ctx, night)}
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
				new int[][] {
						new int[] {state},
						new int[] {}
				},
				new int[] {
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
		res.addState(new int[] {state}, stateDrawable);
		res.addState(new int[] {}, normal);
		return res;
	}

	public static LayerDrawable createProgressDrawable(@ColorInt int bgColor, @ColorInt int progressColor) {
		ShapeDrawable bg = new ShapeDrawable();
		bg.getPaint().setColor(bgColor);

		ShapeDrawable progress = new ShapeDrawable();
		progress.getPaint().setColor(progressColor);

		LayerDrawable res = new LayerDrawable(new Drawable[] {
				bg,
				new ClipDrawable(progress, Gravity.START, ClipDrawable.HORIZONTAL)
		});

		res.setId(0, android.R.id.background);
		res.setId(1, android.R.id.progress);

		return res;
	}

	public static void setBackground(Context ctx, View view, boolean night, int lightResId, int darkResId) {
		setBackground(ctx, view, night ? darkResId : lightResId);
	}

	public static void setBackground(Context ctx, View view, int resId) {
		setBackground(view, AppCompatResources.getDrawable(ctx, resId));
	}

	public static void setBackground(View view, Drawable drawable) {
		view.setBackground(drawable);
	}

	public static void setForeground(Context ctx, View view, boolean night, int lightResId, int darkResId) {
		view.setForeground(AppCompatResources.getDrawable(ctx, night ? darkResId : lightResId));
	}

	public static void setDashButtonBackground(Context ctx, View view, boolean night) {
		setBackground(ctx, view, night, R.drawable.dashboard_button_light, R.drawable.dashboard_button_dark);
	}

	public static void setBackgroundColor(Context ctx, View view, boolean night, int lightResId, int darkResId) {
		setBackgroundColor(ctx, view, night ? darkResId : lightResId);
	}

	public static void setBackgroundColor(Context ctx, View view, @ColorRes int colorId) {
		view.setBackgroundColor(ContextCompat.getColor(ctx, colorId));
	}

	public static void setListItemBackground(Context ctx, View view, boolean night) {
		setBackgroundColor(ctx, view, ColorUtilities.getListBgColorId(night));
	}

	public static void setTextPrimaryColor(Context ctx, TextView textView, boolean night) {
		textView.setTextColor(ColorUtilities.getPrimaryTextColor(ctx, night));
	}

	public static void setTextSecondaryColor(Context ctx, TextView textView, boolean night) {
		textView.setTextColor(ColorUtilities.getSecondaryTextColor(ctx, night));
	}

	public static void setHintTextSecondaryColor(Context ctx, TextView textView, boolean night) {
		textView.setHintTextColor(ColorUtilities.getSecondaryTextColor(ctx, night));
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

	public static void setTruncatedText(OutlinedTextContainer textView, String text) {
		Paint paint = new Paint();
		paint.setTextSize(textView.getTextSize());
		float textWidth = paint.measureText(text);
		int viewWidth = textView.getWidth();
		if (textWidth > viewWidth) {
			int charactersToShow = paint.breakText(text, true, viewWidth, null);
			text = text.substring(0, charactersToShow);
		}
		textView.setText(text);
	}

	public static int getMaxPossibleTextSize(@NonNull String text, @Nullable Typeface typeface,
	                                         int viewWidthPx, int minSizePx, int maxSizePx) {
		int textWidthAtMax = getTextWidth(typeface, maxSizePx, text);
		if (textWidthAtMax <= viewWidthPx) {
			return maxSizePx;
		}

		float scale = (float) viewWidthPx / textWidthAtMax;
		int scaledSize = (int) Math.floor(maxSizePx * scale);

		int candidateSize = Math.min(maxSizePx, Math.max(minSizePx, scaledSize));
		int candidateWidth = getTextWidth(typeface, candidateSize, text);
		if (candidateWidth <= viewWidthPx) {
			return candidateSize;
		}

		while (candidateSize > minSizePx) {
			candidateSize--;
			candidateWidth = getTextWidth(typeface, candidateSize, text);
			if (candidateWidth <= viewWidthPx) {
				return candidateSize;
			}
		}
		return minSizePx;
	}

	public static int getTextWidth(float textSize, @NonNull String text) {
		return getTextWidth(null, textSize, text);
	}

	public static int getTextWidth(@Nullable Typeface typeface, float textSize, @NonNull String text) {
		Paint paint = new Paint();
		paint.setTextSize(textSize);
		if (typeface != null) {
			paint.setTypeface(typeface);
		}
		return (int) paint.measureText(text);
	}

	public static int getTextHeight(@NonNull Paint paint) {
		Paint.FontMetrics fm = paint.getFontMetrics();
		float height = fm.bottom - fm.top;
		return (int) height;
	}

	public static float dpToPxF(@NonNull Context ctx, float dp) {
		Resources r = ctx.getResources();
		return TypedValue.applyDimension(COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
	}

	public static int dpToPx(@NonNull Context ctx, float dp) {
		Resources r = ctx.getResources();
		return (int) TypedValue.applyDimension(COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
	}

	public static float pxToDpF(@NonNull Context ctx, int px) {
		if (VERSION.SDK_INT >= VERSION_CODES.UPSIDE_DOWN_CAKE) {
			return TypedValue.deriveDimension(COMPLEX_UNIT_DIP, px, ctx.getResources().getDisplayMetrics());
		} else {
			return px / dpToPxF(ctx, 1);
		}
	}

	public static int dpToPxAuto(@NonNull Context ctx, float dp) {
		OsmandApplication app = (OsmandApplication) ctx.getApplicationContext();
		float scaleCoef = app.getOsmandMap().getCarDensityScaleCoef();
		Resources r = ctx.getResources();
		return (int) (TypedValue.applyDimension(
				COMPLEX_UNIT_DIP,
				dp,
				r.getDisplayMetrics()
		) * scaleCoef);
	}

	public static int spToPx(@NonNull Context ctx, float sp) {
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

	public static int resolveAttribute(@NonNull Context ctx, int attribute) {
		TypedValue outValue = new TypedValue();
		ctx.getTheme().resolveAttribute(attribute, outValue, true);
		return outValue.resourceId;
	}

	public static float getFloatValueFromRes(@NonNull Context ctx, int resId) {
		TypedValue outValue = new TypedValue();
		ctx.getResources().getValue(resId, outValue, true);
		return outValue.getFloat();
	}

	@DrawableRes
	public static int getActivityIconId(@NonNull Context app, @Nullable RouteActivity activity) {
		return activity != null
				? getDrawableId(app, activity.getIconName(), R.drawable.ic_action_info_dark)
				: R.drawable.ic_action_activity;
	}

	public static boolean hasDrawableId(@NonNull Context app, @NonNull String iconName) {
		return getDrawableId(app, iconName, 0) != 0;
	}

	@DrawableRes
	public static int getDrawableId(@NonNull Context app, @NonNull String iconName, @DrawableRes int defRes) {
		int iconId = getDrawableId(app, iconName);
		if (iconId <= 0) {
			iconId = RenderingIcons.getBigIconResourceId(iconName);
		}
		return iconId > 0 ? iconId : defRes;
	}

	public static int getDrawableId(@NonNull Context context, String id) {
		if (!Algorithms.isEmpty(id)) {
			return context.getResources().getIdentifier(id, "drawable", context.getPackageName());
		}
		return 0;
	}

	public static int getStatusBarHeight(@NonNull Context ctx) {
		int result = 0;
		int resourceId = ctx.getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0) {
			result = ctx.getResources().getDimensionPixelSize(resourceId);
		}
		return result;
	}

	public static int getCutoutHeight(@NonNull Activity activity) {
		int cutoutHeight = 0;

		if (activity != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
			DisplayCutout displayCutout = activity.getWindowManager().getDefaultDisplay().getCutout();
			if (displayCutout != null) {
				Rect rect = displayCutout.getBoundingRectTop();
				cutoutHeight = rect.height();
			}
		}

		return cutoutHeight;
	}

	public static void addStatusBarPadding21v(@NonNull Activity activity, @NonNull View view) {
		if (isInFullScreenMode(activity)) {
			int paddingLeft = view.getPaddingLeft();
			int paddingTop = view.getPaddingTop();
			int paddingRight = view.getPaddingRight();
			int paddingBottom = view.getPaddingBottom();

			int statusBarHeight = getStatusBarHeight(activity);
			int cutoutHeight = getCutoutHeight(activity);

			view.setPadding(paddingLeft,
					paddingTop + statusBarHeight > cutoutHeight ? statusBarHeight : cutoutHeight,
					paddingRight,
					paddingBottom);
		}
	}

	public static int resizeViewForKeyboard(Activity activity, View view, int layoutHeightPrevious) {
		Rect visibleDisplayFrame = new Rect();
		view.getWindowVisibleDisplayFrame(visibleDisplayFrame);
		int layoutHeight = visibleDisplayFrame.bottom;
		if (!isInFullScreenMode(activity)) {
			layoutHeight -= getStatusBarHeight(activity);
		}
		if (layoutHeight != layoutHeightPrevious) {
			FrameLayout.LayoutParams rootViewLayout = (FrameLayout.LayoutParams) view.getLayoutParams();
			rootViewLayout.height = layoutHeight;
			view.requestLayout();
		}
		return layoutHeight;
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
		if (!isNavBarVisible(activity)) {
			switchNavBarVisibility(activity);
		}
	}

	public static void hideNavBar(Activity activity) {
		if (isNavBarVisible(activity)) {
			switchNavBarVisibility(activity);
		}
	}

	public static boolean isNavBarVisible(Activity activity) {
		int uiOptions = activity.getWindow().getDecorView().getSystemUiVisibility();
		return !((uiOptions | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == uiOptions);
	}

	public static void switchNavBarVisibility(@NonNull Activity activity) {
		View decorView = activity.getWindow().getDecorView();
		int uiOptions = decorView.getSystemUiVisibility();
		uiOptions ^= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
		uiOptions ^= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
		decorView.setSystemUiVisibility(uiOptions);
	}

	@NonNull
	public static Rect getViewBoundOnScreen(@NonNull View view) {
		if (view.getVisibility() != View.GONE) {
			int[] pixel = getLocationOnScreen(view);
			int left = pixel[0];
			int top = pixel[1];
			return new Rect(left, top, left + view.getWidth(), top + view.getHeight());
		}
		return new Rect();
	}

	@NonNull
	public static Rect getViewBoundOnWindow(@NonNull View view) {
		if (view.getVisibility() != View.GONE) {
			int[] pixel = new int[2];
			view.getLocationInWindow(pixel);
			int left = pixel[0];
			int top = pixel[1];
			return new Rect(left, top, left + view.getWidth(), top + view.getHeight());
		}
		return new Rect();
	}

	public static int[] getCenterViewCoordinates(@NonNull View view) {
		int[] coordinates = new int[2];
		if (view.getVisibility() != View.GONE) {
			view.getLocationOnScreen(coordinates);
			coordinates[0] += view.getWidth() / 2;
			coordinates[1] += view.getHeight() / 2;
		}
		return coordinates;
	}

	public static int getViewOnScreenX(@NonNull View view) {
		return getLocationOnScreen(view)[0];
	}

	public static int getViewOnScreenY(@NonNull View view) {
		return getLocationOnScreen(view)[1];
	}

	public static int[] getLocationOnScreen(@NonNull View view) {
		int[] locationOnScreen = new int[2];
		view.getLocationOnScreen(locationOnScreen);
		return locationOnScreen;
	}

	public static void enterToFullScreen(Activity activity, View view) {
		requestLayout(view);
		activity.getWindow().getDecorView().setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
	}

	public static void exitFromFullScreen(Activity activity, View view) {
		requestLayout(view);
		activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
	}

	public static boolean isInFullScreenMode(Activity activity) {
		int uiMode = activity.getWindow().getDecorView().getSystemUiVisibility();
		return (uiMode & View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN) != 0;
	}

	private static void requestLayout(View view) {
		if (view != null) {
			ViewTreeObserver vto = view.getViewTreeObserver();
			vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

				@Override
				public void onGlobalLayout() {
					ViewTreeObserver obs = view.getViewTreeObserver();
					obs.removeOnGlobalLayoutListener(this);
					view.requestLayout();
				}
			});
		}
	}

	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
		List<Map.Entry<K, V>> list = new LinkedList<>(map.entrySet());
		list.sort(Entry.comparingByValue());

		Map<K, V> result = new LinkedHashMap<>();
		for (Map.Entry<K, V> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}

	public static void setCompoundDrawablesWithIntrinsicBounds(@NonNull TextView tv, Drawable start, Drawable top, Drawable end, Drawable bottom) {
			tv.setCompoundDrawablesRelativeWithIntrinsicBounds(start, top, end, bottom);
	}

	public static Drawable[] getCompoundDrawables(@NonNull TextView tv) {
			return tv.getCompoundDrawablesRelative();
	}

	public static void setPadding(View view, int start, int top, int end, int bottom) {
		view.setPaddingRelative(start, top, end, bottom);
	}

	public static void setMargins(ViewGroup.MarginLayoutParams layoutParams, int vertical, int horizontal) {
		setMargins(layoutParams, horizontal, vertical, horizontal, vertical);
	}

	public static void setMargins(ViewGroup.MarginLayoutParams layoutParams, int start, int top, int end, int bottom) {
		layoutParams.setMargins(start, top, end, bottom);
			layoutParams.setMarginStart(start);
			layoutParams.setMarginEnd(end);
	}

	public static int getLayoutDirection(@NonNull Context ctx) {
		Locale currentLocale = ctx.getResources().getConfiguration().locale;
		return TextUtilsCompat.getLayoutDirectionFromLocale(currentLocale);
	}

	public static int getNavigationIconResId(@NonNull Context ctx) {
		return isLayoutRtl(ctx) ? R.drawable.ic_arrow_forward : R.drawable.ic_arrow_back;
	}

	@NonNull
	public static Drawable getDrawableForDirection(@NonNull Context ctx, @NonNull Drawable drawable) {
		return isLayoutRtl(ctx) ? getMirroredDrawable(drawable) : drawable;
	}

	@NonNull
	public static Drawable getMirroredDrawable(@NonNull Drawable drawable) {
		drawable.setAutoMirrored(true);
		return drawable;
	}

	@NonNull
	public static Bitmap drawableToBitmap(@NonNull Drawable drawable) {
		return drawableToBitmap(drawable, false);
	}

	@NonNull
	public static Bitmap drawableToBitmap(@NonNull Drawable drawable, boolean noOptimization) {
		int width = drawable.getIntrinsicWidth() <= 0 ? 1 : drawable.getIntrinsicWidth();
		int height = drawable.getIntrinsicHeight() <= 0 ? 1 : drawable.getIntrinsicHeight();
		return drawableToBitmap(drawable, width, height, noOptimization);
	}

	@NonNull
	public static Bitmap drawableToBitmap(@NonNull Drawable drawable, float scale, boolean noOptimization) {
		int width = (int) (drawable.getIntrinsicWidth() * scale);
		int height = (int) (drawable.getIntrinsicHeight() * scale);
		return drawableToBitmap(drawable, width, height, noOptimization);
	}

	@NonNull
	public static Bitmap drawableToBitmap(@NonNull Drawable drawable, int width, int height,
			boolean noOptimization) {
		if (!noOptimization && drawable instanceof BitmapDrawable bitmap && bitmap.getBitmap() != null) {
			return bitmap.getBitmap();
		}
		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
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
			boolean isLayoutRtl = isLayoutRtl(tv.getContext());
			int gravity = Gravity.LEFT;
			if (isLayoutRtl && (hGravity == Gravity.START)
					|| !isLayoutRtl && hGravity == Gravity.END) {
				gravity = Gravity.RIGHT;
			}
			tv.setGravity(gravity);
		}
	}

	public static boolean isLayoutMirrored(@NonNull View view) {
		return ViewCompat.getLayoutDirection(view) == ViewCompat.LAYOUT_DIRECTION_RTL;
	}

	public static boolean isLayoutRtl(Context ctx) {
		Configuration config = ctx.getResources().getConfiguration();
		return config.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
	}

	public static List<View> getChildrenViews(ViewGroup vg) {
		List<View> result = new ArrayList<>();
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
				return fs.getAvailableBlocksLong() * fs.getBlockSizeLong();
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
				return fs.getBlockCountLong() * fs.getBlockSizeLong();
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
				return (float) (fs.getBlockSizeLong()) * fs.getAvailableBlocksLong() / (1 << 30);
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
			SpannableStringBuilder builder = new SpannableStringBuilder(nStr);
			if (baseStyle != null) {
				if (indexOfPlaceholder > 0) {
					builder.setSpan(baseStyle, 0, indexOfPlaceholder, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
				if (indexOfPlaceholder + stringToInsertAndStyle.length() < nStr.length()) {
					builder.setSpan(baseStyle,
							indexOfPlaceholder + stringToInsertAndStyle.length(),
							nStr.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
			}
			if (replaceStyle != null) {
				builder.setSpan(replaceStyle, indexOfPlaceholder,
						indexOfPlaceholder + stringToInsertAndStyle.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			return builder;
		} else {
			return baseString;
		}
	}

	@NonNull
	public static String createNewFileName(@NonNull String fileName) {
		int index = fileName.lastIndexOf('.');
		String name = fileName.substring(0, index);
		String extension = fileName.substring(index);

		Matcher matcher = Pattern.compile("\\s[(]\\d+[)]$").matcher(name);
		if (matcher.find()) {
			int startIndex = name.lastIndexOf('(');
			int endIndex = name.lastIndexOf(')');
			int counter = Algorithms.parseIntSilently(name.substring(startIndex + 1, endIndex), 1);

			return name.substring(0, startIndex + 1) + (counter + 1) + ")" + extension;
		}
		return name + " (2)" + extension;
	}

	public static StringBuilder formatWarnings(Collection<String> warnings) {
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

	public static String getIconStringPropertyName(@NonNull Context ctx, @NonNull String propertyName) {
		String value = getStringByProperty(ctx, "icon_group_" + propertyName);
		return value != null ? value : propertyName;
	}

	public static int getRenderPropertySelectedValueIndex(@NonNull OsmandApplication app,
	                                                      @NonNull RenderingRuleProperty property) {
		OsmandSettings settings = app.getSettings();
		String value = settings.getRenderPropertyValue(property);
		int index = List.of(property.getPossibleValues()).indexOf(value);
		if (index >= 0) {
			return ++index;
		} else if (Algorithms.isEmpty(value)) {
			return 0;
		}
		return index;
	}

	@NonNull
	public static String getRenderingStringPropertyValue(@NonNull OsmandApplication app,
	                                                     @NonNull RenderingRuleProperty property) {
		OsmandSettings settings = app.getSettings();
		String value = settings.getRenderPropertyValue(property);
		String key = !Algorithms.isEmpty(value) ? value : property.getDefaultValueDescription();
		return AndroidUtils.getRenderingStringPropertyValue(app, key);
	}

	@NonNull
	public static String getRenderingStringPropertyValue(@NonNull Context ctx, @Nullable String propertyValue) {
		if (propertyValue == null) {
			return "";
		}
		String propertyValueReplaced = propertyValue.replaceAll("\\s+", "_");
		String value = getStringByProperty(ctx, "rendering_value_" + propertyValueReplaced + "_name");
		return value != null ? value : propertyValue;
	}

	@NonNull
	public static String getRenderingStringPropertyDescription(Context ctx, String propertyValue) {
		if (propertyValue == null) {
			return "";
		}
		String propertyValueReplaced = propertyValue.replaceAll("\\s+", "_");
		String value = getStringByProperty(ctx, "rendering_value_" + propertyValueReplaced + "_description");
		return value != null ? value : propertyValue;
	}

	@NonNull
	public static String getStringRouteInfoPropertyValue(Context ctx, String propertyValue) {
		if (propertyValue == null) {
			return "";
		}
		String propertyValueReplaced = propertyValue.replaceAll("\\s+", "_");
		String value = getStringByProperty(ctx, "routeInfo_" + propertyValueReplaced + "_name");
		return value != null ? value : propertyValue;
	}

	@NonNull
	public static String getStringRouteInfoPropertyDescription(Context ctx, String propertyValue) {
		if (propertyValue == null) {
			return "";
		}
		String propertyValueReplaced = propertyValue.replaceAll("\\s+", "_");
		String value = getStringByProperty(ctx, "routeInfo_" + propertyValueReplaced + "_description");
		return value != null ? value : propertyValue;
	}

	@DrawableRes
	public static int getActivityTypeIcon(@NonNull Context ctx, @NonNull OsmRouteType activityType) {
		int iconId = ctx.getResources().getIdentifier("mx_" + activityType.getIcon(), "drawable", ctx.getPackageName());
		return iconId != 0 ? iconId : R.drawable.mx_special_marker;
	}

	@NonNull
	public static String getActivityTypeTitle(@NonNull Context ctx, @NonNull OsmRouteType activityType) {
		return getActivityTypeStringPropertyName(ctx, activityType.getName(),
				Algorithms.capitalizeFirstLetterAndLowercase(activityType.getName()));
	}

	@NonNull
	public static String getActivityTypeStringPropertyName(@NonNull Context ctx, @NonNull String propertyName,
	                                                       @NonNull String defValue) {
		String value = getStringByProperty(ctx, "activity_type_" + propertyName + "_name");
		return value != null ? value : defValue;
	}

	public static String getLangTranslation(@NonNull Context context, @NonNull String lang) {
		String property = lang.replace("-", "_").toLowerCase();
		String value = getStringByProperty(context, "lang_" + property);
		return value != null ? value : lang;
	}

	@Nullable
	public static String getStringByProperty(@NonNull Context ctx, @NonNull String property) {
		try {
			Field field = R.string.class.getField(property);
			return getStringForField(ctx, field);
		} catch (Exception e) {
			LOG.warn("String not found: " + e.getMessage());
		}
		return null;
	}

	@Nullable
	private static String getStringForField(@NonNull Context ctx, @Nullable Field field) throws IllegalAccessException {
		if (field != null) {
			Integer in = (Integer) field.get(null);
			return ctx.getString(in);
		}
		return null;
	}

	public static void openUrl(@NonNull Context context, int urlStringId, boolean nightMode) {
		openUrl(context, context.getString(urlStringId), nightMode);
	}

	public static void openUrl(@NonNull Context context, @NonNull String url, boolean nightMode) {
		openUrl(context, Uri.parse(url), nightMode);
	}

	public static void openUrl(@NonNull Context context, @NonNull Uri uri, boolean nightMode) {
		CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder()
				.setToolbarColor(ColorUtilities.getAppBarColor(context, nightMode))
				.build();
		customTabsIntent.intent.setData(uri);
		try {
			customTabsIntent.launchUrl(context, uri);
		} catch (ActivityNotFoundException e) {
			getApp(context).showToastMessage(R.string.no_activity_for_intent);
		}
	}

	public static boolean hasPermission(@NonNull Context context, String permission) {
		return ActivityCompat.checkSelfPermission(
				context,
				permission
		) == PackageManager.PERMISSION_GRANTED;
	}

	public static boolean hasBLEPermission(@NonNull Context context) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			return hasPermission(context, BLUETOOTH_SCAN) &&
					hasPermission(context, BLUETOOTH_CONNECT);
		} else {
			return hasPermission(context, BLUETOOTH) &&
					hasPermission(context, BLUETOOTH_ADMIN) &&
					hasPermission(context, ACCESS_FINE_LOCATION);
		}
	}

	private static final int BLUETOOTH_REQUEST_CODE = 2;
	private static final int BLUETOOTH_ADMIN_REQUEST_CODE = 2;
	private static final int BLUETOOTH_SCAN_REQUEST_CODE = 4;
	private static final int BLUETOOTH_CONNECT_REQUEST_CODE = 5;

	public static boolean requestBLEPermissions(@NonNull Activity activity) {
		return requestBLEPermissions(activity, BLUETOOTH_CONNECT_REQUEST_CODE);
	}

	public static boolean requestBLEPermissions(@NonNull Activity activity, int requestCode) {
		List<String> permissions = new ArrayList<>();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			if (!AndroidUtils.hasPermission(activity, BLUETOOTH_SCAN)) {
				permissions.add(BLUETOOTH_SCAN);
			}
			if (!AndroidUtils.hasPermission(activity, BLUETOOTH_CONNECT)) {
				permissions.add(BLUETOOTH_CONNECT);
			}
		} else {
			if (!AndroidUtils.hasPermission(activity, BLUETOOTH)) {
				permissions.add(BLUETOOTH);
			}
			if (!AndroidUtils.hasPermission(activity, BLUETOOTH_ADMIN)) {
				permissions.add(BLUETOOTH_ADMIN);
			}
			if (!AndroidUtils.hasPermission(activity, ACCESS_FINE_LOCATION)) {
				permissions.add(ACCESS_FINE_LOCATION);
			}
		}
		if (!Algorithms.isEmpty(permissions)) {
			ActivityCompat.requestPermissions(
					activity,
					permissions.toArray(new String[0]),
					requestCode);

		}
		return Algorithms.isEmpty(permissions);
	}

	public static final int POST_NOTIFICATIONS_REQUEST_CODE = 6;

	public static boolean hasPostNotificationPermission(@NonNull Context context) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			return ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
		} else {
			return true;
		}
	}

	public static void requestNotificationPermissionIfNeeded(@NonNull FragmentActivity activity) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			if (!AndroidUtils.hasPermission(activity, Manifest.permission.POST_NOTIFICATIONS)) {
				ActivityCompat.requestPermissions(activity,
						new String[] {Manifest.permission.POST_NOTIFICATIONS},
						POST_NOTIFICATIONS_REQUEST_CODE);
			}
		}
	}

	@Nullable
	public static <T extends Serializable> T getSerializable(@NonNull Bundle bundle, @NonNull String key, @NonNull Class<T> clazz) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			return bundle.getSerializable(key, clazz);
		} else {
			return (T) bundle.getSerializable(key);
		}
	}

	public static int calculateTotalSizePx(@NonNull Context context, @DimenRes int... dimensId) {
		int result = 0;
		for (int id : dimensId) {
			result += context.getResources().getDimensionPixelSize(id);
		}
		return result;
	}

	public static boolean isBluetoothEnabled(@NonNull Context context) {
		BluetoothManager bluetoothManager = context.getSystemService(BluetoothManager.class);
		BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
		return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
	}

	public static Display getDisplay(@NonNull Context context) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			OsmandApplication app = (OsmandApplication) context.getApplicationContext();
			OsmandMap osmandMap = app.getOsmandMap();
			if (osmandMap != null) {
				MapActivity activity = osmandMap.getMapView().getMapActivity();
				if (activity != null) {
					return activity.getDisplay();
				}
			}
			DisplayManager manager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
			return manager.getDisplay(Display.DEFAULT_DISPLAY);
		} else {
			WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
			return manager.getDefaultDisplay();
		}
	}

	@NonNull
	public static Context createDisplayContext(@NonNull Context context) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			return context.createDisplayContext(getDisplay(context))
					.createWindowContext(TYPE_APPLICATION_OVERLAY, null);
		}
		return context;
	}

	@NonNull
	public static int[] getRelativeMargins(@NonNull View parentView, @NonNull View view) {
		int[] childLocation = new int[2];
		int[] parentLocation = new int[2];

		view.getLocationInWindow(childLocation);
		parentView.getLocationInWindow(parentLocation);

		int topMargin = childLocation[1] - parentLocation[1];
		int leftMargin = childLocation[0] - parentLocation[0];

		int childWidth = view.getWidth();
		int childHeight = view.getHeight();

		int parentWidth = parentView.getWidth();
		int parentHeight = parentView.getHeight();

		int rightMargin = (parentWidth - leftMargin - childWidth);
		int bottomMargin = (parentHeight - topMargin - childHeight);

		if (isLayoutRtl(parentView.getContext())) {
			return new int[] {rightMargin, topMargin, leftMargin, bottomMargin};
		} else {
			return new int[] {leftMargin, topMargin, rightMargin, bottomMargin};
		}
	}

	@NonNull
	public static OsmandApplication getApp(@NonNull Context context) {
		return ((OsmandApplication) context.getApplicationContext());
	}
}
