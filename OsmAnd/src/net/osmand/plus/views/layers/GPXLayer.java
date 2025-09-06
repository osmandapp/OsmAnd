package net.osmand.plus.views.layers;

import static net.osmand.plus.configmap.ConfigureMapMenu.CURRENT_TRACK_COLOR_ATTR;
import static net.osmand.plus.configmap.ConfigureMapMenu.CURRENT_TRACK_WIDTH_ATTR;
import static net.osmand.plus.routing.ColoringStyleAlgorithms.isAvailableInSubscription;
import static net.osmand.plus.track.Gpx3DVisualizationType.FIXED_HEIGHT;
import static net.osmand.shared.gpx.GpxParameter.COLORING_TYPE;

import android.content.Context;
import android.graphics.*;
import android.graphics.Paint.Align;
import android.graphics.PointF;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Pair;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import net.osmand.PlatformUtil;
import net.osmand.core.android.MapRendererContext;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.*;
import net.osmand.core.jni.GpxAdditionalIconsProvider.SplitLabel;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.ChartPointsHelper;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.card.color.ColoringStyle;
import net.osmand.plus.card.color.palette.gradient.PaletteGradientColor;
import net.osmand.plus.charts.TrackChartPoints;
import net.osmand.plus.mapcontextmenu.controllers.SelectedGpxMenuController.SelectedGpxPoint;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.render.OsmandDashPathEffect;
import net.osmand.plus.render.OsmandRenderer;
import net.osmand.plus.render.OsmandRenderer.RenderingContext;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.plus.track.CachedTrack;
import net.osmand.plus.track.CachedTrackParams;
import net.osmand.plus.track.Gpx3DLinePositionType;
import net.osmand.plus.track.Gpx3DVisualizationType;
import net.osmand.plus.track.Track3DStyle;
import net.osmand.plus.track.TrackDrawInfo;
import net.osmand.plus.track.fragments.GpsFilterFragment;
import net.osmand.plus.track.fragments.TrackAppearanceFragment;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.track.helpers.*;
import net.osmand.plus.track.helpers.ParseGpxRouteTask.ParseGpxRouteListener;
import net.osmand.plus.track.helpers.save.SaveGpxHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.PointImageDrawable;
import net.osmand.plus.views.PointImageUtils;
import net.osmand.plus.views.Renderable.CurrentTrack;
import net.osmand.plus.views.Renderable.RenderableSegment;
import net.osmand.plus.views.Renderable.StandardTrack;
import net.osmand.plus.views.corenative.NativeCoreContext;
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
import net.osmand.shared.data.KQuadRect;
import net.osmand.shared.gpx.GpxDbHelper;
import net.osmand.shared.gpx.*;
import net.osmand.shared.gpx.primitives.TrkSegment;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.shared.io.KFile;
import net.osmand.shared.routing.ColoringType;
import net.osmand.shared.routing.Gpx3DWallColorType;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GPXLayer extends OsmandMapLayer implements IContextMenuProvider, IMoveObjectProvider, MapTextProvider<WptPt> {

	private static final Log log = PlatformUtil.getLog(GPXLayer.class);

	private static final int DEFAULT_WIDTH_MULTIPLIER = 7;
	private static final int START_ZOOM = 7;
	private static final int MAX_SUPPORTED_TRACK_WIDTH_DP = 48;

	private Paint paint;
	private Paint borderPaint;
	private Paint shadowPaint;

	private int cachedHash;
	@ColorInt
	private int cachedColor;
	private float defaultTrackWidth;
	private final Map<String, Float> cachedTrackWidth = new HashMap<>();
	private final Map<String, Gpx3DVisualizationType> cachedTracksWith3dVisualization = new HashMap<>();
	private final Map<String, Gpx3DLinePositionType> cachedTracksWith3dLinePosition = new HashMap<>();
	private final Map<String, Float> cachedTracksVerticalExaggeration = new HashMap<>();
	private final Map<String, Float> cachedTracksElevation = new HashMap<>();
	private final Map<String, Integer> cachedTracksColors = new HashMap<>();

	private Drawable startPointIcon;
	private Drawable finishPointIcon;
	private Drawable startAndFinishIcon;
	private Bitmap startPointImage;
	private Bitmap finishPointImage;
	private Bitmap startAndFinishImage;
	private Bitmap highlightedPointImage;
	private float textScale = 1f;
	private boolean nightMode;
	private boolean changeMarkerPositionModeCached;

	private ChartPointsHelper chartPointsHelper;
	private GpxAppearanceHelper gpxAppearanceHelper;
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
	private List<GpxAdditionalIconsProvider> additionalIconsProviders = new ArrayList<>();
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
	@ColorInt
	private int altitudeAscColor;
	@ColorInt
	private int altitudeDescColor;

	private CommonPreference<String> defaultColorPref;
	private CommonPreference<String> defaultWidthPref;

	public GPXLayer(@NonNull Context ctx) {
		super(ctx);
	}

	@Override
	public void initLayer(@NonNull OsmandMapTileView view) {
		super.initLayer(view);

		app = view.getApplication();
		settings = app.getSettings();
		gpxDbHelper = app.getGpxDbHelper();
		mapMarkersHelper = app.getMapMarkersHelper();
		selectedGpxHelper = app.getSelectedGpxHelper();
		osmandRenderer = app.getResourceManager().getRenderer().getRenderer();
		chartPointsHelper = new ChartPointsHelper(getContext());
		gpxAppearanceHelper = new GpxAppearanceHelper(app);

		defaultColorPref = settings.getCustomRenderProperty(CURRENT_TRACK_COLOR_ATTR).cache();
		defaultWidthPref = settings.getCustomRenderProperty(CURRENT_TRACK_WIDTH_ATTR).cache();

		initUI();
	}

	public void setTrackChartPoints(@Nullable TrackChartPoints trackChartPoints) {
		this.trackChartPoints = trackChartPoints;
	}

	public int getSegmentsCacheHash() {
		return segmentsCache.hashCode();
	}

	public boolean isInTrackAppearanceMode() {
		return gpxAppearanceHelper.isInTrackAppearanceMode();
	}

	public void setTrackDrawInfo(@Nullable TrackDrawInfo trackDrawInfo) {
		gpxAppearanceHelper.setTrackDrawInfo(trackDrawInfo);
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
		altitudeAscColor = ContextCompat.getColor(app, R.color.gpx_altitude_asc);
		altitudeDescColor = ContextCompat.getColor(app, R.color.gpx_altitude_desc);

		wayContext = new GpxGeometryWayContext(getContext(), view.getDensity());
	}

	@Override
	protected void updateResources() {
		super.updateResources();
		initUI();
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
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
			if (mapRendererChanged) {
				clearSelectedFilesSegments();
			}
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
		setInvalidated(false);
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
				CachedTrack cachedTrack = segmentsCache.remove(selectedGpxFile.getGpxFile().getPath());
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
		if (rrs == null) {
			return false;
		}
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
				cachedTrackWidth.replaceAll((k, v) -> defaultTrackWidth);
			}
		}
		paint.setColor(color == 0 ? cachedColor : color);
		paint.setStrokeWidth(getTrackWidth(width, defaultTrackWidth));
		borderPaint.setStrokeWidth(paint.getStrokeWidth() + AndroidUtils.dpToPx(getContext(), 2));
		return hashChanged;
	}

	private void acquireTrackWidth(@NonNull String widthKey, @NonNull RenderingRulesStorage rrs,
			@NonNull RenderingRuleSearchRequest req, @NonNull RenderingContext rc) {
		if (!Algorithms.isEmpty(widthKey) && Algorithms.isInt(widthKey)) {
			try {
				int widthDp = Math.min(Integer.parseInt(widthKey), MAX_SUPPORTED_TRACK_WIDTH_DP);
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

	private boolean isHeightmapsActive() {
		MapRendererContext mapRendererContext = NativeCoreContext.getMapRendererContext();
		if (mapRendererContext != null) {
			return mapRendererContext.isHeightmapsActive();
		}
		return false;
	}

	private void drawSelectedFilesSplits(@NonNull Canvas canvas, @NonNull RotatedTileBox tileBox,
	                                     @NonNull List<SelectedGpxFile> selectedGPXFiles) {
		if (tileBox.getZoom() >= START_ZOOM) {
			// request to load
			for (SelectedGpxFile selectedGpxFile : selectedGPXFiles) {
				List<GpxDisplayGroup> groups = selectedGpxFile.getSplitGroups(app);
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
			boolean heightmapsActive = isHeightmapsActive();
			int startFinishPointsCount = 0;
			int splitLabelsCount = 0;
			for (SelectedGpxFile selectedGpxFile : selectedGPXFiles) {
				GpxFile gpxFile = selectedGpxFile.getGpxFile();
				String gpxPath = gpxFile.getPath();
				Gpx3DLinePositionType trackLinePosition = getTrackLinePositionType(gpxFile);
				Gpx3DLinePositionType cachedTrackLinePositionType = cachedTracksWith3dLinePosition.get(gpxPath);
				Gpx3DVisualizationType trackVisualizationType = getTrackVisualizationType(gpxFile);
				Gpx3DVisualizationType cachedTrackVisualizationType = cachedTracksWith3dVisualization.get(gpxPath);
				float elevationMeters = getElevationMeters(gpxFile);
				float trackVerticalExaggeration = getTrackExaggeration(gpxFile);
				Float cachedTrackElevationMeters = cachedTracksElevation.get(gpxPath);
				Float cachedTrackVerticalExaggeration = cachedTracksVerticalExaggeration.get(gpxPath);
				int trackColor = getTrackColor(gpxFile, cachedColor);
				if (!Algorithms.objectEquals(trackColor, cachedTracksColors.get(gpxPath))) {
					cachedTracksColors.put(gpxPath, trackColor);
					changed = true;
				}
				if (cachedTrackVisualizationType != trackVisualizationType
						|| cachedTrackElevationMeters == null || elevationMeters != cachedTrackElevationMeters
						|| cachedTrackVerticalExaggeration == null || trackVerticalExaggeration != cachedTrackVerticalExaggeration
						|| trackLinePosition != cachedTrackLinePositionType) {
					cachedTracksWith3dVisualization.put(gpxPath, trackVisualizationType);
					cachedTracksVerticalExaggeration.put(gpxPath, trackVerticalExaggeration);
					cachedTracksWith3dLinePosition.put(gpxPath, trackLinePosition);
					cachedTracksElevation.put(gpxPath, elevationMeters);
					changed = true;
				}
				if (isShowStartFinishForTrack(selectedGpxFile.getGpxFile())) {
					List<TrkSegment> segments = selectedGpxFile.getPointsToDisplay();
					for (TrkSegment segment : segments) {
						if (segment.getPoints().size() >= 2) {
							startFinishPointsCount += 2;
						}
					}
				}
				List<GpxDisplayGroup> groups = selectedGpxFile.getSplitGroups(app);
				if (!Algorithms.isEmpty(groups)) {
					List<GpxDisplayItem> items = groups.get(0).getDisplayItems();
					for (GpxDisplayItem item : items) {
						if (item.getLabelName(app) != null) {
							splitLabelsCount++;
						}
					}
				}
			}
			changed |= startFinishPointsCount != startFinishPointsCountCached;
			changed |= splitLabelsCount != splitLabelsCountCached;
			if (!changed && !mapActivityInvalidated && !invalidated) {
				return;
			}
			startFinishPointsCountCached = startFinishPointsCount;
			splitLabelsCountCached = splitLabelsCount;
			clearSelectedFilesSplits();

			QListFloat startFinishHeights = new QListFloat();
			for (SelectedGpxFile selectedGpxFile : selectedGPXFiles) {
				QListPointI startFinishPoints = new QListPointI();
				SplitLabelList splitLabels = new SplitLabelList();

				GpxFile gpxFile = selectedGpxFile.getGpxFile();
				Track3DStyle track3DStyle = getTrack3DStyle(gpxFile);
				Gpx3DLinePositionType trackLinePosition = track3DStyle.getLinePositionType();
				Gpx3DVisualizationType visualizationType = track3DStyle.getVisualizationType();

				if (isShowStartFinishForTrack(gpxFile)) {
					List<TrkSegment> segments = selectedGpxFile.getPointsToDisplay();
					for (TrkSegment segment : segments) {
						if (segment.getPoints().size() >= 2) {
							WptPt start = segment.getPoints().get(0);
							WptPt finish = segment.getPoints().get(segment.getPoints().size() - 1);
							if (visualizationType != Gpx3DVisualizationType.NONE && trackLinePosition == Gpx3DLinePositionType.TOP) {
								startFinishHeights.add((float) Gpx3DVisualizationType.getPointElevation(start, track3DStyle, heightmapsActive));
								startFinishHeights.add((float) Gpx3DVisualizationType.getPointElevation(finish, track3DStyle, heightmapsActive));
							}
							startFinishPoints.add(new PointI(Utilities.get31TileNumberX(start.getLon()), Utilities.get31TileNumberY(start.getLat())));
							startFinishPoints.add(new PointI(Utilities.get31TileNumberX(finish.getLon()), Utilities.get31TileNumberY(finish.getLat())));
						}
					}
				}
				List<GpxDisplayGroup> groups = selectedGpxFile.getSplitGroups(app);
				if (!Algorithms.isEmpty(groups)) {
					int trackColor = getTrackColor(gpxFile, cachedColor);
					List<GpxDisplayItem> items = groups.get(0).getDisplayItems();
					for (GpxDisplayItem item : items) {
						WptPt point = item.getLabelPoint();
						String name = item.getLabelName(app);
						int color = item.getLabelColor(trackColor, altitudeAscColor, altitudeDescColor);

						if (name != null) {
							SplitLabel splitLabel;
							PointI point31 = new PointI(Utilities.get31TileNumberX(point.getLon()), Utilities.get31TileNumberY(point.getLat()));
							if (visualizationType == Gpx3DVisualizationType.NONE || trackLinePosition != Gpx3DLinePositionType.TOP) {
								splitLabel = new SplitLabel(point31, name, NativeUtilities.createColorARGB(color, 179));
							} else {
								float labelHeight = (float) Gpx3DVisualizationType.getPointElevation(point, track3DStyle, heightmapsActive);
								splitLabel = new SplitLabel(point31, name, NativeUtilities.createColorARGB(color, 179), labelHeight);
							}
							splitLabels.add(splitLabel);
						}
					}
				}
				if (!startFinishPoints.isEmpty() || !splitLabels.isEmpty()) {
					GpxAdditionalIconsProvider additionalIconsProvider = new GpxAdditionalIconsProvider(getPointsOrder() - selectedGPXFiles.size() - 101, tileBox.getDensity(),
							startFinishPoints, splitLabels,
							NativeUtilities.createSkImageFromBitmap(startPointImage),
							NativeUtilities.createSkImageFromBitmap(finishPointImage),
							NativeUtilities.createSkImageFromBitmap(startAndFinishImage),
							startFinishHeights,
							track3DStyle.getExaggeration());
					mapRenderer.addSymbolsProvider(additionalIconsProvider);
					additionalIconsProviders.add(additionalIconsProvider);
				}
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
		if (mapRenderer != null && !Algorithms.isEmpty(additionalIconsProviders)) {
			List<GpxAdditionalIconsProvider> oldProviders = additionalIconsProviders;
			additionalIconsProviders = new ArrayList<>();
			for (GpxAdditionalIconsProvider provider : oldProviders) {
				mapRenderer.removeSymbolsProvider(provider);
			}
		}
	}

	@Nullable
	@Override
	protected Bitmap getScaledBitmap(int drawableId) {
		return app.getUIUtilities().getScaledBitmap(getMapActivity(), drawableId, textScale);
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
			WptPt point = i.getLabelPoint();
			if (point != null && point.getLat() >= latLonBounds.bottom && point.getLat() <= latLonBounds.top
					&& point.getLon() >= latLonBounds.left && point.getLon() <= latLonBounds.right) {
				float x = tileBox.getPixXFromLatLon(point.getLat(), point.getLon());
				float y = tileBox.getPixYFromLatLon(point.getLat(), point.getLon());
				if (px != -1 || py != -1) {
					if (Math.abs(x - px) <= dr && Math.abs(y - py) <= dr) {
						continue;
					}
				}
				px = x;
				py = y;
				String name = i.getLabelName(app);
				if (name != null) {
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
			KQuadRect kCorrectedQuadRect = SharedUtil.kQuadRect(correctedQuadRect);
			for (SelectedGpxFile selectedGpxFile : selectedGPXFiles) {
				GpxFile gpxFile = selectedGpxFile.getGpxFile();
				boolean showArrows = isShowArrowsForTrack(gpxFile);
				String coloringTypeName = getAvailableOrDefaultColoringType(selectedGpxFile);
				ColoringType coloringType = ColoringType.Companion.requireValueOf(ColoringPurpose.TRACK, coloringTypeName);
				String gradientColorPalette = getTrackGradientPalette(selectedGpxFile.getGpxFile());

				if (!showArrows || coloringType.isRouteInfoAttribute()
						|| !KQuadRect.Companion.trivialOverlap(kCorrectedQuadRect,
						GpxUtilities.INSTANCE.calculateTrackBounds(selectedGpxFile.getPointsToDisplay()))) {
					continue;
				}
				String width = gpxAppearanceHelper.getTrackWidth(gpxFile, defaultWidthPref.get());
				float trackWidth = getTrackWidth(width, defaultTrackWidth);
				int trackColor = getTrackColor(gpxFile, cachedColor);
				GradientScaleType scaleType = coloringType.toGradientScaleType();

				List<TrkSegment> segments = scaleType != null
						? getCachedSegments(selectedGpxFile, scaleType, gradientColorPalette, false)
						: selectedGpxFile.getPointsToDisplay();
				for (TrkSegment segment : segments) {
					if (segment.getRenderer() instanceof RenderableSegment) {
						((RenderableSegment) segment.getRenderer()).drawGeometry(canvas, tileBox, correctedQuadRect,
								trackColor, trackWidth, null, true, null, invalidated);
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
						if (segment.getPoints().size() >= 2) {
							WptPt start = segment.getPoints().get(0);
							WptPt end = segment.getPoints().get(segment.getPoints().size() - 1);
							drawStartEndPoints(canvas, tileBox, start, selectedGpxFile.isShowCurrentTrack() ? null : end);
						}
					}
				}
			}
		}
	}

	private void drawStartEndPoints(@NonNull Canvas canvas, @NonNull RotatedTileBox tileBox, @Nullable WptPt start, @Nullable WptPt end) {
		int startX = start != null ? (int) tileBox.getPixXFromLatLon(start.getLat(), start.getLon()) : 0;
		int startY = start != null ? (int) tileBox.getPixYFromLatLon(start.getLat(), start.getLon()) : 0;
		int endX = end != null ? (int) tileBox.getPixXFromLatLon(end.getLat(), end.getLon()) : 0;
		int endY = end != null ? (int) tileBox.getPixYFromLatLon(end.getLat(), end.getLon()) : 0;

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
					if (wpt.getLat() >= latLonBounds.bottom && wpt.getLat() <= latLonBounds.top
							&& wpt.getLon() >= latLonBounds.left && wpt.getLon() <= latLonBounds.right
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
						float x = tileBox.getPixXFromLatLon(wpt.getLat(), wpt.getLon());
						float y = tileBox.getPixYFromLatLon(wpt.getLat(), wpt.getLon());

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
							PointImageDrawable pointImageDrawable = PointImageUtils.getFromPoint(getContext(), color,
									true, wpt);
							pointImageDrawable.drawSmallPoint(canvas, x, y, textScale);
							smallObjectsLatLon.add(new LatLon(wpt.getLat(), wpt.getLon()));
						} else {
							fullObjects.add(new Pair<>(wpt, marker));
							fullObjectsLatLon.add(new LatLon(wpt.getLat(), wpt.getLon()));
						}
					}
					if (wpt == contextMenuLayer.getMoveableObject()) {
						pointFileMap.put(wpt, g);
					}
				}
				for (Pair<WptPt, MapMarker> pair : fullObjects) {
					WptPt wpt = pair.first;
					float x = tileBox.getPixXFromLatLon(wpt.getLat(), wpt.getLon());
					float y = tileBox.getPixYFromLatLon(wpt.getLat(), wpt.getLon());
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
			if (!forceUpdate && pointCountCached == pointsCount
					&& hiddenGroupsCountCached == hiddenGroupsCount
					&& textVisible == textVisibleCached
					&& changeMarkerPositionModeCached == changeMarkerPositionMode
					&& !mapActivityInvalidated && !invalidated) {
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

	private boolean isSynced(@NonNull GpxFile gpxFile) {
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
			if (Algorithms.objectEquals(xAxisPointsCached, xAxisPoints)
					&& trackChartPointsProvider != null && !mapActivityInvalidated && !invalidated) {
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

	private void drawBigPoint(@NonNull Canvas canvas, @Nullable WptPt wpt, int pointColor,
	                          float x, float y, @Nullable MapMarker marker, float textScale) {
		PointImageDrawable drawable = createWaypointIcon(pointColor, wpt, marker);
		boolean history = marker != null && marker.history;
		drawable.drawPoint(canvas, x, y, textScale, history);
	}

	@NonNull
	public PointImageDrawable createWaypointIcon(@ColorInt int pointColor, @Nullable WptPt wpt,
	                                             @Nullable MapMarker marker) {
		return createWaypointIcon(pointColor, wpt, marker != null);
	}

	@NonNull
	public PointImageDrawable createWaypointIcon(@ColorInt int pointColor, @Nullable WptPt wpt, boolean synced) {
		return PointImageUtils.getFromPoint(getContext(), pointColor, true, synced, wpt);
	}

	@NonNull
	public PointImageDrawable createWaypointIcon(@ColorInt int pointColor, boolean synced,
	                                             @NonNull String iconName, @Nullable String bgTypeName) {
		return PointImageUtils.getFromPoint(getContext(), pointColor, true, synced, iconName, bgTypeName);
	}

	@ColorInt
	private int getPointColor(@NonNull WptPt o, @ColorInt int fileColor) {
		boolean visit = isPointVisited(o);
		return visit ? visitedColor : Objects.requireNonNull(o.getColor(fileColor));
	}

	private void drawSelectedFilesSegments(Canvas canvas, RotatedTileBox tileBox,
	                                       List<SelectedGpxFile> selectedGPXFiles, DrawSettings settings) {
		SelectedGpxFile currentTrack = null;
		int baseOrder = getBaseOrder();
		for (SelectedGpxFile selectedGpxFile : selectedGPXFiles) {
			GpxFile gpxFile = selectedGpxFile.getGpxFile();
			String width = gpxAppearanceHelper.getTrackWidth(gpxFile, defaultWidthPref.get());
			cachedTrackWidth.putIfAbsent(width, null);
			if (selectedGpxFile.isShowCurrentTrack()) {
				currentTrack = selectedGpxFile;
			} else {
				drawSelectedFileSegments(selectedGpxFile, false, canvas, tileBox, settings, baseOrder);
			}
			if (!renderedSegmentsCache.containsKey(gpxFile.getPath())) {
				renderedSegmentsCache.remove(gpxFile.getPath());
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
		GpxFile gpxFile = selectedGpxFile.getGpxFileToDisplay();
		String gpxFilePath = gpxFile.getPath();
		QuadRect correctedQuadRect = getCorrectedQuadRect(tileBox.getLatLonBounds());
		String coloringTypeName = getAvailableOrDefaultColoringType(selectedGpxFile);
		ColoringType coloringType = ColoringType.Companion.requireValueOf(ColoringPurpose.TRACK, coloringTypeName);
		String colorPalette = getTrackGradientPalette(selectedGpxFile.getGpxFile());
		String routeIndoAttribute = ColoringType.Companion.getRouteInfoAttribute(coloringTypeName);

		Track3DStyle track3DStyle = getTrack3DStyle(gpxFile);
		ColoringType outlineColoringType = ColoringType.Companion.valueOf(track3DStyle.getWallColorType());
		GradientScaleType scaleType = coloringType.toGradientScaleType();
		GradientScaleType outlineScaleType = outlineColoringType != null ? outlineColoringType.toGradientScaleType() : null;

		boolean gradient = scaleType != null || hasMapRenderer && outlineScaleType != null;
		boolean visible = isGpxFileVisible(selectedGpxFile, tileBox);
		if (!gpxFile.hasTrkPt() && gradient || !visible) {
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
		if (gradient) {
			if (hasMapRenderer) {
				segments.addAll(getCachedSegments(selectedGpxFile, scaleType, outlineScaleType, colorPalette));
			} else {
				segments.addAll(getCachedSegments(selectedGpxFile, scaleType, colorPalette, true));
			}
		} else if (coloringType.isTrackSolid() || coloringType.isRouteInfoAttribute()) {
			segments.addAll(selectedGpxFile.getPointsToDisplay());
		}

		List<TrkSegment> oldSegments = new ArrayList<>();
		Set<TrkSegment> renderedSegments = renderedSegmentsCache.get(gpxFilePath);
		if (renderedSegments != null) {
			Iterator<TrkSegment> it = renderedSegments.iterator();
			while (it.hasNext()) {
				TrkSegment renderedSegment = it.next();
				if (!segments.contains(renderedSegment)) {
					if (renderedSegment.getRenderer() instanceof RenderableSegment renderableSegment) {
						oldSegments.add(renderedSegment);
					} else {
						resetSymbolProviders(renderedSegment);
					}
					it.remove();
				}
			}
		} else {
			renderedSegments = new HashSet<>();
			renderedSegmentsCache.put(gpxFilePath, renderedSegments);
		}
		String actualGpxWidth = gpxAppearanceHelper.getTrackWidth(gpxFile, null);
		String defaultGpxWidth = gpxAppearanceHelper.getTrackWidth(gpxFile, defaultWidthPref.get());
		for (int segmentIdx = 0; segmentIdx < segments.size(); segmentIdx++) {
			TrkSegment ts = segments.get(segmentIdx);
			String width = actualGpxWidth != null ? actualGpxWidth : ts.getWidth(defaultGpxWidth);
			cachedTrackWidth.putIfAbsent(width, null);
			int color = getTrackColor(gpxFile, ts.getColor(cachedColor));

			boolean newTsRenderer = false;
			if (ts.getRenderer() == null && !ts.getPoints().isEmpty()) {
				List<WptPt> points = ts.getPoints();
				RenderableSegment renderer = currentTrack ? new CurrentTrack(points) : new StandardTrack(points, 17.2);
				renderer.setBorderPaint(borderPaint);
				ts.setRenderer(renderer);
				GpxGeometryWay geometryWay = new GpxGeometryWay(wayContext);
				geometryWay.updateTrack3DStyle(track3DStyle);
				geometryWay.updateColoringType(coloringType);
				geometryWay.updateCustomWidth(paint.getStrokeWidth());

				if (!oldSegments.isEmpty() && oldSegments.get(0).getRenderer() instanceof CurrentTrack track) {
					GpxGeometryWay gpxGeometryWay = track.getGeometryWay();
					if (gpxGeometryWay != null) {
						geometryWay.vectorLinesCollection = gpxGeometryWay.vectorLinesCollection;
						geometryWay.vectorLineArrowsProvider = gpxGeometryWay.vectorLineArrowsProvider;
						geometryWay.updateCustomWidth(gpxGeometryWay.getCustomWidth());
						geometryWay.updateDrawDirectionArrows(gpxGeometryWay.getDrawDirectionArrows());
						oldSegments.remove(0);
					}
				}
				geometryWay.baseOrder = baseOrder--;
				renderer.setGeometryWay(geometryWay);
				newTsRenderer = true;
			}
			boolean updated = updatePaints(color, width, selectedGpxFile.isRoutePoints(), currentTrack, settings, tileBox)
					|| mapActivityInvalidated || invalidated || newTsRenderer || !renderedSegments.contains(ts);
			if (ts.getRenderer() instanceof RenderableSegment renderableSegment) {
				updated |= renderableSegment.setTrackParams(color, width, coloringType, routeIndoAttribute, colorPalette);
				if (hasMapRenderer || coloringType.isRouteInfoAttribute()) {
					boolean showArrows = isShowArrowsForTrack(gpxFile);
					CachedTrack cachedTrack = getCachedTrack(selectedGpxFile);
					updated |= renderableSegment.setRoute(getCachedRouteSegments(cachedTrack, segmentIdx));
					updated |= renderableSegment.setDrawArrows(showArrows);
					updated |= renderableSegment.setTrack3DStyle(track3DStyle);
					if (updated || !hasMapRenderer) {
						float[] intervals = null;
						PathEffect pathEffect = paint.getPathEffect();
						if (pathEffect instanceof OsmandDashPathEffect) {
							intervals = ((OsmandDashPathEffect) pathEffect).getIntervals();
						}
						renderableSegment.drawGeometry(canvas, tileBox, correctedQuadRect,
								paint.getColor(), paint.getStrokeWidth(), intervals, showArrows, track3DStyle, invalidated);
						renderedSegments.add(ts);
					}
				} else {
					renderableSegment.drawSegment(view.getZoom(), paint, canvas, tileBox);
				}
			}
		}
		for (TrkSegment oldSegment : oldSegments) {
			resetSymbolProviders(oldSegment);
		}
	}

	@NonNull
	private List<TrkSegment> getCachedSegments(@NonNull SelectedGpxFile selectedGpxFile,
	                                           @Nullable GradientScaleType scaleType,
	                                           @Nullable GradientScaleType outlineScaleType,
	                                           @NonNull String palette) {
		CachedTrack cachedTrack = getCachedTrack(selectedGpxFile);
		return cachedTrack.getTrackSegments(scaleType, outlineScaleType, palette);
	}


	@NonNull
	private List<TrkSegment> getCachedSegments(@NonNull SelectedGpxFile selectedGpxFile,
	                                           @NonNull GradientScaleType scaleType,
	                                           @NonNull String palette, boolean simplify) {
		CachedTrack cachedTrack = getCachedTrack(selectedGpxFile);
		return simplify
				? cachedTrack.getSimplifiedTrackSegments(view.getZoom(), scaleType, palette)
				: cachedTrack.getTrackSegments(scaleType, null, palette);
	}

	private float getTrackWidth(String width, float defaultTrackWidth) {
		Float trackWidth = cachedTrackWidth.get(width);
		return trackWidth != null ? trackWidth : defaultTrackWidth;
	}

	private int getTrackColor(@NonNull GpxFile gpxFile, int defaultColor) {
		return isGpxFileSelected(gpxFile) ? gpxAppearanceHelper.getTrackColor(gpxFile, defaultColor)
				: ColorUtilities.getColorWithAlpha(disabledColor, 0.5f);
	}

	@NonNull
	private String getTrackGradientPalette(@NonNull GpxFile gpxFile) {
		String gradientPaletteName = gpxAppearanceHelper.getGradientPaletteName(gpxFile);
		return !Algorithms.isEmpty(gradientPaletteName) ? gradientPaletteName : PaletteGradientColor.DEFAULT_NAME;
	}

	private String getAvailableOrDefaultColoringType(SelectedGpxFile selectedGpxFile) {
		GpxFile gpxFile = selectedGpxFile.getGpxFileToDisplay();

		if (!isGpxFileSelected(gpxFile)) {
			return ColoringType.TRACK_SOLID.getName(null);
		}

		String drawInfoColoringType = gpxAppearanceHelper.getColoringType(gpxFile);
		if (!Algorithms.isEmpty(drawInfoColoringType)) {
			return drawInfoColoringType;
		}

		GpxDataItem dataItem = null;
		String defaultColoringType = ColoringType.TRACK_SOLID.getName(null);
		ColoringType coloringType = null;
		String routeInfoAttribute = null;
		boolean isCurrentTrack = gpxFile.isShowCurrentTrack();

		if (isCurrentTrack) {
			coloringType = settings.CURRENT_TRACK_COLORING_TYPE.get();
			routeInfoAttribute = settings.CURRENT_TRACK_ROUTE_INFO_ATTRIBUTE.get();
		} else {
			dataItem = gpxDbHelper.getItem(new KFile(gpxFile.getPath()));
			if (dataItem != null) {
				coloringType = ColoringType.Companion.requireValueOf(ColoringPurpose.TRACK, dataItem.getParameter(COLORING_TYPE));
				routeInfoAttribute = ColoringType.Companion.getRouteInfoAttribute(dataItem.getParameter(COLORING_TYPE));
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
				gpxDbHelper.updateDataItemParameter(dataItem, COLORING_TYPE, defaultColoringType);
			}
			return defaultColoringType;
		}
	}

	private boolean isGpxFileSelected(@NonNull GpxFile gpxFile) {
		return customObjectsDelegate != null || GpxSelectionHelper.isGpxFileSelected(app, gpxFile);
	}

	private Gpx3DVisualizationType getTrackVisualizationType(@NonNull GpxFile gpxFile) {
		if (isGpxFileSelected(gpxFile)) {
			return gpxAppearanceHelper.getTrackVisualizationForTrack(gpxFile);
		} else {
			return Gpx3DVisualizationType.NONE;
		}
	}

	private Gpx3DWallColorType getTrackWallColorType(@NonNull GpxFile gpxFile) {
		if (isGpxFileSelected(gpxFile)) {
			return gpxAppearanceHelper.getTrackWallColorType(gpxFile);
		} else {
			return Gpx3DWallColorType.NONE;
		}
	}

	@NonNull
	private Track3DStyle getTrack3DStyle(@NonNull GpxFile gpxFile) {
		Gpx3DVisualizationType type = getTrackVisualizationType(gpxFile);
		float exaggeration = type != FIXED_HEIGHT ? getTrackExaggeration(gpxFile) : 1f;

		return new Track3DStyle(type, getTrackWallColorType(gpxFile),
				getTrackLinePositionType(gpxFile), exaggeration, getElevationMeters(gpxFile));
	}

	private Gpx3DLinePositionType getTrackLinePositionType(@NonNull GpxFile gpxFile) {
		if (isGpxFileSelected(gpxFile)) {
			return gpxAppearanceHelper.getTrackLinePositionType(gpxFile);
		} else {
			return Gpx3DLinePositionType.TOP;
		}
	}

	private float getTrackExaggeration(@NonNull GpxFile gpxFile) {
		return isGpxFileSelected(gpxFile) ? gpxAppearanceHelper.getAdditionalExaggeration(gpxFile) : 1f;
	}

	private float getElevationMeters(@NonNull GpxFile gpxFile) {
		return isGpxFileSelected(gpxFile) ? gpxAppearanceHelper.getElevationMeters(gpxFile) : 1000;
	}

	private boolean isShowArrowsForTrack(@NonNull GpxFile gpxFile) {
		return isGpxFileSelected(gpxFile) && gpxAppearanceHelper.isShowArrowsForTrack(gpxFile);
	}

	private boolean isShowStartFinishForTrack(@NonNull GpxFile gpxFile) {
		return isGpxFileSelected(gpxFile) && gpxAppearanceHelper.isShowStartFinishForTrack(gpxFile);
	}

	@NonNull
	private CachedTrack getCachedTrack(@NonNull SelectedGpxFile selectedGpxFile) {
		String path = selectedGpxFile.getGpxFile().getPath();
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
		GpxFile gpxFile = cachedTrack.getSelectedGpxFile().getGpxFileToDisplay();

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
				parseGpxRouteTasks.remove(gpxFile.getPath());
			};
			ParseGpxRouteTask task = new ParseGpxRouteTask(gpxFile, trackParams, nonEmptySegmentIdx, listener);
			parseGpxRouteTasks.put(gpxFile.getPath(), task);
			OsmAndTaskManager.executeTask(task, parseGpxRouteSingleThreadExecutor);
		}
	}

	private boolean cachedTrackParamsChanged(@NonNull GpxFile gpxFile, @NonNull CachedTrackParams params) {
		ParseGpxRouteTask task = parseGpxRouteTasks.get(gpxFile.getPath());
		if (task != null) {
			return !Algorithms.objectEquals(params, task.getCachedTrackParams());
		}
		return false;
	}

	public boolean isParsingGpxRoute(@NonNull GpxFile gpxFile) {
		return parseGpxRouteTasks.containsKey(gpxFile.getPath());
	}

	public void cancelGpxRouteParsing(@NonNull GpxFile gpxFile) {
		ParseGpxRouteTask task = parseGpxRouteTasks.get(gpxFile.getPath());
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
		return g.getGpxFile().getPointsList();
	}

	private int getSelectedFilePointsSize(@NonNull SelectedGpxFile g) {
		return g.getGpxFile().getPointsSize();
	}

	private boolean isPointHidden(SelectedGpxFile selectedGpxFile, WptPt point) {
		return selectedGpxFile.isGroupHidden(point.getCategory());
	}

	public void collectWptFromPoint(@NonNull MapSelectionResult result) {
		PointF point = result.getPoint();
		RotatedTileBox tb = result.getTileBox();
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
						? NativeUtilities.isPointInsidePolygon(waypoint.getLat(), waypoint.getLon(), touchPolygon31)
						: tb.isLatLonNearPixel(waypoint.getLat(), waypoint.getLon(), point.x, point.y, radius);
				if (add) {
					result.collect(waypoint, this);
				}
			}
		}
	}

	public void collectTracksFromPoint(@NonNull MapSelectionResult result, boolean showTrackPointMenu) {
		List<SelectedGpxFile> selectedGpxFiles = new ArrayList<>(selectedGpxHelper.getSelectedGPXFiles());
		if (selectedGpxFiles.isEmpty()) {
			return;
		}
		PointF point = result.getPoint();
		RotatedTileBox tb = result.getTileBox();
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
						? GpxUtils.findLineInPolygon31(touchPolygon31, segment.getPoints())
						: GpxUtils.findLineNearPoint(tb, segment.getPoints(), radius, (int) point.x, (int) point.y);
				if (line != null) {
					break;
				}
			}
			if (line != null) {
				if (latLonFromPixel == null) {
					latLonFromPixel = NativeUtilities.getLatLonFromElevatedPixel(mapRenderer, tb, point.x, point.y);
				}
				result.collect(GpxUtils.createSelectedGpxPoint(selectedGpxFile, line.first, line.second, latLonFromPixel,
						showTrackPointMenu), this);
			}
		}
	}

	@Override
	public PointDescription getObjectName(Object o) {
		if (o instanceof WptPt) {
			return new PointDescription(PointDescription.POINT_TYPE_WPT, ((WptPt) o).getName());
		} else if (o instanceof SelectedGpxPoint) {
			SelectedGpxFile selectedGpxFile = ((SelectedGpxPoint) o).getSelectedGpxFile();
			GpxFile gpxFile = selectedGpxFile.getGpxFile();

			String name;
			if (selectedGpxFile.isShowCurrentTrack()) {
				name = getContext().getString(R.string.shared_string_currently_recording_track);
			} else if (!Algorithms.isEmpty(gpxFile.getArticleTitle())) {
				name = gpxFile.getArticleTitle();
			} else {
				name = GpxHelper.INSTANCE.getGpxTitle(Algorithms.getFileWithoutDirs(gpxFile.getPath()));
			}
			return new PointDescription(PointDescription.POINT_TYPE_GPX, name);
		}
		return null;
	}

	private void removeCachedUnselectedTracks(List<SelectedGpxFile> selectedGpxFiles) {
		List<String> selectedTracksPaths = new ArrayList<>();
		for (SelectedGpxFile gpx : selectedGpxFiles) {
			selectedTracksPaths.add(gpx.getGpxFile().getPath());
		}
		List<SelectedGpxFile> unselectedGpxFiles = new ArrayList<>();
		for (SelectedGpxFile gpx : visibleGPXFilesMap.keySet()) {
			if (!selectedTracksPaths.contains(gpx.getGpxFile().getPath())) {
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
		if (segment.getRenderer() instanceof RenderableSegment renderableSegment) {
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
			MapSelectionResult result = new MapSelectionResult(app, tileBox, point);
			collectTracksFromPoint(result, false);
			return !result.isEmpty();
		}
		return false;
	}

	@Override
	public void collectObjectsFromPoint(@NonNull MapSelectionResult result,
			boolean unknownLocation, boolean excludeUntouchableObjects) {
		if (result.getTileBox().getZoom() >= START_ZOOM) {
			collectWptFromPoint(result);

			if (!excludeUntouchableObjects) {
				collectTracksFromPoint(result, false);
			}
		}
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if (o instanceof WptPt) {
			return new LatLon(((WptPt) o).getLat(), ((WptPt) o).getLon());
		} else if (o instanceof SelectedGpxPoint) {
			WptPt point = ((SelectedGpxPoint) o).getSelectedPoint();
			return new LatLon(point.getLat(), point.getLon());
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
			MapSelectionResult result = new MapSelectionResult(app, tileBox, point);
			collectTracksFromPoint(result, true);

			List<SelectedMapObject> objects = result.getAllObjects();
			if (!Algorithms.isEmpty(objects)) {
				LatLon latLon = NativeUtilities.getLatLonFromElevatedPixel(getMapRenderer(), tileBox, point);
				if (objects.size() == 1) {
					SelectedGpxPoint gpxPoint = (SelectedGpxPoint) objects.get(0).object();
					contextMenuLayer.showContextMenu(latLon, getObjectName(gpxPoint), gpxPoint, null);
				} else if (objects.size() > 1) {
					contextMenuLayer.showContextMenuForSelectedObjects(latLon, objects);
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
			if (object instanceof SelectedGpxPoint gpxPoint) {
				if (gpxPoint.shouldShowTrackPointMenu()) {
					WptPt wptPt = gpxPoint.getSelectedPoint();
					LatLon latLon = new LatLon(wptPt.getLat(), wptPt.getLon());
					contextMenuLayer.showContextMenu(latLon, getObjectName(gpxPoint), gpxPoint, null);
				} else {
					SelectedGpxFile selectedGpxFile = gpxPoint.getSelectedGpxFile();
					TrackMenuFragment.showInstance(mapActivity, selectedGpxFile, gpxPoint);
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public LatLon getTextLocation(WptPt o) {
		return new LatLon(o.getLat(), o.getLon());
	}

	@Override
	public int getTextShift(WptPt o, RotatedTileBox rb) {
		return (int) (16 * rb.getDensity() * getTextScale());
	}

	@Override
	public String getText(WptPt o) {
		return o.getName();
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
	public Object getMoveableObjectIcon(@NonNull Object o) {
		if (o instanceof WptPt wptPt) {
			SelectedGpxFile gpxFile = pointFileMap.get(wptPt);
			if (gpxFile != null) {
				MapMarker mapMarker = mapMarkersHelper.getMapMarker(wptPt);
				int fileColor = getFileColor(gpxFile);
				int pointColor = getPointColor(wptPt, fileColor);
				return createWaypointIcon(pointColor, wptPt, mapMarker);
			}
		}
		return null;
	}

	@Override
	public void applyNewObjectPosition(@NonNull Object o, @NonNull LatLon latLon,
	                                   @Nullable ApplyMovedObjectCallback callback) {
		if (o instanceof WptPt wptPt) {
			SelectedGpxFile selectedGpxFile = pointFileMap.get(wptPt);
			if (selectedGpxFile != null) {
				GpxFile gpxFile = selectedGpxFile.getGpxFile();
				if (gpxFile.isShowCurrentTrack()) {
					app.getSavingTrackHelper().updatePointData(wptPt, latLon.getLatitude(), latLon.getLongitude(),
							wptPt.getDesc(), wptPt.getName(), wptPt.getCategory(), wptPt.getColor(),
							wptPt.getIconName(), wptPt.getBackgroundType());

					if (callback != null) {
						callback.onApplyMovedObject(true, wptPt);
					}
				} else {
					WptPt newPoint = new WptPt(latLon.getLatitude(), latLon.getLongitude(), wptPt.getDesc(),
							wptPt.getName(), wptPt.getCategory(), Algorithms.colorToString(wptPt.getColor()),
							wptPt.getIconName(), wptPt.getBackgroundType());

					gpxFile.updateWptPt(wptPt, newPoint, true);
					SaveGpxHelper.saveGpx(new File(gpxFile.getPath()), gpxFile, errorMessage -> {
						if (callback != null) {
							callback.onApplyMovedObject(errorMessage == null, wptPt);
						}
					});
				}
				syncGpx(gpxFile);
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

	private void syncGpx(GpxFile gpxFile) {
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