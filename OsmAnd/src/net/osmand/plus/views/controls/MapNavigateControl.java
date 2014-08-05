package net.osmand.plus.views.controls;

import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.List;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.ShadowText;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.TextPaint;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

public class MapNavigateControl extends MapControls {
	private Button navigateButton;
	private MapRouteInfoControl ri;
	private Runnable delayStart;
	private Drawable navigateShadow;
	private Bitmap mapMagnifier;
	private TextPaint counterTextPaint;
	private Paint bitmapPaint;
	private static long startCounter = 0; 
	
	
	public MapNavigateControl(MapRouteInfoControl ri,  MapActivity mapActivity, Handler showUIHandler, float scaleCoefficient) {
		super(mapActivity, showUIHandler, scaleCoefficient);
		this.ri = ri;
		counterTextPaint = new TextPaint();
		counterTextPaint.setTextSize(18 * scaleCoefficient);
		counterTextPaint.setAntiAlias(true);
		counterTextPaint.setFakeBoldText(true);
	}
	
	
	public void startCounter() {
		OsmandSettings settings = mapActivity.getMyApplication().getSettings();
		if(settings.DELAY_TO_START_NAVIGATION.get() <= 0) {
			return;
		}
		if (startCounter <= 0) {
			startCounter = System.currentTimeMillis() + settings.DELAY_TO_START_NAVIGATION.get() * 1000;
			delayStart = new Runnable() {
				@Override
				public void run() {
					if (startCounter > 0) {
						if (System.currentTimeMillis() > startCounter) {
							startCounter = 0;
							startNavigation();
						} else {
							mapActivity.refreshMap();
							showUIHandler.postDelayed(delayStart, 1000);
						}
					}
				}
			};
			delayStart.run();
		}
		
	}
	
	
	private void startNavigation() {
		stopCounter();
		OsmandApplication app = mapActivity.getMyApplication();
		RoutingHelper routingHelper = app.getRoutingHelper();
		if(routingHelper.isFollowingMode()) {
			routingHelper.setRoutePlanningMode(false);
			MapActivity.getMapViewTrackingUtilities().switchToRoutePlanningMode();
		} else {
			OsmandApplication ctx = mapActivity.getMyApplication();
			if(!ctx.getTargetPointsHelper().checkPointToNavigateShort()) {
				ri.showDialog();
			} else {
				MapActivity.getMapViewTrackingUtilities().backToLocationImpl();
				app.getSettings().FOLLOW_THE_ROUTE.set(true);
				routingHelper.setFollowingMode(true);
				routingHelper.setRoutePlanningMode(false);
				MapActivity.getMapViewTrackingUtilities().switchToRoutePlanningMode();
				routingHelper.setCurrentLocation(app.getLocationProvider().getLastKnownLocation(), false);
				app.getRoutingHelper().notifyIfRouteIsCalculated();
			}
		}
	}
	
	@Override
	public void showControls(final FrameLayout parent) {
		navigateButton = addButton(parent, R.string.get_directions, R.drawable.map_btn_navigate);
		navigateButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				notifyClicked();
				startNavigation();
			}
		});
		if(!mapActivity.getRoutingHelper().isFollowingMode() && !mapActivity.getRoutingHelper().isPauseNavigation()) {
			startCounter();
		}
	}

	@Override
	protected void initControls(FrameLayout layout) {
		super.initControls(layout);
		navigateShadow = mapActivity.getResources().getDrawable(R.drawable.zoom_background).mutate();
		mapMagnifier = BitmapFactory.decodeResource(mapActivity.getResources(), R.drawable.map_magnifier);
		bitmapPaint = new Paint();
	}
	@Override
	public void hideControls(FrameLayout layout) {
		removeButton(layout, navigateButton);
		stopCounter();
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings nightMode) {
		if(!mapActivity.getRoutingHelper().isFollowingMode()) {
			drawCount(canvas, tileBox);
		}
	}
	

	public boolean onTouchEvent(MotionEvent event, RotatedTileBox tileBox) {
		stopCounter();
		return false;
	}
	
	public boolean onSingleTap(PointF point, RotatedTileBox tileBox) {
		stopCounter();
		if (navigateShadow.getBounds().contains((int) point.x, (int) point.y)) {
			openDialog();
			return true;
		}
		return false;
	}
	
	private void openDialog() {
		Builder bld = new AlertDialog.Builder(mapActivity);
		final TIntArrayList opt = new TIntArrayList();
		List<String> items = new ArrayList<String>(); 
		int[] checkedItem = new int[]{ -1};
		Integer selected = mapActivity.getMyApplication().getSettings().DELAY_TO_START_NAVIGATION.get();
		addOpt(-1, items, opt, checkedItem, selected);
		addOpt(3, items, opt, checkedItem, selected);
		addOpt(5, items, opt, checkedItem, selected);
		addOpt(7, items, opt, checkedItem, selected);
		addOpt(10, items, opt, checkedItem, selected);
		addOpt(15, items, opt, checkedItem, selected);
		addOpt(20, items, opt, checkedItem, selected);
		bld.setTitle(R.string.delay_navigation_start);
		bld.setSingleChoiceItems(items.toArray(new String[items.size()]), checkedItem[0], new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				notifyClicked();
				dialog.dismiss();
				mapActivity.getMyApplication().getSettings().DELAY_TO_START_NAVIGATION.set(opt.get(which));
				startCounter();
			}
		});
		bld.show();
	}


	private void addOpt(int i, List<String> items, TIntArrayList opt, int[] checkedItem, int selected) {
		if(i == selected) {
			checkedItem[0] = items.size();
		}
		opt.add(i);
		if(i < 0) {
			items.add(mapActivity.getString(R.string.auto_follow_route_never));
		} else {
			items.add(i + " " +mapActivity.getString(R.string.int_seconds));
		}
	}


	private void drawCount(Canvas canvas, RotatedTileBox tb) {
		if ((navigateShadow.getBounds().width() == 0 && navigateButton.getWidth() > 0 )||
				navigateShadow.getBounds().bottom != navigateButton.getBottom()) {
			navigateShadow.setBounds(navigateButton.getLeft() - 2, navigateButton.getTop()
					- (int) (18 * scaleCoefficient), navigateButton.getRight(), navigateButton.getBottom());
		}
		if(navigateShadow.getBounds().width() > 0) {
			navigateShadow.draw(canvas);
		}
		if (startCounter > 0) {
			int get = (int) ((startCounter -System.currentTimeMillis())  / 1000l);
			final String text = get + "";
			float length = counterTextPaint.measureText(text);
			ShadowText.draw(text, canvas, navigateButton.getLeft() + (navigateButton.getWidth() - length - 2) / 2,
					navigateButton.getTop() + 4 * scaleCoefficient, counterTextPaint, shadowColor);
		} else {
			int size = (int) (16 * scaleCoefficient);
			int top = (int) (navigateButton.getTop() - size - 4 * scaleCoefficient);
			int left = (int) (navigateButton.getLeft() + (navigateButton.getWidth() - mapMagnifier.getWidth() - 2 * scaleCoefficient) / 2);
			// canvas density /2 ? size * 2
			canvas.drawBitmap(mapMagnifier, null, new Rect(left, top, left + size * 2, top + size * 2), bitmapPaint);
		}
	}
	
	public int getWidth() {
		if (width == 0) {
			Drawable buttonDrawable = mapActivity.getResources().getDrawable(R.drawable.map_btn_navigate);
			width = buttonDrawable.getMinimumWidth();
		}
		return width ;
	}


	public void stopCounter() {
		startCounter = 0;
	}
}
