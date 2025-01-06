package net.osmand.plus.views.mapwidgets;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.TurnPathHelper;
import net.osmand.router.TurnType;

import org.apache.commons.logging.Log;

public class TurnDrawable extends Drawable {

	protected static final Log log = PlatformUtil.getLog(TurnDrawable.class);

	protected Paint paintTurnOutlayStroke;
	protected Paint paintTurnOutlayFill;
	protected Paint paintRouteDirection;
	protected Path pathForTurn = new Path();
	protected Path pathForTurnOutlay = new Path();
	private final Path originalPathForTurn = new Path();
	private final Path originalPathForTurnOutlay = new Path();
	protected TurnType turnType;
	protected int turnImminent;
	protected boolean deviatedFromRoute;
	private final Context ctx;
	private final boolean mini;
	private final PointF centerText;
	private TextPaint textPaint;
	private Boolean nightMode;
	private int routeDirectionColorId;

	public TurnDrawable(Context ctx, boolean mini) {
		this.ctx = ctx;
		this.mini = mini;
		centerText = new PointF();
		paintTurnOutlayStroke = new Paint();
		paintTurnOutlayStroke.setStyle(Paint.Style.STROKE);
		paintTurnOutlayStroke.setColor(getColor(R.color.nav_arrow_stroke_color));
		paintTurnOutlayStroke.setAntiAlias(true);
		paintTurnOutlayStroke.setStrokeWidth(2.5f);

		paintTurnOutlayFill = new Paint();
		paintTurnOutlayFill.setStyle(Paint.Style.FILL);
		paintTurnOutlayFill.setColor(getColorFromAttr(R.attr.nav_arrow_circle_color));
		paintTurnOutlayFill.setAntiAlias(true);

		paintRouteDirection = new Paint();
		paintRouteDirection.setStyle(Paint.Style.FILL);
		paintRouteDirection.setAntiAlias(true);
		setRouteDirectionColor(R.color.nav_arrow);
	}

	public void setRouteDirectionColor(@ColorRes int routeDirectionColorId) {
		if (routeDirectionColorId != this.routeDirectionColorId) {
			this.routeDirectionColorId = routeDirectionColorId;
			paintRouteDirection.setColor(getColor(routeDirectionColorId));
		}
	}

	@Override
	protected void onBoundsChange(Rect bounds) {
		Matrix m = new Matrix();
		float scaleX = bounds.width() / 72f;
		float scaleY = bounds.height() / 72f;
		m.setScale(scaleX, scaleY);

		pathForTurn.set(originalPathForTurn);
		pathForTurn.transform(m);

		pathForTurnOutlay.set(originalPathForTurnOutlay);
		pathForTurnOutlay.transform(m);

		centerText.x = scaleX * centerText.x;
		centerText.y = scaleY * centerText.y;
	}

	public int getTurnImminent() {
		return turnImminent;
	}

	public boolean isDeviatedFromRoute() {
		return deviatedFromRoute;
	}

	public void setTurnImminent(int turnImminent, boolean deviatedFromRoute) {
		//if user deviates from route that we should draw grey arrow
		this.turnImminent = turnImminent;
		this.deviatedFromRoute = deviatedFromRoute;
		if (deviatedFromRoute) {
			paintRouteDirection.setColor(getColor(R.color.nav_arrow_distant));
		} else if (turnImminent > 0) {
			paintRouteDirection.setColor(getColor(R.color.nav_arrow));
		} else if (turnImminent == 0) {
			paintRouteDirection.setColor(getColor(R.color.nav_arrow_imminent));
		} else {
			paintRouteDirection.setColor(getColor(R.color.nav_arrow_distant));
		}
		invalidateSelf();
	}

	@Override
	public void draw(@NonNull Canvas canvas) {
		/// small indent
		// canvas.translate(0, 3 * scaleCoefficient);
		canvas.drawPath(pathForTurnOutlay, paintTurnOutlayFill);
		canvas.drawPath(pathForTurnOutlay, paintTurnOutlayStroke);
		canvas.drawPath(pathForTurn, paintRouteDirection);
		canvas.drawPath(pathForTurn, paintTurnOutlayStroke);
		if (textPaint != null) {
			if (turnType != null && !mini && turnType.getExitOut() > 0) {
				canvas.drawText(turnType.getExitOut() + "", centerText.x,
						centerText.y - (textPaint.descent() + textPaint.ascent()) / 2, textPaint);
			}
		}
	}

	public void updateTextPaint(@NonNull TextPaint textPaint, boolean nightMode) {
		this.textPaint = textPaint;
		this.textPaint.setTextAlign(Paint.Align.CENTER);
		this.textPaint.setColor(ColorUtilities.getPrimaryTextColor(ctx, nightMode));
	}

	public void updateColors(boolean nightMode) {
		if (this.nightMode == null || this.nightMode != nightMode) {
			this.nightMode = nightMode;
			int outlayFillColor = ctx.getColor(nightMode
					? R.color.nav_arrow_circle_color_dark
					: R.color.nav_arrow_circle_color_light);
			paintTurnOutlayFill.setColor(outlayFillColor);
		}
	}

	@Nullable
	public TurnType getTurnType() {
		return turnType;
	}

	public boolean setTurnType(@Nullable TurnType turnType) {
		if (turnType != this.turnType && !getBounds().isEmpty()) {
			this.turnType = turnType;
			TurnPathHelper.calcTurnPath(originalPathForTurn, originalPathForTurnOutlay, turnType, null,
					centerText, false, false, true, false);
			onBoundsChange(getBounds());
			return true;
		}
		return false;
	}

	@Override
	public void setAlpha(int alpha) {
		paintRouteDirection.setAlpha(alpha);
	}

	@Override
	public void setColorFilter(ColorFilter cf) {
		paintRouteDirection.setColorFilter(cf);
	}

	@Override
	public int getOpacity() {
		return 0;
	}

	@ColorInt
	private int getColor(@ColorRes int colorId) {
		return ColorUtilities.getColor(ctx, colorId);
	}

	@ColorInt
	private int getColorFromAttr(@AttrRes int attrId) {
		return AndroidUtils.getColorFromAttr(ctx, attrId);
	}
}