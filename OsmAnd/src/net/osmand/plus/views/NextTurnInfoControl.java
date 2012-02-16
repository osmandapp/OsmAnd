package net.osmand.plus.views;

import net.osmand.OsmAndFormatter;
import net.osmand.plus.routing.RoutingHelper.TurnType;
import net.osmand.plus.routing.RoutingHelper;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;


public class NextTurnInfoControl extends MapInfoControl {

	private float scaleCoefficient = MapInfoLayer.scaleCoefficient;
	private final float width = 72 * scaleCoefficient;
	private final float height = 75 * scaleCoefficient;

	protected Path pathForTurn = new Path();

	protected TurnType turnType = null;
	protected String exitOut = null;
	protected int nextTurnDirection = 0;

	private final Paint textPaint;
	private final Paint subtextPaint;
	private Paint paintBlack;
	private Paint paintRouteDirection;

	private boolean makeUturnWhenPossible;
	private boolean turnImminent;

	public NextTurnInfoControl(Context ctx, Paint textPaint, Paint subtextPaint) {
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
		pathTransform.postScale(scaleCoefficient, scaleCoefficient);
	}

	protected Matrix pathTransform = new Matrix();

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int h = (int) (5 * scaleCoefficient + Math.max(textPaint.getTextSize(), subtextPaint.getTextSize()));
		setWDimensions((int) width, (int) height + h);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (pathForTurn != null) {
			turnImminent = RoutingHelper.turnImminent();
			if (turnImminent == false) {
				paintRouteDirection.setColor(getResources().getColor(R.color.nav_arrow));
			} else {
				paintRouteDirection.setColor(getResources().getColor(R.color.nav_arrow_imminent));
			}
			// small indent
			canvas.translate(0, 3 * scaleCoefficient);
			canvas.drawPath(pathForTurn, paintRouteDirection);
			canvas.drawPath(pathForTurn, paintBlack);
			if (exitOut != null) {
				drawShadowText(canvas, exitOut, (getWWidth()) / 2 - 7 * scaleCoefficient, 
						getWHeight() / 2 - textPaint.getTextSize() / 2 + 3 * scaleCoefficient, textPaint);
			}
			String text = OsmAndFormatter.getFormattedDistance(nextTurnDirection, getContext());
			String subtext = null;

			// Issue 863: distance "as soon as possible" should be displayed for unscheduled U-turn
			makeUturnWhenPossible = RoutingHelper.makeUturnWhenPossible();
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
			float startX = Math.max((getWWidth() - st - mt) / 2, 2 * scaleCoefficient);
			drawShadowText(canvas, text, startX, getWHeight() - 5 * scaleCoefficient, textPaint);
			if (subtext != null) {
				drawShadowText(canvas, subtext, startX + 2 * scaleCoefficient + mt, getWHeight() - 5 * scaleCoefficient, subtextPaint);
			}
		}
	}

	@Override
	public boolean updateInfo() {
		return false;
	}
}