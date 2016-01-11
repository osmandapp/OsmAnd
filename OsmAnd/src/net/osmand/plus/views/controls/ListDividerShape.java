package net.osmand.plus.views.controls;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.shapes.Shape;

public class ListDividerShape extends Shape {
	private RectF mRect = new RectF();
	private int lineColor;
	private int paddingLeft;
	private int pointColor;
	private float pointRadius;
	private boolean drawPoint;

	public ListDividerShape(int lineColor, int paddingLeft) {
		this.lineColor = lineColor;
		this.paddingLeft = paddingLeft;
		this.pointColor = 0;
		this.pointRadius = 0f;
		this.drawPoint = false;
	}

	public ListDividerShape(int lineColor, int paddingLeft, int pointColor, float pointRadius, boolean drawPoint) {
		this.lineColor = lineColor;
		this.paddingLeft = paddingLeft;
		this.pointColor = pointColor;
		this.pointRadius = pointRadius;
		this.drawPoint = drawPoint;
	}

	@Override
	public void draw(Canvas canvas, Paint paint) {
		paint.setColor(lineColor);
		mRect.left = paddingLeft;
		canvas.drawRect(mRect, paint);
		if (paddingLeft > 0 && drawPoint) {
			paint.setColor(pointColor);
			canvas.drawCircle(paddingLeft / 2f, (mRect.bottom - mRect.top) / 2f, pointRadius, paint);
		}
	}

	@Override
	protected void onResize(float width, float height) {
		mRect.set(0, 0, width, height);
	}

	@Override
	public ListDividerShape clone() throws CloneNotSupportedException {
		final ListDividerShape shape = (ListDividerShape) super.clone();
		shape.mRect = new RectF(mRect);
		shape.lineColor = lineColor;
		shape.paddingLeft = paddingLeft;
		shape.pointColor = pointColor;
		shape.pointRadius = pointRadius;
		shape.drawPoint = drawPoint;
		return shape;
	}
}
