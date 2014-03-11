package net.osmand.plus.views.controls;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmAndConstants;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.BaseMapLayer;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.ShadowText;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.text.TextPaint;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.FrameLayout;

public class MapZoomControls extends MapControls {
	private static final int SHOW_ZOOM_LEVEL_MSG_ID = OsmAndConstants.UI_HANDLER_MAP_CONTROLS + 1;
	private static final int SHOW_ZOOM_BUTTON_MSG_ID = OsmAndConstants.UI_HANDLER_MAP_CONTROLS + 2;
	private static final int SHOW_ZOOM_LEVEL_DELAY = 1000;
	private static final int SHOW_ZOOM_LEVEL_BUTTON_DELAY = 1500;
	
	private Button zoomInButton;
	private Button zoomOutButton;

	private TextPaint zoomTextPaint;
	private Drawable zoomShadow;
	private Bitmap mapMagnifier;
	private Paint bitmapPaint;
	private boolean showZoomLevel = false;
	private boolean showZoomLevelButton = false;
	private OsmandMapTileView view;

	public MapZoomControls(MapActivity mapActivity, Handler showUIHandler, float scaleCoefficient) {
		super(mapActivity, showUIHandler, scaleCoefficient);
		view = mapActivity.getMapView();
	}

	public boolean onSingleTap(PointF point, RotatedTileBox tileBox) {
		if (isShowZoomLevel() && zoomShadow.getBounds().contains((int) point.x, (int) point.y)) {
			MapMagnifier.getOnClickMagnifierListener(view).onLongClick(null);
			return true;
		}
		return false;
	}

	
	@Override
	protected void showControls(FrameLayout parent) {
		int minimumWidth = view.getResources().getDrawable(R.drawable.map_zoom_in).getMinimumWidth();
		Context ctx = view.getContext();
		zoomInButton = new Button(ctx);
		zoomInButton.setBackgroundResource(R.drawable.map_zoom_in);
		android.widget.FrameLayout.LayoutParams params = 
				new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM
				| Gravity.RIGHT);
		params.setMargins(0, 0, 0, 0);
		zoomInButton.setContentDescription(ctx.getString(R.string.zoomIn));
		parent.addView(zoomInButton, params);

		zoomOutButton = new Button(ctx);
		zoomOutButton.setBackgroundResource(R.drawable.map_zoom_out);
		params = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM
				| Gravity.RIGHT);

		params.setMargins(0, 0, minimumWidth, 0);
		zoomOutButton.setContentDescription(ctx.getString(R.string.zoomOut));
		parent.addView(zoomOutButton, params);

		mapActivity.accessibleContent.add(zoomInButton);
		mapActivity.accessibleContent.add(zoomOutButton);

		zoomInButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (view.isZooming()) {
					mapActivity.changeZoom(2);
				} else {
					mapActivity.changeZoom(1);
				}

			}
		});
		final View.OnLongClickListener listener = MapMagnifier.getOnClickMagnifierListener(view);
		zoomInButton.setOnLongClickListener(listener);
		zoomOutButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mapActivity.changeZoom(-1);
			}
		});
		zoomOutButton.setOnLongClickListener(listener);		
	}

	@Override
	public void initControls(FrameLayout parent) {
		zoomTextPaint = new TextPaint();
		zoomTextPaint.setTextSize(18 * scaleCoefficient);
		zoomTextPaint.setAntiAlias(true);
		zoomTextPaint.setFakeBoldText(true);

		zoomShadow = view.getResources().getDrawable(R.drawable.zoom_background).mutate();
		mapMagnifier = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_magnifier);
		bitmapPaint = new Paint();

	}

	@Override
	public int getWidth() {
		return 0; // TODO
	}

	@Override
	public void hideControls(FrameLayout layout) {
		mapActivity.accessibleContent.remove(zoomInButton);
		mapActivity.accessibleContent.remove(zoomOutButton);
	}

	private void drawZoomLevel(Canvas canvas, RotatedTileBox tb, boolean drawZoomLevel) {
		if (zoomShadow.getBounds().width() == 0) {
			zoomShadow.setBounds(zoomInButton.getLeft() - 2, zoomInButton.getTop() - (int) (18 * scaleCoefficient),
					zoomInButton.getRight(), zoomInButton.getBottom());
		}
		zoomShadow.draw(canvas);
		if (drawZoomLevel) {
			String zoomText = tb.getZoom() + "";
			float frac = tb.getZoomScale();
			if (frac != 0) {
				int ifrac = (int) (frac * 10);
				boolean pos = ifrac > 0;
				zoomText += (pos ? "+" : "-");
				zoomText += Math.abs(ifrac) / 10;
				if (ifrac % 10 != 0) {
					zoomText += "." + Math.abs(ifrac) % 10;
				}
			}
			float length = zoomTextPaint.measureText(zoomText);

			ShadowText.draw(zoomText, canvas, zoomInButton.getLeft() + (zoomInButton.getWidth() - length - 2) / 2,
					zoomInButton.getTop() + 4 * scaleCoefficient, zoomTextPaint, shadowColor);
		} else {
			int size = (int) (16 * scaleCoefficient);
			int top = (int) (zoomInButton.getTop() - size - 4 * scaleCoefficient);
			int left = (int) (zoomInButton.getLeft() + (zoomInButton.getWidth() - mapMagnifier.getWidth() - 2 * scaleCoefficient) / 2);
			// canvas density /2 ? size * 2
			canvas.drawBitmap(mapMagnifier, null, new Rect(left, top, left + size * 2, top + size * 2), bitmapPaint);
		}
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings nightMode) {
		BaseMapLayer mainLayer = view.getMainLayer();
		boolean zoomInEnabled = mainLayer != null && tileBox.getZoom() < mainLayer.getMaximumShownMapZoom();
		boolean zoomOutEnabled = mainLayer != null && tileBox.getZoom() > mainLayer.getMinimumShownMapZoom();
		if (zoomInButton.isEnabled() != zoomInEnabled) {
			zoomInButton.setEnabled(zoomInEnabled);
		}
		if (zoomOutButton.isEnabled() != zoomOutEnabled) {
			zoomOutButton.setEnabled(zoomOutEnabled);
		}

		if (view.isZooming()) {
			showZoomLevel =  true;
			showZoomLevelButton = false;
			showUIHandler.removeMessages(SHOW_ZOOM_LEVEL_MSG_ID);
			showUIHandler.removeMessages(SHOW_ZOOM_BUTTON_MSG_ID);
		} else {
			if (isShowZoomLevel() && view.getSettings().SHOW_RULER.get()) {
				hideZoomLevelInTime();
			}
		}
		boolean drawZoomLevel = isShowZoomLevel() || !view.getSettings().SHOW_RULER.get();
		if (drawZoomLevel) {
			drawZoomLevel(canvas, tileBox, !showZoomLevelButton);
		}
	}

	private void sendMessageToShowZoomLevel() {
		Message msg = Message.obtain(showUIHandler, new Runnable() {
			@Override
			public void run() {
				showZoomLevelButton = true;
				sendMessageToShowZoomButton();
				view.refreshMap();
			}

		});
		msg.what = SHOW_ZOOM_LEVEL_MSG_ID;
		showUIHandler.sendMessageDelayed(msg, SHOW_ZOOM_LEVEL_DELAY);
	}

	private void sendMessageToShowZoomButton() {
		Message msg = Message.obtain(showUIHandler, new Runnable() {
			@Override
			public void run() {
				showZoomLevelButton = false;
				showZoomLevel = false;
				view.refreshMap();
			}

		});
		msg.what = SHOW_ZOOM_BUTTON_MSG_ID;
		showUIHandler.sendMessageDelayed(msg, SHOW_ZOOM_LEVEL_BUTTON_DELAY);
	}

	private void hideZoomLevelInTime() {
		if (!showUIHandler.hasMessages(SHOW_ZOOM_LEVEL_MSG_ID) && !showUIHandler.hasMessages(SHOW_ZOOM_BUTTON_MSG_ID)) {
			sendMessageToShowZoomLevel();
		}
	}

	@Override
	public void setShadowColor(int textColor, int shadowColor) {
		super.setShadowColor(textColor, shadowColor);
		zoomTextPaint.setColor(textColor);
	}

	public boolean isShowZoomLevel() {
		return showZoomLevel;
	}

	public int getHeight() {
		Drawable buttonDrawable = view.getResources().getDrawable(R.drawable.map_zoom_in);
		return buttonDrawable.getMinimumHeight();
	}

}