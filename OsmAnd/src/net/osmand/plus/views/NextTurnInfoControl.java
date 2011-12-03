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

	public NextTurnInfoControl(Context ctx, int background, Paint textPaint, Paint subtextPaint) {
		super(ctx, background);
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
	}

	protected Matrix pathTransform = new Matrix();

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		pathTransform = new Matrix();
		pathTransform.postScale(scaleCoefficient, scaleCoefficient);
		pathTransform.postTranslate(left, top);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int h = (int) (5 * scaleCoefficient + Math.max(textPaint.getTextSize(), subtextPaint.getTextSize()));
		setMeasuredDimension((int) width, (int) height + h);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (pathForTurn != null) {
			canvas.drawPath(pathForTurn, paintRouteDirection);
			canvas.drawPath(pathForTurn, paintBlack);
			// TODO test
			if (exitOut != null) {
				canvas.drawText(exitOut, (getLeft() + getRight()) / 2 - 6 * scaleCoefficient, (getTop() + getBottom()) / 2 - 9
						* scaleCoefficient, paintBlack);
			}
			String text = OsmAndFormatter.getFormattedDistance(nextTurnDirection, getContext());
			String subtext = null;
			int ls = text.lastIndexOf(' ');
			if (ls != -1) {
				subtext = text.substring(ls + 1);
				text = text.substring(0, ls);
			}
			// TODO align center
			int margin = (int) (10 * scaleCoefficient);
			canvas.drawText(text, margin + getLeft(), getBottom() - 3 * scaleCoefficient, textPaint);
			if (subtext != null) {
				canvas.drawText(subtext, getLeft() + margin + 2 * scaleCoefficient + textPaint.measureText(text), getBottom() - 3
						* scaleCoefficient, subtextPaint);
			}
		}
	}

	@Override
	public boolean updateInfo() {
		return false;
	}
}