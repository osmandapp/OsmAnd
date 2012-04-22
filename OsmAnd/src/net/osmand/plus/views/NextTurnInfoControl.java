package net.osmand.plus.views;

import net.osmand.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.plus.routing.RoutingHelper.TurnType;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;



public class NextTurnInfoControl extends MapInfoControl {

	private float scaleCoefficient = MapInfoLayer.scaleCoefficient;
	private final float width;
	private final float height ;
	private static final float miniCoeff = 2.5f;

	protected Path pathForTurn = new Path();

	protected TurnType turnType = null;
	protected String exitOut = null;
	protected int nextTurnDirection = 0;
	

	private final Paint textPaint;
	private final Paint subtextPaint;
	private Paint paintBlack;
	private Paint paintRouteDirection;

	protected boolean makeUturnWhenPossible;
	protected int turnImminent;
	private final boolean horisontalMini;

	public NextTurnInfoControl(Context ctx, Paint textPaint, Paint subtextPaint, boolean horisontalMini) {
		super(ctx);
		this.textPaint = textPaint;
		this.subtextPaint = subtextPaint;
		this.horisontalMini = horisontalMini;

		paintBlack = new Paint();
		paintBlack.setStyle(Style.STROKE);
		paintBlack.setColor(Color.BLACK);
		paintBlack.setAntiAlias(true);
		paintBlack.setStrokeWidth(2.5f);

		paintRouteDirection = new Paint();
		paintRouteDirection.setStyle(Style.FILL);
		paintRouteDirection.setColor(getResources().getColor(R.color.nav_arrow));
		paintRouteDirection.setAntiAlias(true);
		
		pathTransform = new Matrix();
		if (horisontalMini) {
			pathTransform.postScale(scaleCoefficient / miniCoeff, scaleCoefficient / miniCoeff);
			width = 72 * scaleCoefficient / miniCoeff;
			height = 72 * scaleCoefficient / miniCoeff;
		} else {
			pathTransform.postScale(scaleCoefficient, scaleCoefficient);
			width = 72 * scaleCoefficient;
			height = 72 * scaleCoefficient;
		}
		
	}

	protected Matrix pathTransform = new Matrix();

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int h = 0;
		int w = 0;
		if (!horisontalMini) {
			h = (int) (8 * scaleCoefficient + Math.max(textPaint.getTextSize(), subtextPaint.getTextSize()));
		} else {
			h = (int) (6 * scaleCoefficient);
			w = (int) textPaint.measureText(OsmAndFormatter.getFormattedDistance(nextTurnDirection, getContext()));
		}
		setWDimensions((int) width + w, (int) height + h);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (pathForTurn != null) {
			if (turnImminent == 1) {
				paintRouteDirection.setColor(getResources().getColor(R.color.nav_arrow_imminent));
			} else if (turnImminent == 0) {
				paintRouteDirection.setColor(getResources().getColor(R.color.nav_arrow));
			} else {
				paintRouteDirection.setColor(getResources().getColor(R.color.nav_arrow_distant));
			}
			// small indent
			canvas.translate(0, 3 * scaleCoefficient);
			canvas.drawPath(pathForTurn, paintRouteDirection);
			canvas.drawPath(pathForTurn, paintBlack);
			if (exitOut != null && !horisontalMini) {
				drawShadowText(canvas, exitOut, (getWWidth()) / 2 - 7 * scaleCoefficient, 
						getWHeight() / 2 - textPaint.getTextSize() / 2 + 3 * scaleCoefficient, textPaint);
			}
			String text = OsmAndFormatter.getFormattedDistance(nextTurnDirection, getContext());
			String subtext = null;

			if (makeUturnWhenPossible == true) {
				text = "ASAP";
			}

			int ls = text.lastIndexOf(' ');
			float st = 0;
			if (ls != -1) {
				subtext = text.substring(ls + 1);
				text = text.substring(0, ls);
				st = textPaint.measureText(subtext);
			}
			float mt = textPaint.measureText(text);
			if (!horisontalMini) {
				float startX = Math.max((getWWidth() - st - mt) / 2, 2 * scaleCoefficient);
				drawShadowText(canvas, text, startX, getWHeight() - 5 * scaleCoefficient, textPaint);
				if (subtext != null) {
					drawShadowText(canvas, subtext, startX + 2 * scaleCoefficient + mt, getWHeight() - 5 * scaleCoefficient, subtextPaint);
				}
			} else {
				drawShadowText(canvas, text, 72 * scaleCoefficient / miniCoeff + 2 * scaleCoefficient,
						height / 2 + 5 * scaleCoefficient, textPaint);
				if (subtext != null) {
					drawShadowText(canvas, subtext, 72 * scaleCoefficient / miniCoeff +  mt
							+ 2 * scaleCoefficient, height / 2 + 5 * scaleCoefficient, subtextPaint);
				}
			}
		}
	}

	@Override
	public boolean updateInfo() {
		return false;
	}
}