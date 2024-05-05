package net.osmand.plus.views.layers;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_CHANGE_MARKER_POSITION;
import static net.osmand.plus.views.layers.geometry.GeometryWayDrawer.VECTOR_LINE_SCALE_COEF;

import android.Manifest;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Vibrator;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.content.res.AppCompatResources;

import net.osmand.CallbackWithObject;
import net.osmand.NativeLibrary.RenderedObject;
import net.osmand.PlatformUtil;
import net.osmand.aidl.AidlMapPointWrapper;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.MapMarker;
import net.osmand.core.jni.MapMarkerBuilder;
import net.osmand.core.jni.MapMarkersCollection;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.QVectorPointI;
import net.osmand.core.jni.VectorLineBuilder;
import net.osmand.core.jni.VectorLinesCollection;
import net.osmand.data.Amenity;
import net.osmand.data.BackgroundType;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivityActions;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.mapcontextmenu.other.MapMultiSelectionMenu;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.weather.WeatherPlugin;
import net.osmand.plus.routepreparationmenu.ChooseRouteFragment;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.AddGpxPointBottomSheetHelper;
import net.osmand.plus.views.AddGpxPointBottomSheetHelper.NewGpxPoint;
import net.osmand.plus.views.MoveMarkerBottomSheetHelper;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.MapSelectionHelper.MapSelectionResult;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.callback.ItemClickListener;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import gnu.trove.list.array.TIntArrayList;

public class ContextMenuLayer extends OsmandMapLayer {

	private static final Log LOG = PlatformUtil.getLog(ContextMenuLayer.class);
	public static final int VIBRATE_SHORT = 100;

	private MapContextMenu menu;
	private MapMultiSelectionMenu multiSelectionMenu;
	private CallbackWithObject<LatLon> selectOnMap;
	private MapQuickActionLayer mapQuickActionLayer;

	private ImageView contextMarker;
	private Paint paint;
	private Paint outlinePaint;

	private final MapSelectionHelper selectionHelper;

	private GestureDetector movementListener;

	private MoveMarkerBottomSheetHelper mMoveMarkerBottomSheetHelper;
	private AddGpxPointBottomSheetHelper mAddGpxPointBottomSheetHelper;
	private boolean mInChangeMarkerPositionMode;
	private boolean cancelApplyingNewMarkerPosition;
	private LatLon applyingMarkerLatLon;
	private IContextMenuProvider selectedObjectContextMenuProvider;
	private boolean mInGpxDetailsMode;
	private boolean mInAddGpxPointMode;
	private boolean carView;

	// OpenGl
	private VectorLinesCollection outlineCollection;
	private MapMarkersCollection contextMarkerCollection;
	private net.osmand.core.jni.MapMarker contextCoreMarker;
	private Bitmap contextMarkerImage;

	private Object selectedObject;
	private Object selectedObjectCached;

	public ContextMenuLayer(@NonNull Context context) {
		super(context);
		selectionHelper = new MapSelectionHelper(context);
	}

	@Override
	public void setMapActivity(@Nullable MapActivity mapActivity) {
		super.setMapActivity(mapActivity);
		if (mapActivity != null) {
			menu = mapActivity.getContextMenu();
			multiSelectionMenu = menu.getMultiSelectionMenu();
			movementListener = new GestureDetector(mapActivity, new MenuLayerOnGestureListener());
			mMoveMarkerBottomSheetHelper = new MoveMarkerBottomSheetHelper(mapActivity, this);
			mAddGpxPointBottomSheetHelper = new AddGpxPointBottomSheetHelper(mapActivity, this);
		} else {
			menu = null;
			multiSelectionMenu = null;
			movementListener = null;
			mMoveMarkerBottomSheetHelper = null;
			mAddGpxPointBottomSheetHelper = null;
		}
	}

	public AddGpxPointBottomSheetHelper getAddGpxPointBottomSheetHelper() {
		return mAddGpxPointBottomSheetHelper;
	}

	@Override
	public void destroyLayer() {
		super.destroyLayer();
		clearContextMarkerCollection();
		clearOutlineCollection();
	}

	@Override
	public void initLayer(@NonNull OsmandMapTileView view) {
		super.initLayer(view);

		Context context = getContext();
		contextMarker = new ImageView(context);
		contextMarker.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		Drawable markerDrawable = AppCompatResources.getDrawable(context, R.drawable.map_pin_context_menu);
		contextMarker.setImageDrawable(markerDrawable);
		contextMarker.setClickable(true);
		updateContextMarker();

		paint = new Paint();
		paint.setColor(0x7f000000);

		outlinePaint = new Paint();
		outlinePaint.setStyle(Paint.Style.STROKE);
		outlinePaint.setAntiAlias(true);
		outlinePaint.setStrokeWidth(AndroidUtils.dpToPx(getContext(), 2f));
		outlinePaint.setStrokeCap(Paint.Cap.ROUND);
		outlinePaint.setColor(getColor(R.color.osmand_orange));
	}

	private void updateContextMarker() {
		float scale = getApplication().getOsmandMap().getCarDensityScaleCoef();
		int width = (int) (contextMarker.getDrawable().getMinimumWidth() * scale);
		int height = (int) (contextMarker.getDrawable().getMinimumHeight() * scale);
		contextMarker.layout(0, 0, width, height);
		contextMarkerImage = getScaledBitmap(R.drawable.map_pin_context_menu, scale);
	}

	public Object getSelectedObject() {
		return selectedObject;
	}

	public void setSelectedObject(Object selectedObject) {
		this.selectedObject = selectedObject;
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox box, DrawSettings nightMode) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}
		boolean carView = getApplication().getOsmandMap().getMapView().isCarView();
		boolean carViewChanged = this.carView != carView;
		this.carView = carView;
		if (carViewChanged) {
			updateContextMarker();
		}
		MapRendererView mapRenderer = getMapRenderer();
		boolean hasMapRenderer = mapRenderer != null;
		if (contextMarkerCollection == null || mapActivityInvalidated || carViewChanged) {
			recreateContextMarkerCollection();
		}
		boolean markerCustomized = false;
		boolean clearSelectedObject = true;
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
			} else if (selectedObject instanceof AidlMapPointWrapper) {
				markerCustomized = true;
			}
			if (x != null && y != null && x.size() > 2) {
				if (hasMapRenderer) {
					clearSelectedObject = false;
					if (selectedObject != selectedObjectCached) {
						clearOutlineCollection();
						VectorLinesCollection outlineCollection = new VectorLinesCollection();
						QVectorPointI points = new QVectorPointI();
						for (int i = 0; i < x.size(); i++) {
							points.add(new PointI(x.get(i), y.get(i)));
						}
						VectorLineBuilder builder = new VectorLineBuilder();
						builder.setPoints(points)
								.setIsHidden(false)
								.setLineId(1)
								.setLineWidth(outlinePaint.getStrokeWidth() * VECTOR_LINE_SCALE_COEF)
								.setFillColor(NativeUtilities.createFColorARGB(outlinePaint.getColor()))
								.setApproximationEnabled(false)
								.setBaseOrder(getBaseOrder());
						builder.buildAndAddToCollection(outlineCollection);
						this.outlineCollection = outlineCollection;
						mapRenderer.addSymbolsProvider(outlineCollection);
					}
				} else {
					float px, py, prevX, prevY;
					prevX = box.getPixXFrom31(x.get(0), y.get(0));
					prevY = box.getPixYFrom31(x.get(0), y.get(0));
					for (int i = 1; i < x.size(); i++) {
						px = box.getPixXFrom31(x.get(i), y.get(i));
						py = box.getPixYFrom31(x.get(i), y.get(i));
						canvas.drawLine(prevX, prevY, px, py, outlinePaint);
						prevX = px;
						prevY = py;
					}
				}
			}
		}
		selectedObjectCached = selectedObject;
		if (clearSelectedObject && hasMapRenderer) {
			clearOutlineCollection();
		}
		float textScale = selectionHelper.hasTouchedMapObjects() ? getTextScale() : 1f;
		for (Entry<LatLon, BackgroundType> entry : selectionHelper.getTouchedSmallMapObjects().entrySet()) {
			LatLon latLon = entry.getKey();
			PointF pixel = NativeUtilities.getElevatedPixelFromLatLon(getMapRenderer(), box, latLon);
			BackgroundType background = entry.getValue();
			Bitmap pressedBitmapSmall = background.getTouchBackground(mapActivity, true);
			Rect destRect = getIconDestinationRect(
					pixel.x, pixel.y, pressedBitmapSmall.getWidth(), pressedBitmapSmall.getHeight(), textScale);
			canvas.drawBitmap(pressedBitmapSmall, null, destRect, paint);
		}
		for (Entry<LatLon, BackgroundType> entry : selectionHelper.getTouchedFullMapObjects().entrySet()) {
			LatLon latLon = entry.getKey();
			PointF pixel = NativeUtilities.getElevatedPixelFromLatLon(getMapRenderer(), box, latLon);
			BackgroundType background = entry.getValue();
			Bitmap pressedBitmap = background.getTouchBackground(mapActivity, false);
			int offsetY = background.getOffsetY(mapActivity, textScale);
			Rect destRect = getIconDestinationRect(
					pixel.x, pixel.y - offsetY, pressedBitmap.getWidth(), pressedBitmap.getHeight(), textScale);
			canvas.drawBitmap(pressedBitmap, null, destRect, paint);
		}

		boolean movingMarker = mapQuickActionLayer != null && mapQuickActionLayer.isInMovingMarkerMode();
		boolean downloadingTiles = mapActivity.getFragmentsHelper().getDownloadTilesFragment() != null;
		if (movingMarker || downloadingTiles) {
			return;
		}

		boolean showMarker = false;
		if (mInChangeMarkerPositionMode) {
			if (menu != null && menu.getObject() == null) {
				canvas.translate(box.getPixWidth() / 2f - contextMarker.getWidth() / 2f, box.getPixHeight() / 2f - contextMarker.getHeight());
				contextMarker.draw(canvas);
			}
			if (mMoveMarkerBottomSheetHelper != null) {
				mMoveMarkerBottomSheetHelper.onDraw(box);
			}
		} else if (mInAddGpxPointMode) {
			canvas.translate(box.getPixWidth() / 2f - contextMarker.getWidth() / 2f, box.getPixHeight() / 2f - contextMarker.getHeight());
			contextMarker.draw(canvas);
			if (mAddGpxPointBottomSheetHelper != null) {
				mAddGpxPointBottomSheetHelper.onDraw(box);
			}
		} else if (!markerCustomized) {
			LatLon latLon = null;
			if (menu != null && menu.isActive()) {
				latLon = menu.getLatLon();
			} else if (mapActivity.getFragmentsHelper().getTrackMenuFragment() != null) {
				latLon = mapActivity.getFragmentsHelper().getTrackMenuFragment().getLatLon();
			}
			if (latLon != null) {
				if (hasMapRenderer) {
					PointI loc31 = new PointI(
							MapUtils.get31TileNumberX(latLon.getLongitude()),
							MapUtils.get31TileNumberY(latLon.getLatitude()));
					contextCoreMarker.setPosition(loc31);
					showMarker = true;
				} else {
					int x = (int) box.getPixXFromLatLon(latLon.getLatitude(), latLon.getLongitude());
					int y = (int) box.getPixYFromLatLon(latLon.getLatitude(), latLon.getLongitude());
					canvas.translate(x - contextMarker.getWidth() / 2f, y - contextMarker.getHeight());
					contextMarker.draw(canvas);
				}
			}
		}
		if (hasMapRenderer) {
			contextCoreMarker.setIsHidden(!showMarker);
		}
		mapActivityInvalidated = false;
	}

	public void setSelectOnMap(CallbackWithObject<LatLon> selectOnMap) {
		this.selectOnMap = selectOnMap;
	}

	public void updateContextMenu() {
		for (OsmandMapLayer layer : view.getLayers()) {
			if (layer instanceof IMoveObjectProvider && ((IMoveObjectProvider) layer).isObjectMovable(selectedObject)) {
				selectedObjectContextMenuProvider = (IContextMenuProvider) layer;
				break;
			}
		}
	}

	private void recreateContextMarkerCollection() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null) {
			clearContextMarkerCollection();

			if (contextMarkerImage == null) {
				return;
			}
			contextMarkerCollection = new MapMarkersCollection();
			MapMarkerBuilder builder = new MapMarkerBuilder();
			builder.setBaseOrder(getPointsOrder() - 100);
			builder.setIsAccuracyCircleSupported(false);
			builder.setIsHidden(true);
			builder.setPinIcon(NativeUtilities.createSkImageFromBitmap(contextMarkerImage));
			builder.setPinIconVerticalAlignment(MapMarker.PinIconVerticalAlignment.Top);
			contextCoreMarker = builder.buildAndAddToCollection(contextMarkerCollection);
			mapRenderer.addSymbolsProvider(contextMarkerCollection);
		}
	}

	private void clearContextMarkerCollection() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null && contextMarkerCollection != null) {
			mapRenderer.removeSymbolsProvider(contextMarkerCollection);
			contextMarkerCollection = null;
		}
	}

	private void clearOutlineCollection() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null && outlineCollection != null) {
			mapRenderer.removeSymbolsProvider(outlineCollection);
			outlineCollection = null;
		}
	}

	@Override
	public void populateObjectContextMenu(@NonNull LatLon latLon, @Nullable Object o, @NonNull ContextMenuAdapter adapter) {
		ItemClickListener listener = (uiAdapter, view, item, isChecked) -> {
			RotatedTileBox tileBox = getMapView().getCurrentRotatedTileBox();
			enterMovingMode(tileBox);
			return true;
		};
		adapter.addItem(new ContextMenuItem(MAP_CONTEXT_MENU_CHANGE_MARKER_POSITION)
				.setTitleId(R.string.change_markers_position, getContext())
				.setIcon(R.drawable.ic_show_on_map)
				.setOrder(MapActivityActions.CHANGE_POSITION_ITEM_ORDER)
				.setClickable(isObjectMoveable(o))
				.setListener(listener));
	}

	@Override
	@RequiresPermission(Manifest.permission.VIBRATE)
	public boolean onLongPressEvent(@NonNull PointF point, @NonNull RotatedTileBox tileBox) {
		if (menu == null || disableLongPressOnMap(point, tileBox)) {
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
		hideVisibleMenues();
		MapRendererView mapRenderer = getMapRenderer();
		LatLon latLon = NativeUtilities.getLatLonFromElevatedPixel(mapRenderer, tileBox, point);
		menu.show(latLon, null, null);

		view.refreshMap();
		return true;
	}

	@NonNull
	public LatLon getMovableCenterLatLon(@NonNull RotatedTileBox tileBox) {
		return applyingMarkerLatLon != null
				? applyingMarkerLatLon
				: tileBox.getCenterLatLon();
	}

	public PointF getMovableCenterPoint(RotatedTileBox tb) {
		if (applyingMarkerLatLon != null) {
			return NativeUtilities.getElevatedPixelFromLatLon(getMapRenderer(), tb, applyingMarkerLatLon);
		} else {
			return new PointF(tb.getPixWidth() / 2f, tb.getPixHeight() / 2f);
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

	public boolean isInAddGpxPointMode() {
		return mInAddGpxPointMode;
	}

	public boolean isObjectMoveable(Object o) {
		if (o != null && selectedObjectContextMenuProvider != null
				&& selectedObjectContextMenuProvider instanceof IMoveObjectProvider) {
			IMoveObjectProvider l = (IMoveObjectProvider) selectedObjectContextMenuProvider;
			return l.isObjectMovable(o);
		}
		return false;
	}

	public void applyMovedObject(Object o, LatLon position, ApplyMovedObjectCallback callback) {
		if (selectedObjectContextMenuProvider != null && !isInAddGpxPointMode()) {
			if (selectedObjectContextMenuProvider instanceof IMoveObjectProvider) {
				IMoveObjectProvider l = (IMoveObjectProvider) selectedObjectContextMenuProvider;
				if (l.isObjectMovable(o)) {
					l.applyNewObjectPosition(o, position, callback);
				}
			}
		} else if (mInChangeMarkerPositionMode || mInAddGpxPointMode) {
			callback.onApplyMovedObject(true, null);
		}
	}

	public void applyNewMarkerPosition() {
		if (!mInChangeMarkerPositionMode) {
			throw new IllegalStateException("Not in change marker position mode");
		}
		if (mMoveMarkerBottomSheetHelper == null) {
			return;
		}

		RotatedTileBox tileBox = getMapView().getCurrentRotatedTileBox();
		PointF newMarkerPosition = getMovableCenterPoint(tileBox);
		LatLon ll = NativeUtilities.getLatLonFromElevatedPixel(getMapRenderer(), tileBox, newMarkerPosition);
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
					menu.close();

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

	public void createGpxPoint() {
		if (!mInAddGpxPointMode) {
			throw new IllegalStateException("Not in add gpx point mode");
		}
		if (mAddGpxPointBottomSheetHelper == null) {
			return;
		}

		RotatedTileBox tileBox = getMapView().getCurrentRotatedTileBox();
		LatLon ll = getMovableCenterLatLon(tileBox);
		applyingMarkerLatLon = ll;

		Object obj = getMoveableObject();
		cancelApplyingNewMarkerPosition = false;
		mAddGpxPointBottomSheetHelper.enterApplyPositionMode();
		applyMovedObject(obj, ll, new ApplyMovedObjectCallback() {
			@Override
			public void onApplyMovedObject(boolean success, @Nullable Object newObject) {
				mAddGpxPointBottomSheetHelper.exitApplyPositionMode();
				if (success && !cancelApplyingNewMarkerPosition) {
					mAddGpxPointBottomSheetHelper.hide();
					quitAddGpxPoint();
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
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}

		menu.updateMapCenter(null);
		menu.hide();

		mInGpxDetailsMode = true;
		mapActivity.disableDrawer();
		AndroidUiHelper.setVisibility(mapActivity, View.INVISIBLE,
				R.id.map_ruler_layout,
				R.id.map_left_widgets_panel,
				R.id.map_right_widgets_panel,
				R.id.map_center_info);
	}

	public void exitGpxDetailsMode() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}

		mInGpxDetailsMode = false;
		mapActivity.enableDrawer();
		AndroidUiHelper.setVisibility(mapActivity, View.VISIBLE,
				R.id.map_ruler_layout,
				R.id.map_left_widgets_panel,
				R.id.map_right_widgets_panel,
				R.id.map_center_info);
	}

	private void quitMovingMarker() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}

		mInChangeMarkerPositionMode = false;
		AndroidUiHelper.setVisibility(mapActivity, View.VISIBLE,
				R.id.map_ruler_layout,
				R.id.map_left_widgets_panel,
				R.id.map_right_widgets_panel,
				R.id.map_center_info);
	}

	public void quitAddGpxPoint() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}

		mInAddGpxPointMode = false;
		AndroidUiHelper.setVisibility(mapActivity, View.VISIBLE,
				R.id.map_ruler_layout,
				R.id.map_left_widgets_panel,
				R.id.map_right_widgets_panel,
				R.id.map_center_info);
	}

	public void enterAddGpxPointMode(NewGpxPoint newGpxPoint) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null || mAddGpxPointBottomSheetHelper == null) {
			return;
		}

		menu.updateMapCenter(null);
		menu.hide();

		mapActivity.disableDrawer();

		mInAddGpxPointMode = true;
		mAddGpxPointBottomSheetHelper.show(newGpxPoint);
		AndroidUiHelper.setVisibility(mapActivity, View.INVISIBLE,
				R.id.map_ruler_layout,
				R.id.map_left_widgets_panel,
				R.id.map_right_widgets_panel,
				R.id.map_center_info);

		view.refreshMap();
	}

	private void enterMovingMode(RotatedTileBox tileBox) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null || mMoveMarkerBottomSheetHelper == null) {
			return;
		}

		Vibrator vibrator = (Vibrator) mapActivity.getSystemService(Context.VIBRATOR_SERVICE);
		if (vibrator != null) {
			vibrator.vibrate(VIBRATE_SHORT);
		}

		menu.updateMapCenter(null);
		menu.hide();

		LatLon ll = menu.getLatLon();
		if (hasMapRenderer()) {
			view.setLatLon(ll.getLatitude(), ll.getLongitude());
		} else {
			RotatedTileBox rb = new RotatedTileBox(tileBox);
			rb.setCenterLocation(0.5f, 0.5f);
			rb.setLatLonCenter(ll.getLatitude(), ll.getLongitude());
			double lat = rb.getLatFromPixel(tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
			double lon = rb.getLonFromPixel(tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
			view.setLatLon(lat, lon);
		}

		mInChangeMarkerPositionMode = true;
		mMoveMarkerBottomSheetHelper.show(menu.getRightIcon());
		AndroidUiHelper.setVisibility(mapActivity, View.INVISIBLE,
				R.id.map_ruler_layout,
				R.id.map_left_widgets_panel,
				R.id.map_right_widgets_panel,
				R.id.map_center_info);

		view.refreshMap();
	}

	public void cancelMovingMarker() {
		if (menu != null) {
			cancelApplyingNewMarkerPosition = true;
			quitMovingMarker();
			menu.show();
			applyingMarkerLatLon = null;
		}
	}

	public void cancelAddGpxPoint() {
		cancelApplyingNewMarkerPosition = true;
		quitAddGpxPoint();
		applyingMarkerLatLon = null;
	}

	public boolean showContextMenuForMyLocation() {
		PointLocationLayer provider = view.getLayerByClass(PointLocationLayer.class);
		if (provider != null) {
			LatLon ll = provider.getObjectLocation(null);
			if (ll != null) {
				PointDescription pointDescription = provider.getObjectName(null);
				return showContextMenu(ll, pointDescription, ll, provider);
			}
		}
		return false;
	}

	public boolean showContextMenu(@NonNull LatLon latLon,
	                               @Nullable PointDescription pointDescription,
	                               @Nullable Object object,
	                               @Nullable IContextMenuProvider provider) {
		if (mInAddGpxPointMode) {
			String title = pointDescription == null ? "" : pointDescription.getName();
			if (mAddGpxPointBottomSheetHelper != null) {
				mAddGpxPointBottomSheetHelper.setTitle(title);
			}
			view.getAnimatedDraggingThread().startMoving(latLon.getLatitude(), latLon.getLongitude(), view.getZoom());
		} else if (provider == null || !provider.showMenuAction(object)) {
			selectedObjectContextMenuProvider = provider;
			hideVisibleMenues();
			getApplication().getMapViewTrackingUtilities().setMapLinkedToLocation(false);
			if (!NativeUtilities.containsLatLon(getMapRenderer(), getMapView().getCurrentRotatedTileBox(), latLon)) {
				menu.setMapCenter(latLon);
				menu.setCenterMarker(true);
			}
			menu.show(latLon, pointDescription, object);
		}
		return true;
	}

	public boolean showContextMenu(PointF point, RotatedTileBox tileBox, boolean showUnknownLocation) {
		if (menu == null || mAddGpxPointBottomSheetHelper == null) {
			return false;
		}
		MapSelectionResult selectionResult = selectionHelper.selectObjectsFromMap(point, tileBox, showUnknownLocation);
		LatLon pointLatLon = selectionResult.pointLatLon;
		Map<Object, IContextMenuProvider> selectedObjects = selectionResult.selectedObjects;

		for (Map.Entry<Object, IContextMenuProvider> entry : selectedObjects.entrySet()) {
			IContextMenuProvider provider = entry.getValue();
			if (provider != null && provider.runExclusiveAction(entry.getKey(), showUnknownLocation)) {
				return true;
			}
		}
		if (selectedObjects.size() == 1) {
			Object selectedObj = selectedObjects.keySet().iterator().next();
			LatLon latLon = selectionResult.getObjectLatLon();
			PointDescription pointDescription = null;
			IContextMenuProvider provider = selectedObjects.get(selectedObj);
			if (provider != null) {
				if (latLon == null) {
					latLon = provider.getObjectLocation(selectedObj);
				}
				pointDescription = provider.getObjectName(selectedObj);
			}
			if (latLon == null) {
				latLon = pointLatLon;
			}
			if (mInAddGpxPointMode) {
				String title = pointDescription == null ? "" : pointDescription.getName();
				mAddGpxPointBottomSheetHelper.setTitle(title);
				view.getAnimatedDraggingThread().startMoving(latLon.getLatitude(), latLon.getLongitude(), view.getZoom());
			} else {
				showContextMenu(latLon, pointDescription, selectedObj, provider);
			}
			return true;
		} else if (selectedObjects.size() > 1) {
			showContextMenuForSelectedObjects(pointLatLon, selectedObjects);
			return true;
		} else if (showUnknownLocation) {
			hideVisibleMenues();
			selectedObjectContextMenuProvider = null;
			getApplication().getMapViewTrackingUtilities().setMapLinkedToLocation(false);
			if (mInAddGpxPointMode) {
				mAddGpxPointBottomSheetHelper.setTitle("");
				view.getAnimatedDraggingThread().startMoving(pointLatLon.getLatitude(), pointLatLon.getLongitude(), view.getZoom());
			} else {
				menu.show(pointLatLon, null, null);
			}
			return true;
		}
		return false;
	}

	public boolean disableSingleTap() {
		MapActivity mapActivity = getMapActivity();
		WeatherPlugin plugin = PluginsHelper.getActivePlugin(WeatherPlugin.class);
		if (mapActivity == null || mapActivity.getMapRouteInfoMenu().isVisible()
				|| MapRouteInfoMenu.waypointsVisible || MapRouteInfoMenu.followTrackVisible
				|| (plugin != null && plugin.hasCustomForecast())) {
			return true;
		}
		boolean res = false;
		for (OsmandMapLayer lt : view.getLayers()) {
			if (lt instanceof IContextMenuProvider) {
				if (((IContextMenuProvider) lt).disableSingleTap()) {
					res = true;
					break;
				}
			}
		}
		return res;
	}

	public boolean disableLongPressOnMap(PointF point, RotatedTileBox tileBox) {
		MapActivity mapActivity = getMapActivity();
		WeatherPlugin plugin = PluginsHelper.getActivePlugin(WeatherPlugin.class);
		if (mInChangeMarkerPositionMode || mInGpxDetailsMode || mInAddGpxPointMode
				|| mapActivity == null || mapActivity.getMapRouteInfoMenu().isVisible()
				|| MapRouteInfoMenu.waypointsVisible || MapRouteInfoMenu.followTrackVisible
				|| mapActivity.getFragmentsHelper().getGpsFilterFragment() != null
				|| mapActivity.getFragmentsHelper().getDownloadTilesFragment() != null
				|| (plugin != null && plugin.hasCustomForecast())) {
			return true;
		}
		boolean res = false;
		for (OsmandMapLayer lt : view.getLayers()) {
			if (lt instanceof IContextMenuProvider) {
				if (((IContextMenuProvider) lt).disableLongPressOnMap(point, tileBox)) {
					res = true;
					break;
				}
			}
		}
		return res;
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
		} else if (menu != null && menu.isActive()) {
			LatLon latLon = menu.getLatLon();
			PointF pixel = NativeUtilities.getElevatedPixelFromLatLon(getMapRenderer(), tb, latLon);
			markerX = pixel.x;
			markerY = pixel.y;
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
	public boolean onSingleTap(@NonNull PointF point, @NonNull RotatedTileBox tileBox) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null || menu == null || mInChangeMarkerPositionMode || mInGpxDetailsMode
				|| mapActivity.getFragmentsHelper().getGpsFilterFragment() != null
				|| mapActivity.getFragmentsHelper().getDownloadTilesFragment() != null) {
			return true;
		}

		if (pressedContextMarker(tileBox, point.x, point.y)) {
			hideVisibleMenues();
			menu.show();
			return true;
		}

		if (selectOnMap != null) {
			MapRendererView mapRenderer = getMapRenderer();
			LatLon latlon = NativeUtilities.getLatLonFromElevatedPixel(mapRenderer, tileBox, point);
			menu.init(latlon, null, null);
			CallbackWithObject<LatLon> cb = selectOnMap;
			cb.processResult(latlon);
			selectOnMap = null;
			return true;
		}

		if (!disableSingleTap()) {
			boolean res = showContextMenu(point, tileBox, false);
			if (res) {
				return true;
			}
		}

		boolean processed = hideVisibleMenues();
		processed |= menu.onSingleTapOnMap();
		if (!processed && MapRouteInfoMenu.chooseRoutesVisible) {
			WeakReference<ChooseRouteFragment> chooseRouteFragmentRef = mapActivity.getMapRouteInfoMenu().findChooseRouteFragment();
			if (chooseRouteFragmentRef != null) {
				ChooseRouteFragment chooseRouteFragment = chooseRouteFragmentRef.get();
				if (chooseRouteFragment != null) {
					chooseRouteFragment.dismiss();
					processed = true;
				}
			}
		}
		if (!processed) {
			MapControlsLayer mapControlsLayer = getApplication().getOsmandMap().getMapLayers().getMapControlsLayer();
			if (mapControlsLayer != null && (!mapControlsLayer.isMapControlsVisible()
					|| getApplication().getSettings().MAP_EMPTY_STATE_ALLOWED.get())) {
				mapControlsLayer.switchMapControlsVisibility(true);
			}
		}
		return false;
	}

	private boolean hideVisibleMenues() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null && mapActivity.getFragmentsHelper().getTrackMenuFragment() != null) {
			mapActivity.getFragmentsHelper().getTrackMenuFragment().dismiss();
			MapActivity.clearPrevActivityIntent();
			return true;
		}
		if (multiSelectionMenu != null && multiSelectionMenu.isVisible()) {
			multiSelectionMenu.hide();
			return true;
		}
		return false;
	}

	protected void showContextMenuForSelectedObjects(LatLon latLon, Map<Object, IContextMenuProvider> selectedObjects) {
		hideVisibleMenues();
		selectedObjectContextMenuProvider = null;
		if (multiSelectionMenu != null) {
			multiSelectionMenu.show(latLon, selectedObjects);
		}
	}

	public void setMapQuickActionLayer(MapQuickActionLayer mapQuickActionLayer) {
		this.mapQuickActionLayer = mapQuickActionLayer;
	}

	@Override
	public boolean onTouchEvent(@NonNull MotionEvent event, @NonNull RotatedTileBox tileBox) {
		if (movementListener != null && movementListener.onTouchEvent(event)) {
			if (multiSelectionMenu != null && multiSelectionMenu.isVisible()) {
				multiSelectionMenu.hide();
			}
		}

		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				if (!mInChangeMarkerPositionMode && !mInGpxDetailsMode) {
					PointF pointF = new PointF(event.getX(), event.getY());
					selectionHelper.acquireTouchedMapObjects(tileBox, pointF, true);
					if (selectionHelper.hasTouchedMapObjects()) {
						view.refreshMap();
					}
				}
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				selectionHelper.clearTouchedMapObjects();
				view.refreshMap();
				break;
		}

		return false;
	}

	public interface IContextMenuProvider {

		/**
		 * @param excludeUntouchableObjects Touchable objects are objects that
		 *                                  change appearance when touched on map
		 */
		void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> o,
		                             boolean unknownLocation, boolean excludeUntouchableObjects);

		LatLon getObjectLocation(Object o);

		PointDescription getObjectName(Object o);

		default boolean disableSingleTap() {
			return false;
		}

		default boolean disableLongPressOnMap(PointF point, RotatedTileBox tileBox) {
			return false;
		}

		default boolean runExclusiveAction(@Nullable Object o, boolean unknownLocation) {
			return false;
		}

		default boolean showMenuAction(@Nullable Object o) {
			return false;
		}
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

	private static class MenuLayerOnGestureListener extends SimpleOnGestureListener {

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
