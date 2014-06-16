package net.osmand.plus.views.controls;

import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.List;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmAndConstants;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.BaseMapLayer;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.ShadowText;
import android.app.AlertDialog;
import android.content.DialogInterface;
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
import android.view.View;
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
		zoomTextPaint = new TextPaint();
		zoomTextPaint.setTextSize(18 * scaleCoefficient);
		zoomTextPaint.setAntiAlias(true);
		zoomTextPaint.setFakeBoldText(true);
	}

	public boolean onSingleTap(PointF point, RotatedTileBox tileBox) {
		if (isShowZoomLevel() && zoomShadow.getBounds().contains((int) point.x, (int) point.y)) {
			getOnClickMagnifierListener(view).onLongClick(null);
			return true;
		}
		return false;
	}

	
	@Override
	protected void showControls(FrameLayout parent) {
		int minimumWidth = view.getResources().getDrawable(R.drawable.map_zoom_in).getMinimumWidth();
		int minimumHeight = view.getResources().getDrawable(R.drawable.map_zoom_in).getMinimumWidth();
		vmargin = 0;
		zoomInButton = addButton(parent, R.string.zoomIn, R.drawable.map_zoom_in);
		if(isBottom()) {
			zoomOutButton = addButton(parent, R.string.zoomOut, R.drawable.map_zoom_out, minimumWidth);
		} else {
			vmargin = minimumHeight;
			zoomOutButton = addButton(parent, R.string.zoomOut, R.drawable.map_zoom_out);
		}
		zoomInButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				notifyClicked();
				if (view.isZooming()) {
					mapActivity.changeZoom(2);
				} else {
					mapActivity.changeZoom(1);
				}

			}
		});
		final View.OnLongClickListener listener = getOnClickMagnifierListener(view);
		zoomInButton.setOnLongClickListener(listener);
		zoomOutButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				notifyClicked();
				mapActivity.changeZoom(-1);
			}
		});
		zoomOutButton.setOnLongClickListener(listener);		
	}

	@Override
	public void initControls(FrameLayout parent) {
		if(isBottom()) {
			zoomShadow = view.getResources().getDrawable(R.drawable.zoom_background).mutate();
		}
		mapMagnifier = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_magnifier);
		bitmapPaint = new Paint();
	}

	@Override
	public void hideControls(FrameLayout layout) {
		removeButton(layout, zoomInButton);
		removeButton(layout, zoomOutButton);
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
		if (isBottom()) {
			if (view.isZooming()) {
				showZoomLevel = true;
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
	public void updateTextColor(int textColor, int shadowColor) {
		super.updateTextColor(textColor, shadowColor);
		zoomTextPaint.setColor(textColor);
	}

	public boolean isShowZoomLevel() {
		return showZoomLevel;
	}

	public int getHeight() {
		if (height == 0) {
			Drawable buttonDrawable = view.getResources().getDrawable(R.drawable.map_zoom_in);
			height = buttonDrawable.getMinimumHeight();
		}
		return height;
	}

	
	public static View.OnLongClickListener getOnClickMagnifierListener(final OsmandMapTileView view) {
		final View.OnLongClickListener listener = new View.OnLongClickListener() {

			@Override
			public boolean onLongClick(View notUseCouldBeNull) {
				final OsmandSettings.OsmandPreference<Float> zoomScale = view.getSettings().MAP_ZOOM_SCALE_BY_DENSITY;
				final AlertDialog.Builder bld = new AlertDialog.Builder(view.getContext());
				float scale = view.getZoomScale();
				int p = (int) ((scale > 0 ? 1 : -1) * Math.round(scale * scale * 100)) + 100;
				final TIntArrayList tlist = new TIntArrayList(new int[] { 75, 100, 150, 200, 300, 400, 500 });
				final List<String> values = new ArrayList<String>();
				int i = -1;
				for (int k = 0; k <= tlist.size(); k++) {
					final boolean end = k == tlist.size();
					if (i == -1) {
						if ((end || p < tlist.get(k))) {
							values.add(p + "%");
							i = k;
						} else if (p == tlist.get(k)) {
							i = k;
						}

					}
					if (k < tlist.size()) {
						values.add(tlist.get(k) + "%");
					}
				}
				if (values.size() != tlist.size()) {
					tlist.insert(i, p);
				}

				bld.setTitle(R.string.map_magnifier);
				bld.setSingleChoiceItems(values.toArray(new String[values.size()]), i,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								int p = tlist.get(which);
								float newScale;
								if (p >= 100) {
									newScale = (float) Math.sqrt((tlist.get(which) - 100f) / 100f);
								} else {
									newScale = -(float) Math.sqrt((100f - tlist.get(which)) / 100f);
								}
								zoomScale.set(newScale - (float) Math.sqrt(Math.max(view.getDensity() - 1, 0)));
								view.getAnimatedDraggingThread().startZooming(view.getZoom(),
										view.getSettingsZoomScale(), false);
								dialog.dismiss();
							}
						});
				bld.show();
				return true;
			}
		};
		return listener;
	}

}