package net.osmand.plus.views;

import net.osmand.plus.R;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.view.View;

public class MiniMapControl extends MapInfoControl {
	private float scaleCoefficient = MapInfoLayer.scaleCoefficient;
	private final float scaleMiniRoute = 0.15f;
	private final float width = 96 * scaleCoefficient;
	private final float height = 96 * scaleCoefficient;
	private final float centerMiniRouteY = 3 * height / 4;
	private final float centerMiniRouteX = width / 2;

	private final OsmandMapTileView view;
	private Paint paintMiniRoute;
	private Paint fillBlack;
	protected Path miniMapPath = null;

	public MiniMapControl(Context ctx, OsmandMapTileView view) {
		super(ctx);
		this.view = view;

		fillBlack = new Paint();
		fillBlack.setStyle(Style.FILL_AND_STROKE);
		fillBlack.setColor(Color.BLACK);
		fillBlack.setAntiAlias(true);

		paintMiniRoute = new Paint();
		paintMiniRoute.setStyle(Style.STROKE);
		paintMiniRoute.setStrokeWidth(35 * scaleCoefficient);
		paintMiniRoute.setStrokeJoin(Join.ROUND);
		paintMiniRoute.setStrokeCap(Cap.ROUND);
		paintMiniRoute.setAntiAlias(true);

	}

	@Override
	public boolean updateInfo() {
		if(getVisibility() == View.VISIBLE) {
			invalidate();
		}
		return true;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		setWDimensions((int) width, (int) height);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		//to change color immediately when needed
		// could be deleted in future
		if (view.getSettings().FLUORESCENT_OVERLAYS.get() && false) {
			paintMiniRoute.setColor(getResources().getColor(R.color.nav_track_fluorescent));
		} else {
			paintMiniRoute.setColor(getResources().getColor(R.color.nav_track));
		}

		if (miniMapPath != null && !miniMapPath.isEmpty()) {
			canvas.save();
			canvas.translate(centerMiniRouteX - view.getCenterPointX(), centerMiniRouteY - view.getCenterPointY());
			canvas.scale(scaleMiniRoute, scaleMiniRoute, view.getCenterPointX(), view.getCenterPointY());
			canvas.rotate(view.getRotate(), view.getCenterPointX(), view.getCenterPointY());
			canvas.drawCircle(view.getCenterPointX(), view.getCenterPointY(), 3 / scaleMiniRoute, fillBlack);
			canvas.drawPath(miniMapPath, paintMiniRoute);
			canvas.restore();
		}
	}
}