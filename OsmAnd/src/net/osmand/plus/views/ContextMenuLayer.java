package net.osmand.plus.views;

import android.Manifest;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresPermission;
import android.support.v4.content.ContextCompat;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;

import net.osmand.AndroidUtils;
import net.osmand.CallbackWithObject;
import net.osmand.NativeLibrary.RenderedObject;
import net.osmand.RenderingContext;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiFilter;
import net.osmand.osm.PoiType;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.mapcontextmenu.other.MapMultiSelectionMenu;
import net.osmand.plus.render.MapRenderRepositories;
import net.osmand.plus.render.NativeOsmandLibrary;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import gnu.trove.list.array.TIntArrayList;

public class ContextMenuLayer extends OsmandMapLayer {
	//private static final Log LOG = PlatformUtil.getLog(ContextMenuLayer.class);
	public static final int VIBRATE_SHORT = 100;

	private OsmandMapTileView view;

	private final MapActivity activity;
	private MapContextMenu menu;
	private MapMultiSelectionMenu multiSelectionMenu;
	private CallbackWithObject<LatLon> selectOnMap = null;
	private MapQuickActionLayer mapQuickActionLayer;

	private ImageView contextMarker;
	private Paint paint;
	private Paint outlinePaint;
	private Bitmap pressedBitmap;
	private Bitmap pressedBitmapSmall;
	private List<LatLon> pressedLatLonFull = new ArrayList<>();
	private List<LatLon> pressedLatLonSmall = new ArrayList<>();

	private GestureDetector movementListener;

	private final MoveMarkerBottomSheetHelper mMoveMarkerBottomSheetHelper;
	private boolean mInChangeMarkerPositionMode;
	private IContextMenuProvider selectedObjectContextMenuProvider;
	private boolean cancelApplyingNewMarkerPosition;
	private LatLon applyingMarkerLatLon;
	private boolean wasCollapseButtonVisible;
	private boolean mInGpxDetailsMode;

	private List<String> publicTransportTypes;
	private Object selectedObject;

	public ContextMenuLayer(MapActivity activity) {
		this.activity = activity;
		menu = activity.getContextMenu();
		multiSelectionMenu = menu.getMultiSelectionMenu();
		movementListener = new GestureDetector(activity, new MenuLayerOnGestureListener());
		mMoveMarkerBottomSheetHelper = new MoveMarkerBottomSheetHelper(activity, this);
	}

	@Override
	public void destroyLayer() {
	}

	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;

		Context context = view.getContext();
		contextMarker = new ImageView(context);
		contextMarker.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		contextMarker.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.map_pin_context_menu));
		contextMarker.setClickable(true);
		int minw = contextMarker.getDrawable().getMinimumWidth();
		int minh = contextMarker.getDrawable().getMinimumHeight();
		contextMarker.layout(0, 0, minw, minh);

		paint = new Paint();
		pressedBitmap = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_shield_tap);
		pressedBitmapSmall = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_shield_tap_small);

		outlinePaint = new Paint();
		outlinePaint.setStyle(Paint.Style.STROKE);
		outlinePaint.setAntiAlias(true);
		outlinePaint.setStrokeWidth(AndroidUtils.dpToPx(activity, 2f));
		outlinePaint.setStrokeCap(Paint.Cap.ROUND);
		outlinePaint.setColor(activity.getResources().getColor(R.color.osmand_orange));
	}

	public boolean isVisible() {
		return menu.isActive();
	}

	public Object getSelectedObject() {
		return selectedObject;
	}

	public void setSelectedObject(Object selectedObject) {
		this.selectedObject = selectedObject;
	}

	private List<String> getPublicTransportTypes() {
		if (publicTransportTypes == null) {
			publicTransportTypes = new ArrayList<>();
			List<PoiFilter> filters = activity.getMyApplication().getPoiTypes().getPoiCategoryByName("transportation").getPoiFilters();
			for (PoiFilter poiFilter : filters) {
				if (poiFilter.getKeyName().equals("public_transport")) {
					for (PoiType poiType : poiFilter.getPoiTypes()) {
						publicTransportTypes.add(poiType.getKeyName());
						for (PoiType poiAdditionalType : poiType.getPoiAdditionals()) {
							publicTransportTypes.add(poiAdditionalType.getKeyName());
						}
					}
				}
			}
		}
		return publicTransportTypes;
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox box, DrawSettings nightMode) {
		if (selectedObject != null) {
			TIntArrayList x = null;
			TIntArrayList y = null;
			if (selectedObject instanceof Amenity) {
				Amenity a = (Amenity) selectedObject;
				x = a.getX();
				y = a.getY();
			} else if (selectedObject instanceof RenderedObject) {
				RenderedObject r = (RenderedObject) selectedObject;
				x = r.getX();
				y = r.getY();
			}
			if (x != null && y != null && x.size() > 2) {
				double lat = MapUtils.get31LatitudeY(y.get(0));
				double lon = MapUtils.get31LongitudeX(x.get(0));
				int px,py, prevX, prevY;
				prevX = (int) box.getPixXFromLatLon(lat, lon);
				prevY = (int) box.getPixYFromLatLon(lat, lon);
				for (int i = 1; i < x.size(); i++) {
					lat = MapUtils.get31LatitudeY(y.get(i));
					lon = MapUtils.get31LongitudeX(x.get(i));
					px = (int) box.getPixXFromLatLon(lat, lon);
					py = (int) box.getPixYFromLatLon(lat, lon);
					canvas.drawLine(prevX, prevY, px, py, outlinePaint);
					prevX = px;
					prevY = py;
				}
			}
		}
		for (LatLon latLon : pressedLatLonSmall) {
			int x = (int) box.getPixXFromLatLon(latLon.getLatitude(), latLon.getLongitude());
			int y = (int) box.getPixYFromLatLon(latLon.getLatitude(), latLon.getLongitude());
			canvas.drawBitmap(pressedBitmapSmall, x - pressedBitmapSmall.getWidth() / 2, y - pressedBitmapSmall.getHeight() / 2, paint);
		}
		for (LatLon latLon : pressedLatLonFull) {
			int x = (int) box.getPixXFromLatLon(latLon.getLatitude(), latLon.getLongitude());
			int y = (int) box.getPixYFromLatLon(latLon.getLatitude(), latLon.getLongitude());
			canvas.drawBitmap(pressedBitmap, x - pressedBitmap.getWidth() / 2, y - pressedBitmap.getHeight() / 2, paint);
		}

		if (mapQuickActionLayer!= null && mapQuickActionLayer.isInChangeMarkerPositionMode())
			return;

		if (mInChangeMarkerPositionMode) {
			if (menu.getObject() == null) {
				canvas.translate(box.getPixWidth() / 2 - contextMarker.getWidth() / 2, box.getPixHeight() / 2 - contextMarker.getHeight());
				contextMarker.draw(canvas);
			}
			mMoveMarkerBottomSheetHelper.onDraw(box);
		} else if (menu.isActive()) {
			LatLon latLon = menu.getLatLon();
			int x = (int) box.getPixXFromLatLon(latLon.getLatitude(), latLon.getLongitude());
			int y = (int) box.getPixYFromLatLon(latLon.getLatitude(), latLon.getLongitude());
			canvas.translate(x - contextMarker.getWidth() / 2, y - contextMarker.getHeight());
			contextMarker.draw(canvas);
		}
	}

	public void setSelectOnMap(CallbackWithObject<LatLon> selectOnMap) {
		this.selectOnMap = selectOnMap;
	}

	@Override
	public void populateObjectContextMenu(LatLon latLon, Object o, ContextMenuAdapter adapter, MapActivity mapActivity) {
		if (menu.hasHiddenBottomInfo()) {
			ContextMenuAdapter.ItemClickListener listener = new ContextMenuAdapter.ItemClickListener() {
				@Override
				public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked) {
					menu.openMenuFullScreen();
					return true;
				}
			};
			adapter.addItem(new ContextMenuItem.ItemBuilder()
					.setTitleId(R.string.shared_string_show_description, activity)
					.setIcon(R.drawable.ic_action_note_dark)
					.setListener(listener)
					.createItem());
		}
		if (isObjectMoveable(o)) {
			ContextMenuAdapter.ItemClickListener listener = new ContextMenuAdapter.ItemClickListener() {
				@Override
				public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked) {
					RotatedTileBox tileBox = activity.getMapView().getCurrentRotatedTileBox();
					enterMovingMode(tileBox);
					return true;
				}
			};
			adapter.addItem(new ContextMenuItem.ItemBuilder()
					.setTitleId(R.string.change_markers_position, activity)
					.setIcon(R.drawable.ic_show_on_map)
					.setListener(listener)
					.createItem());
		}
	}

	@Override
	@RequiresPermission(Manifest.permission.VIBRATE)
	public boolean onLongPressEvent(PointF point, RotatedTileBox tileBox) {
		if (disableLongPressOnMap()) {
			return false;
		}
		if (pressedContextMarker(tileBox, point.x, point.y)) {
			Object obj = menu.getObject();
			if (isObjectMoveable(obj)) {
				enterMovingMode(tileBox);
				return true;
			}
			return false;
		}

		showContextMenu(point, tileBox, true);
		view.refreshMap();
		return true;
	}


	public PointF getMovableCenterPoint(RotatedTileBox tb) {
		if (applyingMarkerLatLon != null) {
			float x = tb.getPixXFromLatLon(applyingMarkerLatLon.getLatitude(), applyingMarkerLatLon.getLongitude());
			float y = tb.getPixYFromLatLon(applyingMarkerLatLon.getLatitude(), applyingMarkerLatLon.getLongitude());
			return new PointF(x, y);
		} else {
			return new PointF(tb.getPixWidth() / 2, tb.getPixHeight() / 2);
		}
	}

	public Object getMoveableObject() {
		return mInChangeMarkerPositionMode ? menu.getObject() : null;
	}

	public boolean isInChangeMarkerPositionMode() {
		return mInChangeMarkerPositionMode;
	}

	public boolean isInGpxDetailsMode() {
		return mInGpxDetailsMode;
	}

	public boolean isObjectMoveable(Object o) {
		if (o == null) {
			return true;
		} else if (selectedObjectContextMenuProvider != null
				&& selectedObjectContextMenuProvider instanceof ContextMenuLayer.IMoveObjectProvider) {
			final IMoveObjectProvider l = (ContextMenuLayer.IMoveObjectProvider) selectedObjectContextMenuProvider;
			if (l.isObjectMovable(o)) {
				return true;
			}
		}
		return false;
	}

	public void applyMovedObject(Object o, LatLon position, ApplyMovedObjectCallback callback) {
		if (selectedObjectContextMenuProvider != null) {
			if (selectedObjectContextMenuProvider instanceof IMoveObjectProvider) {
				final IMoveObjectProvider l = (IMoveObjectProvider) selectedObjectContextMenuProvider;
				if (l.isObjectMovable(o)) {
					l.applyNewObjectPosition(o, position, callback);
				}
			}
		} else if (mInChangeMarkerPositionMode) {
			callback.onApplyMovedObject(true, null);
		}
	}

	public void applyNewMarkerPosition() {
		if (!mInChangeMarkerPositionMode) {
			throw new IllegalStateException("Not in change marker position mode");
		}

		RotatedTileBox tileBox = activity.getMapView().getCurrentRotatedTileBox();
		PointF newMarkerPosition = getMovableCenterPoint(tileBox);
		final LatLon ll = tileBox.getLatLonFromPixel(newMarkerPosition.x, newMarkerPosition.y);
		applyingMarkerLatLon = ll;

		Object obj = getMoveableObject();
		cancelApplyingNewMarkerPosition = false;
		mMoveMarkerBottomSheetHelper.enterApplyPositionMode();
		applyMovedObject(obj, ll, new ApplyMovedObjectCallback() {
			@Override
			public void onApplyMovedObject(boolean success, @Nullable Object newObject) {
				mMoveMarkerBottomSheetHelper.exitApplyPositionMode();
				if (success && !cancelApplyingNewMarkerPosition) {
					mMoveMarkerBottomSheetHelper.hide();
					quitMovingMarker();

					PointDescription pointDescription = null;
					if (selectedObjectContextMenuProvider != null) {
						pointDescription = selectedObjectContextMenuProvider.getObjectName(newObject);
					}
					menu.show(ll, pointDescription, newObject);
					view.refreshMap();
				}
				selectedObjectContextMenuProvider = null;
				applyingMarkerLatLon = null;
			}

			@Override
			public boolean isCancelled() {
				return cancelApplyingNewMarkerPosition;
			}
		});
	}

	public void enterGpxDetailsMode() {
		menu.updateMapCenter(null);
		menu.hide();

		mInGpxDetailsMode = true;
		if (!activity.getMyApplication().getSettings().NEW_MAP_VIEW.get()) {
			activity.disableDrawer();
		}
		mark(View.INVISIBLE, R.id.map_ruler_layout,
				R.id.map_left_widgets_panel, R.id.map_right_widgets_panel, R.id.map_center_info);

		View collapseButton = activity.findViewById(R.id.map_collapse_button);
		if (collapseButton != null && collapseButton.getVisibility() == View.VISIBLE) {
			wasCollapseButtonVisible = true;
			collapseButton.setVisibility(View.INVISIBLE);
		} else {
			wasCollapseButtonVisible = false;
		}
	}

	public void exitGpxDetailsMode() {
		mInGpxDetailsMode = false;
		if (!activity.getMyApplication().getSettings().NEW_MAP_VIEW.get()) {
			activity.enableDrawer();
		}
		mark(View.VISIBLE, R.id.map_ruler_layout,
				R.id.map_left_widgets_panel, R.id.map_right_widgets_panel, R.id.map_center_info);

		View collapseButton = activity.findViewById(R.id.map_collapse_button);
		if (collapseButton != null && wasCollapseButtonVisible) {
			collapseButton.setVisibility(View.VISIBLE);
		}
	}

	private void quitMovingMarker() {
		mInChangeMarkerPositionMode = false;
		mark(View.VISIBLE, R.id.map_ruler_layout,
				R.id.map_left_widgets_panel, R.id.map_right_widgets_panel, R.id.map_center_info);

		View collapseButton = activity.findViewById(R.id.map_collapse_button);
		if (collapseButton != null && wasCollapseButtonVisible) {
			collapseButton.setVisibility(View.VISIBLE);
		}
	}

	private void enterMovingMode(RotatedTileBox tileBox) {
		Vibrator vibrator = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
		vibrator.vibrate(VIBRATE_SHORT);

		menu.updateMapCenter(null);
		menu.hide();

		LatLon ll = menu.getLatLon();
		RotatedTileBox rb = new RotatedTileBox(tileBox);
		rb.setCenterLocation(0.5f, 0.5f);
		rb.setLatLonCenter(ll.getLatitude(), ll.getLongitude());
		double lat = rb.getLatFromPixel(tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
		double lon = rb.getLonFromPixel(tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
		view.setLatLon(lat, lon);

		mInChangeMarkerPositionMode = true;
		mMoveMarkerBottomSheetHelper.show(menu.getLeftIcon());
		mark(View.INVISIBLE, R.id.map_ruler_layout,
				R.id.map_left_widgets_panel, R.id.map_right_widgets_panel, R.id.map_center_info);

		View collapseButton = activity.findViewById(R.id.map_collapse_button);
		if (collapseButton != null && collapseButton.getVisibility() == View.VISIBLE) {
			wasCollapseButtonVisible = true;
			collapseButton.setVisibility(View.INVISIBLE);
		} else {
			wasCollapseButtonVisible = false;
		}

		view.refreshMap();
	}

	private void mark(int status, int... widgets) {
		for (int widget : widgets) {
			View v = activity.findViewById(widget);
			if (v != null) {
				v.setVisibility(status);
			}
		}
	}

	public void cancelMovingMarker() {
		cancelApplyingNewMarkerPosition = true;
		quitMovingMarker();
		activity.getContextMenu().show();
		applyingMarkerLatLon = null;
	}

	public boolean showContextMenu(double latitude, double longitude, boolean showUnknownLocation) {
		RotatedTileBox cp = activity.getMapView().getCurrentRotatedTileBox();
		float x = cp.getPixXFromLatLon(latitude, longitude);
		float y = cp.getPixYFromLatLon(latitude, longitude);
		return showContextMenu(new PointF(x, y), activity.getMapView().getCurrentRotatedTileBox(), showUnknownLocation);
	}

	public boolean showContextMenu(@NonNull LatLon latLon,
								   @Nullable PointDescription pointDescription,
								   @Nullable Object object,
								   @Nullable IContextMenuProvider provider) {
		selectedObjectContextMenuProvider = provider;
		hideVisibleMenues();
		activity.getMapViewTrackingUtilities().setMapLinkedToLocation(false);
		if (!activity.getMapView().getCurrentRotatedTileBox().containsLatLon(latLon)) {
			menu.setMapCenter(latLon);
			menu.setMapPosition(activity.getMapView().getMapPosition());
			menu.setCenterMarker(true);
		}
		menu.show(latLon, pointDescription, object);
		return true;
	}

	private boolean showContextMenu(PointF point, RotatedTileBox tileBox, boolean showUnknownLocation) {
		LatLon objectLatLon = null;
		Map<Object, IContextMenuProvider> selectedObjects = selectObjectsForContextMenu(tileBox, point, false);
		NativeOsmandLibrary nativeLib = NativeOsmandLibrary.getLoadedLibrary();
		if (nativeLib != null) {
			MapRenderRepositories maps = activity.getMyApplication().getResourceManager().getRenderer();
			RenderingContext rc = maps.getVisibleRenderingContext();
			RenderedObject[] renderedObjects = null;
			if (rc != null && rc.zoom == tileBox.getZoom()) {
				double lat = MapUtils.get31LatitudeY((int) (rc.topY * rc.tileDivisor));
				double lon = MapUtils.get31LongitudeX((int) (rc.leftX * rc.tileDivisor));
				float x = tileBox.getPixXFromLatLon(lat, lon);
				float y = tileBox.getPixYFromLatLon(lat, lon);
				renderedObjects = nativeLib.searchRenderedObjectsFromContext(rc, (int) (point.x - x), (int) (point.y - y));
			}
			if (renderedObjects != null) {
				// double tx = c.first/ (rc->tileDivisor);
				// double ty = c.second / (rc->tileDivisor);
				// float dTileX = tx - rc->getLeft();
				// float dTileY = ty - rc->getTop();
				// rc->calcX = rc->cosRotateTileSize * dTileX - rc->sinRotateTileSize * dTileY;
				// rc->calcY = rc->sinRotateTileSize * dTileX + rc->cosRotateTileSize * dTileY;
				int TILE_SIZE = 256;
				double cosRotateTileSize = Math.cos(Math.toRadians(rc.rotate)) * TILE_SIZE;
				double sinRotateTileSize  = Math.sin(Math.toRadians(rc.rotate)) * TILE_SIZE;
				for(RenderedObject r : renderedObjects) {
					double cx = r.getBbox().centerX();
					double cy = r.getBbox().centerY();
					double dTileX = (cx * cosRotateTileSize + cy * sinRotateTileSize) / (TILE_SIZE * TILE_SIZE);
					double dTileY = (cy * cosRotateTileSize - cx * sinRotateTileSize) / (TILE_SIZE * TILE_SIZE);
					int x31 = (int) ((dTileX + rc.leftX) * rc.tileDivisor);
					int y31 = (int) ((dTileY + rc.topY) * rc.tileDivisor);
					double lat = MapUtils.get31LatitudeY(y31);
					double lon = MapUtils.get31LongitudeX(x31);
					r.setLabelLatLon(new LatLon(lat, lon));
				}
				IContextMenuProvider poiMenuProvider = activity.getMapLayers().getPoiMapLayer();
				for (RenderedObject renderedObject : renderedObjects) {
					if (renderedObject.getX() != null && renderedObject.getX().size() == 1
							&& renderedObject.getY() != null && renderedObject.getY().size() == 1) {
						objectLatLon = new LatLon(MapUtils.get31LatitudeY(renderedObject.getY().get(0)),
								MapUtils.get31LongitudeX(renderedObject.getX().get(0)));
					} else if (renderedObject.getLabelLatLon() != null) {
						objectLatLon = renderedObject.getLabelLatLon();
					}
					if (renderedObject.getId() != null) {
						List<String> names = new ArrayList<>();
						if (!Algorithms.isEmpty(renderedObject.getName())) {
							names.add(renderedObject.getName());
						}
						for (Entry<String, String> entry : renderedObject.getTags().entrySet()) {
							if (entry.getKey().startsWith("name:") && !entry.getValue().equals("")) {
								names.add(entry.getValue());
							}
						}
						LatLon searchLatLon = objectLatLon;
						if (searchLatLon == null) {
							searchLatLon = tileBox.getLatLonFromPixel(point.x, point.y);
						}
						Amenity amenity = findAmenity(renderedObject.getId() >> 7, names, searchLatLon);
						if (amenity != null) {
							if (renderedObject.getX() != null && renderedObject.getX().size() > 1
									&& renderedObject.getY() != null && renderedObject.getY().size() > 1) {
								amenity.getX().addAll(renderedObject.getX());
								amenity.getY().addAll(renderedObject.getY());
							}
							selectedObjects.put(amenity, poiMenuProvider);
							continue;
						}
						selectedObjects.put(renderedObject, null);
					}
				}
			}
		}
		if (selectedObjects.size() == 1) {
			Object selectedObj = selectedObjects.keySet().iterator().next();
			LatLon latLon = null;
			PointDescription pointDescription = null;
			final IContextMenuProvider provider = selectedObjects.get(selectedObj);
			if (provider != null) {
				latLon = provider.getObjectLocation(selectedObj);
				pointDescription = provider.getObjectName(selectedObj);
			}
			if (latLon == null) {
				latLon = objectLatLon;
			}
			if (latLon == null) {
				latLon = getLatLon(point, tileBox);
			}
			showContextMenu(latLon, pointDescription, selectedObj, provider);
			return true;

		} else if (selectedObjects.size() > 1) {
			selectedObjectContextMenuProvider = null;
			showContextMenuForSelectedObjects(getLatLon(point, tileBox), selectedObjects);
			return true;

		} else if (showUnknownLocation) {
			hideVisibleMenues();
			selectedObjectContextMenuProvider = null;
			LatLon latLon = getLatLon(point, tileBox);
			activity.getMapViewTrackingUtilities().setMapLinkedToLocation(false);
			menu.show(latLon, null, null);
			return true;
		}
		return false;
	}

	@NonNull
	private LatLon getLatLon(PointF point, RotatedTileBox tileBox) {
		LatLon latLon;
		final double lat = tileBox.getLatFromPixel((int) point.x, (int) point.y);
		final double lon = tileBox.getLonFromPixel((int) point.x, (int) point.y);
		latLon = new LatLon(lat, lon);
		return latLon;
	}

	public boolean disableSingleTap() {
		boolean res = false;
		for (OsmandMapLayer lt : view.getLayers()) {
			if (lt instanceof ContextMenuLayer.IContextMenuProvider) {
				if (((IContextMenuProvider) lt).disableSingleTap()) {
					res = true;
					break;
				}
			}
		}
		return res;
	}

	public boolean disableLongPressOnMap() {
		if (mInChangeMarkerPositionMode || mInGpxDetailsMode) {
			return true;
		}
		boolean res = false;
		for (OsmandMapLayer lt : view.getLayers()) {
			if (lt instanceof ContextMenuLayer.IContextMenuProvider) {
				if (((IContextMenuProvider) lt).disableLongPressOnMap()) {
					res = true;
					break;
				}
			}
		}
		return res;
	}

	private Map<Object, IContextMenuProvider> selectObjectsForContextMenu(RotatedTileBox tileBox,
																		  PointF point, boolean acquireObjLatLon) {
		List<LatLon> pressedLatLonFull = new ArrayList<>();
		List<LatLon> pressedLatLonSmall = new ArrayList<>();
		Map<Object, IContextMenuProvider> selectedObjects = new HashMap<>();
		List<Object> s = new ArrayList<>();
		for (OsmandMapLayer lt : view.getLayers()) {
			if (lt instanceof ContextMenuLayer.IContextMenuProvider) {
				s.clear();
				final IContextMenuProvider l = (ContextMenuLayer.IContextMenuProvider) lt;
				l.collectObjectsFromPoint(point, tileBox, s);
				for (Object o : s) {
					selectedObjects.put(o, l);
					if (acquireObjLatLon && l.isObjectClickable(o)) {
						LatLon latLon = l.getObjectLocation(o);
						if (lt.isPresentInFullObjects(latLon) && !pressedLatLonFull.contains(latLon)) {
							pressedLatLonFull.add(latLon);
						} else if (lt.isPresentInSmallObjects(latLon) && !pressedLatLonSmall.contains(latLon)) {
							pressedLatLonSmall.add(latLon);
						}
					}
				}
			}
		}
		if (acquireObjLatLon) {
			this.pressedLatLonFull = pressedLatLonFull;
			this.pressedLatLonSmall = pressedLatLonSmall;
		}
		return selectedObjects;
	}

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}

	public boolean pressedContextMarker(RotatedTileBox tb, float px, float py) {
		float markerX;
		float markerY;
		if (mInChangeMarkerPositionMode) {
			markerX = tb.getCenterPixelX();
			markerY = tb.getCenterPixelY();
		} else if (menu.isActive()) {
			LatLon latLon = menu.getLatLon();
			markerX = tb.getPixXFromLatLon(latLon.getLatitude(), latLon.getLongitude());
			markerY = tb.getPixYFromLatLon(latLon.getLatitude(), latLon.getLongitude());
		} else {
			return false;
		}
		Rect bs = contextMarker.getDrawable().getBounds();
		int dx = (int) (px - markerX);
		int dy = (int) (py - markerY);
		int bx = dx + bs.width() / 2;
		int by = dy + bs.height();
		return (bs.contains(bx, by));
	}

	@Override
	public boolean onSingleTap(PointF point, RotatedTileBox tileBox) {
		if (mInChangeMarkerPositionMode || mInGpxDetailsMode) {
			return true;
		}

		if (pressedContextMarker(tileBox, point.x, point.y)) {
			hideVisibleMenues();
			menu.show();
			return true;
		}

		if (selectOnMap != null) {
			LatLon latlon = tileBox.getLatLonFromPixel(point.x, point.y);
			CallbackWithObject<LatLon> cb = selectOnMap;
			cb.processResult(latlon);
			menu.init(latlon, null, null);
			selectOnMap = null;
			return true;
		}

		if (!disableSingleTap()) {
			boolean res = showContextMenu(point, tileBox, false);
			if (res) {
				return true;
			}
		}

		hideVisibleMenues();
		menu.onSingleTapOnMap();
		return false;
	}

	private Amenity findAmenity(long id, List<String> names, LatLon latLon) {
		QuadRect rect = MapUtils.calculateLatLonBbox(latLon.getLatitude(), latLon.getLongitude(), 50);
		List<Amenity> amenities = activity.getMyApplication().getResourceManager().searchAmenities(
				new BinaryMapIndexReader.SearchPoiTypeFilter() {
					@Override
					public boolean accept(PoiCategory type, String subcategory) {
						return true;
					}

					@Override
					public boolean isEmpty() {
						return false;
					}
				}, rect.top, rect.left, rect.bottom, rect.right, -1, null);

		Amenity res = null;
		for (Amenity amenity : amenities) {
			Long amenityId = amenity.getId() >> 1;
			if (amenityId == id) {
				res = amenity;
				break;
			}
		}
		if (res == null && names != null && names.size() > 0) {
			for (Amenity amenity : amenities) {
				for (String name : names) {
					if (name.equals(amenity.getName())) {
						res = amenity;
						break;
					}
				}
				if (res != null) {
					break;
				}
			}
		}

		return res;
	}

	private void hideVisibleMenues() {
		if (multiSelectionMenu.isVisible()) {
			multiSelectionMenu.hide();
		}
	}

	private void showContextMenuForSelectedObjects(final LatLon latLon, final Map<Object, IContextMenuProvider> selectedObjects) {
		multiSelectionMenu.show(latLon, selectedObjects);
	}

	public void setMapQuickActionLayer(MapQuickActionLayer mapQuickActionLayer) {
		this.mapQuickActionLayer = mapQuickActionLayer;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event, RotatedTileBox tileBox) {

		if (movementListener.onTouchEvent(event)) {
			if (menu.isVisible()) {
				menu.hide();
			}
			if (multiSelectionMenu.isVisible()) {
				multiSelectionMenu.hide();
			}
		}

		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				if (!mInChangeMarkerPositionMode && !mInGpxDetailsMode) {
					selectObjectsForContextMenu(tileBox, new PointF(event.getX(), event.getY()), true);
					if (pressedLatLonFull.size() > 0 || pressedLatLonSmall.size() > 0) {
						view.refreshMap();
					}
				}
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				pressedLatLonFull.clear();
				pressedLatLonSmall.clear();
				view.refreshMap();
				break;
		}

		return false;
	}

	public interface IContextMenuProvider {

		void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> o);

		LatLon getObjectLocation(Object o);

		PointDescription getObjectName(Object o);

		boolean disableSingleTap();

		boolean disableLongPressOnMap();

		boolean isObjectClickable(Object o);

	}

	public interface IMoveObjectProvider {

		boolean isObjectMovable(Object o);

		void applyNewObjectPosition(@NonNull Object o,
									@NonNull LatLon position,
									@Nullable ApplyMovedObjectCallback callback);
	}

	public interface ApplyMovedObjectCallback {

		void onApplyMovedObject(boolean success, @Nullable Object newObject);

		boolean isCancelled();
	}

	public interface IContextMenuProviderSelection {

		int getOrder(Object o);

		void setSelectedObject(Object o);

		void clearSelectedObject();
	}

	private class MenuLayerOnGestureListener extends GestureDetector.SimpleOnGestureListener {

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			return true;
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			return true;
		}
	}
}
