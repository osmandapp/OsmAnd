package net.osmand.plus.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

/**
 * Created by Denis
 * on 10.12.2014.
 */
public class DirectionDrawable extends Drawable {
	Paint paintRouteDirection;
	float width;
	float height;
	private float angle;
	int resourceId = -1;
	Drawable arrowImage ;
	private final OsmandApplication app;

	public DirectionDrawable(OsmandApplication ctx, float width, float height, int resourceId, int clrId) {
		this(ctx, width, height);
		UiUtilities iconsCache = ctx.getUIUtilities();
		arrowImage = iconsCache.getIcon(resourceId, clrId);
		this.resourceId = resourceId;
	}
	
	public DirectionDrawable(OsmandApplication app, float width, float height) {
		this.app = app;
		this.width = width;
		this.height = height;
		paintRouteDirection = new Paint();
		paintRouteDirection.setStyle(Paint.Style.FILL_AND_STROKE);
		paintRouteDirection.setColor(ColorUtilities.getColor(app, R.color.color_unknown));
		paintRouteDirection.setAntiAlias(true);
	}
	
	public void setImage(int resourceId, int clrId) {
		UiUtilities iconsCache = app.getUIUtilities();
		arrowImage = iconsCache.getIcon(resourceId, clrId);
		this.resourceId = resourceId;
		onBoundsChange(getBounds());
	}

	public void setImage(int resourceId) {
		UiUtilities iconsCache = app.getUIUtilities();
		arrowImage = iconsCache.getIcon(resourceId, 0);
		this.resourceId = resourceId;
		onBoundsChange(getBounds());
	}
	
	
	public void setColorId(int clrId) {
		// R.color.color_ok, R.color.color_unknown, R.color.color_warning
		if(arrowImage != null) {
			UiUtilities iconsCache = app.getUIUtilities();
			arrowImage = iconsCache.getIcon(resourceId, clrId);
		} else {
			paintRouteDirection.setColor(ColorUtilities.getColor(app, clrId));
		}
	}


	public void setAngle(float angle) {
		this.angle = angle;
	}
	

	@Override
	public int getIntrinsicWidth() {
		if (arrowImage != null) {
			return arrowImage.getIntrinsicWidth();
		}
		return super.getIntrinsicWidth();
	}
	
	@Override
	public int getIntrinsicHeight() {
		if (arrowImage != null) {
			return arrowImage.getIntrinsicHeight();
		}
		return super.getIntrinsicHeight();
	}
	
	@Override
	protected void onBoundsChange(Rect bounds) {
		super.onBoundsChange(bounds);
		if (arrowImage != null) {
			Rect rect = bounds;
			int w = arrowImage.getIntrinsicWidth();
			int h = arrowImage.getIntrinsicHeight();
			int dx = Math.max(0, rect.width() - w);
			int dy = Math.max(0, rect.height() - h);
			if(rect.width() == 0 && rect.height() == 0) {
				arrowImage.setBounds(0, 0, w, h);
			} else {
				arrowImage.setBounds(rect.left + dx / 2, rect.top + dy / 2, rect.right - dx / 2, rect.bottom - dy / 2);
			}
		}
	}

	@Override
	public void draw(Canvas canvas) {
		canvas.save();
		if (arrowImage != null) {
			Rect r = getBounds();
			canvas.rotate(angle, r.centerX(), r.centerY());
			arrowImage.draw(canvas);
		} else {
			canvas.rotate(angle, canvas.getWidth() / 2, canvas.getHeight() / 2);
			Path directionPath = createDirectionPath();
			canvas.drawPath(directionPath, paintRouteDirection);
		}
		canvas.restore();
	}

	@Override
	public int getOpacity() {
		return 0;
	}

	@Override
	public void setAlpha(int alpha) {
		paintRouteDirection.setAlpha(alpha);

	}

	@Override
	public void setColorFilter(ColorFilter cf) {
		paintRouteDirection.setColorFilter(cf);
	}

	private Path createDirectionPath() {
		int h = 15;
		int w = 4;
		float sarrowL = 8; // side of arrow
		float harrowL = (float) Math.sqrt(2) * sarrowL; // hypotenuse of arrow
		float hpartArrowL = (harrowL - w) / 2;
		Path path = new Path();
		path.moveTo(width / 2, height - (height - h) / 3);
		path.rMoveTo(w / 2, 0);
		path.rLineTo(0, -h);
		path.rLineTo(hpartArrowL, 0);
		path.rLineTo(-harrowL / 2, -harrowL / 2); // center
		path.rLineTo(-harrowL / 2, harrowL / 2);
		path.rLineTo(hpartArrowL, 0);
		path.rLineTo(0, h);

		DisplayMetrics dm = new DisplayMetrics();
		Matrix pathTransform = new Matrix();
		WindowManager mgr = (WindowManager) app.getSystemService(Context.WINDOW_SERVICE);
		mgr.getDefaultDisplay().getMetrics(dm);
		pathTransform.postScale(dm.density, dm.density);
		path.transform(pathTransform);
		width *= dm.density;
		height *= dm.density;
		return path;
	}

}
