package net.osmand.plus.plugins.osmedit;

import static net.osmand.data.FavouritePoint.DEFAULT_BACKGROUND_TYPE;
import static net.osmand.data.PointDescription.POINT_TYPE_OSM_BUG;
import static net.osmand.data.PointDescription.POINT_TYPE_OSM_NOTE;
import static net.osmand.plus.AppInitEvents.POI_TYPES_INITIALIZED;
import static net.osmand.plus.plugins.osmedit.data.OsmPoint.Group.BUG;
import static net.osmand.plus.plugins.osmedit.data.OsmPoint.Group.POI;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PointF;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.MapMarker;
import net.osmand.core.jni.MapMarkerBuilder;
import net.osmand.core.jni.MapMarkersCollection;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.TextRasterizer;
import net.osmand.data.BackgroundType;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.osm.PoiType;
import net.osmand.osm.edit.Entity;
import net.osmand.plus.AppInitEvents;
import net.osmand.plus.AppInitializeListener;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.osmedit.asynctasks.SaveOsmChangeAsyncTask;
import net.osmand.plus.plugins.osmedit.asynctasks.SaveOsmNoteAsyncTask;
import net.osmand.plus.plugins.osmedit.data.OpenstreetmapPoint;
import net.osmand.plus.plugins.osmedit.data.OsmNotesPoint;
import net.osmand.plus.plugins.osmedit.data.OsmPoint;
import net.osmand.plus.plugins.osmedit.helpers.OpenstreetmapLocalUtil;
import net.osmand.plus.plugins.osmedit.helpers.OsmBugsLocalUtil;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.PointImageDrawable;
import net.osmand.plus.views.PointImageUtils;
import net.osmand.plus.views.layers.ContextMenuLayer;
import net.osmand.plus.views.layers.ContextMenuLayer.ApplyMovedObjectCallback;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.layers.ContextMenuLayer.IMoveObjectProvider;
import net.osmand.plus.views.layers.MapSelectionResult;
import net.osmand.plus.views.layers.MapTextLayer;
import net.osmand.plus.views.layers.MapTextLayer.MapTextProvider;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OsmEditsLayer extends OsmandMapLayer implements IContextMenuProvider, IMoveObjectProvider,
		MapTextProvider<OpenstreetmapPoint> {

	private static final int START_ZOOM = 10;

	private final OsmandApplication app;
	private final OsmEditingPlugin plugin;
	private final Context ctx;
	private final OpenstreetmapLocalUtil mOsmChangeUtil;
	private final OsmBugsLocalUtil mOsmBugsUtil;

	private final List<OsmPoint> drawnOsmEdits = new ArrayList<>();

	@Nullable
	private MapTextLayer mapTextLayer;
	@Nullable
	private ContextMenuLayer contextMenuLayer;

	//OpenGL
	private boolean nightMode;
	private float storedTextScale = 1.0f;
	private boolean poiTypesInitialized;

	public OsmEditsLayer(@NonNull Context context, @NonNull OsmEditingPlugin plugin) {
		super(context);
		this.ctx = context;
		this.plugin = plugin;
		app = getApplication();
		mOsmChangeUtil = plugin.getPoiModificationLocalUtil();
		mOsmBugsUtil = plugin.getOsmNotesLocalUtil();
	}

	@Override
	public void initLayer(@NonNull OsmandMapTileView view) {
		super.initLayer(view);

		mapTextLayer = view.getLayerByClass(MapTextLayer.class);
		contextMenuLayer = view.getLayerByClass(ContextMenuLayer.class);
		addInitPoiTypesListener();
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		if (contextMenuLayer != null) {
			if (contextMenuLayer.getMoveableObject() instanceof OsmPoint point) {
				setMovableObject(point.getLatitude(), point.getLongitude());
			}
			if (movableObject != null && !contextMenuLayer.isInChangeMarkerPositionMode()) {
				cancelMovableObject();
			}
		}
	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		super.onPrepareBufferImage(canvas, tileBox, settings);
		MapRendererView mapRenderer = getMapView().getMapRenderer();
		if (mapRenderer != null) {
			if (!poiTypesInitialized) {
				return;
			}
			if (tileBox.getZoom() < START_ZOOM) {
				clearMapMarkersCollections();
				return;
			}
			List<OsmNotesPoint> notesPoints = plugin.getDBBug().getOsmBugsPoints();
			List<OpenstreetmapPoint> osmPoints = plugin.getDBPOI().getOpenstreetmapPoints();
			int pointsSize = notesPoints.size() + osmPoints.size();
			if ((mapMarkersCollection != null && mapMarkersCollection.getMarkers().size() != pointsSize)
					|| nightMode != settings.isNightMode() || storedTextScale != getTextScale()) {
				clearMapMarkersCollections();
			}
			nightMode = settings.isNightMode();
			storedTextScale = getTextScale();
			if (pointsSize > 0 && mapMarkersCollection == null) {
				mapMarkersCollection = new MapMarkersCollection();
				List<LatLon> fullObjectsLatLon = new ArrayList<>();
				showOsmPoints(notesPoints, fullObjectsLatLon);
				showOsmPoints(osmPoints, fullObjectsLatLon);
				if (!fullObjectsLatLon.isEmpty()) {
					mapRenderer.addSymbolsProvider(mapMarkersCollection);
					this.fullObjectsLatLon = fullObjectsLatLon;
				}
			}
		} else {
			drawnOsmEdits.clear();
			if (tileBox.getZoom() >= START_ZOOM) {
				List<LatLon> fullObjectsLatLon = new ArrayList<>();
				drawOsmBugsPoints(canvas, tileBox, fullObjectsLatLon);
				drawOpenstreetmapPoints(canvas, tileBox, fullObjectsLatLon);
				this.fullObjectsLatLon = fullObjectsLatLon;
			}
			if (mapTextLayer != null && isTextVisible()) {
				mapTextLayer.putData(this, drawnOsmEdits);
			}
		}
	}

	private void drawOsmBugsPoints(Canvas canvas, RotatedTileBox tileBox, List<LatLon> fullObjectsLatLon) {
		drawPoints(canvas, tileBox, plugin.getDBBug().getOsmBugsPoints(), fullObjectsLatLon);
	}

	private void drawOpenstreetmapPoints(Canvas canvas, RotatedTileBox tileBox, List<LatLon> fullObjectsLatLon) {
		List<OpenstreetmapPoint> objects = plugin.getDBPOI().getOpenstreetmapPoints();
		drawnOsmEdits.addAll(drawPoints(canvas, tileBox, objects, fullObjectsLatLon));
	}

	@NonNull
	private List<OsmPoint> drawPoints(Canvas canvas, RotatedTileBox tileBox,
	                                  List<? extends OsmPoint> objects, List<LatLon> fullObjectsLatLon) {
		List<OsmPoint> fullObjects = new ArrayList<>();
		if (contextMenuLayer != null) {
			float iconSize = getIconSize(app);
			for (OsmPoint o : objects) {
				if (contextMenuLayer.getMoveableObject() != o) {
					float x = tileBox.getPixXFromLatLon(o.getLatitude(), o.getLongitude());
					float y = tileBox.getPixYFromLatLon(o.getLatitude(), o.getLongitude());
					if (tileBox.containsPoint(x, y, iconSize)) {
						drawPoint(canvas, o, x, y);
						fullObjects.add(o);
						fullObjectsLatLon.add(new LatLon(o.getLatitude(), o.getLongitude()));
					}
				}
			}
		}
		return fullObjects;
	}

	private void drawPoint(@NonNull Canvas canvas, @NonNull OsmPoint osmPoint, float x, float y) {
		float textScale = getTextScale();
		PointImageDrawable pointImageDrawable = createOsmPointIcon(osmPoint);
		int offsetY = pointImageDrawable.getBackgroundType().getOffsetY(ctx, textScale);
		pointImageDrawable.drawPoint(canvas, x, y - offsetY, textScale, false);
	}

	@NonNull
	public PointImageDrawable createOsmNoteIcon() {
		return createOsmPointIcon(getBugIconId(), true);
	}

	@NonNull
	public PointImageDrawable createOsmPointIcon(@NonNull OsmPoint osmPoint) {
		int iconId = getIconId(osmPoint);
		return createOsmPointIcon(iconId, osmPoint.getGroup() == BUG);
	}

	@NonNull
	public PointImageDrawable createOsmPointIcon(@DrawableRes int iconId, boolean isBug) {
		BackgroundType backgroundType = isBug ? BackgroundType.COMMENT : DEFAULT_BACKGROUND_TYPE;
		int pointColor = ColorUtilities.getColor(ctx, R.color.created_poi_icon_color);
		PointImageDrawable pointImageDrawable = PointImageUtils.getOrCreate(
				ctx, pointColor, true, false, iconId, backgroundType);
		pointImageDrawable.setAlpha(0.8f);
		return pointImageDrawable;
	}

	public int getIconId(@NonNull OsmPoint osmPoint) {
		if (osmPoint.getGroup() == POI) {
			OpenstreetmapPoint osmP = (OpenstreetmapPoint) osmPoint;
			int iconResId = 0;
			String poiTranslation = osmP.getEntity().getTag(Entity.POI_TYPE_TAG);
			if (poiTranslation != null && ctx != null) {
				Map<String, PoiType> poiTypeMap = app.getPoiTypes().getAllTranslatedNames(false);
				PoiType poiType = poiTypeMap.get(poiTranslation.toLowerCase());
				if (poiType != null) {
					String id = null;
					if (RenderingIcons.containsBigIcon(poiType.getIconKeyName())) {
						id = poiType.getIconKeyName();
					} else if (RenderingIcons.containsBigIcon(poiType.getOsmTag() + "_" + poiType.getOsmValue())) {
						id = poiType.getOsmTag() + "_" + poiType.getOsmValue();
					}
					if (id != null) {
						iconResId = RenderingIcons.getBigIconResourceId(id);
					}
				}
			}
			if (iconResId == 0) {
				iconResId = R.drawable.ic_action_info_dark;
			}
			return iconResId;
		} else if (osmPoint.getGroup() == BUG) {
			return getBugIconId();
		} else {
			return 0;
		}
	}

	public int getBugIconId() {
		return R.drawable.mm_special_symbol_plus;
	}

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}

	public void collectOsmEditsFromPoint(@NonNull MapSelectionResult result) {
		PointF point = result.getPoint();
		RotatedTileBox tileBox = result.getTileBox();
		if (tileBox.getZoom() < START_ZOOM) {
			return;
		}
		float radius = getScaledTouchRadius(app, getRadiusPoi(tileBox)) * TOUCH_RADIUS_MULTIPLIER;

		List<OsmPoint> osmBugs = new ArrayList<>(plugin.getDBBug().getOsmBugsPoints());
		if (!Algorithms.isEmpty(osmBugs)) {
			QuadRect screenArea = new QuadRect(
					point.x - radius,
					point.y - radius / 3f,
					point.x + radius,
					point.y + radius * 1.5f
			);
			collectOsmEditsFromScreenArea(tileBox, osmBugs, screenArea, result);
		}

		List<OsmPoint> osmEdits = new ArrayList<>(plugin.getDBPOI().getOpenstreetmapPoints());
		if (!Algorithms.isEmpty(osmEdits)) {
			QuadRect screenArea = new QuadRect(
					point.x - radius,
					point.y - radius,
					point.x + radius,
					point.y + radius
			);
			collectOsmEditsFromScreenArea(tileBox, osmEdits, screenArea, result);
		}
	}

	public void collectOsmEditsFromScreenArea(@NonNull RotatedTileBox tileBox,
	                                      @NonNull List<OsmPoint> osmEdits,
	                                      @NonNull QuadRect screenArea,
	                                      @NonNull MapSelectionResult result) {
		MapRendererView mapRenderer = getMapRenderer();
		List<PointI> touchPolygon31 = null;
		if (mapRenderer != null) {
			touchPolygon31 = NativeUtilities.getPolygon31FromScreenArea(mapRenderer, screenArea);
			if (touchPolygon31 == null) {
				return;
			}
		}

		for (OsmPoint osmEdit : osmEdits) {
			LatLon latLon = osmEdit.getLocation();
			boolean add = mapRenderer != null
					? NativeUtilities.isPointInsidePolygon(latLon, touchPolygon31)
					: tileBox.isLatLonInsidePixelArea(latLon, screenArea);
			if (add) {
				result.collect(osmEdit, this);
			}
		}
	}

	public int getRadiusPoi(RotatedTileBox tb) {
		int r;
		if (tb.getZoom() < START_ZOOM) {
			r = 0;
		} else {
			r = 15;
		}
		return (int) (r * tb.getDensity());
	}

	@Override
	public void collectObjectsFromPoint(@NonNull MapSelectionResult result,
	                                    boolean unknownLocation, boolean excludeUntouchableObjects) {
		if (result.getTileBox().getZoom() >= START_ZOOM) {
			collectOsmEditsFromPoint(result);
		}
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if (o instanceof OsmPoint) {
			return new LatLon(((OsmPoint) o).getLatitude(), ((OsmPoint) o).getLongitude());
		}
		return null;
	}

	@Override
	public PointDescription getObjectName(Object o) {
		if (o instanceof OsmPoint point) {
			String name = "";
			String type = "";
			if (point.getGroup() == POI) {
				name = ((OpenstreetmapPoint) point).getName();
				type = POINT_TYPE_OSM_NOTE;
			} else if (point.getGroup() == BUG) {
				name = ((OsmNotesPoint) point).getText();
				type = POINT_TYPE_OSM_BUG;
			}
			return new PointDescription(type, name);
		}
		return null;
	}

	@Override
	public boolean isObjectMovable(Object o) {
		return o instanceof OsmPoint;
	}

	@Override
	public Object getMoveableObjectIcon(@NonNull Object o) {
		if (o instanceof OsmPoint osmPoint) {
			return createOsmPointIcon(osmPoint);
		}
		return null;
	}

	@Override
	public void applyNewObjectPosition(@NonNull Object o, @NonNull LatLon position, @Nullable ApplyMovedObjectCallback callback) {
		if (o instanceof OsmPoint) {
			if (o instanceof OpenstreetmapPoint objectInMotion) {
				Entity entity = objectInMotion.getEntity();
				entity.setLatitude(position.getLatitude());
				entity.setLongitude(position.getLongitude());
				OsmAndTaskManager.executeTask(new SaveOsmChangeAsyncTask(mOsmChangeUtil, objectInMotion, callback));
			} else if (o instanceof OsmNotesPoint objectInMotion) {
				objectInMotion.setLatitude(position.getLatitude());
				objectInMotion.setLongitude(position.getLongitude());
				OsmAndTaskManager.executeTask(new SaveOsmNoteAsyncTask(getApplication(),
								mOsmBugsUtil, objectInMotion.getText(), callback), objectInMotion);
			}
			applyMovableObject(position);
		}
	}

	@Override
	public LatLon getTextLocation(OpenstreetmapPoint o) {
		return o.getLocation();
	}

	@Override
	public int getTextShift(OpenstreetmapPoint o, RotatedTileBox rb) {
		return (int) (16 * rb.getDensity() * getTextScale());
	}

	@Override
	public String getText(OpenstreetmapPoint o) {
		return o.getName();
	}

	@Override
	public boolean isTextVisible() {
		return app.getSettings().SHOW_POI_LABEL.get();
	}

	@Override
	public boolean isFakeBoldText() {
		return false;
	}

	/** OpenGL */
	private void showOsmPoints(List<? extends OsmPoint> objects, List<LatLon> fullObjectsLatLon) {
		MapRendererView mapRenderer = getMapView().getMapRenderer();
		if (mapRenderer == null) {
			return;
		}
		for (OsmPoint o : objects) {
			drawPoint(o);
			fullObjectsLatLon.add(new LatLon(o.getLatitude(), o.getLongitude()));
		}
	}

	/** OpenGL */
	private void drawPoint(@NonNull OsmPoint osmPoint) {
		if (mapMarkersCollection == null) {
			return;
		}
		float textScale = getTextScale();
		int x = MapUtils.get31TileNumberX(osmPoint.getLongitude());
		int y = MapUtils.get31TileNumberY(osmPoint.getLatitude());
		PointI position = new PointI(x, y);

		PointImageDrawable pointImageDrawable = createOsmPointIcon(osmPoint); //TODO bug with detect icon in getIcon()
		Bitmap bitmap = pointImageDrawable.getBigMergedBitmap(textScale, false);
		if (bitmap == null) {
			return;
		}

		MapMarkerBuilder mapMarkerBuilder = new MapMarkerBuilder();
		mapMarkerBuilder
				.setPosition(position)
				.setIsHidden(false)
				.setBaseOrder(getPointsOrder())
				.setPinIcon(NativeUtilities.createSkImageFromBitmap(bitmap))
				.setPinIconHorisontalAlignment(MapMarker.PinIconHorisontalAlignment.CenterHorizontal);

		mapMarkerBuilder.setPinIconVerticalAlignment(MapMarker.PinIconVerticalAlignment.CenterVertical);
		mapMarkerBuilder.setPinIconOffset(new PointI(0, -pointImageDrawable.getBackgroundType().getOffsetY(ctx, textScale)));

		if (isTextVisible() && osmPoint instanceof OpenstreetmapPoint) {
			mapMarkerBuilder
					.setCaptionStyle(getTextStyle())
					.setCaptionTopSpace(0)
					.setCaption(getText((OpenstreetmapPoint) osmPoint));
		}
		mapMarkerBuilder.buildAndAddToCollection(mapMarkersCollection);
	}

	/** OpenGL */
	private TextRasterizer.Style getTextStyle() {
		return MapTextLayer.getTextStyle(getContext(), nightMode, getTextScale(), view.getDensity());
	}

	private void addInitPoiTypesListener() {
		if (app.isApplicationInitializing()) {
			app.getAppInitializer().addListener(new AppInitializeListener() {

				@Override
				public void onProgress(@NonNull AppInitializer init, @NonNull AppInitEvents event) {
					if (event == POI_TYPES_INITIALIZED) {
						poiTypesInitialized = true;
					}
				}
			});
		} else {
			poiTypesInitialized = true;
		}
	}
}
