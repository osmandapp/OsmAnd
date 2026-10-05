package net.osmand.shared.compat;

import static org.junit.Assert.assertEquals;

import net.osmand.gpx.TravelObfGpxTrackOptimizer;
import net.osmand.shared.gpx.primitives.Track;
import net.osmand.shared.gpx.primitives.TrkSegment;
import net.osmand.shared.gpx.primitives.WptPt;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * {@link net.osmand.shared.gpx.TravelObfGpxTrackOptimizer} is a copy of
 * {@link TravelObfGpxTrackOptimizer}, and both work on the same shared gpx types, so the same track
 * can be handed to each and the two results compared point for point.
 *
 * This is what joins a route back together after the indexer cut it up: a route crossing a region
 * boundary is written once per file, the pieces overlap by a few points near the edge, and they
 * come back in whatever order the files were read in.
 */
public class TravelOptimizerCompatTest {

	/** The pieces arrive out of order - A D C B E - and have to end up as one segment. */
	@Test
	public void outOfOrderPiecesAreJoinedTheSame() {
		double[][] a = line(48.000, 17.000, 48.010, 17.000, 6);
		double[][] b = line(48.010, 17.000, 48.020, 17.000, 6);
		double[][] c = line(48.020, 17.000, 48.030, 17.000, 6);
		double[][] d = line(48.030, 17.000, 48.040, 17.000, 6);
		double[][] e = line(48.040, 17.000, 48.050, 17.000, 6);
		assertSegments("A D C B E", 1, assertSame("A D C B E", a, d, c, b, e));
		assertSegments("A B C D E", 1, assertSame("A B C D E", a, b, c, d, e));
		assertSegments("E D C B A", 1, assertSame("E D C B A", e, d, c, b, a));
	}

	/**
	 * Two pieces of one route from two files, overlapping near the boundary. The overlap is the
	 * reason a plain concatenation gives four segments where there should be one.
	 */
	@Test
	public void overlappingPiecesAtARegionBoundaryAreTheSame() {
		// one route, and the two files it was written to, sharing the points near the boundary
		double[][] whole = line(48.000, 17.000, 48.040, 17.000, 41);
		double[][] west = part(whole, 0, 20);
		double[][] east = part(whole, 18, 40);
		assertSegments("overlap of three points", 1,
				assertSame("overlap of three points", west, east));

		double[][] longEast = part(whole, 10, 40);
		assertSegments("overlap of eleven points", 1,
				assertSame("overlap of eleven points", west, longEast));

		// The ends of one piece land between two points of the other rather than on them. Only the
		// ends are recognised that way, so how much is trimmed depends on the shape; no count is
		// claimed here, only that both readers do the same thing with it.
		assertSame("displaced overlap", west, shift(east, 0.00003));

		// the pieces meet exactly, without sharing anything
		assertSegments("no overlap at all", 1,
				assertSame("no overlap at all", part(whole, 0, 20), part(whole, 20, 40)));
	}

	/** A closed loop must not be joined onto anything, or the route swallows its own tail. */
	@Test
	public void closedLoopsAreTheSame() {
		double[][] loop = {
				{48.000, 17.000}, {48.010, 17.000}, {48.010, 17.010}, {48.000, 17.010}, {48.000, 17.000}
		};
		double[][] tail = line(48.000, 17.000, 48.000, 16.990, 6);
		assertSegments("loop alone", 1, assertSame("loop alone", loop));
		assertSegments("loop and a tail", 2, assertSame("loop and a tail", loop, tail));
		assertSegments("tail and a loop", 2, assertSame("tail and a loop", tail, loop));
	}

	/** The shapes the optimizer has to refuse to join, and the empty cases. */
	@Test
	public void unjoinablePiecesAreTheSame() {
		double[][] here = line(48.000, 17.000, 48.010, 17.000, 6);
		double[][] faraway = line(50.000, 20.000, 50.010, 20.000, 6);
		assertSegments("two apart", 2, assertSame("two apart", here, faraway));
		assertSegments("nothing", 0, assertSame("nothing"));
		assertSame("one empty piece", new double[0][]);
		assertSame("a piece and an empty one", here, new double[0][]);
		assertSame("one point", new double[][] {{48.0, 17.0}});
		assertSame("two points", new double[][] {{48.0, 17.0}, {48.0, 17.0}});
	}

	/** Random tracks, where the pieces touch, overlap, reverse or miss each other. */
	@Test
	public void randomTracksAreTheSame() {
		Random random = new Random(19);
		for (int round = 0; round < 400; round++) {
			int pieces = 1 + random.nextInt(5);
			double[][][] segments = new double[pieces][][];
			double lat = 48 + random.nextDouble();
			double lon = 17 + random.nextDouble();
			for (int i = 0; i < pieces; i++) {
				int points = 2 + random.nextInt(8);
				double step = 0.0005 + random.nextDouble() * 0.002;
				double endLat = lat + step * points;
				double[][] piece = line(lat, lon, endLat, lon, points);
				if (random.nextBoolean()) {
					piece = reverse(piece);
				}
				if (random.nextInt(4) == 0) {
					piece = shift(piece, (random.nextDouble() - 0.5) * 0.0001);
				}
				segments[i] = piece;
				// the next piece starts at the end of this one, a little before it, or elsewhere
				int how = random.nextInt(3);
				lat = how == 0 ? endLat : how == 1 ? endLat - step * 2 : lat + 1 + random.nextDouble();
			}
			assertSame("round " + round, segments);
		}
	}

	/** Runs both and returns java's result, so a case can also say what it expects to happen. */
	private static Track assertSame(String m, double[][]... segments) {
		Track jresult = TravelObfGpxTrackOptimizer.mergeOverlappedSegmentsAtEdges(track(segments));
		Track kresult = net.osmand.shared.gpx.TravelObfGpxTrackOptimizer.INSTANCE
				.mergeOverlappedSegmentsAtEdges(track(segments));
		assertEquals(m, describe(jresult), describe(kresult));
		return jresult;
	}

	/** Both agreeing is not enough: the cases below also say what the answer should be. */
	private static void assertSegments(String m, int expected, Track track) {
		assertEquals(m + " segments", expected, track.getSegments().size());
	}

	private static Track track(double[][][] segments) {
		Track track = new Track();
		List<TrkSegment> list = new ArrayList<>();
		for (double[][] segment : segments) {
			TrkSegment trkSegment = new TrkSegment();
			for (double[] point : segment) {
				trkSegment.getPoints().add(new WptPt(point[0], point[1]));
			}
			list.add(trkSegment);
		}
		track.setSegments(list);
		return track;
	}

	private static String describe(Track track) {
		StringBuilder sb = new StringBuilder();
		sb.append(track.getSegments().size()).append(" segments\n");
		for (TrkSegment segment : track.getSegments()) {
			sb.append(" segment of ").append(segment.getPoints().size()).append(":");
			for (WptPt point : segment.getPoints()) {
				sb.append(' ').append(round(point.getLat())).append(',').append(round(point.getLon()));
			}
			sb.append('\n');
		}
		return sb.toString();
	}

	private static String round(double value) {
		return String.format(java.util.Locale.US, "%.7f", value);
	}

	/** The points of [whole] from [from] to [to], both included, as one piece. */
	private static double[][] part(double[][] whole, int from, int to) {
		double[][] points = new double[to - from + 1][2];
		for (int i = from; i <= to; i++) {
			points[i - from] = whole[i];
		}
		return points;
	}

	/** [count] points evenly spaced from one corner to the other, ends included. */
	private static double[][] line(double fromLat, double fromLon, double toLat, double toLon, int count) {
		double[][] points = new double[count][2];
		for (int i = 0; i < count; i++) {
			double k = count == 1 ? 0 : (double) i / (count - 1);
			points[i][0] = fromLat + (toLat - fromLat) * k;
			points[i][1] = fromLon + (toLon - fromLon) * k;
		}
		return points;
	}

	private static double[][] shift(double[][] points, double by) {
		double[][] shifted = new double[points.length][2];
		for (int i = 0; i < points.length; i++) {
			shifted[i][0] = points[i][0] + by;
			shifted[i][1] = points[i][1] + by;
		}
		return shifted;
	}

	private static double[][] reverse(double[][] points) {
		double[][] reversed = new double[points.length][2];
		for (int i = 0; i < points.length; i++) {
			reversed[i] = points[points.length - 1 - i];
		}
		return reversed;
	}
}
