package net.osmand.plus.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.view.View;

public class TextInfoControl extends MapInfoControl {
		
		String text;
		Paint textPaint;
		String subtext;
		Paint subtextPaint;
		int leftMargin = 0;
		private Drawable imageDrawable;
		private float scaleCoefficient;
		
		public TextInfoControl(Context ctx, int background, Drawable drawable, int leftMargin, Paint textPaint,
				Paint subtextPaint) {
			super(ctx, background);
			scaleCoefficient = MapInfoLayer.scaleCoefficient;
			this.leftMargin = leftMargin;
			this.imageDrawable = drawable;
			this.textPaint = textPaint;
			this.subtextPaint = subtextPaint;
		}
		
		@Override
		protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
			super.onLayout(changed, left, top, right, bottom);
			if (imageDrawable != null) {
				// Unknown reason to add 3*scaleCoefficient
				imageDrawable.setBounds(0, (int) (3 *  scaleCoefficient), imageDrawable.getMinimumWidth(), imageDrawable.getMinimumHeight()
						+ (int) (3 * scaleCoefficient));
			}
		}
		
		public void setText(String text, String subtext) {
			this.text = text;
			this.subtext = subtext;
			updateVisibility(text == null);
			requestLayout();
			invalidate();
		}

		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			// ignore attributes
			int w = 0;
			int h = 0;
			if (text != null) {
				if(imageDrawable != null) {
					w += imageDrawable.getMinimumWidth();
				}
				w += leftMargin;
				w += textPaint.measureText(text);
				if (subtext != null) {
					w += subtextPaint.measureText(subtext) + 2 * scaleCoefficient;
				}
				
				h = (int) (5 * scaleCoefficient + Math.max(textPaint.getTextSize(), subtextPaint.getTextSize()));
				if(imageDrawable != null) {
					h = Math.max(h, (int)(imageDrawable.getMinimumHeight()));
				}
			}
			setMeasuredDimension(w, h);
		}
		
		@Override
		protected void onDraw(Canvas cv) {
			super.onDraw(cv);
			if (isVisible()) {
				int margin = 0;
				if(imageDrawable != null) {
					imageDrawable.draw(cv);
					margin = imageDrawable.getBounds().width();
				}
				margin += leftMargin;
				cv.drawText(text, margin, getBottom() - getTop() - 3 * scaleCoefficient, textPaint);
				if (subtext != null) {
					cv.drawText(subtext, margin + 2 * scaleCoefficient + textPaint.measureText(text), 
							getBottom() - getTop() - 3 * scaleCoefficient, subtextPaint);
				}
			}
		}
		
		public boolean isVisible() {
			return text != null && text.length() > 0;
		}

		@Override
		public boolean updateInfo() {
			return false;
		}
	}