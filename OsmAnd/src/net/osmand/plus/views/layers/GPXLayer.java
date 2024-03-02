package net.osmand.plus.views.layers;

import static net.osmand.gpx.GPXUtilities.calculateTrackBounds;
import static net.osmand.plus.configmap.ConfigureMapMenu.CURRENT_TRACK_COLOR_ATTR;
import static net.osmand.plus.configmap.ConfigureMapMenu.CURRENT_TRACK_WIDTH_ATTR;
import static net.osmand.gpx.GpxParameter.COLOR;
import static net.osmand.gpx.GpxParameter.COLORING_TYPE;
import static net.osmand.gpx.GpxParameter.SHOW_ARROWS;
import static net.osmand.gpx.GpxParameter.SHOW_START_FINISH;
import static net.osmand.gpx.GpxParameter.WIDTH;
import static net.osmand.plus.routing.ColoringStyleAlgorithms.isAvailableInSubscription;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.PathEffect;
import android.graphics.PointF;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Pair;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import net.osmand.PlatformUtil;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.GpxAdditionalIconsProvider;
import net.osmand.core.jni.GpxAdditionalIconsProvider.SplitLabel;
import net.osmand.core.jni.MapMarkerBuilder;
import net.osmand.core.jni.MapMarkersCollection;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.QListPointI;
import net.osmand.core.jni.SplitLabelList;
import net.osmand.core.jni.TextRasterizer;
import net.osmand.core.jni.Utilities;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.data.RotatedTileBox;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities.TrkSegment;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.plus.ChartPointsHelper;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.card.color.ColoringPurpose;
import net.osmand.plus.card.color.ColoringStyle;
import net.osmand.plus.charts.TrackChartPoints;
import net.osmand.plus.mapcontextmenu.controllers.SelectedGpxMenuController.SelectedGpxPoint;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.render.OsmandDashPathEffect;
import net.osmand.plus.render.OsmandRenderer;
import net.osmand.plus.render.OsmandRenderer.RenderingContext;
import net.osmand.plus.routing.ColoringType;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.track.CachedTrack;
import net.osmand.plus.track.CachedTrackParams;
import net.osmand.plus.track.GradientScaleType;
import net.osmand.plus.track.TrackDrawInfo;
import net.osmand.plus.track.fragments.GpsFilterFragment;
import net.osmand.plus.track.fragments.TrackAppearanceFragment;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.track.helpers.GpxDataItem;
import net.osmand.plus.track.helpers.GpxDbHelper;
import net.osmand.plus.track.helpers.GpxDisplayGroup;
import net.osmand.plus.track.helpers.GpxDisplayItem;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.track.helpers.GpxUtils;
import net.osmand.plus.track.helpers.ParseGpxRouteTask;
import net.osmand.plus.track.helpers.ParseGpxRouteTask.ParseGpxRouteListener;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.track.helpers.save.SaveGpxHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.PointImageDrawable;
import net.osmand.plus.views.Renderable;
import net.osmand.plus.views.Renderable.RenderableSegment;
import net.osmand.plus.views.layers.ContextMenuLayer.ApplyMovedObjectCallback;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.layers.ContextMenuLayer.IMoveObjectProvider;
import net.osmand.plus.views.layers.MapTextLayer.MapTextProvider;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.layers.core.LocationPointsTileProvider;
import net.osmand.plus.views.layers.core.WptPtTileProvider;
import net.osmand.plus.views.layers.geometry.GpxGeometryWay;
import net.osmand.plus.views.layers.geometry.GpxGeometryWayContext;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.router.RouteSegmentResult;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GPXLayer extends OsmandMapLayer implements IContextMenuProvider, IMoveObjectProvider, MapTextProvider<WptPt> {

	private static final Log log = PlatformUtil.getLog(GPXLayer.class);

	private static final int DEFAULT_WIDTH_MULTIPLIER = 7;
	private static final int START_ZOOM = 7;

	private Paint paint;
	private Paint borderPaint;
	private Paint shadowPaint;

	private int cachedHash;
	@ColorInt
	private int cachedColor;
	private float defaultTrackWidth;
	private final Map<String, Float> cachedTrackWidth = new HashMap<>();

	private Drawable startPointIcon;
	private Drawable finishPointIcon;
	private Drawable startAndFinishIcon;
	private Bitmap startPointImage;
	private Bitmap finishPointImage;
	private Bitmap startAndFinishImage;
	private Bitmap highlightedPointImage;
	private TrackDrawInfo trackDrawInfo;
	private float textScale = 1f;
	private boolean nightMode;
	private boolean changeMarkerPositionModeCached;

	private ChartPointsHelper chartPointsHelper;
	private TrackChartPoints trackChartPoints;
	private List<LatLon> xAxisPointsCached = new ArrayList<>();

	private OsmandApplication app;
	private OsmandSettings settings;
	private GpxDbHelper gpxDbHelper;
	private MapMarkersHelper mapMarkersHelper;
	private GpxSelectionHelper selectedGpxHelper;

	private final Map<String, ParseGpxRouteTask> parseGpxRouteTasks = new ConcurrentHashMap<>();
	private final ExecutorService parseGpxRouteSingleThreadExecutor = Executors.newSingleThreadExecutor();

	private Map<SelectedGpxFile, Long> visibleGPXFilesMap = new HashMap<>();
	private final Map<String, CachedTrack> segmentsCache = new HashMap<>();
	private final Map<String, Set<TrkSegment>> renderedSegmentsCache = new HashMap<>();
	private SelectedGpxFile tmpVisibleTrack;

	private final List<WptPt> pointsCache = new ArrayList<>();
	private Map<WptPt, SelectedGpxFile> pointFileMap = new HashMap<>();
	private MapTextLayer textLayer;
	public CustomMapObjects<SelectedGpxFile> customObjectsDelegate;

	private Paint paintOuterRect;
	private Paint paintInnerRect;

	private Paint paintTextIcon;

	private GpxGeometryWayContext wayContext;

	private OsmandRenderer osmandRenderer;

	//OpenGl
	private GpxAdditionalIconsProvider additionalIconsProvider;
	private int startFinishPointsCountCached;
	private int splitLabelsCountCached;
	private int pointCountCached;
	private int hiddenGroupsCountCached;
	private boolean textVisibleCached;
	private WptPtTileProvider pointsTileProvider;
	private LocationPointsTileProvider trackChartPointsProvider;
	private MapMarkersCollection highlightedPointCollection;
	private net.osmand.core.jni.MapMarker highlightedPointMarker;
	private LatLon highlightedPointLocationCached;
	private long trackMarkersChangedTime;

	private ContextMenuLayer contextMenuLayer;

	@ColorInt
	private int visitedColor;
	@ColorInt
	private int defPointColor;
	@ColorInt
	private int grayColor;
	@ColorInt
	private int disabledColor;

	private CommonPreference<String> defaultColorPref;
	private CommonPreference<String> defaultWidthPref;

	private CommonPreference<Integer> currentTrackColorPref;
	private CommonPreference<ColoringType> currentTrackColoringTypePref;
	private CommonPreference<String> currentTrackRouteInfoAttributePref;
	private CommonPreference<String> currentTrackWidthPref;
	private CommonPreference<Boolean> currentTrackShowArrowsPref;
	private OsmandPreference<Boolean> currentTrackShowStartFinishPref;

	public GPXLayer(@NonNull Context ctx) {
		super(ctx);
	}

	@Override
	public void initLayer(@NonNull OsmandMapTileView view) {
		super.initLayer(view);

		app = view.getApplication();
		settings = app.getSettings();
		gpxDbHelper = app.getGpxDbHelper();
		selectedGpxHelper = app.getSelectedGpxHelper();
		mapMarkersHelper = app.getMapMarkersHelper();
		osmandRenderer = app.getResourceManager().getRenderer().getRenderer();
		chartPointsHelper = new ChartPointsHelper(getContext());

		currentTrackColorPref = settings.CURRENT_TRACK_COLOR;
		currentTrackColoringTypePref = settings.CURRENT_TRACK_COLORING_TYPE;
		currentTrackRouteInfoAttributePref = settings.CURRENT_TRACK_ROUTE_INFO_ATTRIBUTE;
		currentTrackWidthPref = settings.CURRENT_TRACK_WIDTH;
		currentTrackShowArrowsPref = settings.CURRENT_TRACK_SHOW_ARROWS;
		currentTrackShowStartFinishPref = settings.CURRENT_TRACK_SHOW_START_FINISH;

		defaultColorPref = settings.getCustomRenderProperty(CURRENT_TRACK_COLOR_ATTR).cache();
		defaultWidthPref = settings.getCustomRenderProperty(CURRENT_TRACK_WIDTH_ATTR).cache();

		initUI();
	}

	public void setTrackChartPoints(@Nullable TrackChartPoints trackChartPoints) {
		this.trackChartPoints = trackChartPoints;
	}

	public boolean isInTrackAppearanceMode() {
		return trackDrawInfo != null;
	}

	public void setTrackDrawInfo(@Nullable TrackDrawInfo trackDrawInfo) {
		this.trackDrawInfo = trackDrawInfo;
	}

	private void initUI() {
		paint = new Paint();
		paint.setStyle(Style.STROKE);
		paint.setAntiAlias(true);

		borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		borderPaint.setStyle(Style.STROKE);
		borderPaint.setStrokeJoin(Paint.Join.ROUND);
		borderPaint.setStrokeCap(Paint.Cap.ROUND);
		borderPaint.setColor(0x80000000);

		shadowPaint = new Paint();
		shadowPaint.setStyle(Style.STROKE);
		shadowPaint.setAntiAlias(true);

		paintTextIcon = new Paint();
		paintTextIcon.setTextSize(10 * view.getDensity());
		paintTextIcon.setTextAlign(Align.CENTER);
		paintTextIcon.setFakeBoldText(true);
		paintTextIcon.setAntiAlias(true);

		textLayer = view.getLayerByClass(MapTextLayer.class);

		paintInnerRect = new Paint();
		paintInnerRect.setStyle(Style.FILL);
		paintInnerRect.setAntiAlias(true);
		paintOuterRect = new Paint();
		paintOuterRect.setStyle(Style.STROKE);
		paintOuterRect.setAntiAlias(true);
		paintOuterRect.setStrokeWidth(3);
		paintOuterRect.setAlpha(255);

		UiUtilities iconsCache = app.getUIUtilities();
		startPointIcon = iconsCache.getIcon(R.drawable.map_track_point_start);
		finishPointIcon = iconsCache.getIcon(R.drawable.map_track_point_finish);
		startAndFinishIcon = iconsCache.getIcon(R.drawable.map_track_point_start_finish);

		contextMenuLayer = view.getLayerByClass(ContextMenuLayer.class);

		visitedColor = ContextCompat.getColor(app, R.color.color_ok);
		defPointColor = ContextCompat.getColor(app, R.color.gpx_color_point);
		grayColor = ContextCompat.getColor(app, R.color.color_favorite_gray);
		disabledColor = ContextCompat.getColor(app, R.color.gpx_disabled_color);

		wayContext = new GpxGeometryWayContext(getContext(), view.getDensity());
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		drawMovableWpt(canvas, tileBox);
	}

	private void drawMovableWpt(@NonNull Canvas canvas, @NonNull RotatedTileBox tileBox) {
		Object movableObject = contextMenuLayer.getMoveableObject();
		if (movableObject instanceof WptPt) {
			WptPt movableWpt = ((WptPt) movableObject);
			SelectedGpxFile gpxFile = pointFileMap.get(movableWpt);

			if (gpxFile != null) {
				PointF pf = contextMenuLayer.getMovableCenterPoint(tileBox);
				MapMarker mapMarker = mapMarkersHelper.getMapMarker(movableWpt);
				float textScale = getTextScale();
				int fileColor = getFileColor(gpxFile);
				int pointColor = getPointColor(movableWpt, fileColor);

				canvas.save();
				canvas.rotate(-tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
				drawBigPoint(canvas, movableWpt, pointColor, pf.x, pf.y, mapMarker, textScale);
				canvas.restore();
			}
		}
	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		super.onPrepareBufferImage(canvas, tileBox, settings);
		List<SelectedGpxFile> visibleGPXFiles;
		if (customObjectsDelegate != null) {
			visibleGPXFiles = customObjectsDelegate.getMapObjects();
		} else {
			visibleGPXFiles = new ArrayList<>(selectedGpxHelper.getSelectedGPXFiles());
		}

		boolean tmpVisibleTrackChanged = updateTmpVisibleTrack(visibleGPXFiles);

		pointsCache.clear();
		removeCachedUnselectedTracks(visibleGPXFiles);

		Map<SelectedGpxFile, Long> visibleGPXFilesMap = new HashMap<>();
		boolean pointsModified = false;
		for (SelectedGpxFile selectedGpxFile : visibleGPXFiles) {
			Long pointsModifiedTime = this.visibleGPXFilesMap.get(selectedGpxFile);
			long newPointsModifiedTime = selectedGpxFile.getPointsModifiedTime();
			if (pointsModifiedTime == null || pointsModifiedTime != newPointsModifiedTime) {
				pointsModified = true;
			}
			visibleGPXFilesMap.put(selectedGpxFile, newPointsModifiedTime);
		}
		this.visibleGPXFilesMap = visibleGPXFilesMap;
		boolean nightMode = settings != null && settings.isNightMode();
		boolean nightModeChanged = this.nightMode != nightMode;
		this.nightMode = nightMode;
		long trackMarkersChangedTime = mapMarkersHelper.getTrackMarkersModifiedTime();
		boolean trackMarkersChanged = this.trackMarkersChangedTime != trackMarkersChangedTime;
		this.trackMarkersChangedTime = trackMarkersChangedTime;
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null) {
			boolean forceUpdate = updateBitmaps() || nightModeChanged || pointsModified || tmpVisibleTrackChanged || mapRendererChanged;
			if (!visibleGPXFiles.isEmpty()) {
				drawSelectedFilesSegments(canvas, tileBox, visibleGPXFiles, settings);
			}
			drawXAxisPointsOpenGl(trackChartPoints, mapRenderer, tileBox);
			drawSelectedFilesSplitsOpenGl(mapRenderer, tileBox, visibleGPXFiles, forceUpdate);
			drawSelectedFilesPointsOpenGl(mapRenderer, tileBox, visibleGPXFiles, forceUpdate || trackMarkersChanged);
			mapRendererChanged = false;
		} else {
			if (!visibleGPXFiles.isEmpty()) {
				drawSelectedFilesSegments(canvas, tileBox, visibleGPXFiles, settings);
				canvas.rotate(-tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
				drawXAxisPoints(trackChartPoints, canvas, tileBox);
				drawDirectionArrows(canvas, tileBox, visibleGPXFiles);
				drawSelectedFilesSplits(canvas, tileBox, visibleGPXFiles);
				drawSelectedFilesPoints(canvas, tileBox, visibleGPXFiles);
				drawSelectedFilesStartEndPoints(canvas, tileBox, visibleGPXFiles);
			}
			if (textLayer != null && isTextVisible()) {
				textLayer.putData(this, pointsCache);
			}
		}
		mapActivityInvalidated = false;
	}

	private boolean updateTmpVisibleTrack(@NonNull List<SelectedGpxFile> visibleGPXFiles) {
		boolean tmpVisibleTrackChanged = false;
		SelectedGpxFile selectedGpxFile = getTmpVisibleTrack(visibleGPXFiles);
		if (selectedGpxFile != null) {
			visibleGPXFiles.add(selectedGpxFile);
			tmpVisibleTrackChanged = tmpVisibleTrack != selectedGpxFile;
			tmpVisibleTrack = selectedGpxFile;

			if (tmpVisibleTrackChanged) {
				CachedTrack cachedTrack = segmentsCache.remove(selectedGpxFile.getGpxFile().path);
				if (hasMapRenderer() && cachedTrack != null) {
					resetSymbolProviders(selectedGpxFile.getPointsToDisplay());
					resetSymbolProviders(cachedTrack.getAllNonSimplifiedCachedTrackSegments());
				}
			}
		}
		return tmpVisibleTrackChanged;
	}

	@Nullable
	private SelectedGpxFile getTmpVisibleTrack(@NonNull List<SelectedGpxFile> selectedGpxFiles) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			SelectedGpxFile selectedGpxFile = null;
			TrackMenuFragment fragment = mapActivity.getFragmentsHelper().getTrackMenuFragment();
			if (fragment != null) {
				selectedGpxFile = fragment.getSelectedGpxFile();
			}
			TrackAppearanceFragment appearanceFragment = mapActivity.getFragmentsHelper().getTrackAppearanceFragment();
			if (appearanceFragment != null) {
				selectedGpxFile = appearanceFragment.getSelectedGpxFile();
			}
			GpsFilterFragment gpsFilterFragment = mapActivity.getFragmentsHelper().getGpsFilterFragment();
			if (gpsFilterFragment != null) {
				selectedGpxFile = gpsFilterFragment.getSelectedGpxFile();
			}
			if (selectedGpxFile != null && !selectedGpxFiles.contains(selectedGpxFile)) {
				return selectedGpxFile;
			}
		}
		return null;
	}

	private boolean updatePaints(int color, String width, boolean routePoints, boolean currentTrack, DrawSettings drawSettings, RotatedTileBox tileBox) {
		RenderingRulesStorage rrs = app.getRendererRegistry().getCurrentSelectedRenderer();
		boolean nightMode = drawSettings != null && drawSettings.isNightMode();
		int hash;
		if (!hasMapRenderer()) {
			hash = calculateHash(rrs, cachedTrackWidth, routePoints, nightMode, tileBox.getMapDensity(), tileBox.getZoom(),
					defaultColorPref.get(), defaultWidthPref.get());
		} else {
			hash = calculateHash(rrs, cachedTrackWidth, routePoints, nightMode, tileBox.getMapDensity(),
					defaultColorPref.get(), defaultWidthPref.get());
		}
		boolean hashChanged = hash != cachedHash;
		if (hashChanged) {
			cachedHash = hash;
			cachedColor = ContextCompat.getColor(app, R.color.gpx_track);
			defaultTrackWidth = DEFAULT_WIDTH_MULTIPLIER * view.getDensity();
			if (rrs != null) {
				RenderingRuleSearchRequest req = new RenderingRuleSearchRequest(rrs);
				req.setBooleanFilter(rrs.PROPS.R_NIGHT_MODE, nightMode);
				if (defaultColorPref != null && defaultColorPref.isSet()) {
					RenderingRuleProperty ctColor = rrs.PROPS.get(CURRENT_TRACK_COLOR_ATTR);
					if (ctColor != null) {
						req.setStringFilter(ctColor, defaultColorPref.get());
					}
				}
				if (defaultWidthPref != null && defaultWidthPref.isSet()) {
					RenderingRuleProperty ctWidth = rrs.PROPS.get(CURRENT_TRACK_WIDTH_ATTR);
					if (ctWidth != null) {
						req.setStringFilter(ctWidth, defaultWidthPref.get());
					}
				}
				String additional = "";
				if (routePoints) {
					additional = "routePoints=true";
				}
				if (currentTrack) {
					additional = (additional.length() == 0 ? "" : ";") + "currentTrack=true";
				}
				req.setIntFilter(rrs.PROPS.R_MINZOOM, tileBox.getZoom());
				req.setIntFilter(rrs.PROPS.R_MAXZOOM, tileBox.getZoom());
				if (additional.length() > 0) {
					req.setStringFilter(rrs.PROPS.R_ADDITIONAL, additional);
				}
				if (req.searchRenderingAttribute("gpx")) {
					RenderingContext rc = new RenderingContext(getContext());
					rc.setDensityValue((float) tileBox.getMapDensity());
					cachedColor = req.getIntPropertyValue(rrs.PROPS.R_COLOR);
					defaultTrackWidth = rc.getComplexValue(req, req.ALL.R_STROKE_WIDTH);
					osmandRenderer.updatePaint(req, paint, 0, false, rc);

					if (req.isSpecified(rrs.PROPS.R_SHADOW_RADIUS)) {
						int shadowColor = req.getIntPropertyValue(rrs.PROPS.R_SHADOW_COLOR);
						float shadowRadius = rc.getComplexValue(req, rrs.PROPS.R_SHADOW_RADIUS);
						shadowPaint.setColorFilter(new PorterDuffColorFilter(shadowColor, Mode.SRC_IN));
						shadowPaint.setStrokeWidth(paint.getStrokeWidth() + 2 * shadowRadius);
					}
					for (String key : cachedTrackWidth.keySet()) {
						acquireTrackWidth(key, rrs, req, rc);
					}
				} else {
					log.error("Rendering attribute gpx is not found !");
					for (String key : cachedTrackWidth.keySet()) {
						cachedTrackWidth.put(key, defaultTrackWidth);
					}
				}
			}
		}
		paint.setColor(color == 0 ? cachedColor : color);
		paint.setStrokeWidth(getTrackWidth(width, defaultTrackWidth));
		borderPaint.setStrokeWidth(paint.getStrokeWidth() + AndroidUtils.dpToPx(getContext(), 2));
		return hashChanged;
	}

	private void acquireTrackWidth(String widthKey, RenderingRulesStorage rrs, RenderingRuleSearchRequest req, RenderingContext rc) {
		if (!Algorithms.isEmpty(widthKey) && Algorithms.isInt(widthKey)) {
			try {
				int widthDp = Integer.parseInt(widthKey);
				float widthF = AndroidUtils.dpToPx(app, widthDp);
				cachedTrackWidth.put(widthKey, widthF);
			} catch (NumberFormatException e) {
				log.error(e.getMessage(), e);
				cachedTrackWidth.put(widthKey, defaultTrackWidth);
			}
		} else {
			RenderingRuleProperty ctWidth = rrs.PROPS.get(CURRENT_TRACK_WIDTH_ATTR);
			if (ctWidth != null) {
				req.setStringFilter(ctWidth, widthKey);
			}
			if (req.searchRenderingAttribute("gpx")) {
				float widthF = rc.getComplexValue(req, req.ALL.R_STROKE_WIDTH);
				cachedTrackWidth.put(widthKey, widthF);
			}
		}
	}

	private int calculateHash(Object... o) {
		return Arrays.hashCode(o);
	}

	private void drawSelectedFilesSplits(@NonNull Canvas canvas, @NonNull RotatedTileBox tileBox,
	                                     @NonNull List<SelectedGpxFile> selectedGPXFiles) {
		if (tileBox.getZoom() >= START_ZOOM) {
			// request to load
			for (SelectedGpxFile selectedGpxFile : selectedGPXFiles) {
				List<GpxDisplayGroup> groups = selectedGpxFile.getDisplayGroups(app);
				if (!Algorithms.isEmpty(groups)) {
					int color = getTrackColor(selectedGpxFile.getGpxFile(), cachedColor);
					paintInnerRect.setColor(color);
					paintInnerRect.setAlpha(179);

					int contrastColor = ColorUtilities.getContrastColor(app, color, false);
					paintTextIcon.setColor(contrastColor);
					paintOuterRect.setColor(contrastColor);

					List<GpxDisplayItem> items = groups.get(0).getDisplayItems();
					drawSplitItems(canvas, tileBox, items);
				}
			}
		}
	}

	private void drawSelectedFilesSplitsOpenGl(@NonNull MapRendererView mapRenderer, @NonNull RotatedTileBox tileBox,
	                                           @NonNull List<SelectedGpxFile> selectedGPXFiles, boolean forceUpdate) {
		if (tileBox.getZoom() >= START_ZOOM) {
			boolean changed = forceUpdate;
			int startFinishPointsCount = 0;
			int splitLabelsCount = 0;
			for (SelectedGpxFile selectedGpxFile : selectedGPXFiles) {
				if (isShowStartFinishForTrack(selectedGpxFile.getGpxFile())) {
					List<TrkSegment> segments = selectedGpxFile.getPointsToDisplay();
					for (TrkSegment segment : segments) {
						if (segment.points.size() >= 2) {
							startFinishPointsCount += 2;
						}
					}
				}
				List<GpxDisplayGroup> groups = selectedGpxFile.getDisplayGroups(app);
				if (!Algorithms.isEmpty(groups)) {
					List<GpxDisplayItem> items = groups.get(0).getDisplayItems();
					for (GpxDisplayItem item : items) {
						if (item.splitName != null) {
							splitLabelsCount++;
						}
					}
				}
			}
			changed |= startFinishPointsCount != startFinishPointsCountCached;
			changed |= splitLabelsCount != splitLabelsCountCached;
			if (!changed && !mapActivityInvalidated) {
				return;
			}
			startFinishPointsCountCached = startFinishPointsCount;
			splitLabelsCountCached = splitLabelsCount;
			clearSelectedFilesSplits();

			QListPointI startFinishPoints = new QListPointI();
			SplitLabelList splitLabels = new SplitLabelList();
			for (SelectedGpxFile selectedGpxFile : selectedGPXFiles) {
				if (isShowStartFinishForTrack(selectedGpxFile.getGpxFile())) {
					List<TrkSegment> segments = selectedGpxFile.getPointsToDisplay();
					for (TrkSegment segment : segments) {
						if (segment.points.size() >= 2) {
							WptPt start = segment.points.get(0);
							WptPt finish = segment.points.get(segment.points.size() - 1);
							startFinishPoints.add(new PointI(Utilities.get31TileNumberX(start.lon), Utilities.get31TileNumberY(start.lat)));
							startFinishPoints.add(new PointI(Utilities.get31TileNumberX(finish.lon), Utilities.get31TileNumberY(finish.lat)));
						}
					}
				}
				List<GpxDisplayGroup> groups = selectedGpxFile.getDisplayGroups(app);
				if (!Algorithms.isEmpty(groups)) {
					int color = getTrackColor(selectedGpxFile.getGpxFile(), cachedColor);
					List<GpxDisplayItem> items = groups.get(0).getDisplayItems();
					for (GpxDisplayItem item : items) {
						WptPt point = item.locationEnd;
						String name = item.splitName;
						if (name != null) {
							int ind = name.indexOf(' ');
							if (ind > 0) {
								name = name.substring(0, ind);
							}
							PointI point31 = new PointI(Utilities.get31TileNumberX(point.lon), Utilities.get31TileNumberY(point.lat));
							splitLabels.add(new SplitLabel(point31, name, NativeUtilities.createColorARGB(color, 179)));
						}
					}
				}
			}
			if (!startFinishPoints.isEmpty() || !splitLabels.isEmpty()) {
				additionalIconsProvider = new GpxAdditionalIconsProvider(getPointsOrder() - selectedGPXFiles.size() - 101, tileBox.getDensity(),
						startFinishPoints, splitLabels,
						NativeUtilities.createSkImageFromBitmap(startPointImage),
						NativeUtilities.createSkImageFromBitmap(finishPointImage),
						NativeUtilities.createSkImageFromBitmap(startAndFinishImage));
				mapRenderer.addSymbolsProvider(additionalIconsProvider);
			}
		} else {
			startFinishPointsCountCached = 0;
			splitLabelsCountCached = 0;
			clearSelectedFilesSplits();
		}
	}

	private boolean updateBitmaps() {
		if (hasMapRenderer()) {
			float textScale = getTextScale();
			if (this.textScale != textScale || startPointImage == null || mapRendererChanged) {
				this.textScale = textScale;
				recreateBitmaps();
				return true;
			}
		}
		return false;
	}

	private void recreateBitmaps() {
		if (hasMapRenderer()) {
			startPointImage = getScaledBitmap(R.drawable.map_track_point_start);
			finishPointImage = getScaledBitmap(R.drawable.map_track_point_finish);
			startAndFinishImage = getScaledBitmap(R.drawable.map_track_point_start_finish);
			highlightedPointImage = chartPointsHelper.createHighlightedPointBitmap();
			recreateHighlightedPointCollection();
		}
	}

	private TextRasterizer.Style getTextStyle() {
		return MapTextLayer.getTextStyle(getContext(), nightMode, textScale, view.getDensity());
	}

	private void clearSelectedFilesSplits() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null && additionalIconsProvider != null) {
			mapRenderer.removeSymbolsProvider(additionalIconsProvider);
			additionalIconsProvider = null;
		}
	}

	@Nullable
	@Override
	protected Bitmap getScaledBitmap(int drawableId) {
		return getScaledBitmap(drawableId, textScale);
	}

	private void drawSplitItems(@NonNull Canvas canvas, @NonNull RotatedTileBox tileBox,
	                            @NonNull List<GpxDisplayItem> items) {
		QuadRect latLonBounds = tileBox.getLatLonBounds();
		int r = (int) (12 * tileBox.getDensity());
		paintTextIcon.setTextSize(r);
		int dr = r * 3 / 2;
		float px = -1;
		float py = -1;
		for (int k = 0; k < items.size(); k++) {
			GpxDisplayItem i = items.get(k);
			WptPt point = i.locationEnd;
			if (point != null && point.lat >= latLonBounds.bottom && point.lat <= latLonBounds.top
					&& point.lon >= latLonBounds.left && point.lon <= latLonBounds.right) {
				float x = tileBox.getPixXFromLatLon(point.lat, point.lon);
				float y = tileBox.getPixYFromLatLon(point.lat, point.lon);
				if (px != -1 || py != -1) {
					if (Math.abs(x - px) <= dr && Math.abs(y - py) <= dr) {
						continue;
					}
				}
				px = x;
				py = y;
				String name = i.splitName;
				if (name != null) {
					int ind = name.indexOf(' ');
					if (ind > 0) {
						name = name.substring(0, ind);
					}
					Rect bounds = new Rect();
					paintTextIcon.getTextBounds(name, 0, name.length(), bounds);

					float nameHalfWidth = bounds.width() / 2f;
					float nameHalfHeight = bounds.height() / 2f;
					float density = (float) Math.ceil(tileBox.getDensity());
					RectF rect = new RectF(x - nameHalfWidth - 2 * density,
							y + nameHalfHeight + 3 * density,
							x + nameHalfWidth + 3 * density,
							y - nameHalfHeight - 2 * density);

					canvas.drawRoundRect(rect, 0, 0, paintInnerRect);
					canvas.drawRoundRect(rect, 0, 0, paintOuterRect);
					canvas.drawText(name, x, y + nameHalfHeight, paintTextIcon);
				}
			}
		}
	}

	private void drawDirectionArrows(Canvas canvas, RotatedTileBox tileBox, List<SelectedGpxFile> selectedGPXFiles) {
		if (!tileBox.isZoomAnimated()) {
			QuadRect correctedQuadRect = getCorrectedQuadRect(tileBox.getLatLonBounds());
			for (SelectedGpxFile selectedGpxFile : selectedGPXFiles) {
				boolean showArrows = isShowArrowsForTrack(selectedGpxFile.getGpxFile());
				String coloringTypeName = getAvailableOrDefaultColoringType(selectedGpxFile);
				ColoringType coloringType = ColoringType.requireValueOf(ColoringPurpose.TRACK, coloringTypeName);

				if (!showArrows || coloringType.isRouteInfoAttribute()
						|| !QuadRect.trivialOverlap(correctedQuadRect, calculateTrackBounds(selectedGpxFile.getPointsToDisplay()))) {
					continue;
				}
				String width = getTrackWidthName(selectedGpxFile.getGpxFile(), defaultWidthPref.get());
				float trackWidth = getTrackWidth(width, defaultTrackWidth);
				int trackColor = getTrackColor(selectedGpxFile.getGpxFile(), cachedColor);
				List<TrkSegment> segments = coloringType.isGradient()
						? getCachedSegments(selectedGpxFile, coloringType.toGradientScaleType(), false)
						: selectedGpxFile.getPointsToDisplay();
				for (TrkSegment segment : segments) {
					if (segment.renderer instanceof RenderableSegment) {
						((RenderableSegment) segment.renderer)
								.drawGeometry(canvas, tileBox, correctedQuadRect, trackColor, trackWidth, null, true);
					}
				}
			}
		}
	}

	private void drawSelectedFilesStartEndPoints(Canvas canvas, RotatedTileBox tileBox, List<SelectedGpxFile> selectedGPXFiles) {
		if (tileBox.getZoom() >= START_ZOOM) {
			for (SelectedGpxFile selectedGpxFile : selectedGPXFiles) {
				if (isShowStartFinishForTrack(selectedGpxFile.getGpxFile())) {
					List<TrkSegment> segments = selectedGpxFile.getPointsToDisplay();
					for (TrkSegment segment : segments) {
						if (segment.points.size() >= 2) {
							WptPt start = segment.points.get(0);
							WptPt end = segment.points.get(segment.points.size() - 1);
							drawStartEndPoints(canvas, tileBox, start, selectedGpxFile.isShowCurrentTrack() ? null : end);
						}
					}
				}
			}
		}
	}

	private void drawStartEndPoints(@NonNull Canvas canvas, @NonNull RotatedTileBox tileBox, @Nullable WptPt start, @Nullable WptPt end) {
		int startX = start != null ? (int) tileBox.getPixXFromLatLon(start.lat, start.lon) : 0;
		int startY = start != null ? (int) tileBox.getPixYFromLatLon(start.lat, start.lon) : 0;
		int endX = end != null ? (int) tileBox.getPixXFromLatLon(end.lat, end.lon) : 0;
		int endY = end != null ? (int) tileBox.getPixYFromLatLon(end.lat, end.lon) : 0;

		int iconSize = AndroidUtils.dpToPx(getContext(), 14);
		QuadRect startRectWithoutShadow = calculateRect(startX, startY, iconSize, iconSize);
		QuadRect endRectWithoutShadow = calculateRect(endX, endY, iconSize, iconSize);

		if (start != null && end != null && QuadRect.intersects(startRectWithoutShadow, endRectWithoutShadow)) {
			QuadRect startAndFinishRect = calculateRect(startX, startY, startAndFinishIcon.getIntrinsicWidth(), startAndFinishIcon.getIntrinsicHeight());
			drawPoint(canvas, startAndFinishRect, startAndFinishIcon);
		} else {
			if (start != null) {
				QuadRect startRect = calculateRect(startX, startY, startPointIcon.getIntrinsicWidth(), startPointIcon.getIntrinsicHeight());
				drawPoint(canvas, startRect, startPointIcon);
			}
			if (end != null) {
				QuadRect endRect = calculateRect(endX, endY, finishPointIcon.getIntrinsicWidth(), finishPointIcon.getIntrinsicHeight());
				drawPoint(canvas, endRect, finishPointIcon);
			}
		}
	}

	private void drawPoint(@NonNull Canvas canvas, @NonNull QuadRect rect, @NonNull Drawable icon) {
		icon.setBounds((int) rect.left, (int) rect.top, (int) rect.right, (int) rect.bottom);
		icon.draw(canvas);
	}

	private void drawSelectedFilesPoints(@NonNull Canvas canvas, @NonNull RotatedTileBox tileBox,
	                                     @NonNull List<SelectedGpxFile> selectedGPXFiles) {
		if (tileBox.getZoom() >= START_ZOOM) {
			float textScale = getTextScale();
			float iconSize = getIconSize(app);
			QuadTree<QuadRect> boundIntersections = initBoundIntersections(tileBox);

			List<LatLon> fullObjectsLatLon = new ArrayList<>();
			List<LatLon> smallObjectsLatLon = new ArrayList<>();
			Map<WptPt, SelectedGpxFile> pointFileMap = new HashMap<>();
			// request to load
			QuadRect latLonBounds = tileBox.getLatLonBounds();
			for (SelectedGpxFile g : selectedGPXFiles) {
				List<Pair<WptPt, MapMarker>> fullObjects = new ArrayList<>();
				int fileColor = getFileColor(g);
				boolean synced = isSynced(g.getGpxFile());
				boolean selected = isGpxFileSelected(g.getGpxFile());
				for (WptPt wpt : getSelectedFilePoints(g)) {
					if (wpt.lat >= latLonBounds.bottom && wpt.lat <= latLonBounds.top
							&& wpt.lon >= latLonBounds.left && wpt.lon <= latLonBounds.right
							&& wpt != contextMenuLayer.getMoveableObject() && !isPointHidden(g, wpt)) {
						pointFileMap.put(wpt, g);
						MapMarker marker = null;
						if (synced) {
							marker = mapMarkersHelper.getMapMarker(wpt);
							if (marker == null || marker.history && !settings.KEEP_PASSED_MARKERS_ON_MAP.get()) {
								continue;
							}
						}
						pointsCache.add(wpt);
						float x = tileBox.getPixXFromLatLon(wpt.lat, wpt.lon);
						float y = tileBox.getPixYFromLatLon(wpt.lat, wpt.lon);

						if (intersects(boundIntersections, x, y, iconSize, iconSize)) {
							@ColorInt
							int color;
							if (selected) {
								if (marker != null && marker.history) {
									color = grayColor;
								} else {
									color = getPointColor(wpt, fileColor);
								}
							} else {
								color = disabledColor;
							}
							PointImageDrawable pointImageDrawable = PointImageDrawable.getFromWpt(getContext(), color,
									true, wpt);
							pointImageDrawable.drawSmallPoint(canvas, x, y, textScale);
							smallObjectsLatLon.add(new LatLon(wpt.lat, wpt.lon));
						} else {
							fullObjects.add(new Pair<>(wpt, marker));
							fullObjectsLatLon.add(new LatLon(wpt.lat, wpt.lon));
						}
					}
					if (wpt == contextMenuLayer.getMoveableObject()) {
						pointFileMap.put(wpt, g);
					}
				}
				for (Pair<WptPt, MapMarker> pair : fullObjects) {
					WptPt wpt = pair.first;
					float x = tileBox.getPixXFromLatLon(wpt.lat, wpt.lon);
					float y = tileBox.getPixYFromLatLon(wpt.lat, wpt.lon);
					int pointColor = selected ? getPointColor(wpt, fileColor) : disabledColor;
					drawBigPoint(canvas, wpt, pointColor, x, y, pair.second, textScale);
				}
			}
			if (trackChartPoints != null && trackChartPoints.getHighlightedPoint() != null) {
				LatLon highlightedPoint = trackChartPoints.getHighlightedPoint();
				chartPointsHelper.drawHighlightedPoint(highlightedPoint, canvas, tileBox);
			}
			this.fullObjectsLatLon = fullObjectsLatLon;
			this.smallObjectsLatLon = smallObjectsLatLon;
			this.pointFileMap = pointFileMap;
		}
	}

	private void drawSelectedFilesPointsOpenGl(@NonNull MapRendererView mapRenderer, @NonNull RotatedTileBox tileBox,
	                                           @NonNull List<SelectedGpxFile> selectedGPXFiles, boolean forceUpdate) {
		if (tileBox.getZoom() >= START_ZOOM) {
			if (trackChartPoints != null && trackChartPoints.getHighlightedPoint() != null) {
				LatLon highlightedPoint = trackChartPoints.getHighlightedPoint();
				if (!Algorithms.objectEquals(highlightedPointLocationCached, highlightedPoint)) {
					highlightedPointLocationCached = highlightedPoint;
					setHighlightedPointMarkerLocation(highlightedPoint);
					setHighlightedPointMarkerVisibility(true);
				}
			} else {
				setHighlightedPointMarkerVisibility(false);
			}

			int pointsCount = 0;
			int hiddenGroupsCount = 0;
			for (SelectedGpxFile g : selectedGPXFiles) {
				pointsCount += getSelectedFilePointsSize(g);
				hiddenGroupsCount += g.getHiddenGroupsCount();
			}
			boolean textVisible = isTextVisible();
			boolean changeMarkerPositionMode = contextMenuLayer.isInChangeMarkerPositionMode();
			if (!forceUpdate && pointCountCached == pointsCount && hiddenGroupsCountCached == hiddenGroupsCount
					&& textVisible == textVisibleCached
					&& changeMarkerPositionModeCached == changeMarkerPositionMode && !mapActivityInvalidated) {
				return;
			}
			pointCountCached = pointsCount;
			hiddenGroupsCountCached = hiddenGroupsCount;
			textVisibleCached = textVisible;
			changeMarkerPositionModeCached = changeMarkerPositionMode;
			clearPoints();

			pointsTileProvider = new WptPtTileProvider(getContext(), getPointsOrder() - 300,
					textVisible, getTextStyle(), view.getDensity());

			float textScale = getTextScale();
			Map<WptPt, SelectedGpxFile> pointFileMap = new HashMap<>();
			for (SelectedGpxFile g : selectedGPXFiles) {
				int fileColor = getFileColor(g);
				boolean synced = isSynced(g.getGpxFile());
				boolean selected = isGpxFileSelected(g.getGpxFile());
				for (WptPt wpt : getSelectedFilePoints(g)) {
					if (wpt != contextMenuLayer.getMoveableObject() && !isPointHidden(g, wpt)) {
						pointFileMap.put(wpt, g);
						MapMarker marker = null;
						if (synced) {
							marker = mapMarkersHelper.getMapMarker(wpt);
							if (marker == null || marker.history && !settings.KEEP_PASSED_MARKERS_ON_MAP.get()) {
								continue;
							}
						}
						boolean history = false;
						int color;
						if (selected) {
							if (marker != null && marker.history) {
								color = grayColor;
								history = true;
							} else {
								color = getPointColor(wpt, fileColor);
							}
						} else {
							color = disabledColor;
						}
						pointsTileProvider.addToData(wpt, color, true, marker != null, history, textScale);
					}
					if (wpt == contextMenuLayer.getMoveableObject()) {
						pointFileMap.put(wpt, g);
					}
				}
			}
			this.pointFileMap = pointFileMap;

			if (!pointsTileProvider.getPoints31().isEmpty()) {
				pointsTileProvider.drawSymbols(mapRenderer);
			}
		} else {
			highlightedPointLocationCached = null;
			setHighlightedPointMarkerVisibility(false);
			pointCountCached = 0;
			hiddenGroupsCountCached = 0;
			clearPoints();
		}
	}

	private void setHighlightedPointMarkerLocation(LatLon latLon) {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null && highlightedPointMarker != null) {
			highlightedPointMarker.setPosition(new PointI(MapUtils.get31TileNumberX(latLon.getLongitude()),
					MapUtils.get31TileNumberY(latLon.getLatitude())));
		}
	}

	private void setHighlightedPointMarkerVisibility(boolean visible) {
		if (highlightedPointMarker != null) {
			highlightedPointMarker.setIsHidden(!visible);
		}
	}

	private void clearPoints() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer == null || pointsTileProvider == null) {
			return;
		}
		pointsTileProvider.clearSymbols(mapRenderer);
		pointsTileProvider = null;
	}

	private boolean isSynced(@NonNull GPXFile gpxFile) {
		MapMarkersGroup markersGroup = mapMarkersHelper.getMarkersGroup(gpxFile);
		return markersGroup != null && !markersGroup.isDisabled();
	}

	private void drawXAxisPoints(@Nullable TrackChartPoints chartPoints, @NonNull Canvas canvas,
	                             @NonNull RotatedTileBox tileBox) {
		if (chartPoints != null) {
			List<LatLon> xAxisPoints = chartPoints.getXAxisPoints();
			if (!Algorithms.isEmpty(xAxisPoints)) {
				int pointColor = trackChartPoints.getSegmentColor();
				if (pointColor == 0) {
					pointColor = getTrackColor(trackChartPoints.getGpx(), cachedColor);
					trackChartPoints.setSegmentColor(pointColor);
				}
				chartPointsHelper.drawXAxisPoints(xAxisPoints, pointColor, canvas, tileBox);
			}
		}
	}

	private void drawXAxisPointsOpenGl(@Nullable TrackChartPoints chartPoints, @NonNull MapRendererView mapRenderer,
	                                   @NonNull RotatedTileBox tileBox) {
		if (chartPoints != null) {
			List<LatLon> xAxisPoints = chartPoints.getXAxisPoints();
			if (Algorithms.objectEquals(xAxisPointsCached, xAxisPoints) && trackChartPointsProvider != null && !mapActivityInvalidated) {
				return;
			}
			xAxisPointsCached = xAxisPoints;
			clearXAxisPoints();
			if (!Algorithms.isEmpty(xAxisPoints)) {
				int pointColor = trackChartPoints.getSegmentColor();
				if (pointColor == 0) {
					pointColor = getTrackColor(trackChartPoints.getGpx(), cachedColor);
				}
				Bitmap pointBitmap = chartPointsHelper.createXAxisPointBitmap(pointColor, tileBox.getDensity());
				trackChartPointsProvider = new LocationPointsTileProvider(getPointsOrder() - 500, xAxisPoints, pointBitmap);
				trackChartPointsProvider.drawPoints(mapRenderer);
			}
		} else {
			clearXAxisPoints();
		}
	}

	private void clearXAxisPoints() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null && trackChartPointsProvider != null) {
			trackChartPointsProvider.clearPoints(mapRenderer);
			trackChartPointsProvider = null;
		}
	}

	private void recreateHighlightedPointCollection() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null) {
			clearHighlightedPointCollection();

			highlightedPointCollection = new MapMarkersCollection();
			MapMarkerBuilder builder = new MapMarkerBuilder();
			builder.setBaseOrder(getPointsOrder() - 600);
			builder.setIsAccuracyCircleSupported(false);
			builder.setIsHidden(true);
			builder.setPinIcon(NativeUtilities.createSkImageFromBitmap(highlightedPointImage));
			highlightedPointMarker = builder.buildAndAddToCollection(highlightedPointCollection);
			mapRenderer.addSymbolsProvider(highlightedPointCollection);
		}
	}

	private void clearHighlightedPointCollection() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null && highlightedPointCollection != null) {
			mapRenderer.removeSymbolsProvider(highlightedPointCollection);
			highlightedPointCollection = null;
		}
	}

	private int getFileColor(@NonNull SelectedGpxFile g) {
		return g.getColor() == 0 ? defPointColor : g.getColor();
	}

	private void drawBigPoint(Canvas canvas, WptPt wpt, int pointColor, float x, float y, @Nullable MapMarker marker, float textScale) {
		PointImageDrawable pointImageDrawable;
		boolean history = false;
		if (marker != null) {
			pointImageDrawable = PointImageDrawable.getOrCreateSyncedIcon(getContext(), pointColor, wpt);
			history = marker.history;
		} else {
			pointImageDrawable = PointImageDrawable.getFromWpt(getContext(), pointColor, true, wpt);
		}
		pointImageDrawable.drawPoint(canvas, x, y, textScale, history);
	}

	@ColorInt
	private int getPointColor(WptPt o, @ColorInt int fileColor) {
		boolean visit = isPointVisited(o);
		return visit ? visitedColor : o.getColor(fileColor);
	}

	private void drawSelectedFilesSegments(Canvas canvas, RotatedTileBox tileBox,
	                                       List<SelectedGpxFile> selectedGPXFiles, DrawSettings settings) {
		SelectedGpxFile currentTrack = null;
		int baseOrder = getBaseOrder();
		for (SelectedGpxFile selectedGpxFile : selectedGPXFiles) {
			String width = getTrackWidthName(selectedGpxFile.getGpxFile(), defaultWidthPref.get());
			if (!cachedTrackWidth.containsKey(width)) {
				cachedTrackWidth.put(width, null);
			}
			if (selectedGpxFile.isShowCurrentTrack()) {
				currentTrack = selectedGpxFile;
			} else {
				drawSelectedFileSegments(selectedGpxFile, false, canvas, tileBox, settings, baseOrder);
			}
			String gpxPath = selectedGpxFile.getGpxFile().path;
			if (!renderedSegmentsCache.containsKey(gpxPath)) {
				renderedSegmentsCache.remove(gpxPath);
			}

			baseOrder -= GpxGeometryWay.VECTOR_LINES_RESERVED;
		}
		if (currentTrack != null) {
			drawSelectedFileSegments(currentTrack, true, canvas, tileBox, settings, baseOrder);
		}
	}

	private void drawSelectedFileSegments(SelectedGpxFile selectedGpxFile, boolean currentTrack,
	                                      Canvas canvas, RotatedTileBox tileBox, DrawSettings settings,
	                                      int baseOrder) {
		boolean hasMapRenderer = hasMapRenderer();
		GPXFile gpxFile = selectedGpxFile.getGpxFileToDisplay();
		String gpxFilePath = selectedGpxFile.getGpxFile().path;
		QuadRect correctedQuadRect = getCorrectedQuadRect(tileBox.getLatLonBounds());
		String coloringTypeName = getAvailableOrDefaultColoringType(selectedGpxFile);
		ColoringType coloringType = ColoringType.requireValueOf(ColoringPurpose.TRACK, coloringTypeName);
		String routeIndoAttribute = ColoringType.getRouteInfoAttribute(coloringTypeName);

		boolean visible = isGpxFileVisible(selectedGpxFile, tileBox);
		if (!gpxFile.hasTrkPt() && coloringType.isGradient() || !visible) {
			Set<TrkSegment> renderedSegments = renderedSegmentsCache.get(gpxFilePath);
			if (renderedSegments != null) {
				Iterator<TrkSegment> it = renderedSegments.iterator();
				while (it.hasNext()) {
					TrkSegment renderedSegment = it.next();
					resetSymbolProviders(renderedSegment);
					it.remove();
				}
			}
			segmentsCache.remove(gpxFilePath);
			return;
		}

		List<TrkSegment> segments = new ArrayList<>();
		if (coloringType.isTrackSolid() || coloringType.isRouteInfoAttribute()) {
			segments.addAll(selectedGpxFile.getPointsToDisplay());
		} else {
			segments.addAll(getCachedSegments(selectedGpxFile, coloringType.toGradientScaleType(), !hasMapRenderer));
		}

		Set<TrkSegment> renderedSegments = renderedSegmentsCache.get(gpxFilePath);
		if (renderedSegments != null) {
			Iterator<TrkSegment> it = renderedSegments.iterator();
			while (it.hasNext()) {
				TrkSegment renderedSegment = it.next();
				if (!segments.contains(renderedSegment)) {
					resetSymbolProviders(renderedSegment);
					it.remove();
				}
			}
		} else {
			renderedSegments = new HashSet<>();
			renderedSegmentsCache.put(gpxFilePath, renderedSegments);
		}
		String width = getTrackWidthName(gpxFile, defaultWidthPref.get());
		for (int segmentIdx = 0; segmentIdx < segments.size(); segmentIdx++) {
			TrkSegment ts = segments.get(segmentIdx);
			int color = getTrackColor(gpxFile, ts.getColor(cachedColor));
			boolean newTsRenderer = false;
			if (ts.renderer == null && !ts.points.isEmpty()) {
				RenderableSegment renderer;
				if (currentTrack) {
					renderer = new Renderable.CurrentTrack(ts.points);
				} else {
					renderer = new Renderable.StandardTrack(ts.points, 17.2);
				}
				renderer.setBorderPaint(borderPaint);
				ts.renderer = renderer;
				GpxGeometryWay geometryWay = new GpxGeometryWay(wayContext);
				geometryWay.baseOrder = baseOrder--;
				renderer.setGeometryWay(geometryWay);
				newTsRenderer = true;
			}
			boolean updated = updatePaints(color, width, selectedGpxFile.isRoutePoints(), currentTrack, settings, tileBox)
					|| mapActivityInvalidated || newTsRenderer || !renderedSegments.contains(ts);
			if (ts.renderer instanceof RenderableSegment) {
				RenderableSegment renderableSegment = (RenderableSegment) ts.renderer;
				updated |= renderableSegment.setTrackParams(color, width, coloringType, routeIndoAttribute);
				if (hasMapRenderer || coloringType.isRouteInfoAttribute()) {
					CachedTrack cachedTrack = getCachedTrack(selectedGpxFile);
					updated |= renderableSegment.setRoute(getCachedRouteSegments(cachedTrack, segmentIdx));
					updated |= renderableSegment.setDrawArrows(isShowArrowsForTrack(selectedGpxFile.getGpxFile()));
					if (updated || !hasMapRenderer) {
						float[] intervals = null;
						PathEffect pathEffect = paint.getPathEffect();
						if (pathEffect instanceof OsmandDashPathEffect) {
							intervals = ((OsmandDashPathEffect) pathEffect).getIntervals();
						}
						renderableSegment.drawGeometry(canvas, tileBox, correctedQuadRect,
								paint.getColor(), paint.getStrokeWidth(), intervals);
						renderedSegments.add(ts);
					}
				} else {
					renderableSegment.drawSegment(view.getZoom(), paint, canvas, tileBox);
				}
			}
		}
	}

	@NonNull
	private List<TrkSegment> getCachedSegments(@NonNull SelectedGpxFile selectedGpxFile,
	                                           @NonNull GradientScaleType scaleType,
	                                           boolean simplify) {
		CachedTrack cachedTrack = getCachedTrack(selectedGpxFile);
		return simplify
				? cachedTrack.getSimplifiedTrackSegments(view.getZoom(), scaleType)
				: cachedTrack.getTrackSegments(scaleType);
	}

	private float getTrackWidth(String width, float defaultTrackWidth) {
		Float trackWidth = cachedTrackWidth.get(width);
		return trackWidth != null ? trackWidth : defaultTrackWidth;
	}

	private int getTrackColor(GPXFile gpxFile, int defaultColor) {
		int color = 0;
		if (!isGpxFileSelected(gpxFile)) {
			color = ColorUtilities.getColorWithAlpha(disabledColor, 0.5f);
		} else if (hasTrackDrawInfoForTrack(gpxFile)) {
			color = trackDrawInfo.getColor();
		} else if (gpxFile.showCurrentTrack) {
			color = currentTrackColorPref.get();
		} else {
			GpxDataItem dataItem = gpxDbHelper.getItem(new File(gpxFile.path));
			if (dataItem != null) {
				color = dataItem.getParameter(COLOR);
			}
		}
		return color != 0 ? color : gpxFile.getColor(defaultColor);
	}

	private String getAvailableOrDefaultColoringType(SelectedGpxFile selectedGpxFile) {
		GPXFile gpxFile = selectedGpxFile.getGpxFileToDisplay();

		if (!isGpxFileSelected(gpxFile)) {
			return ColoringType.TRACK_SOLID.getName(null);
		}
		if (hasTrackDrawInfoForTrack(gpxFile)) {
			return trackDrawInfo.getColoringType().getName(trackDrawInfo.getRouteInfoAttribute());
		}

		GpxDataItem dataItem = null;
		String defaultColoringType = ColoringType.TRACK_SOLID.getName(null);
		ColoringType coloringType = null;
		String routeInfoAttribute = null;
		boolean isCurrentTrack = gpxFile.showCurrentTrack;

		if (isCurrentTrack) {
			coloringType = currentTrackColoringTypePref.get();
			routeInfoAttribute = currentTrackRouteInfoAttributePref.get();
		} else {
			dataItem = gpxDbHelper.getItem(new File(gpxFile.path));
			if (dataItem != null) {
				coloringType = ColoringType.requireValueOf(ColoringPurpose.TRACK, dataItem.getParameter(COLORING_TYPE));
				routeInfoAttribute = ColoringType.getRouteInfoAttribute(dataItem.getParameter(COLORING_TYPE));
			}
		}

		if (coloringType == null) {
			return defaultColoringType;
		} else if (!isAvailableInSubscription(app, new ColoringStyle(coloringType, routeInfoAttribute))) {
			return defaultColoringType;
		} else if (getCachedTrack(selectedGpxFile).isColoringTypeAvailable(coloringType, routeInfoAttribute)) {
			return coloringType.getName(routeInfoAttribute);
		} else {
			if (!isCurrentTrack) {
				dataItem.setParameter(COLORING_TYPE, defaultColoringType);
				gpxDbHelper.updateDataItem(dataItem);
			}
			return defaultColoringType;
		}
	}

	private boolean isGpxFileSelected(GPXFile gpxFile) {
		return customObjectsDelegate != null || GpxSelectionHelper.isGpxFileSelected(app, gpxFile);
	}

	private String getTrackWidthName(GPXFile gpxFile, String defaultWidth) {
		String width = null;
		if (hasTrackDrawInfoForTrack(gpxFile)) {
			width = trackDrawInfo.getWidth();
		} else if (gpxFile.showCurrentTrack) {
			width = currentTrackWidthPref.get();
		} else {
			GpxDataItem dataItem = gpxDbHelper.getItem(new File(gpxFile.path));
			if (dataItem != null) {
				width = dataItem.getParameter(WIDTH);
			}
		}
		return width != null ? width : gpxFile.getWidth(defaultWidth);
	}

	private boolean isShowArrowsForTrack(@NonNull GPXFile gpxFile) {
		if (!isGpxFileSelected(gpxFile)) {
			return false;
		} else if (hasTrackDrawInfoForTrack(gpxFile)) {
			return trackDrawInfo.isShowArrows();
		} else if (gpxFile.showCurrentTrack) {
			return currentTrackShowArrowsPref.get();
		} else {
			GpxDataItem dataItem = gpxDbHelper.getItem(new File(gpxFile.path));
			if (dataItem != null) {
				return dataItem.getParameter(SHOW_ARROWS);
			}
			return gpxFile.isShowArrows();
		}
	}

	private boolean isShowStartFinishForTrack(@NonNull GPXFile gpxFile) {
		if (!isGpxFileSelected(gpxFile)) {
			return false;
		} else if (hasTrackDrawInfoForTrack(gpxFile)) {
			return trackDrawInfo.isShowStartFinish();
		} else if (gpxFile.showCurrentTrack) {
			return currentTrackShowStartFinishPref.get();
		} else {
			GpxDataItem dataItem = gpxDbHelper.getItem(new File(gpxFile.path));
			if (dataItem != null) {
				return dataItem.getParameter(SHOW_START_FINISH);
			}
			return gpxFile.isShowStartFinish();
		}
	}

	private boolean hasTrackDrawInfoForTrack(@NonNull GPXFile gpxFile) {
		return trackDrawInfo != null && (trackDrawInfo.isCurrentRecording() && gpxFile.showCurrentTrack
				|| gpxFile.path.equals(trackDrawInfo.getFilePath()));
	}

	@NonNull
	private CachedTrack getCachedTrack(@NonNull SelectedGpxFile selectedGpxFile) {
		String path = selectedGpxFile.getGpxFile().path;
		CachedTrack cachedTrack = segmentsCache.get(path);
		if (cachedTrack == null) {
			cachedTrack = new CachedTrack(app, selectedGpxFile);
			segmentsCache.put(path, cachedTrack);
		}
		return cachedTrack;
	}

	@NonNull
	public List<RouteSegmentResult> getCachedRouteSegments(@NonNull CachedTrack cachedTrack, int nonEmptySegmentIdx) {
		List<RouteSegmentResult> routeSegments = cachedTrack.getCachedRouteSegments(nonEmptySegmentIdx);
		if (routeSegments == null) {
			loadRouteSegments(cachedTrack, nonEmptySegmentIdx);
		}
		return routeSegments != null ? routeSegments : new ArrayList<>();
	}

	private void loadRouteSegments(@NonNull CachedTrack cachedTrack, int nonEmptySegmentIdx) {
		CachedTrackParams trackParams = cachedTrack.getCachedTrackParams();
		GPXFile gpxFile = cachedTrack.getSelectedGpxFile().getGpxFileToDisplay();

		boolean parsingGpxRoute = isParsingGpxRoute(gpxFile);
		boolean paramsChanged = cachedTrackParamsChanged(gpxFile, trackParams);
		if (paramsChanged) {
			cancelGpxRouteParsing(gpxFile);
		}
		if (paramsChanged || !parsingGpxRoute) {
			ParseGpxRouteListener listener = (routeSegments, success) -> {
				if (success) {
					cachedTrack.setCachedRouteSegments(routeSegments, nonEmptySegmentIdx);
				}
				parseGpxRouteTasks.remove(gpxFile.path);
			};
			ParseGpxRouteTask task = new ParseGpxRouteTask(gpxFile, trackParams, nonEmptySegmentIdx, listener);
			parseGpxRouteTasks.put(gpxFile.path, task);
			task.executeOnExecutor(parseGpxRouteSingleThreadExecutor);
		}
	}

	private boolean cachedTrackParamsChanged(@NonNull GPXFile gpxFile, @NonNull CachedTrackParams params) {
		ParseGpxRouteTask task = parseGpxRouteTasks.get(gpxFile.path);
		if (task != null) {
			return !Algorithms.objectEquals(params, task.getCachedTrackParams());
		}
		return false;
	}

	public boolean isParsingGpxRoute(@NonNull GPXFile gpxFile) {
		return parseGpxRouteTasks.containsKey(gpxFile.path);
	}

	public void cancelGpxRouteParsing(@NonNull GPXFile gpxFile) {
		ParseGpxRouteTask task = parseGpxRouteTasks.get(gpxFile.path);
		if (task != null && task.getStatus() == AsyncTask.Status.RUNNING) {
			task.cancel(false);
		}
	}

	private boolean isPointVisited(WptPt o) {
		boolean visit = false;
		String visited = o.getExtensionsToRead().get("VISITED_KEY");
		if (visited != null && !visited.equals("0")) {
			visit = true;
		}
		return visit;
	}

	private List<WptPt> getSelectedFilePoints(@NonNull SelectedGpxFile g) {
		return g.getGpxFile().getPoints();
	}

	private int getSelectedFilePointsSize(@NonNull SelectedGpxFile g) {
		return g.getGpxFile().getPointsSize();
	}

	private boolean isPointHidden(SelectedGpxFile selectedGpxFile, WptPt point) {
		return selectedGpxFile.isGroupHidden(point.category);
	}

	public void getWptFromPoint(RotatedTileBox tb, PointF point, List<? super WptPt> res) {
		MapRendererView mapRenderer = getMapRenderer();
		float radius = getScaledTouchRadius(app, tb.getDefaultRadiusPoi()) * TOUCH_RADIUS_MULTIPLIER;
		List<PointI> touchPolygon31 = null;
		if (mapRenderer != null) {
			touchPolygon31 = NativeUtilities.getPolygon31FromPixelAndRadius(mapRenderer, point, radius);
			if (touchPolygon31 == null) {
				return;
			}
		}

		List<SelectedGpxFile> visibleGpxFiles = new ArrayList<>(selectedGpxHelper.getSelectedGPXFiles());
		for (SelectedGpxFile g : visibleGpxFiles) {
			List<WptPt> pts = getSelectedFilePoints(g);
			for (WptPt waypoint : pts) {
				if (isPointHidden(g, waypoint)) {
					continue;
				}

				boolean add = mapRenderer != null
						? NativeUtilities.isPointInsidePolygon(waypoint.lat, waypoint.lon, touchPolygon31)
						: tb.isLatLonNearPixel(waypoint.lat, waypoint.lon, point.x, point.y, radius);
				if (add) {
					res.add(waypoint);
				}
			}
		}
	}

	public void getTracksFromPoint(RotatedTileBox tb, PointF point, List<Object> res, boolean showTrackPointMenu) {
		List<SelectedGpxFile> selectedGpxFiles = new ArrayList<>(selectedGpxHelper.getSelectedGPXFiles());
		if (selectedGpxFiles.isEmpty()) {
			return;
		}

		MapRendererView mapRenderer = getMapRenderer();
		int radius = getScaledTouchRadius(app, tb.getDefaultRadiusPoi());
		List<PointI> touchPolygon31 = null;
		if (mapRenderer != null) {
			touchPolygon31 = NativeUtilities.getPolygon31FromPixelAndRadius(mapRenderer, point, radius);
			if (touchPolygon31 == null) {
				return;
			}
		}

		LatLon latLonFromPixel = null;

		for (SelectedGpxFile selectedGpxFile : selectedGpxFiles) {
			if (!isGpxFileVisible(selectedGpxFile, tb)) {
				continue;
			}

			Pair<WptPt, WptPt> line = null;
			for (TrkSegment segment : selectedGpxFile.getPointsToDisplay()) {
				line = mapRenderer != null
						? GpxUtils.findLineInPolygon31(touchPolygon31, segment.points)
						: GpxUtils.findLineNearPoint(tb, segment.points, radius, (int) point.x, (int) point.y);
				if (line != null) {
					break;
				}
			}
			if (line != null) {
				if (latLonFromPixel == null) {
					latLonFromPixel = NativeUtilities.getLatLonFromElevatedPixel(mapRenderer, tb, point.x, point.y);
				}
				res.add(GpxUtils.createSelectedGpxPoint(selectedGpxFile, line.first, line.second, latLonFromPixel,
						showTrackPointMenu));
			}
		}
	}

	@Override
	public PointDescription getObjectName(Object o) {
		if (o instanceof WptPt) {
			return new PointDescription(PointDescription.POINT_TYPE_WPT, ((WptPt) o).name);
		} else if (o instanceof SelectedGpxPoint) {
			SelectedGpxFile selectedGpxFile = ((SelectedGpxPoint) o).getSelectedGpxFile();
			GPXFile gpxFile = selectedGpxFile.getGpxFile();

			String name;
			if (selectedGpxFile.isShowCurrentTrack()) {
				name = getContext().getString(R.string.shared_string_currently_recording_track);
			} else if (!Algorithms.isEmpty(gpxFile.getArticleTitle())) {
				name = gpxFile.getArticleTitle();
			} else {
				name = GpxUiHelper.getGpxTitle(Algorithms.getFileWithoutDirs(gpxFile.path));
			}
			return new PointDescription(PointDescription.POINT_TYPE_GPX, name);
		}
		return null;
	}

	private void removeCachedUnselectedTracks(List<SelectedGpxFile> selectedGpxFiles) {
		List<String> selectedTracksPaths = new ArrayList<>();
		for (SelectedGpxFile gpx : selectedGpxFiles) {
			selectedTracksPaths.add(gpx.getGpxFile().path);
		}
		List<SelectedGpxFile> unselectedGpxFiles = new ArrayList<>();
		for (SelectedGpxFile gpx : visibleGPXFilesMap.keySet()) {
			if (!selectedTracksPaths.contains(gpx.getGpxFile().path)) {
				unselectedGpxFiles.add(gpx);
			}
		}
		for (SelectedGpxFile gpx : unselectedGpxFiles) {
			resetSymbolProviders(gpx.getPointsToDisplay());
		}
		Set<String> cachedTracksPaths = segmentsCache.keySet();
		for (Iterator<String> iterator = cachedTracksPaths.iterator(); iterator.hasNext(); ) {
			String cachedTrackPath = iterator.next();
			boolean trackHidden = !selectedTracksPaths.contains(cachedTrackPath);
			if (trackHidden) {
				if (hasMapRenderer()) {
					CachedTrack cachedTrack = segmentsCache.get(cachedTrackPath);
					if (cachedTrack != null) {
						resetSymbolProviders(cachedTrack.getAllNonSimplifiedCachedTrackSegments());
					}
				}
				iterator.remove();
			}
		}
	}

	private void resetSymbolProviders(@NonNull TrkSegment segment) {
		if (segment.renderer instanceof RenderableSegment) {
			RenderableSegment renderableSegment = (RenderableSegment) segment.renderer;
			GpxGeometryWay geometryWay = renderableSegment.getGeometryWay();
			if (geometryWay != null && geometryWay.hasMapRenderer()) {
				geometryWay.resetSymbolProviders();
			}
		}
	}

	private void resetSymbolProviders(@NonNull List<TrkSegment> segments) {
		for (TrkSegment segment : segments) {
			resetSymbolProviders(segment);
		}
	}

	private boolean isGpxFileVisible(@NonNull SelectedGpxFile selectedGpxFile, @NonNull RotatedTileBox tileBox) {
		MapRendererView mapRenderer = getMapRenderer();
		QuadRect gpxFileBounds = selectedGpxFile.getBoundsToDisplay();
		if (mapRenderer != null) {
			return mapRenderer.isAreaVisible(selectedGpxFile.getAreaToDisplay());
		} else {
			return QuadRect.trivialOverlap(tileBox.getLatLonBounds(), gpxFileBounds);
		}
	}

	@Override
	public boolean disableSingleTap() {
		return isInTrackAppearanceMode();
	}

	@Override
	public boolean disableLongPressOnMap(PointF point, RotatedTileBox tileBox) {
		if (isInTrackAppearanceMode()) {
			return true;
		}
		if (tileBox.getZoom() >= START_ZOOM) {
			List<Object> res = new ArrayList<>();
			getTracksFromPoint(tileBox, point, res, false);
			return !Algorithms.isEmpty(res);
		}
		return false;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> res,
	                                    boolean unknownLocation, boolean excludeUntouchableObjects) {
		if (tileBox.getZoom() >= START_ZOOM) {
			getWptFromPoint(tileBox, point, res);
			if (!excludeUntouchableObjects) {
				getTracksFromPoint(tileBox, point, res, false);
			}
		}
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if (o instanceof WptPt) {
			return new LatLon(((WptPt) o).lat, ((WptPt) o).lon);
		} else if (o instanceof SelectedGpxPoint) {
			WptPt point = ((SelectedGpxPoint) o).getSelectedPoint();
			return new LatLon(point.lat, point.lon);
		}
		return null;
	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}

	@Override
	public boolean onLongPressEvent(@NonNull PointF point, @NonNull RotatedTileBox tileBox) {
		if (tileBox.getZoom() >= START_ZOOM) {
			List<Object> trackPoints = new ArrayList<>();
			getTracksFromPoint(tileBox, point, trackPoints, true);

			if (!Algorithms.isEmpty(trackPoints)) {
				LatLon latLon = NativeUtilities.getLatLonFromElevatedPixel(getMapRenderer(), tileBox, point);
				if (trackPoints.size() == 1) {
					SelectedGpxPoint gpxPoint = (SelectedGpxPoint) trackPoints.get(0);
					contextMenuLayer.showContextMenu(latLon, getObjectName(gpxPoint), gpxPoint, null);
				} else if (trackPoints.size() > 1) {
					Map<Object, IContextMenuProvider> selectedObjects = new HashMap<>();
					for (Object object : trackPoints) {
						selectedObjects.put(object, this);
					}
					contextMenuLayer.showContextMenuForSelectedObjects(latLon, selectedObjects);
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean showMenuAction(@Nullable Object object) {
		MapActivity mapActivity = view.getMapActivity();
		if (mapActivity != null) {
			if (object instanceof SelectedGpxPoint) {
				SelectedGpxPoint selectedGpxPoint = (SelectedGpxPoint) object;
				if (selectedGpxPoint.shouldShowTrackPointMenu()) {
					WptPt selectedWptPt = selectedGpxPoint.getSelectedPoint();
					LatLon latLon = new LatLon(selectedWptPt.lat, selectedWptPt.lon);
					contextMenuLayer.showContextMenu(latLon, getObjectName(selectedGpxPoint), selectedGpxPoint, null);
				} else {
					SelectedGpxFile selectedGpxFile = selectedGpxPoint.getSelectedGpxFile();
					TrackMenuFragment.showInstance(mapActivity, selectedGpxFile, selectedGpxPoint);
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public LatLon getTextLocation(WptPt o) {
		return new LatLon(o.lat, o.lon);
	}

	@Override
	public int getTextShift(WptPt o, RotatedTileBox rb) {
		return (int) (16 * rb.getDensity() * getTextScale());
	}

	@Override
	public String getText(WptPt o) {
		return o.name;
	}

	@Override
	public boolean isTextVisible() {
		return settings.SHOW_POI_LABEL.get();
	}

	@Override
	public boolean isFakeBoldText() {
		return false;
	}


	@Override
	public boolean isObjectMovable(Object o) {
		return o instanceof WptPt;
	}

	@Override
	public void applyNewObjectPosition(@NonNull Object o, @NonNull LatLon position,
	                                   @Nullable ApplyMovedObjectCallback callback) {
		if (o instanceof WptPt) {
			WptPt objectInMotion = (WptPt) o;
			SelectedGpxFile selectedGpxFile = pointFileMap.get(objectInMotion);
			if (selectedGpxFile != null) {
				GPXFile gpxFile = selectedGpxFile.getGpxFile();
				WptPt wptInfo = new WptPt(position.getLatitude(), position.getLongitude(), objectInMotion.desc, objectInMotion.name, objectInMotion.category,
						Algorithms.colorToString(objectInMotion.getColor()), objectInMotion.getIconName(), objectInMotion.getBackgroundType());
				gpxFile.updateWptPt(objectInMotion, wptInfo);
				syncGpx(gpxFile);
				if (gpxFile.showCurrentTrack) {
					if (callback != null) {
						callback.onApplyMovedObject(true, objectInMotion);
					}
				} else {
					SaveGpxHelper.saveGpx(new File(gpxFile.path), gpxFile, errorMessage -> {
						if (callback != null) {
							callback.onApplyMovedObject(errorMessage == null, objectInMotion);
						}
					});
				}
			}
		} else if (callback != null) {
			callback.onApplyMovedObject(false, o);
		}
	}

	@Override
	protected void cleanupResources() {
		super.cleanupResources();
		clearSelectedFilesSegments();
		clearXAxisPoints();
		clearPoints();
		clearSelectedFilesSplits();
	}

	private void clearSelectedFilesSegments() {
		segmentsCache.clear();
		if (hasMapRenderer()) {
			for (Set<TrkSegment> segments : renderedSegmentsCache.values()) {
				for (TrkSegment segment : segments) {
					resetSymbolProviders(segment);
				}
			}
		}
	}

	private void syncGpx(GPXFile gpxFile) {
		MapMarkersGroup group = app.getMapMarkersHelper().getMarkersGroup(gpxFile);
		if (group != null) {
			mapMarkersHelper.runSynchronization(group);
		}
	}

	public void setCustomMapObjects(List<SelectedGpxFile> gpxFiles) {
		if (customObjectsDelegate != null) {
			customObjectsDelegate.setCustomMapObjects(gpxFiles);
			getApplication().getOsmandMap().refreshMap();
		}
	}
}