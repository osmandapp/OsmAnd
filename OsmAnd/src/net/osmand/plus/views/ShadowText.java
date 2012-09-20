package net.osmand.plus.views;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;

public class ShadowText {
	private String text;
	
	public ShadowText(String text) {
		this.text = text;
	}
	public static ShadowText create(String text) {
		return new ShadowText(text);
	}


	
	public void draw(Canvas cv, float centerX, float centerY, Paint textPaint, int shadowColor) {
		draw(text, cv, centerX, centerY, textPaint, shadowColor);
	}
	public static void draw(String text, Canvas cv, float centerX, float centerY, Paint textPaint, int shadowColor) {
		int c = textPaint.getColor();
		textPaint.setStyle(Style.STROKE);
		textPaint.setColor(shadowColor);
		textPaint.setStrokeWidth(4);
		cv.drawText(text, centerX, centerY, textPaint);
		// reset
		textPaint.setStrokeWidth(1);
		textPaint.setStyle(Style.FILL);
		textPaint.setColor(c);
		cv.drawText(text, centerX, centerY, textPaint);
	}
	
	public String getText() {
		return text;
	}
}
