package net.osmand.router.sea;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gnu.trove.list.array.TIntArrayList;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.MapIndex;
import net.osmand.binary.BinaryMapIndexReader.SearchFilter;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.binary.BinaryMapIndexReader.TagValuePair;
import net.osmand.data.LatLon;
import net.osmand.util.MapUtils;

/**
 * Shores that a boat may not cross, taken from the map section of an OBF and indexed for segment
 * queries in a local metric projection.
 *
 * Two kinds of shore are loaded and behave the same way once loaded, each piece remembering which of
 * its sides is land: natural=coastline, where OSM puts land on the left of the way, and the rings of
 * water areas (natural=water, waterway=riverbank), where land is outside the ring. Without the second
 * kind a lake such as the IJsselmeer carries no coastline at all and a router sees open water.
 */
public class SeaObstacles {

	public static final double METERS_PER_DEGREE_LAT = 110540;
	private static final int STRIDE = 4;
	private static final int LAND_ON_LEFT = 1;
	private static final int LAND_ON_RIGHT = -1;
	/** A barrier - the edge of a tidal flat - blocks a leg but says nothing about which side is land. */
	private static final int NO_LAND_SIDE = 0;
	/** Smaller water areas - ponds, docks - are left out: they only add segments far from any route. */
	private static final double MIN_WATER_AREA = 1000000;

	private final double baseLat;
	private final double metersPerDegreeLon;

	private final List<double[]> pieces = new ArrayList<>();
	/** Pieces of coastline, as opposed to lakes and tidal flats: the coarse way is planned on these only. */
	private final java.util.BitSet coastPieces = new java.util.BitSet();
	/** The basemap's coastline, for the coarse way: one source, with no gaps between maps. */
	private final List<double[]> coarseCoast = new ArrayList<>();
	private final Set<Long> coarseCoastCells = new HashSet<>();
	private final TIntArrayList pieceLandSide = new TIntArrayList();
	private double[] segments = new double[4096];
	private int[] segmentPiece = new int[1024];
	private int segmentsCount;

	private double cellSize = 500;
	/** Cells of the grid in cell numbers, with a margin around the shores; set by {@link #build()}. */
	private int gridLeft, gridTop, gridWidth, gridHeight;
	/** The box the shores were read in, x1, y1, x2, y2; beyond it nothing is known. Null when not read from maps. */
	private double[] readBox;
	private final Map<Long, TIntArrayList> grid = new HashMap<>();
	private int[] segmentStamp = new int[0];
	private int stamp;

	private double closedRingsArea;
	private int closedRings;

	/** Water areas as piece indexes, the outer ring first and its islands after it. */
	private final List<int[]> waterAreas = new ArrayList<>();

	public SeaObstacles(double baseLat) {
		this.baseLat = baseLat;
		this.metersPerDegreeLon = METERS_PER_DEGREE_LAT * Math.cos(Math.toRadians(baseLat));
	}

	/**
	 * Reads the shores a boat may not cross in the given box: natural=coastline, and the edges of tidal flats
	 * (wetland=tidalflat), which dry out and are crossed by channels only - over the Wadden Sea the straight way is
	 * shorter than the fairway on the map and impassable on the water.
	 *
	 * The zoom picks the stored generalization: a coarse zoom is enough to plan on and keeps the graph small, a
	 * detailed one is what a result should be verified against.
	 */
	public static SeaObstacles readShores(List<BinaryMapIndexReader> readers, double minLat, double minLon,
			double maxLat, double maxLon, int zoom) throws IOException {
		SeaObstacles obstacles = new SeaObstacles((minLat + maxLat) / 2);
		obstacles.readBox = new double[] { obstacles.x(minLon), obstacles.y(minLat), obstacles.x(maxLon), obstacles.y(maxLat) };
		SearchRequest<BinaryMapDataObject> req = BinaryMapIndexReader.buildSearchRequest(
				MapUtils.get31TileNumberX(minLon), MapUtils.get31TileNumberX(maxLon),
				MapUtils.get31TileNumberY(maxLat), MapUtils.get31TileNumberY(minLat), zoom, SHORE_FILTER);
		Set<Long> loaded = new HashSet<>();
		List<double[]> coastPieces = new ArrayList<>();
		for (BinaryMapIndexReader reader : readers) {
			for (MapIndex index : reader.getMapIndexes()) {
				for (BinaryMapDataObject o : reader.searchMapIndex(req, index)) {
					if (!loaded.add(o.getId())) {
						continue; // the same piece at another zoom level or in an overlapping map
					}
					if (hasTag(o, index, "natural", "coastline")) {
						double[] piece = obstacles.toPiece(o.getPointsLength(), o, null);
						coastPieces.add(piece);
						obstacles.accountRing(piece);
						if (reader.isBasemap()) {
							obstacles.coarseCoast.add(piece);
						}
					} else if (o.isArea() && hasTag(o, index, "natural", "water") && !isRiverOrCanal(o, index)) {
						double[] outer = obstacles.toPiece(o.getPointsLength(), o, null);
						if (Math.abs(signedArea(outer)) < MIN_WATER_AREA) {
							continue;
						}
						int[][] inner = o.getPolygonInnerCoordinates();
						double[][] islands = new double[inner == null ? 0 : inner.length][];
						for (int k = 0; k < islands.length; k++) {
							islands[k] = obstacles.toPiece(inner[k].length / 2, null, inner[k]);
						}
						obstacles.addWaterArea(outer, islands);
					} else if (hasTag(o, index, "wetland", "tidalflat")) {
						obstacles.addPiece(obstacles.toPiece(o.getPointsLength(), o, null), NO_LAND_SIDE);
						int[][] inner = o.getPolygonInnerCoordinates();
						for (int k = 0; inner != null && k < inner.length; k++) {
							obstacles.addPiece(obstacles.toPiece(inner[k].length / 2, null, inner[k]), NO_LAND_SIDE);
						}
					}
				}
			}
		}
		// the convention holds per map, so the sign is decided once all rings are known
		int landSide = obstacles.getCoastlineOrientation();
		for (double[] piece : coastPieces) {
			obstacles.addCoastPiece(piece, landSide);
		}
		obstacles.build();
		return obstacles;
	}

	private double[] toPiece(int count, BinaryMapDataObject o, int[] xy) {
		double[] piece = new double[count * 2];
		for (int i = 0; i < count; i++) {
			int x31 = o != null ? o.getPoint31XTile(i) : xy[i * 2];
			int y31 = o != null ? o.getPoint31YTile(i) : xy[i * 2 + 1];
			piece[i * 2] = x(MapUtils.get31LongitudeX(x31));
			piece[i * 2 + 1] = y(MapUtils.get31LatitudeY(y31));
		}
		return piece;
	}

	private static boolean hasTag(BinaryMapDataObject o, MapIndex index, String tag, String value) {
		for (int t : o.getTypes()) {
			TagValuePair p = index.decodeType(t);
			if (p != null && tag.equals(p.tag) && value.equals(p.value)) {
				return true;
			}
		}
		int[] additional = o.getAdditionalTypes();
		for (int i = 0; additional != null && i < additional.length; i++) {
			TagValuePair p = index.decodeType(additional[i]);
			if (p != null && tag.equals(p.tag) && value.equals(p.value)) {
				return true;
			}
		}
		return false;
	}

	/** Rivers and canals are the water network's to route along, not open water to cross. */
	private static boolean isRiverOrCanal(BinaryMapDataObject o, MapIndex index) {
		return hasTag(o, index, "water", "river") || hasTag(o, index, "water", "canal")
				|| hasTag(o, index, "waterway", "riverbank") || hasTag(o, index, "waterway", "canal");
	}

	private static final SearchFilter SHORE_FILTER = new SearchFilter() {
		@Override
		public boolean accept(TIntArrayList types, MapIndex index) {
			for (int i = 0; i < types.size(); i++) {
				TagValuePair p = index.decodeType(types.get(i));
				if (p != null && ("natural".equals(p.tag) && ("coastline".equals(p.value) || "wetland".equals(p.value)
						|| "water".equals(p.value))
						|| "wetland".equals(p.tag) && "tidalflat".equals(p.value))) {
					return true;
				}
			}
			return false;
		}
	};

	/** A coastline piece given as lat, lon, lat, lon…, land on the left as in OSM. */
	public void addCoastline(double... latLon) {
		double[] piece = toPiece(latLon);
		accountRing(piece);
		addCoastPiece(piece, LAND_ON_LEFT);
	}

	/** A ring of a water area given as lat, lon, lat, lon…: land is whatever lies outside it. */
	public void addWaterAreaRing(double... latLon) {
		addWaterArea(toPiece(latLon));
	}

	/** A water area: land outside the outer ring and inside every island. */
	private void addWaterArea(double[] outer, double[]... islands) {
		int[] area = new int[islands.length + 1];
		area[0] = pieces.size();
		addPiece(outer, signedArea(outer) >= 0 ? LAND_ON_RIGHT : LAND_ON_LEFT);
		for (int k = 0; k < islands.length; k++) {
			area[k + 1] = pieces.size();
			addPiece(islands[k], signedArea(islands[k]) >= 0 ? LAND_ON_LEFT : LAND_ON_RIGHT);
		}
		waterAreas.add(area);
	}

	/** Inside a water area and outside its islands, however far the shore. */
	private boolean inWaterArea(double px, double py) {
		for (int[] area : waterAreas) {
			if (area[0] < pieces.size() && pieces.get(area[0]).length >= 6 && inside(pieces.get(area[0]), px, py)) {
				boolean onIsland = false;
				for (int k = 1; k < area.length && !onIsland; k++) {
					onIsland = inside(pieces.get(area[k]), px, py);
				}
				if (!onIsland) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean inside(double[] ring, double px, double py) {
		boolean in = false;
		int n = ring.length;
		for (int i = 0, j = n - 2; i < n; j = i, i += 2) {
			double xi = ring[i], yi = ring[i + 1], xj = ring[j], yj = ring[j + 1];
			if ((yi > py) != (yj > py) && px < (xj - xi) * (py - yi) / (yj - yi) + xi) {
				in = !in;
			}
		}
		return in;
	}

	/** A barrier given as lat, lon, lat, lon…: legs may not cross it, and it does not tell land from water. */
	public void addBarrier(double... latLon) {
		addPiece(toPiece(latLon), NO_LAND_SIDE);
	}

	private double[] toPiece(double[] latLon) {
		double[] piece = new double[latLon.length];
		for (int i = 0; i < latLon.length; i += 2) {
			piece[i] = x(latLon[i + 1]);
			piece[i + 1] = y(latLon[i]);
		}
		return piece;
	}

	private void accountRing(double[] piece) {
		int last = piece.length - 2;
		if (piece.length >= 8 && piece[0] == piece[last] && piece[1] == piece[last + 1]) {
			closedRings++;
			closedRingsArea += signedArea(piece);
		}
	}

	private void addCoastPiece(double[] piece, int landSide) {
		int index = pieces.size();
		addPiece(piece, landSide);
		if (pieces.size() > index) {
			coastPieces.set(index);
		}
	}

	private void addPiece(double[] piece, int landSide) {
		if (piece.length < 4) {
			return;
		}
		int pieceIndex = pieces.size();
		pieces.add(piece);
		pieceLandSide.add(landSide);
		for (int i = 2; i < piece.length; i += 2) {
			addSegment(piece[i - 2], piece[i - 1], piece[i], piece[i + 1], pieceIndex);
		}
	}

	private void addSegment(double x1, double y1, double x2, double y2, int pieceIndex) {
		if ((segmentsCount + 1) * STRIDE > segments.length) {
			double[] bigger = new double[segments.length * 2];
			System.arraycopy(segments, 0, bigger, 0, segments.length);
			segments = bigger;
		}
		if (segmentsCount + 1 > segmentPiece.length) {
			int[] bigger = new int[segmentPiece.length * 2];
			System.arraycopy(segmentPiece, 0, bigger, 0, segmentPiece.length);
			segmentPiece = bigger;
		}
		int p = segmentsCount * STRIDE;
		segments[p] = x1;
		segments[p + 1] = y1;
		segments[p + 2] = x2;
		segments[p + 3] = y2;
		segmentPiece[segmentsCount] = pieceIndex;
		segmentsCount++;
	}

	/** Indexes the segments. Call once after the pieces are added. */
	public void build() {
		double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
		for (int i = 0; i < segmentsCount * STRIDE; i += STRIDE) {
			minX = Math.min(minX, Math.min(segments[i], segments[i + 2]));
			maxX = Math.max(maxX, Math.max(segments[i], segments[i + 2]));
			minY = Math.min(minY, Math.min(segments[i + 1], segments[i + 3]));
			maxY = Math.max(maxY, Math.max(segments[i + 1], segments[i + 3]));
		}
		if (segmentsCount > 0) {
			double span = Math.max(maxX - minX, maxY - minY);
			// about 256 cells across: a box of a continent stays a small grid for the coarse way, the shores of a leg
			// are read in a box of their own
			cellSize = Math.max(250, span / 256);
			gridLeft = (int) Math.floor(minX / cellSize) - GRID_MARGIN;
			gridTop = (int) Math.floor(minY / cellSize) - GRID_MARGIN;
			gridWidth = (int) Math.floor(maxX / cellSize) + GRID_MARGIN - gridLeft + 1;
			gridHeight = (int) Math.floor(maxY / cellSize) + GRID_MARGIN - gridTop + 1;
		}
		coarseCoastCells.clear();
		for (double[] piece : coarseCoast) {
			for (int i = 2; i < piece.length; i += 2) {
				walkCells(piece[i - 2], piece[i - 1], piece[i], piece[i + 1], 0, new CellVisitor() {
					@Override
					public boolean cell(long key) {
						coarseCoastCells.add(key);
						return true;
					}
				});
			}
		}
		grid.clear();
		segmentStamp = new int[segmentsCount];
		stamp = 0;
		for (int i = 0; i < segmentsCount; i++) {
			int p = i * STRIDE;
			final int segment = i;
			walkCells(segments[p], segments[p + 1], segments[p + 2], segments[p + 3], 0, new CellVisitor() {
				@Override
				public boolean cell(long key) {
					TIntArrayList inCell = grid.get(key);
					if (inCell == null) {
						inCell = new TIntArrayList(4);
						grid.put(key, inCell);
					}
					inCell.add(segment);
					return true;
				}
			});
		}
	}

	/** True when the straight leg neither crosses a shore nor passes closer to one than minClearance. */
	public boolean isClear(double x1, double y1, double x2, double y2, final double minClearance) {
		final double[] leg = { x1, y1, x2, y2 };
		final boolean[] clear = { true };
		stamp++;
		walkCells(x1, y1, x2, y2, minClearance, new CellVisitor() {
			@Override
			public boolean cell(long key) {
				TIntArrayList inCell = grid.get(key);
				if (inCell == null) {
					return true;
				}
				for (int i = 0; i < inCell.size(); i++) {
					int segment = inCell.get(i);
					if (segmentStamp[segment] == stamp) {
						continue;
					}
					segmentStamp[segment] = stamp;
					if (ignored(segment)) {
						continue;
					}
					int p = segment * STRIDE;
					double d = segmentDistance(leg[0], leg[1], leg[2], leg[3], segments[p], segments[p + 1],
							segments[p + 2], segments[p + 3]);
					if (d == 0 || d < minClearance) {
						clear[0] = false;
						return false;
					}
				}
				return true;
			}
		});
		return clear[0];
	}

	/** Distance from the leg to the nearest shore, capped at maxDistance. */
	public double clearance(double x1, double y1, double x2, double y2, double maxDistance) {
		final double[] leg = { x1, y1, x2, y2 };
		final double[] min = { maxDistance };
		stamp++;
		walkCells(x1, y1, x2, y2, maxDistance, new CellVisitor() {
			@Override
			public boolean cell(long key) {
				TIntArrayList inCell = grid.get(key);
				if (inCell == null) {
					return true;
				}
				for (int i = 0; i < inCell.size(); i++) {
					int segment = inCell.get(i);
					if (segmentStamp[segment] == stamp) {
						continue;
					}
					segmentStamp[segment] = stamp;
					if (ignored(segment)) {
						continue;
					}
					int p = segment * STRIDE;
					min[0] = Math.min(min[0], segmentDistance(leg[0], leg[1], leg[2], leg[3], segments[p],
							segments[p + 1], segments[p + 2], segments[p + 3]));
				}
				return min[0] > 0;
			}
		});
		return min[0];
	}

	/**
	 * Tells land from water where no shore is near enough to judge by its side: the middle of the sea and the
	 * middle of a continent look the same to a coastline. See {@link BasemapLandTiles}.
	 */
	public interface FarFromShore {
		boolean isLand(double lat, double lon);
	}

	private FarFromShore farFromShore;
	private boolean barriersEnabled = true;

	public void setFarFromShore(FarFromShore farFromShore) {
		this.farFromShore = farFromShore;
	}

	/**
	 * Barriers such as tidal flat edges can be switched off for the last few kilometres to a point that sits on a
	 * flat itself: nothing else reaches it.
	 */
	public void setBarriersEnabled(boolean barriersEnabled) {
		this.barriersEnabled = barriersEnabled;
	}

	private boolean ignored(int segment) {
		return !barriersEnabled && pieceLandSide.get(segmentPiece[segment]) == NO_LAND_SIDE;
	}

	/**
	 * Land or water by the side of the nearest shore segment. With no shore within searchRadius the point is
	 * either far out at sea or far inland, which {@link #setFarFromShore} decides; without it, water.
	 */
	public boolean isLand(LatLon point, double searchRadius) {
		double px = x(point.getLongitude()), py = y(point.getLatitude());
		int segment = nearestSegment(px, py, searchRadius);
		if (segment >= 0) {
			return landSide(segment, px, py) > 0;
		}
		if (inWaterArea(px, py)) {
			return false;
		}
		return farFromShore != null && farFromShore.isLand(point.getLatitude(), point.getLongitude());
	}

	public boolean isLand(LatLon point) {
		return isLand(point, 3000);
	}

	/** Land or water by the side of a shore within searchRadius; null when there is none that near. */
	public Boolean isLandByShore(LatLon point, double searchRadius) {
		double px = x(point.getLongitude()), py = y(point.getLatitude());
		int segment = nearestSegment(px, py, searchRadius);
		return segment < 0 ? null : landSide(segment, px, py) > 0;
	}

	/**
	 * Moves a point that sits on land - a berth in a marina, a pier - onto open water, keeping the
	 * nearest water within maxRadius. Returns the point itself when it is already clear of the shore.
	 */
	public LatLon snapToWater(LatLon point, double clearance, double maxRadius) {
		List<LatLon> candidates = waterCandidates(point, clearance, maxRadius);
		return candidates.isEmpty() ? null : candidates.get(0);
	}

	/**
	 * Water points around a point, nearest first: the point itself when it is clear of the shore, then one
	 * point per ring of growing radius. The nearest water is not always usable - in Vlissingen it is a
	 * dock behind a lock - so a router keeps the farther ones to fall back on.
	 */
	public List<LatLon> waterCandidates(LatLon point, double clearance, double maxRadius) {
		List<LatLon> candidates = new ArrayList<>();
		double px = x(point.getLongitude()), py = y(point.getLatitude());
		boolean onLand = isLand(point, maxRadius);
		if (!onLand && isClear(px, py, px, py, clearance)) {
			candidates.add(point);
		}
		for (double radius = clearance; radius <= maxRadius; radius *= 1.6) {
			for (int i = 0; i < 64; i++) {
				double a = Math.PI * 2 * i / 64;
				double cx = px + Math.cos(a) * radius, cy = py + Math.sin(a) * radius;
				LatLon candidate = latLon(cx, cy);
				if (isLand(candidate, maxRadius) || !isClear(cx, cy, cx, cy, clearance)) {
					continue;
				}
				if (!onLand && !isClear(px, py, cx, cy, 0)) {
					continue; // water on the other side of a spit; from land the shore is crossed anyway
				}
				candidates.add(candidate);
				break;
			}
		}
		return candidates;
	}

	private int nearestSegment(double px, double py, double searchRadius) {
		return nearestSegment(px, py, searchRadius, false);
	}

	private int nearestSegment(double px, double py, double searchRadius, final boolean coastOnly) {
		final double[] state = { searchRadius, px, py };
		final int[] best = { -1 };
		stamp++;
		walkCells(px, py, px, py, searchRadius, new CellVisitor() {
			@Override
			public boolean cell(long key) {
				TIntArrayList inCell = grid.get(key);
				if (inCell == null) {
					return true;
				}
				for (int i = 0; i < inCell.size(); i++) {
					int segment = inCell.get(i);
					if (segmentStamp[segment] == stamp) {
						continue;
					}
					segmentStamp[segment] = stamp;
					if (pieceLandSide.get(segmentPiece[segment]) == NO_LAND_SIDE
							|| (coastOnly && !coastPieces.get(segmentPiece[segment]))) {
						continue; // a barrier cannot tell land from water
					}
					int p = segment * STRIDE;
					double d = pointToSegment(state[1], state[2], segments[p], segments[p + 1], segments[p + 2],
							segments[p + 3]);
					if (d < state[0]) {
						state[0] = d;
						best[0] = segment;
					}
				}
				return true;
			}
		});
		return best[0];
	}

	private double landSide(int segment, double px, double py) {
		int p = segment * STRIDE;
		return cross(segments[p], segments[p + 1], segments[p + 2], segments[p + 3], px, py)
				* pieceLandSide.get(segmentPiece[segment]);
	}

	/** +1 when the loaded rings follow the OSM convention (land on the left), -1 when they are reversed. */
	public int getCoastlineOrientation() {
		return closedRingsArea >= 0 ? LAND_ON_LEFT : LAND_ON_RIGHT;
	}

	public int getClosedRings() {
		return closedRings;
	}

	public int getSegmentsCount() {
		return segmentsCount;
	}

	public List<double[]> getPieces() {
		return pieces;
	}

	public int getPieceLandSide(int pieceIndex) {
		return pieceLandSide.get(pieceIndex);
	}

	public double x(double lon) {
		return lon * metersPerDegreeLon;
	}

	public double y(double lat) {
		return lat * METERS_PER_DEGREE_LAT;
	}

	public double lon(double x) {
		return x / metersPerDegreeLon;
	}

	public double lat(double y) {
		return y / METERS_PER_DEGREE_LAT;
	}

	public LatLon latLon(double x, double y) {
		return new LatLon(lat(y), lon(x));
	}

	public double getBaseLat() {
		return baseLat;
	}

	interface CellVisitor {
		/** Returns false to stop the walk. */
		boolean cell(long key);
	}

	/**
	 * Visits the grid cells a segment passes through, widened by margin. Walking the line rather than
	 * its bounding box is what keeps a 150 km leg at a few hundred cells.
	 */
	private void walkCells(double x1, double y1, double x2, double y2, double margin, CellVisitor visitor) {
		// every cell the line passes through, in order (Amanatides and Woo): sampling one cell per step would miss
		// the corners a line clips between samples
		int ring = (int) Math.ceil(margin / cellSize);
		int cx = (int) Math.floor(x1 / cellSize), cy = (int) Math.floor(y1 / cellSize);
		int ex = (int) Math.floor(x2 / cellSize), ey = (int) Math.floor(y2 / cellSize);
		double dx = x2 - x1, dy = y2 - y1;
		int stepX = dx > 0 ? 1 : -1, stepY = dy > 0 ? 1 : -1;
		double nextX = dx == 0 ? Double.POSITIVE_INFINITY : ((stepX > 0 ? cx + 1 : cx) * cellSize - x1) / dx;
		double nextY = dy == 0 ? Double.POSITIVE_INFINITY : ((stepY > 0 ? cy + 1 : cy) * cellSize - y1) / dy;
		double deltaX = dx == 0 ? Double.POSITIVE_INFINITY : cellSize / Math.abs(dx);
		double deltaY = dy == 0 ? Double.POSITIVE_INFINITY : cellSize / Math.abs(dy);
		int steps = Math.abs(ex - cx) + Math.abs(ey - cy);
		for (int step = 0; ; step++) {
			if (!visitRing(cx, cy, ring, visitor)) {
				return;
			}
			if ((cx == ex && cy == ey) || step >= steps) {
				return;
			}
			if (nextX < nextY) {
				nextX += deltaX;
				cx += stepX;
			} else if (nextY < nextX) {
				nextY += deltaY;
				cy += stepY;
			} else {
				// through a corner exactly: both side cells touch the line
				if (!visitRing(cx + stepX, cy, ring, visitor) || !visitRing(cx, cy + stepY, ring, visitor)) {
					return;
				}
				nextX += deltaX;
				nextY += deltaY;
				cx += stepX;
				cy += stepY;
				step++;
			}
		}
	}

	private static boolean visitRing(int cx, int cy, int ring, CellVisitor visitor) {
		for (int dx = -ring; dx <= ring; dx++) {
			for (int dy = -ring; dy <= ring; dy++) {
				if (!visitor.cell(cellKey(cx + dx, cy + dy))) {
					return false;
				}
			}
		}
		return true;
	}

	private static final int GRID_MARGIN = 4;
	private static final double SHORE_CELL_COST = 4;
	/** Cells of an empty region whose squares vote on land or sea. */
	private static final int REGION_VOTES = 16;
	private static final byte CELL_PENDING = 4;
	private static final byte CELL_SHORE = 1, CELL_WATER = 2, CELL_LAND = 3;

	/** The size of a grid cell: a coastline generalized at a low zoom is less than this off the true one. */
	public double getCellSize() {
		return cellSize;
	}

	/**
	 * A coarse way from one water point to another over the grid cells, to be refined by the shore geometry. A cell
	 * with shores in it may hold a passage; an empty region of cells - no shore inside - is all water or all land, so
	 * one {@link #isLand} per region decides it (the basemap's squares when the region is far from any shore). The
	 * closed cells count as land: passages the refinement could not get through. Null when there is no grid or no
	 * way over the cells.
	 */
	public CoarseWay coarseWay(double x1, double y1, double x2, double y2, Set<Long> closed) {
		if (gridWidth <= 0 || gridHeight <= 0) {
			return null;
		}
		// the shores' cells and both points, which can lie far out at sea beyond any shore
		int left = Math.min(gridLeft, Math.min(cell(x1), cell(x2)) - GRID_MARGIN);
		int top = Math.min(gridTop, Math.min(cell(y1), cell(y2)) - GRID_MARGIN);
		int w = Math.max(gridLeft + gridWidth, Math.max(cell(x1), cell(x2)) + GRID_MARGIN + 1) - left;
		int h = Math.max(gridTop + gridHeight, Math.max(cell(y1), cell(y2)) + GRID_MARGIN + 1) - top;
		byte[] state = new byte[w * h];
		for (int i = 0; i < w; i++) {
			for (int j = 0; j < h; j++) {
				if (hasCoast(cellKey(left + i, top + j))) {
					state[i + j * w] = CELL_SHORE;
				}
			}
		}
		// beyond the box the shores were read in nothing is known: a wall, or land and sea would meet around it
		for (int i = 0; readBox != null && i < w; i++) {
			for (int j = 0; j < h; j++) {
				double cx = (left + i + 0.5) * cellSize, cy = (top + j + 0.5) * cellSize;
				if (state[i + j * w] == 0 && (cx < Math.min(readBox[0], readBox[2]) || cx > Math.max(readBox[0], readBox[2])
						|| cy < Math.min(readBox[1], readBox[3]) || cy > Math.max(readBox[1], readBox[3]))) {
					state[i + j * w] = CELL_LAND;
				}
			}
		}
		// an empty region - no coastline inside - is all land or all sea; the basemap's squares of a few cells spread
		// over it vote, so one square at its edge that is part sea cannot turn northern Germany into sea
		int[] queue = new int[w * h];
		for (int start = 0; start < w * h; start++) {
			if (state[start] != 0) {
				continue;
			}
			int head = 0, tail = 0;
			queue[tail++] = start;
			state[start] = CELL_PENDING;
			while (head < tail) {
				int c = queue[head++], ci = c % w, cj = c / w;
				for (int k = 0; k < 4; k++) {
					int ni = ci + (k == 0 ? 1 : k == 1 ? -1 : 0), nj = cj + (k == 2 ? 1 : k == 3 ? -1 : 0);
					if (ni >= 0 && nj >= 0 && ni < w && nj < h && state[ni + nj * w] == 0) {
						state[ni + nj * w] = CELL_PENDING;
						queue[tail++] = ni + nj * w;
					}
				}
			}
			int land = 0, votes = Math.min(REGION_VOTES, tail);
			for (int v = 0; v < votes; v++) {
				int c = queue[(int) ((long) tail * v / votes)];
				LatLon centre = latLon((left + c % w + 0.5) * cellSize, (top + c / w + 0.5) * cellSize);
				boolean isLand = farFromShore != null ? farFromShore.isLand(centre.getLatitude(), centre.getLongitude())
						: isLandByCoast(centre);
				land += isLand ? 1 : 0;
			}
			byte region = land * 2 > votes ? CELL_LAND : CELL_WATER;
			for (int k = 0; k < tail; k++) {
				state[queue[k]] = region;
			}
		}
		for (long key : closed) {
			int i = (int) (key >> 32) - left, j = (int) key - top;
			if (i >= 0 && j >= 0 && i < w && j < h) {
				state[i + j * w] = CELL_LAND;
			}
		}
		int from = cell(x1) - left + (cell(y1) - top) * w, to = cell(x2) - left + (cell(y2) - top) * w;
		state[from] = state[from] == CELL_LAND ? CELL_SHORE : state[from];
		state[to] = state[to] == CELL_LAND ? CELL_SHORE : state[to];
		// A* over the cells that are not land, eight neighbours; a cell with shores may be land all through - a chain
		// of estuaries across Cornwall - so open water is preferred and shores are crossed only where there is no other way
		double[] g = new double[w * h];
		int[] parent = new int[w * h];
		java.util.Arrays.fill(g, Double.POSITIVE_INFINITY);
		g[from] = 0;
		parent[from] = -1;
		int ti = to % w, tj = to / w;
		java.util.PriorityQueue<double[]> open = new java.util.PriorityQueue<>((a, b) -> Double.compare(a[0], b[0]));
		open.add(new double[] { Math.hypot(from % w - ti, from / w - tj), from });
		boolean found = false;
		while (!open.isEmpty()) {
			double[] next = open.poll();
			int c = (int) next[1], ci = c % w, cj = c / w;
			if (c == to) {
				found = true;
				break;
			}
			if (next[0] - Math.hypot(ci - ti, cj - tj) > g[c] + 1e-9) {
				continue;
			}
			for (int di = -1; di <= 1; di++) {
				for (int dj = -1; dj <= 1; dj++) {
					int ni = ci + di, nj = cj + dj, n = ni + nj * w;
					if ((di == 0 && dj == 0) || ni < 0 || nj < 0 || ni >= w || nj >= h || state[n] == CELL_LAND) {
						continue;
					}
					double d = g[c] + Math.hypot(di, dj) * (state[n] == CELL_SHORE ? SHORE_CELL_COST : 1);
					if (d < g[n]) {
						g[n] = d;
						parent[n] = c;
						open.add(new double[] { d + Math.hypot(ni - ti, nj - tj), n });
					}
				}
			}
		}
		if (!found) {
			return null;
		}
		CoarseWay way = new CoarseWay();
		for (int c = to; c != -1; c = parent[c]) {
			way.x.add(0, (left + c % w + 0.5) * cellSize);
			way.y.add(0, (top + c / w + 0.5) * cellSize);
			way.water.add(0, state[c] == CELL_WATER);
			way.cells.add(0, cellKey(left + c % w, top + c / w));
		}
		return way;
	}

	private boolean hasCoast(long key) {
		if (!coarseCoast.isEmpty()) {
			return coarseCoastCells.contains(key);
		}
		TIntArrayList inCell = grid.get(key);
		for (int i = 0; inCell != null && i < inCell.size(); i++) {
			if (coastPieces.get(segmentPiece[inCell.get(i)])) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Land or sea by the coastline alone, for the coarse way: a lake is land to it, and a point in one must not make
	 * the region around it sea.
	 */
	private boolean isLandByCoast(LatLon point) {
		double px = x(point.getLongitude()), py = y(point.getLatitude());
		int segment = nearestSegment(px, py, 3000, true);
		if (segment >= 0) {
			return landSide(segment, px, py) > 0;
		}
		return farFromShore != null && farFromShore.isLand(point.getLatitude(), point.getLongitude());
	}

	/**
	 * The grid cells with shores in the box of two points: when no route was found between them over water, none of
	 * these cells has a passage the coarse way may take.
	 */
	public Set<Long> shoreCellsBetween(double x1, double y1, double x2, double y2) {
		Set<Long> cells = new HashSet<>();
		for (int i = cell(Math.min(x1, x2)); i <= cell(Math.max(x1, x2)); i++) {
			for (int j = cell(Math.min(y1, y2)); j <= cell(Math.max(y1, y2)); j++) {
				if (hasCoast(cellKey(i, j))) {
					cells.add(cellKey(i, j));
				}
			}
		}
		return cells;
	}

	/** Cell centres of a coarse way, from its start to its end; open water or with shores. */
	public static class CoarseWay {
		public final List<Double> x = new ArrayList<>(), y = new ArrayList<>();
		public final List<Boolean> water = new ArrayList<>();
		public final List<Long> cells = new ArrayList<>();

		public int size() {
			return x.size();
		}
	}

	private int cell(double coordinate) {
		return (int) Math.floor(coordinate / cellSize);
	}

	private static long cellKey(int cx, int cy) {
		return (((long) cx) << 32) | (cy & 0xffffffffL);
	}

	private static double signedArea(double[] piece) {
		double area = 0;
		for (int i = 2; i < piece.length; i += 2) {
			area += piece[i - 2] * piece[i + 1] - piece[i] * piece[i - 1];
		}
		return area / 2;
	}

	public static double cross(double ax, double ay, double bx, double by, double px, double py) {
		return (bx - ax) * (py - ay) - (by - ay) * (px - ax);
	}

	public static boolean intersects(double ax, double ay, double bx, double by, double cx, double cy, double dx,
			double dy) {
		double o1 = cross(ax, ay, bx, by, cx, cy), o2 = cross(ax, ay, bx, by, dx, dy);
		double o3 = cross(cx, cy, dx, dy, ax, ay), o4 = cross(cx, cy, dx, dy, bx, by);
		if (o1 == 0 && o2 == 0) {
			return Math.max(Math.min(ax, bx), Math.min(cx, dx)) <= Math.min(Math.max(ax, bx), Math.max(cx, dx))
					&& Math.max(Math.min(ay, by), Math.min(cy, dy)) <= Math.min(Math.max(ay, by), Math.max(cy, dy));
		}
		return o1 * o2 <= 0 && o3 * o4 <= 0;
	}

	public static double pointToSegment(double px, double py, double ax, double ay, double bx, double by) {
		double dx = bx - ax, dy = by - ay, len = dx * dx + dy * dy;
		double t = len == 0 ? 0 : Math.max(0, Math.min(1, ((px - ax) * dx + (py - ay) * dy) / len));
		return Math.hypot(px - ax - t * dx, py - ay - t * dy);
	}

	/** Distance between two segments; without a crossing the closest pair always involves an endpoint. */
	public static double segmentDistance(double ax, double ay, double bx, double by, double cx, double cy, double dx,
			double dy) {
		if (intersects(ax, ay, bx, by, cx, cy, dx, dy)) {
			return 0;
		}
		return Math.min(Math.min(pointToSegment(ax, ay, cx, cy, dx, dy), pointToSegment(bx, by, cx, cy, dx, dy)),
				Math.min(pointToSegment(cx, cy, ax, ay, bx, by), pointToSegment(dx, dy, ax, ay, bx, by)));
	}
}
