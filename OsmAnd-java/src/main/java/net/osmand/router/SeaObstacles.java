package net.osmand.router;

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

	private final double baseLat;
	private final double metersPerDegreeLon;

	private final List<double[]> pieces = new ArrayList<>();
	private final TIntArrayList pieceLandSide = new TIntArrayList();
	private double[] segments = new double[4096];
	private int[] segmentPiece = new int[1024];
	private int segmentsCount;

	private double cellSize = 500;
	private final Map<Long, TIntArrayList> grid = new HashMap<>();
	private int[] segmentStamp = new int[0];
	private int stamp;

	private double closedRingsArea;
	private int closedRings;

	public SeaObstacles(double baseLat) {
		this.baseLat = baseLat;
		this.metersPerDegreeLon = METERS_PER_DEGREE_LAT * Math.cos(Math.toRadians(baseLat));
	}

	/**
	 * Reads natural=coastline of the given box. The zoom picks the stored generalization: a coarse zoom
	 * is enough to plan on and keeps the graph small, a detailed one is what a result should be verified
	 * against.
	 */
	public static SeaObstacles readCoastline(List<BinaryMapIndexReader> readers, double minLat, double minLon,
			double maxLat, double maxLon, int zoom) throws IOException {
		SeaObstacles obstacles = new SeaObstacles((minLat + maxLat) / 2);
		SearchRequest<BinaryMapDataObject> req = BinaryMapIndexReader.buildSearchRequest(
				MapUtils.get31TileNumberX(minLon), MapUtils.get31TileNumberX(maxLon),
				MapUtils.get31TileNumberY(maxLat), MapUtils.get31TileNumberY(minLat), zoom, COASTLINE_FILTER);
		Set<Long> loaded = new HashSet<>();
		List<double[]> coastPieces = new ArrayList<>();
		for (BinaryMapIndexReader reader : readers) {
			for (MapIndex index : reader.getMapIndexes()) {
				for (BinaryMapDataObject o : reader.searchMapIndex(req, index)) {
					if (!loaded.add(o.getId())) {
						continue; // the same piece at another zoom level or in an overlapping map
					}
					double[] piece = new double[o.getPointsLength() * 2];
					for (int i = 0; i < o.getPointsLength(); i++) {
						piece[i * 2] = obstacles.x(MapUtils.get31LongitudeX(o.getPoint31XTile(i)));
						piece[i * 2 + 1] = obstacles.y(MapUtils.get31LatitudeY(o.getPoint31YTile(i)));
					}
					coastPieces.add(piece);
					obstacles.accountRing(piece);
				}
			}
		}
		// the convention holds per map, so the sign is decided once all rings are known
		int landSide = obstacles.getCoastlineOrientation();
		for (double[] piece : coastPieces) {
			obstacles.addPiece(piece, landSide);
		}
		obstacles.build();
		return obstacles;
	}

	private static final SearchFilter COASTLINE_FILTER = new SearchFilter() {
		@Override
		public boolean accept(TIntArrayList types, MapIndex index) {
			for (int i = 0; i < types.size(); i++) {
				TagValuePair p = index.decodeType(types.get(i));
				if (p != null && "natural".equals(p.tag) && "coastline".equals(p.value)) {
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
		addPiece(piece, LAND_ON_LEFT);
	}

	/** A ring of a water area given as lat, lon, lat, lon…: land is whatever lies outside it. */
	public void addWaterAreaRing(double... latLon) {
		double[] piece = toPiece(latLon);
		addPiece(piece, signedArea(piece) >= 0 ? LAND_ON_RIGHT : LAND_ON_LEFT);
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
			cellSize = Math.max(250, Math.min(5000, span / 256));
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
	 * Land or water by the side of the nearest shore segment. Water is also the answer when no shore is
	 * within searchRadius, which is the open sea.
	 */
	public boolean isLand(LatLon point, double searchRadius) {
		int segment = nearestSegment(x(point.getLongitude()), y(point.getLatitude()), searchRadius);
		return segment >= 0 && landSide(segment, x(point.getLongitude()), y(point.getLatitude())) > 0;
	}

	public boolean isLand(LatLon point) {
		return isLand(point, 3000);
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
		int ring = (int) Math.ceil(margin / cellSize);
		int cx1 = (int) Math.floor(x1 / cellSize), cy1 = (int) Math.floor(y1 / cellSize);
		int cx2 = (int) Math.floor(x2 / cellSize), cy2 = (int) Math.floor(y2 / cellSize);
		int steps = Math.max(Math.abs(cx2 - cx1), Math.abs(cy2 - cy1));
		for (int s = 0; s <= steps; s++) {
			double t = steps == 0 ? 0 : (double) s / steps;
			int cx = (int) Math.floor((x1 + (x2 - x1) * t) / cellSize);
			int cy = (int) Math.floor((y1 + (y2 - y1) * t) / cellSize);
			for (int dx = -ring; dx <= ring; dx++) {
				for (int dy = -ring; dy <= ring; dy++) {
					if (!visitor.cell(cellKey(cx + dx, cy + dy))) {
						return;
					}
				}
			}
		}
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
