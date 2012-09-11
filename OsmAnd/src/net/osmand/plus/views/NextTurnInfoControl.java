package net.osmand.plus.views;

import net.osmand.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.router.TurnType;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;



public class NextTurnInfoControl extends MapInfoControl {

	private float scaleCoefficient = MapInfoLayer.scaleCoefficient;
	private float width;
	private float height ;
	private static final float miniCoeff = 2.5f;

	protected Path pathForTurn = new Path();

	protected TurnType turnType = null;
	protected String exitOut = null;
	protected int nextTurnDirection = 0;
	

	protected Paint textPaint;
	protected Paint subtextPaint;
	private Paint paintBlack;
	private Paint paintRouteDirection;

	protected boolean makeUturnWhenPossible;
	protected int turnImminent;
	protected boolean horisontalMini;

	public NextTurnInfoControl(Context ctx, Paint textPaint, Paint subtextPaint, boolean horisontalMini) {
		super(ctx);
		this.textPaint = textPaint;
		this.subtextPaint = subtextPaint;


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
		updateHorisontalMini(horisontalMini);
		
	}

	protected void updateHorisontalMini(boolean horisontalMini) {
		this.horisontalMini = horisontalMini;
		if (horisontalMini) {
			pathTransform.postScale(scaleCoefficient / miniCoeff, scaleCoefficient / miniCoeff);
			width = 72 * scaleCoefficient / miniCoeff;
			height = 72 * scaleCoefficient / miniCoeff;
		} else {
			pathTransform.postScale(scaleCoefficient, scaleCoefficient);
			width = 72 * scaleCoefficient;
			height = 72 * scaleCoefficient;
		}
		requestLayout();
	}
	

	protected Matrix pathTransform = new Matrix();

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int h = 0;
		int w = 0;
		if (!horisontalMini) {
			h = (int) (8 * scaleCoefficient + Math.max(textPaint.getTextSize(), subtextPaint.getTextSize()));
		} else {
			h = (int) (7 * scaleCoefficient);
			w = (int) textPaint.measureText(OsmAndFormatter.getFormattedDistance(nextTurnDirection, getContext()));
		}
		setWDimensions((int) width + w, (int) height + h);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (pathForTurn != null) {
			if (turnImminent > 0) {
				paintRouteDirection.setColor(getResources().getColor(R.color.nav_arrow));
			} else if (turnImminent == 0) {
				paintRouteDirection.setColor(getResources().getColor(R.color.nav_arrow_imminent));
			} else {
				paintRouteDirection.setColor(getResources().getColor(R.color.nav_arrow_distant));
			}
			// small indent
			canvas.translate(0, 3 * scaleCoefficient);
			canvas.drawPath(pathForTurn, paintRouteDirection);
			canvas.drawPath(pathForTurn, paintBlack);
			if (exitOut != null && !horisontalMini && !makeUturnWhenPossible) {
				drawShadowText(canvas, exitOut, width / 2 - 7 * scaleCoefficient, 
						height / 2 + textPaint.getTextSize() / 2 - 3 * scaleCoefficient, textPaint);
			}
			String text = OsmAndFormatter.getFormattedDistance(nextTurnDirection, getContext());
			String subtext = null;

			if (makeUturnWhenPossible == true) {
				text = getResources().getString(R.string.asap);
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
						height / 2 + 7 * scaleCoefficient, textPaint);
				if (subtext != null) {
					drawShadowText(canvas, subtext, 72 * scaleCoefficient / miniCoeff +  mt
							+ 2 * scaleCoefficient, height / 2 + 7 * scaleCoefficient, subtextPaint);
				}
			}
		}
	}

	@Override
	public boolean updateInfo() {
		return false;
	}
}