package net.osmand.plus.plugins.aistracker;

import static net.osmand.shared.aistracker.AisObjType.AIS_AIRPLANE;
import static net.osmand.shared.aistracker.AisObjType.AIS_ATON;
import static net.osmand.shared.aistracker.AisObjType.AIS_ATON_VIRTUAL;
import static net.osmand.shared.aistracker.AisObjType.AIS_LANDSTATION;
import static net.osmand.shared.aistracker.AisObjType.AIS_SART;
import static net.osmand.shared.aistracker.AisObjType.AIS_VESSEL_SAR;
import static net.osmand.shared.aistracker.AisObjectConstants.CPA_UPDATE_TIMEOUT_IN_SECONDS;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.MapMarkersCollection;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.SingleSkImage;
import net.osmand.core.jni.VectorLinesCollection;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.aistracker.AisTrackerPlugin.AisDataManager.AisObjectListener;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProviderSelection;
import net.osmand.plus.views.layers.MapSelectionResult;
import net.osmand.plus.views.layers.MapSelectionRules;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.shared.aistracker.AisCpa;
import net.osmand.shared.aistracker.AisLatLon;
import net.osmand.shared.aistracker.AisLocation;
import net.osmand.shared.aistracker.AisObjType;
import net.osmand.shared.aistracker.AisObject;
import net.osmand.shared.aistracker.AisObjectConstants;
import net.osmand.shared.aistracker.AisTrackerMath;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Virtualized AIS layer. The data manager may retain 20,000 targets, while this layer owns
 * native render resources only for a stable, bounded, non-overlapping viewport subset.
 */
public class AisTrackerLayer extends OsmandMapLayer implements IContextMenuProvider,
		IContextMenuProviderSelection, AisObjectListener {

	public static final int START_ZOOM = 6;
	public static final int START_ZOOM_SHOW_SHAPE = 16;
	public static final int START_ZOOM_SHOW_DIRECTION = 10;

	private static final Log LOG = PlatformUtil.getLog(AisTrackerLayer.class);
	private static final int SPATIAL_INDEX_ZOOM = 8;
	private static final int SPATIAL_BUCKET_COUNT = 1 << SPATIAL_INDEX_ZOOM;
	private static final int SPATIAL_31_SHIFT = 31 - SPATIAL_INDEX_ZOOM;
	private static final int MAX_RENDERED_OBJECTS = 1000;
	private static final int MAX_PROJECTION_CANDIDATES = 4000;
	private static final int COARSE_CANDIDATES_PER_CELL = 4;
	private static final float VIEWPORT_MARGIN_FACTOR = 0.2f;
	private static final float BASE_ICON_SIZE_DP = 48f;
	private static final float COLLISION_PADDING_DP = 4f;
	private static final long RENDER_UPDATE_INTERVAL_MS = 200;
	private static final double ZOOM_EPSILON = 0.02;

	private final AisTrackerPlugin plugin = PluginsHelper.requirePlugin(AisTrackerPlugin.class);
	private final Paint bitmapPaint = new Paint();
	private final Object indexLock = new Object();
	private final Map<Integer, RenderRecord> objectRecords = new HashMap<>();
	private final Map<Long, Set<Integer>> spatialBuckets = new HashMap<>();
	private final Map<Integer, AisObjectDrawable> objectDrawables = new LinkedHashMap<>();
	private Map<Integer, RenderRecord> renderedRecords = new LinkedHashMap<>();

	private MapMarkersCollection markersCollection;
	private VectorLinesCollection vectorLinesCollection;
	private MapRendererView collectionsRenderer;
	private Bitmap aisRestBitmap;
	private SingleSkImage aisRestImage;
	private float textScale = 1f;
	private boolean indexLoaded;
	private volatile boolean dataDirty;
	private boolean refreshScheduled;
	private long nextVersion = 1;
	private long lastRenderTimeMs;
	private ViewportSignature lastViewport;
	private Integer selectedMmsi;
	private int peakDrawableCount;

	public AisTrackerLayer(@NonNull Context context) {
		super(context);
	}

	@Override
	public void initLayer(@NonNull OsmandMapTileView view) {
		super.initLayer(view);
		bitmapPaint.setAntiAlias(true);
		bitmapPaint.setFilterBitmap(true);
		bitmapPaint.setStrokeWidth(4);
		bitmapPaint.setColor(Color.DKGRAY);
		textScale = getTextScale();
		aisRestBitmap = createOutlinedRestBitmap();
	}

	@NonNull
	private Bitmap createOutlinedRestBitmap() {
		// Keep the previous 54 px rest-circle footprint, replacing its pale ring with
		// a strong dark outline so green stationary targets remain legible.
		float density = 5f;
		float outerRadius = 5f * density;
		float innerRadius = 4f * density;
		int margin = 2;
		int size = Math.round(outerRadius * 2 + margin * 2);
		Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setStyle(Paint.Style.FILL);
		float center = outerRadius + margin;
		paint.setColor(Color.DKGRAY);
		canvas.drawCircle(center, center, outerRadius, paint);
		paint.setColor(Color.WHITE);
		canvas.drawCircle(center, center, innerRadius, paint);
		return bitmap;
	}

	@Override
	public void cleanupResources() {
		clearRenderResources();
		plugin.getAisImagesCache().clearCache();
		synchronized (indexLock) {
			objectRecords.clear();
			spatialBuckets.clear();
			indexLoaded = false;
		}
		selectedMmsi = null;
		dataDirty = false;
		refreshScheduled = false;
		nextVersion = 1;
		peakDrawableCount = 0;
	}

	private void clearRenderResources() {
		if (markersCollection != null) {
			markersCollection.removeAllMarkers();
		}
		if (vectorLinesCollection != null) {
			vectorLinesCollection.removeAllLines();
		}
		if (collectionsRenderer != null) {
			if (markersCollection != null) {
				collectionsRenderer.removeSymbolsProvider(markersCollection);
			}
			if (vectorLinesCollection != null) {
				collectionsRenderer.removeSymbolsProvider(vectorLinesCollection);
			}
		}
		objectDrawables.clear();
		renderedRecords = new LinkedHashMap<>();
		markersCollection = null;
		vectorLinesCollection = null;
		collectionsRenderer = null;
		aisRestImage = null;
		lastViewport = null;
		lastRenderTimeMs = 0;
	}

	private void ensureNativeCollections(@NonNull MapRendererView renderer) {
		if (collectionsRenderer == renderer && markersCollection != null && vectorLinesCollection != null) {
			return;
		}
		clearRenderResources();
		markersCollection = new MapMarkersCollection();
		vectorLinesCollection = new VectorLinesCollection();
		aisRestImage = NativeUtilities.createSkImageFromBitmap(aisRestBitmap);
		renderer.addSymbolsProvider(markersCollection);
		renderer.addSymbolsProvider(vectorLinesCollection);
		collectionsRenderer = renderer;
		dataDirty = true;
	}

	private long bucketKey(int x, int y) {
		return ((long) x << 32) | (y & 0xffffffffL);
	}

	private void addToBucket(@NonNull RenderRecord record) {
		spatialBuckets.computeIfAbsent(record.bucketKey, ignored -> new HashSet<>()).add(record.mmsi);
	}

	private void removeFromBucket(@NonNull RenderRecord record) {
		Set<Integer> bucket = spatialBuckets.get(record.bucketKey);
		if (bucket != null) {
			bucket.remove(record.mmsi);
			if (bucket.isEmpty()) {
				spatialBuckets.remove(record.bucketKey);
			}
		}
	}

	private void upsertIndex(@NonNull AisObject ais) {
		synchronized (indexLock) {
			RenderRecord previous = objectRecords.remove(ais.getMmsi());
			if (previous != null) {
				removeFromBucket(previous);
			}
			AisLatLon position = ais.getPosition();
			if (position != null && !isOwnObjectHidden(ais)) {
				int x31 = MapUtils.get31TileNumberX(position.getLongitude());
				int y31 = MapUtils.get31TileNumberY(position.getLatitude());
				RenderRecord record = new RenderRecord(ais, x31, y31, nextVersion++);
				if (previous != null) {
					record.lastCpaUpdate = previous.lastCpaUpdate;
				}
				record.bucketKey = bucketKey(x31 >>> SPATIAL_31_SHIFT, y31 >>> SPATIAL_31_SHIFT);
				objectRecords.put(record.mmsi, record);
				addToBucket(record);
			}
			indexLoaded = true;
		}
	}

	private void removeFromIndex(int mmsi) {
		synchronized (indexLock) {
			RenderRecord record = objectRecords.remove(mmsi);
			if (record != null) {
				removeFromBucket(record);
			}
		}
	}

	private void ensureIndexLoaded() {
		synchronized (indexLock) {
			if (indexLoaded) {
				return;
			}
		}
		// Do not hold the index lock while taking the data-manager snapshot: AIS callbacks
		// arrive while the manager is synchronized and update this index afterwards.
		List<AisObject> snapshot = plugin.getAisObjects();
		synchronized (indexLock) {
			if (indexLoaded) {
				return;
			}
			objectRecords.clear();
			spatialBuckets.clear();
			for (AisObject ais : snapshot) {
				AisLatLon position = ais.getPosition();
				if (position != null && !isOwnObjectHidden(ais)) {
					int x31 = MapUtils.get31TileNumberX(position.getLongitude());
					int y31 = MapUtils.get31TileNumberY(position.getLatitude());
					RenderRecord record = new RenderRecord(ais, x31, y31, nextVersion++);
					record.bucketKey = bucketKey(x31 >>> SPATIAL_31_SHIFT, y31 >>> SPATIAL_31_SHIFT);
					objectRecords.put(record.mmsi, record);
					addToBucket(record);
				}
			}
			indexLoaded = true;
			dataDirty = true;
		}
	}

	@Override
	public void onAisObjectReceived(@NonNull AisObject ais) {
		upsertIndex(ais);
		dataDirty = true;
		scheduleFrameRefresh();
	}

	@Override
	public void onAisObjectRemoved(@NonNull AisObject ais) {
		removeFromIndex(ais.getMmsi());
		if (selectedMmsi != null && selectedMmsi == ais.getMmsi()) {
			selectedMmsi = null;
		}
		dataDirty = true;
		scheduleFrameRefresh();
	}

	private boolean isOwnObject(@NonNull AisObject ais) {
		return ais.getMmsi() == plugin.AIS_OWN_MMSI.get();
	}

	private boolean isOwnObjectHidden(@NonNull AisObject ais) {
		return isOwnObject(ais) && !plugin.AIS_DISPLAY_OWN_POSITION.get();
	}

	public void refreshOwnObjectVisibility() {
		synchronized (indexLock) {
			indexLoaded = false;
		}
		dataDirty = true;
		scheduleFrameRefresh();
	}

	private void scheduleFrameRefresh() {
		synchronized (this) {
			if (refreshScheduled) {
				return;
			}
			refreshScheduled = true;
		}
		long elapsed = SystemClock.elapsedRealtime() - lastRenderTimeMs;
		long delay = Math.max(0, RENDER_UPDATE_INTERVAL_MS - elapsed);
		getApplication().runInUIThread(() -> {
			synchronized (AisTrackerLayer.this) {
				refreshScheduled = false;
			}
			OsmandMapTileView tileView = getTileView();
			if (tileView != null) {
				tileView.refreshMap();
			}
		}, delay);
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		super.onPrepareBufferImage(canvas, tileBox, settings);
		ensureIndexLoaded();

		float currentTextScale = getTextScale();
		boolean scaleChanged = Math.abs(textScale - currentTextScale) > 0.0001f;
		if (scaleChanged) {
			textScale = currentTextScale;
			plugin.getAisImagesCache().clearCache();
			aisRestBitmap = createOutlinedRestBitmap();
			clearRenderResources();
			dataDirty = true;
		}

		MapRendererView renderer = getMapRenderer();
		if (mapRendererChanged || (renderer == null && collectionsRenderer != null)) {
			clearRenderResources();
			dataDirty = true;
		}
		if (renderer != null) {
			ensureNativeCollections(renderer);
		}

		ViewportSignature viewport = new ViewportSignature(tileBox);
		boolean viewportChanged = !viewport.equals(lastViewport);
		long now = SystemClock.elapsedRealtime();
		boolean intervalElapsed = lastRenderTimeMs == 0 || now - lastRenderTimeMs >= RENDER_UPDATE_INTERVAL_MS;
		if ((dataDirty || viewportChanged) && intervalElapsed) {
			SelectionResult selection = selectRecords(tileBox, renderer);
			reconcile(selection, tileBox, renderer != null);
			lastViewport = viewport;
			lastRenderTimeMs = now;
			dataDirty = false;
		} else if (dataDirty || viewportChanged) {
			scheduleFrameRefresh();
		}

		if (renderer == null) {
			for (AisObjectDrawable drawable : objectDrawables.values()) {
				drawable.draw(bitmapPaint, canvas, tileBox);
			}
		}
		mapActivityInvalidated = false;
		mapRendererChanged = false;
	}

	private SelectionResult selectRecords(@NonNull RotatedTileBox tileBox,
			@Nullable MapRendererView renderer) {
		long started = SystemClock.elapsedRealtimeNanos();
		int zoom = tileBox.getZoom();
		float density = Math.max(1f, tileBox.getDensity());
		float iconSize = Math.max(BASE_ICON_SIZE_DP * density * textScale,
				aisRestBitmap == null ? 0 : aisRestBitmap.getWidth());
		float footprint = iconSize + COLLISION_PADDING_DP * density;
		float marginX = tileBox.getPixWidth() * VIEWPORT_MARGIN_FACTOR;
		float marginY = tileBox.getPixHeight() * VIEWPORT_MARGIN_FACTOR;
		RectF renderBounds = new RectF(-marginX, -marginY,
				tileBox.getPixWidth() + marginX, tileBox.getPixHeight() + marginY);
		int columns = Math.max(1, (int) Math.ceil(renderBounds.width() / footprint));
		int rows = Math.max(1, (int) Math.ceil(renderBounds.height() / footprint));

		RenderRecord selectedRecord;
		List<RenderRecord> candidates;
		synchronized (indexLock) {
			selectedRecord = selectedMmsi == null ? null : objectRecords.get(selectedMmsi);
			candidates = zoom >= START_ZOOM ? queryViewportBuckets(tileBox, marginX, marginY) : new ArrayList<>();
		}
		int renderBudget = zoom >= START_ZOOM
				? Math.min(MAX_RENDERED_OBJECTS, columns * rows)
				: selectedRecord == null ? 0 : 1;

		Set<Integer> incumbentKeys = new HashSet<>(objectDrawables.keySet());
		List<RenderRecord> incumbents = new ArrayList<>();
		for (RenderRecord record : candidates) {
			record.updateSelectionState(plugin);
			record.hasScreenPoint = false;
			record.cpaWarning = false;
			if (record != selectedRecord && incumbentKeys.contains(record.mmsi)) {
				incumbents.add(record);
			}
		}
		if (selectedRecord != null) {
			selectedRecord.updateSelectionState(plugin);
			selectedRecord.hasScreenPoint = false;
			selectedRecord.cpaWarning = false;
		}
		incumbents.sort(Comparator.comparingInt(record -> record.mmsi));

		double world31PerPixel = Math.pow(2, 31 - tileBox.getFullZoom())
				/ Math.max(1d, 256d * tileBox.getMapDensity());
		double coarseCellSize31 = Math.max(1d, footprint * world31PerPixel);
		Map<Long, List<RenderRecord>> coarseCells = new HashMap<>();
		for (RenderRecord record : candidates) {
			if (record == selectedRecord || incumbentKeys.contains(record.mmsi)) {
				continue;
			}
			long cellX = (long) Math.floor(record.x31 / coarseCellSize31);
			long cellY = (long) Math.floor(record.y31 / coarseCellSize31);
			long key = (cellX << 32) ^ (cellY & 0xffffffffL);
			List<RenderRecord> cell = coarseCells.computeIfAbsent(key, ignored -> new ArrayList<>(COARSE_CANDIDATES_PER_CELL));
			cell.add(record);
			cell.sort(COARSE_COMPARATOR);
			if (cell.size() > COARSE_CANDIDATES_PER_CELL) {
				cell.remove(cell.size() - 1);
			}
		}

		int reserved = incumbents.size() + (selectedRecord == null ? 0 : 1);
		int projectionBudget = Math.min(MAX_PROJECTION_CANDIDATES,
				Math.max(reserved, Math.max(renderBudget, renderBudget * COARSE_CANDIDATES_PER_CELL)));
		List<RenderRecord> shortlist = new ArrayList<>(projectionBudget);
		if (selectedRecord != null) {
			shortlist.add(selectedRecord);
		}
		shortlist.addAll(incumbents);
		List<Long> orderedCells = new ArrayList<>(coarseCells.keySet());
		Collections.sort(orderedCells);
		for (int rank = 0; rank < COARSE_CANDIDATES_PER_CELL && shortlist.size() < projectionBudget; rank++) {
			for (Long cellKey : orderedCells) {
				List<RenderRecord> cell = coarseCells.get(cellKey);
				if (rank < cell.size()) {
					shortlist.add(cell.get(rank));
				}
				if (shortlist.size() >= projectionBudget) {
					break;
				}
			}
		}

		for (RenderRecord record : shortlist) {
			record.cpaWarning = evaluateCpaWarning(record);
		}
		List<RenderRecord> safety = new ArrayList<>();
		List<RenderRecord> retained = new ArrayList<>();
		List<RenderRecord> newcomers = new ArrayList<>();
		for (RenderRecord record : shortlist) {
			if (record == selectedRecord) {
				continue;
			}
			if (record.cpaWarning || record.emergency) {
				safety.add(record);
			} else if (incumbentKeys.contains(record.mmsi)) {
				retained.add(record);
			} else {
				newcomers.add(record);
			}
		}
		safety.sort(Comparator.comparing((RenderRecord record) -> !record.cpaWarning)
				.thenComparing(record -> !record.emergency)
				.thenComparing(record -> !incumbentKeys.contains(record.mmsi))
				.thenComparing(record -> !record.isMoving())
				.thenComparing((RenderRecord record) -> -record.lastUpdate)
				.thenComparingInt(record -> record.mmsi));
		retained.sort(INCUMBENT_COMPARATOR);
		newcomers.sort(NEWCOMER_COMPARATOR);

		SelectionResult result = new SelectionResult(candidates.size(), started);
		Map<Long, List<AcceptedRect>> occupied = new HashMap<>();
		if (selectedRecord != null) {
			trySelect(selectedRecord, true, tileBox, renderer, footprint, renderBounds, renderBudget,
					incumbentKeys, occupied, result);
		}
		for (RenderRecord record : safety) {
			trySelect(record, false, tileBox, renderer, footprint, renderBounds, renderBudget,
					incumbentKeys, occupied, result);
		}
		for (RenderRecord record : retained) {
			trySelect(record, false, tileBox, renderer, footprint, renderBounds, renderBudget,
					incumbentKeys, occupied, result);
		}
		for (RenderRecord record : newcomers) {
			trySelect(record, false, tileBox, renderer, footprint, renderBounds, renderBudget,
					incumbentKeys, occupied, result);
		}
		return result;
	}

	@NonNull
	private List<RenderRecord> queryViewportBuckets(@NonNull RotatedTileBox tileBox,
			float marginX, float marginY) {
		float[] xs = {-marginX, tileBox.getPixWidth() + marginX};
		float[] ys = {-marginY, tileBox.getPixHeight() + marginY};
		int[] bucketXs = new int[4];
		int minY = SPATIAL_BUCKET_COUNT - 1;
		int maxY = 0;
		int index = 0;
		for (float x : xs) {
			for (float y : ys) {
				double lon = tileBox.getLonFromPixel(x, y);
				double lat = tileBox.getLatFromPixel(x, y);
				int bx = MapUtils.get31TileNumberX(lon) >>> SPATIAL_31_SHIFT;
				int by = MapUtils.get31TileNumberY(lat) >>> SPATIAL_31_SHIFT;
				bucketXs[index++] = bx;
				minY = Math.min(minY, by);
				maxY = Math.max(maxY, by);
			}
		}
		Arrays.sort(bucketXs);
		int largestGap = -1;
		int arcStart = bucketXs[0];
		int arcEnd = bucketXs[bucketXs.length - 1];
		for (int i = 0; i < bucketXs.length; i++) {
			int current = bucketXs[i];
			int next = i + 1 < bucketXs.length ? bucketXs[i + 1] : bucketXs[0] + SPATIAL_BUCKET_COUNT;
			int gap = next - current;
			if (gap > largestGap) {
				largestGap = gap;
				arcStart = next % SPATIAL_BUCKET_COUNT;
				arcEnd = current;
			}
		}

		List<RenderRecord> records = new ArrayList<>();
		int bx = arcStart;
		while (true) {
			for (int by = minY; by <= maxY; by++) {
				Set<Integer> bucket = spatialBuckets.get(bucketKey(bx, by));
				if (bucket != null) {
					for (Integer mmsi : bucket) {
						RenderRecord record = objectRecords.get(mmsi);
						if (record != null) {
							records.add(record);
						}
					}
				}
			}
			if (bx == arcEnd) {
				break;
			}
			bx = (bx + 1) % SPATIAL_BUCKET_COUNT;
		}
		return records;
	}

	private boolean evaluateCpaWarning(@NonNull RenderRecord record) {
		AisObject ais = record.object;
		int warningTime = plugin.getCpaWarningTime();
		if (!ais.isMovable() || ais.getObjectClass() == AIS_AIRPLANE || warningTime <= 0
				|| ais.getSog() <= AisObjectConstants.SPEED_CONSIDERED_IN_REST) {
			return false;
		}
		AisCpa cpa = ais.getCpa();
		long now = System.currentTimeMillis();
		Location ownPosition = plugin.getOwnPosition();
		if ((now - record.lastCpaUpdate) / 1000 > CPA_UPDATE_TIMEOUT_IN_SECONDS && ownPosition != null) {
			AisLocation position = ais.getExtrapolatedLocation(now);
			if (position != null) {
				AisTrackerMath.INSTANCE.getCpa(AisObjectAndroidHelperKt.toAisLocation(ownPosition), position, cpa);
				record.lastCpaUpdate = now;
			}
		}
		return cpa.getValid() && cpa.getTcpa() > 0
				&& cpa.getCpa() <= plugin.getCpaWarningDistance()
				&& cpa.getTcpa() * 60 <= warningTime
				&& cpa.getT1() >= 0 && cpa.getT2() >= 0;
	}

	private void trySelect(@NonNull RenderRecord record, boolean forceAdmission,
			@NonNull RotatedTileBox tileBox, @Nullable MapRendererView renderer,
			float footprint, @NonNull RectF renderBounds,
			int renderBudget, @NonNull Set<Integer> incumbentKeys,
			@NonNull Map<Long, List<AcceptedRect>> occupied, @NonNull SelectionResult result) {
		if (result.desired.size() >= renderBudget) {
			return;
		}
		result.projected++;
		AisLatLon position = record.object.getPosition();
		if (position == null) {
			return;
		}
		PointF screenPoint;
		if (renderer != null) {
			screenPoint = NativeUtilities.getPixelFrom31(renderer, tileBox,
					new PointI(record.x31, record.y31));
		} else {
			double longitude = normalizeLongitudeNear(position.getLongitude(), tileBox.getLongitude());
			screenPoint = new PointF(
					tileBox.getPixXFromLatLon(position.getLatitude(), longitude),
					tileBox.getPixYFromLatLon(position.getLatitude(), longitude));
		}
		float x = screenPoint.x;
		float y = screenPoint.y;
		if (!renderBounds.contains(x, y)) {
			if (forceAdmission) {
				result.desired.put(record.mmsi, record);
			}
			return;
		}

		RectF rect = new RectF(x - footprint / 2, y - footprint / 2,
				x + footprint / 2, y + footprint / 2);
		record.screenX = x;
		record.screenY = y;
		record.iconRect = rect;
		record.hasScreenPoint = true;
		int minCellX = (int) Math.floor((rect.left - renderBounds.left) / footprint);
		int maxCellX = (int) Math.floor((rect.right - renderBounds.left) / footprint);
		int minCellY = (int) Math.floor((rect.top - renderBounds.top) / footprint);
		int maxCellY = (int) Math.floor((rect.bottom - renderBounds.top) / footprint);
		boolean overlaps = false;
		Set<Integer> inspected = new HashSet<>();
		Set<Integer> safetyDisplacers = new HashSet<>();
		for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
			for (int cellY = minCellY; cellY <= maxCellY; cellY++) {
				List<AcceptedRect> accepted = occupied.get(screenCellKey(cellX, cellY));
				if (accepted == null) {
					continue;
				}
				for (AcceptedRect other : accepted) {
					if (inspected.add(other.mmsi) && RectF.intersects(rect, other.rect)) {
						overlaps = true;
						if (incumbentKeys.contains(record.mmsi) && other.safety) {
							safetyDisplacers.add(other.mmsi);
						}
					}
				}
			}
		}
		if (overlaps && !forceAdmission) {
			result.collisionRejected++;
			if (!safetyDisplacers.isEmpty()) {
				result.safetyDisplacers.put(record.mmsi, safetyDisplacers);
			}
			return;
		}
		if (!overlaps) {
			AcceptedRect accepted = new AcceptedRect(record.mmsi, rect,
					record.cpaWarning || record.emergency);
			for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
				for (int cellY = minCellY; cellY <= maxCellY; cellY++) {
					occupied.computeIfAbsent(screenCellKey(cellX, cellY), ignored -> new ArrayList<>())
							.add(accepted);
				}
			}
		}
		result.desired.put(record.mmsi, record);
	}

	private static long screenCellKey(int x, int y) {
		return ((long) x << 32) ^ (y & 0xffffffffL);
	}

	private static double normalizeLongitudeNear(double longitude, double reference) {
		double result = longitude;
		while (result - reference > 180) {
			result -= 360;
		}
		while (result - reference < -180) {
			result += 360;
		}
		return result;
	}

	private void reconcile(@NonNull SelectionResult selection, @NonNull RotatedTileBox tileBox,
			boolean nativeRenderer) {
		Set<Integer> previous = new HashSet<>(objectDrawables.keySet());
		Set<Integer> failed = new HashSet<>();
		for (Map.Entry<Integer, RenderRecord> entry : new ArrayList<>(selection.desired.entrySet())) {
			int mmsi = entry.getKey();
			RenderRecord record = entry.getValue();
			AisObjectDrawable drawable = objectDrawables.get(mmsi);
			if (drawable == null) {
				drawable = new AisObjectDrawable(plugin, record.object);
				objectDrawables.put(mmsi, drawable);
			}
			drawable.set(record.object, record.visualState);
			drawable.setOwnObject(isOwnObject(record.object));
			if (nativeRenderer) {
				if ((drawable.isRenderKeyChanged() || (drawable.hasAnyAisRenderData() && !drawable.hasAisRenderData()))
						&& markersCollection != null && vectorLinesCollection != null) {
					drawable.clearAisRenderData(markersCollection, vectorLinesCollection);
				}
				if (!drawable.hasAisRenderData() && (markersCollection == null || vectorLinesCollection == null
						|| aisRestImage == null || !drawable.createAisRenderData(
						getBaseOrder(), markersCollection, aisRestImage))) {
					objectDrawables.remove(mmsi);
					failed.add(mmsi);
					continue;
				}
				double zoom = tileBox.getFullZoom();
				if (drawable.getRenderedVersion() != record.version
						|| drawable.isCpaWarningActive() != record.cpaWarning
						|| Math.abs(drawable.getRenderedZoom() - zoom) > ZOOM_EPSILON) {
					drawable.updateAisRenderData(getTileView(), record.cpaWarning,
							tileBox.getZoom() >= START_ZOOM || (selectedMmsi != null && selectedMmsi == mmsi),
							vectorLinesCollection);
					drawable.setRenderedVersion(record.version);
				}
			} else {
				drawable.setSoftwareCpaWarning(record.cpaWarning);
			}
		}
		for (Integer failedMmsi : failed) {
			selection.desired.remove(failedMmsi);
		}

		for (Map.Entry<Integer, Set<Integer>> displacement : selection.safetyDisplacers.entrySet()) {
			int incumbentMmsi = displacement.getKey();
			if (selection.desired.containsKey(incumbentMmsi) || !previous.contains(incumbentMmsi)
					|| !failed.containsAll(displacement.getValue())) {
				continue;
			}
			RenderRecord incumbent;
			synchronized (indexLock) {
				incumbent = objectRecords.get(incumbentMmsi);
			}
			AisObjectDrawable drawable = objectDrawables.get(incumbentMmsi);
			if (incumbent != null && drawable != null && incumbent.hasScreenPoint) {
				selection.desired.put(incumbentMmsi, incumbent);
			}
		}

		Set<Integer> actualDesired = selection.desired.keySet();
		for (Integer mmsi : previous) {
			if (!actualDesired.contains(mmsi)) {
				AisObjectDrawable drawable = objectDrawables.remove(mmsi);
				if (nativeRenderer && drawable != null && markersCollection != null && vectorLinesCollection != null) {
					drawable.clearAisRenderData(markersCollection, vectorLinesCollection);
				}
			}
		}
		renderedRecords = new LinkedHashMap<>(selection.desired);
		peakDrawableCount = Math.max(peakDrawableCount, objectDrawables.size());

		if (LOG.isDebugEnabled()) {
			Set<Integer> actual = new HashSet<>(actualDesired);
			Set<Integer> retained = new HashSet<>(previous);
			retained.retainAll(actual);
			Set<Integer> admitted = new HashSet<>(actual);
			admitted.removeAll(previous);
			Set<Integer> removed = new HashSet<>(previous);
			removed.removeAll(actual);
			int safetyDisplaced = 0;
			for (Integer incumbent : selection.safetyDisplacers.keySet()) {
				if (!actual.contains(incumbent)) {
					safetyDisplaced++;
				}
			}
			int markerCount = markersCollection == null ? 0 : (int) markersCollection.getMarkers().size();
			int lineCount = vectorLinesCollection == null ? 0 : vectorLinesCollection.getLinesCount();
			double elapsedMs = (SystemClock.elapsedRealtimeNanos() - selection.startedNanos) / 1_000_000d;
			LOG.debug("AIS render candidates=" + selection.candidateCount
					+ " projected=" + selection.projected
					+ " visible=" + actual.size()
					+ " retained=" + retained.size()
					+ " admitted=" + admitted.size()
					+ " removed=" + removed.size()
					+ " collisionRejected=" + selection.collisionRejected
					+ " safetyDisplaced=" + safetyDisplaced
					+ " markers=" + markerCount
					+ " lines=" + lineCount
					+ " peak=" + peakDrawableCount
					+ " time=" + String.format("%.1fms", elapsedMs));
		}
	}

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}

	@Override
	public void collectObjectsFromPoint(@NonNull MapSelectionResult result, @NonNull MapSelectionRules rules) {
		if (renderedRecords.isEmpty()) {
			return;
		}
		PointF point = result.getPoint();
		float touchRadius = getScaledTouchRadius(getApplication(),
				result.getTileBox().getDefaultRadiusPoi()) * TOUCH_RADIUS_MULTIPLIER;
		for (RenderRecord record : new ArrayList<>(renderedRecords.values())) {
			if (record.hasScreenPoint && record.iconRect != null) {
				RectF touchRect = new RectF(record.iconRect);
				touchRect.inset(-touchRadius * 0.25f, -touchRadius * 0.25f);
				if (touchRect.contains(point.x, point.y)) {
					result.collect(record.object, this);
				}
			}
		}
	}

	@Override
	public LatLon getObjectLocation(Object object) {
		if (object instanceof AisObject ais) {
			AisLatLon position = ais.getPosition();
			if (position != null) {
				return new LatLon(position.getLatitude(), position.getLongitude());
			}
		}
		return null;
	}

	@Override
	public PointDescription getObjectName(Object object) {
		if (object instanceof AisObject ais) {
			AisObjType objectClass = ais.getObjectClass();
			if (ais.getShipName() != null) {
				return new PointDescription("AIS object", ais.getShipName()
						+ (isSignalLost(ais) ? " (signal lost)" : ""));
			} else if (objectClass == AIS_LANDSTATION) {
				return new PointDescription("AIS object", "Land Station with MMSI " + ais.getMmsi());
			} else if (objectClass == AIS_AIRPLANE) {
				return new PointDescription("AIS object", "Airplane with MMSI " + ais.getMmsi()
						+ (isSignalLost(ais) ? " (signal lost)" : ""));
			} else if (objectClass == AIS_ATON || objectClass == AIS_ATON_VIRTUAL) {
				return new PointDescription("AIS object", "Aid to Navigation");
			} else if (objectClass == AIS_SART) {
				return new PointDescription("AIS object", "SART (Search and Rescue Transmitter)");
			}
			return new PointDescription("AIS object", "AIS object with MMSI " + ais.getMmsi()
					+ (isSignalLost(ais) ? " (signal lost)" : ""));
		}
		return null;
	}

	@Override
	public int getOrder(Object object) {
		return 0;
	}

	@Override
	public void setSelectedObject(Object object) {
		Integer newSelectedMmsi = object instanceof AisObject ais ? ais.getMmsi() : null;
		if (newSelectedMmsi == null ? selectedMmsi == null : newSelectedMmsi.equals(selectedMmsi)) {
			return;
		}
		selectedMmsi = newSelectedMmsi;
		dataDirty = true;
		scheduleFrameRefresh();
	}

	@Override
	public void clearSelectedObject() {
		if (selectedMmsi != null) {
			selectedMmsi = null;
			dataDirty = true;
			scheduleFrameRefresh();
		}
	}

	private boolean isSignalLost(@NonNull AisObject ais) {
		return ais.isLost(plugin.getVesselLostTimeoutInMinutes())
				&& ais.isMovable() && !ais.isVesselAtRest();
	}

	private static final Comparator<RenderRecord> COARSE_COMPARATOR =
			Comparator.comparing((RenderRecord record) -> !record.emergency)
					.thenComparing(record -> !record.isMoving())
					.thenComparing((RenderRecord record) -> -record.lastUpdate)
					.thenComparingInt(record -> record.mmsi);

	private static final Comparator<RenderRecord> INCUMBENT_COMPARATOR =
			Comparator.comparing((RenderRecord record) -> !record.isMoving())
					.thenComparingInt(record -> record.mmsi);

	private static final Comparator<RenderRecord> NEWCOMER_COMPARATOR =
			Comparator.comparing((RenderRecord record) -> !record.isMoving())
					.thenComparing((RenderRecord record) -> -record.lastUpdate)
					.thenComparingInt(record -> record.mmsi);

	private static class RenderRecord {
		final int mmsi;
		final AisObject object;
		final int x31;
		final int y31;
		final long version;
		long bucketKey;
		long lastUpdate;
		long lastCpaUpdate;
		boolean emergency;
		boolean movable;
		boolean cpaWarning;
		int visualState;
		float screenX;
		float screenY;
		boolean hasScreenPoint;
		RectF iconRect;

		RenderRecord(@NonNull AisObject object, int x31, int y31, long version) {
			this.object = object;
			this.mmsi = object.getMmsi();
			this.x31 = x31;
			this.y31 = y31;
			this.version = version;
			this.lastUpdate = object.getLastUpdate();
			this.emergency = object.getObjectClass() == AIS_SART || object.getObjectClass() == AIS_VESSEL_SAR;
			this.movable = object.isMovable();
		}

		void updateSelectionState(@NonNull AisTrackerPlugin plugin) {
			lastUpdate = object.getLastUpdate();
			emergency = object.getObjectClass() == AIS_SART || object.getObjectClass() == AIS_VESSEL_SAR;
			movable = object.isMovable();
			if (object.isVesselAtRest()) {
				visualState = 1;
			} else if (object.isMovable() && object.isLost(plugin.getVesselLostTimeoutInMinutes())) {
				visualState = 2;
			} else {
				visualState = 0;
			}
		}

		boolean isMoving() {
			return visualState == 0 && movable;
		}
	}

	private static class AcceptedRect {
		final int mmsi;
		final RectF rect;
		final boolean safety;

		AcceptedRect(int mmsi, @NonNull RectF rect, boolean safety) {
			this.mmsi = mmsi;
			this.rect = rect;
			this.safety = safety;
		}
	}

	private static class SelectionResult {
		final Map<Integer, RenderRecord> desired = new LinkedHashMap<>();
		final Map<Integer, Set<Integer>> safetyDisplacers = new HashMap<>();
		final int candidateCount;
		final long startedNanos;
		int projected;
		int collisionRejected;

		SelectionResult(int candidateCount, long startedNanos) {
			this.candidateCount = candidateCount;
			this.startedNanos = startedNanos;
		}
	}

	private static class ViewportSignature {
		final double latitude;
		final double longitude;
		final double zoom;
		final float rotation;
		final int width;
		final int height;

		ViewportSignature(@NonNull RotatedTileBox tileBox) {
			latitude = tileBox.getLatitude();
			longitude = tileBox.getLongitude();
			zoom = tileBox.getFullZoom();
			rotation = tileBox.getRotate();
			width = tileBox.getPixWidth();
			height = tileBox.getPixHeight();
		}

		@Override
		public boolean equals(@Nullable Object object) {
			if (!(object instanceof ViewportSignature other)) {
				return false;
			}
			return Math.abs(latitude - other.latitude) < 1e-8
					&& Math.abs(longitude - other.longitude) < 1e-8
					&& Math.abs(zoom - other.zoom) < ZOOM_EPSILON
					&& Math.abs(rotation - other.rotation) < 0.01f
					&& width == other.width && height == other.height;
		}

		@Override
		public int hashCode() {
			return 0;
		}
	}
}
