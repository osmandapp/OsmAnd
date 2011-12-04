package net.osmand.plus.views;

import net.osmand.OsmAndFormatter;
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
	private final float width = 96 * scaleCoefficient;
	private final float height = 96 * scaleCoefficient;

	protected Path pathForTurn = new Path();

	protected TurnType turnType = null;
	protected String exitOut = null;
	protected int nextTurnDirection = 0;

	private final Paint textPaint;
	private final Paint subtextPaint;
	private Paint paintBlack;
	private Paint paintRouteDirection;

	public NextTurnInfoControl(Context ctx, Paint textPaint, Paint subtextPaint) {
		super(ctx);
		this.textPaint = textPaint;
		this.subtextPaint = subtextPaint;

		paintBlack = new Paint();
		paintBlack.setStyle(Style.STROKE);
		paintBlack.setColor(Color.BLACK);
		paintBlack.setAntiAlias(true);

		paintRouteDirection = new Paint();
		paintRouteDirection.setStyle(Style.FILL_AND_STROKE);
		paintRouteDirection.setColor(Color.rgb(100, 0, 255));
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
			canvas.drawPath(pathForTurn, paintRouteDirection);
			canvas.drawPath(pathForTurn, paintBlack);
			// TODO test
			if (exitOut != null) {
				canvas.drawText(exitOut, (getWWidth()) / 2 - 6 * scaleCoefficient, getWHeight() / 2 - 9
						* scaleCoefficient, paintBlack);
			}
			String text = OsmAndFormatter.getFormattedDistance(nextTurnDirection, getContext());
			String subtext = null;
			int ls = text.lastIndexOf(' ');
			float st = 0;
			if (ls != -1) {
				subtext = text.substring(ls + 1);
				text = text.substring(0, ls);
				st = textPaint.measureText(subtext);
			}
			float mt = textPaint.measureText(text);
			drawShadowText(canvas, text, 
					(getWWidth() - st - mt) / 2 - scaleCoefficient, getWHeight() - 3 * scaleCoefficient, textPaint);
			if (subtext != null) {
				drawShadowText(canvas, subtext, (getWWidth() - st - mt) / 2 + 2 * scaleCoefficient + mt, getWHeight() - 3
						* scaleCoefficient, subtextPaint);
			}
		}
	}

	@Override
	public boolean updateInfo() {
		return false;
	}
}