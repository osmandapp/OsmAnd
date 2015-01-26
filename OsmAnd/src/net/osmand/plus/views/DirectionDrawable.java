package net.osmand.plus.views;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import net.osmand.plus.R;

/**
 * Created by Denis
 * on 10.12.2014.
 */
public class DirectionDrawable extends Drawable {
	Paint paintRouteDirection;
	float width;
	float height;
	Context ctx;
	private float angle;
	int resourceId = -1;

	public DirectionDrawable(Context ctx, float width, float height, int resourceId) {
		this(ctx, width, height);
		this.resourceId = resourceId;
	}


	public DirectionDrawable(Context ctx, float width, float height) {
		this.ctx = ctx;
		this.width = width;
		this.height = height;
		paintRouteDirection = new Paint();
		paintRouteDirection.setStyle(Paint.Style.FILL_AND_STROKE);
		paintRouteDirection.setColor(ctx.getResources().getColor(R.color.color_unknown));
		paintRouteDirection.setAntiAlias(true);
	}

	public void setOpenedColor(int opened) {
		if (opened == 0) {
			paintRouteDirection.setColor(ctx.getResources().getColor(R.color.color_ok));
		} else if (opened == -1) {
			paintRouteDirection.setColor(ctx.getResources().getColor(R.color.color_unknown));
		} else {
			paintRouteDirection.setColor(ctx.getResources().getColor(R.color.color_warning));
		}
	}


	public void setAngle(float angle) {
		this.angle = angle;
	}

	@Override
	public void draw(Canvas canvas) {
		if (resourceId != -1) {
			canvas.rotate(angle, canvas.getHeight()/2, canvas.getWidth()/2);
			Bitmap arrow = BitmapFactory.decodeResource(ctx.getResources(), resourceId);
			canvas.drawBitmap(arrow, null, new Rect(0,0,arrow.getHeight(), arrow.getWidth()), null);
		} else {
			canvas.rotate(angle, canvas.getHeight()/2, canvas.getWidth() / 2);
			Path directionPath = createDirectionPath();
			canvas.drawPath(directionPath, paintRouteDirection);
		}
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
		WindowManager mgr = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
		mgr.getDefaultDisplay().getMetrics(dm);
		pathTransform.postScale(dm.density, dm.density);
		path.transform(pathTransform);
		width *= dm.density;
		height *= dm.density;
		return path;
	}

}
