package net.osmand.plus.views.layers.geometry;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;

import net.osmand.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.profiles.ProfileIconColors;
import net.osmand.util.Algorithms;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

public class MultiProfileGeometryWayContext extends GeometryWayContext {

	@DrawableRes
	private static final int STRAIGHT_LINE_ICON_RES = R.drawable.ic_action_split_interval;

	private static final float PATH_BORDER_WIDTH_DP = 14;
	private static final float PATH_WIDTH_DP = 10;
	private static final float PROFILE_ICON_BORDER_WIDTH_DP = 2;

	private static final float MIN_PROFILE_ICON_MARGIN_DP = 30;
	private static final float PROFILE_ICON_FRAME_SIZE_DP = 70;
	private static final float USER_POINT_ICON_SIZE_DP = 22;

	private static final String pointColorHex = "#637EFB";

	private final UiUtilities iconsCache;

	private final Paint profileIconBackgroundPaint;
	private final Paint profileIconBorderPaint;
	private final Paint pathBorderPaint;
	private final Paint pathPaint;

	public final float minProfileIconMarginPx;
	public final float profileIconFrameSizePx;
	public final float userPointIconSizePx;

	private final Bitmap userPointIcon;
	private final Map<String, Bitmap> profileIconsBitmapCache;

	public MultiProfileGeometryWayContext(Context ctx, UiUtilities iconsCache, float density) {
		super(ctx, density);

		this.iconsCache = iconsCache;
		this.profileIconsBitmapCache = new HashMap<>();

		this.minProfileIconMarginPx = MIN_PROFILE_ICON_MARGIN_DP * density;
		this.profileIconFrameSizePx = PROFILE_ICON_FRAME_SIZE_DP * density;
		this.userPointIconSizePx = USER_POINT_ICON_SIZE_DP * density;

		this.profileIconBackgroundPaint = createPaint(Style.FILL, 0xFFFFFFFF, 0);
		this.profileIconBorderPaint = createPaint(Style.STROKE, 0, PROFILE_ICON_BORDER_WIDTH_DP);
		this.pathBorderPaint = createPaint(Style.STROKE, 0, PATH_BORDER_WIDTH_DP);
		this.pathPaint = createPaint(Style.STROKE, 0, PATH_WIDTH_DP);

		this.userPointIcon = createUserPointIcon();
	}

	private Paint createPaint(@NonNull Style style, @ColorInt int color, float strokeWidthDp) {
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setDither(true);
		paint.setStyle(style);
		paint.setColor(color);
		paint.setStrokeWidth(strokeWidthDp * getDensity());
		return paint;
	}

	@NonNull
	public Paint getPathPaint() {
		return pathPaint;
	}

	@NonNull
	public Paint getPathBorderPaint() {
		return pathBorderPaint;
	}

	@NonNull
	public Paint getProfileIconBackgroundPaint() {
		return profileIconBackgroundPaint;
	}

	@NonNull
	public Bitmap getProfileIconBitmap(@DrawableRes int iconRes, @ColorInt int color) {
		String key = iconRes + "_" + color;
		Bitmap framedProfileIconBitmap = profileIconsBitmapCache.get(key);
		if (framedProfileIconBitmap == null) {
			framedProfileIconBitmap = Bitmap.createBitmap((int) profileIconFrameSizePx,
					(int) profileIconFrameSizePx, Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(framedProfileIconBitmap);
			float center = framedProfileIconBitmap.getWidth() / 2f;

			canvas.drawCircle(center, center, center / 2, profileIconBackgroundPaint);
			profileIconBorderPaint.setColor(color);
			canvas.drawCircle(center, center, center / 2, profileIconBorderPaint);

			float iconSize = center - getDensity() * 10;
			Bitmap profileIconBitmap = AndroidUtils.createScaledBitmap(
					iconsCache.getPaintedIcon(iconRes, color), (int) iconSize, (int) iconSize);
			canvas.drawBitmap(profileIconBitmap, center - iconSize / 2, center - iconSize / 2,
					profileIconBorderPaint);

			profileIconsBitmapCache.put(key, framedProfileIconBitmap);
		}
		return framedProfileIconBitmap;
	}

	private Bitmap createUserPointIcon() {
		float density = getDensity();
		float outerRadius = density * 11f;
		float centerRadius = density * 10.5f;
		float innerRadius = density * 6.5f;
		float centerXY = userPointIconSizePx / 2;

		Bitmap userPointIcon = Bitmap.createBitmap((int) userPointIconSizePx, (int) userPointIconSizePx,
				Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(userPointIcon);
		Paint paint = new Paint();
		paint.setStyle(Paint.Style.FILL);

		paint.setColor(Color.BLACK);
		canvas.drawCircle(centerXY, centerXY, outerRadius, paint);

		paint.setColor(Color.WHITE);
		canvas.drawCircle(centerXY, centerXY, centerRadius, paint);

		paint.setColor(Algorithms.parseColor(pointColorHex));
		canvas.drawCircle(centerXY, centerXY, innerRadius, paint);

		return userPointIcon;
	}

	@NonNull
	public Bitmap getUserPointIcon() {
		return userPointIcon;
	}

	@Override
	protected int getArrowBitmapResId() {
		return R.drawable.ic_action_split_interval;
	}

	@DrawableRes
	public int getStraightLineIconRes() {
		return STRAIGHT_LINE_ICON_RES;
	}

	@ColorInt
	public int getStraightLineColor() {
		return ContextCompat.getColor(getCtx(), ProfileIconColors.DARK_YELLOW.getColor(isNightMode()));
	}
}