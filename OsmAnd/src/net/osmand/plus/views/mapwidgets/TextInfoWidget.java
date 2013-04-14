package net.osmand.plus.views.mapwidgets;

import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;

public class TextInfoWidget extends BaseMapWidget {

	String text;
	Paint textPaint;
	String subtext;
	Paint subtextPaint;
	int leftMargin = 0;
	private Drawable imageDrawable;
	private float scaleCoefficient;

	public TextInfoWidget(Context ctx, int leftMargin, Paint textPaint, Paint subtextPaint) {
		super(ctx);
		scaleCoefficient = MapInfoLayer.scaleCoefficient;
		this.leftMargin = leftMargin;
		this.textPaint = textPaint;
		this.subtextPaint = subtextPaint;
	}
	
	public void setImageDrawable(Drawable imageDrawable) {
		this.imageDrawable = imageDrawable;
	}
	
	public Drawable getImageDrawable() {
		return imageDrawable;
	}

	@Override
	protected void onWLayout(int w, int h) {
		if (imageDrawable != null) {
			// Unknown reason to add 3*scaleCoefficient
			imageDrawable.setBounds(0, (int) (3 * scaleCoefficient), imageDrawable.getMinimumWidth(), imageDrawable.getMinimumHeight()
					+ (int) (3 * scaleCoefficient));
		}
	}

	public void setText(String text, String subtext) {
		this.text = text;
		this.subtext = subtext;
		if (text != null) {
			if (subtext != null)
				setContentDescription(text + " " + subtext); //$NON-NLS-1$
			else setContentDescription(text);
		} else {
			setContentDescription(subtext);
		}
		updateVisibility(text != null);
		requestLayout();
		invalidate();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// ignore attributes
		int w = 0;
		int h = 0;
		if (text != null) {
			if (imageDrawable != null) {
				w += imageDrawable.getMinimumWidth() + 2 * scaleCoefficient;
			}
			w += leftMargin;
			w += textPaint.measureText(text);
			if (subtext != null) {
				w += subtextPaint.measureText(subtext) + 2 * scaleCoefficient;
			}

			h = (int) (5 * scaleCoefficient + Math.max(textPaint.getTextSize(), subtextPaint.getTextSize()));
			if (imageDrawable != null) {
				h = Math.max(h, (int) (imageDrawable.getMinimumHeight()));
			}
		}
		setWDimensions(w, h);
	}

	@Override
	protected void onDraw(Canvas cv) {
		super.onDraw(cv);
		if (isVisible()) {
			int margin = 0;
			if (imageDrawable != null) {
				imageDrawable.draw(cv);
				margin = (int) (imageDrawable.getBounds().width() + 2 * scaleCoefficient);
			}
			margin += leftMargin;
			drawShadowText(cv, text, margin, getWHeight() - 3 * scaleCoefficient, textPaint);
			if (subtext != null) {
				drawShadowText(cv, subtext, margin + 2 * scaleCoefficient + textPaint.measureText(text), getWHeight() - 3
						* scaleCoefficient, subtextPaint);
			}
		}
	}
	

	public boolean isVisible() {
		return text != null && (text.length() > 0 || subtext != null);
	}

	@Override
	public boolean updateInfo(DrawSettings drawSettings) {
		return false;
	}
	
}