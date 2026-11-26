package net.osmand.plus.views.mapwidgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import net.osmand.plus.R;
import net.osmand.plus.views.TurnPathHelper;
import net.osmand.router.TurnType;

public class TurnDrawable extends Drawable {

	protected Paint paintBlack;
	protected Paint paintRouteDirection;
	protected Path pathForTurn = new Path();
	protected Path pathForTurnOutlay = new Path();
	protected TurnType turnType;
	protected int turnImminent;
	protected boolean deviatedFromRoute;
	private final Context ctx;
	private final boolean mini;
	private final PointF centerText;
	private TextPaint textPaint;
	private int clr;

	public TurnDrawable(Context ctx, boolean mini) {
		this.ctx = ctx;
		this.mini = mini;
		centerText = new PointF();
		paintBlack = new Paint();
		paintBlack.setStyle(Paint.Style.STROKE);
		paintBlack.setColor(Color.BLACK);
		paintBlack.setAntiAlias(true);
		paintBlack.setStrokeWidth(2.5f);

		paintRouteDirection = new Paint();
		paintRouteDirection.setStyle(Paint.Style.FILL);
		paintRouteDirection.setAntiAlias(true);
		setColor(R.color.nav_arrow);
	}

	public void setColor(@ColorRes int clr) {
		if (clr != this.clr) {
			this.clr = clr;
			paintRouteDirection.setColor(ContextCompat.getColor(ctx, clr));
		}
	}

	@Override
	protected void onBoundsChange(Rect bounds) {
		Matrix m = new Matrix();
		float scaleX = bounds.width() / 72f;
		float scaleY = bounds.height() / 72f;
		m.setScale(scaleX, scaleY);
		pathForTurn.transform(m, pathForTurn);
		centerText.x = scaleX * centerText.x;
		centerText.y = scaleY * centerText.y;
		pathForTurnOutlay.transform(m, pathForTurnOutlay);
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
			paintRouteDirection.setColor(ctx.getColor(R.color.nav_arrow_distant));
		} else if (turnImminent > 0) {
			paintRouteDirection.setColor(ctx.getColor(R.color.nav_arrow));
		} else if (turnImminent == 0) {
			paintRouteDirection.setColor(ctx.getColor(R.color.nav_arrow_imminent));
		} else {
			paintRouteDirection.setColor(ctx.getColor(R.color.nav_arrow_distant));
		}
		invalidateSelf();
	}

	@Override
	public void draw(@NonNull Canvas canvas) {
		/// small indent
		// canvas.translate(0, 3 * scaleCoefficient);
		canvas.drawPath(pathForTurnOutlay, paintBlack);
		canvas.drawPath(pathForTurn, paintRouteDirection);
		canvas.drawPath(pathForTurn, paintBlack);
		if (textPaint != null) {
			if (turnType != null && !mini && turnType.getExitOut() > 0) {
				canvas.drawText(turnType.getExitOut() + "", centerText.x,
						centerText.y - (textPaint.descent() + textPaint.ascent()) / 2, textPaint);
			}
		}
	}

	public void setTextPaint(TextPaint textPaint) {
		this.textPaint = textPaint;
		this.textPaint.setTextAlign(Paint.Align.CENTER);
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

	public TurnType getTurnType() {
		return turnType;
	}

	public boolean setTurnType(TurnType turnType) {
		if (turnType != this.turnType && !getBounds().isEmpty()) {
			this.turnType = turnType;
			TurnPathHelper.calcTurnPath(pathForTurn, pathForTurnOutlay, turnType, null,
					centerText, mini, false, true, false);
			onBoundsChange(getBounds());
			return true;
		}
		return false;
	}
}