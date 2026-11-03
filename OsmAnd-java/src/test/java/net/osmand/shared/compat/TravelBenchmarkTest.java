package net.osmand.shared.compat;

import static org.junit.Assert.assertEquals;

import net.osmand.gpx.TravelObfGpxTrackOptimizer;
import net.osmand.shared.gpx.primitives.Track;
import net.osmand.shared.gpx.primitives.TrkSegment;
import net.osmand.shared.gpx.primitives.WptPt;

import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Timing comparison of {@link net.osmand.shared.gpx.TravelObfGpxTrackOptimizer} against
 * {@link TravelObfGpxTrackOptimizer}: joining the pieces a long route was cut into per region,
 * which runs once for every travel track opened and grows with its length.
 *
 * <b>Disabled on purpose.</b> The copy is a straight port, so there is no design decision here to
 * defend; what this answers is whether the work fits the budget the issue sets for opening a route.
 * The cross platform half, which gives the Kotlin/Native number and also times building the points
 * of a track, is in
 * OsmAnd-shared/src/commonTest/kotlin/net/osmand/shared/travel/TravelBenchmarkTest.kt. Neither is a
 * regression gate, and this one asserts only that both optimizers produced the same track. What the
 * copy has to answer is covered by {@link TravelOptimizerCompatTest}, which does run.
 *
 * To measure, remove the {@code @Ignore} below, run it, and put the annotation back:
 * <pre>
 * ./gradlew :OsmAnd-java:test --tests "*TravelBenchmarkTest" -i
 * </pre>
 *
 * A route of 20000 points in 8 pieces, which is about what the 736 km Cesta hrdinov SNP comes to:
 * <pre>
 * join the pieces of a route   java   2.3 ms   copy   2.5 ms   20000 points
 * </pre>
 * The two are the same on the jvm. On Kotlin/Native the same route costs 6.4 ms, see the cross
 * platform benchmark; that is nothing against the budget the issue sets for opening a route.
 */
@Ignore("benchmark, run manually")
public class TravelBenchmarkTest {

	private static final int WARMUP_ROUNDS = 3;
	private static final int MEASURED_ROUNDS = 5;
	private static final int POINTS = 20000;
	private static final int PIECES = 8;
	private static final int OVERLAP = 20;

	@Test
	public void joiningThePiecesOfARoute() {
		long java = Long.MAX_VALUE;
		long copy = Long.MAX_VALUE;
		int points = 0;
		for (int round = 0; round < WARMUP_ROUNDS + MEASURED_ROUNDS; round++) {
			Track javaTrack = routeCutIntoPieces();
			long started = System.nanoTime();
			Track javaResult = TravelObfGpxTrackOptimizer.mergeOverlappedSegmentsAtEdges(javaTrack);
			long javaRound = System.nanoTime() - started;

			Track copyTrack = routeCutIntoPieces();
			started = System.nanoTime();
			Track copyResult = net.osmand.shared.gpx.TravelObfGpxTrackOptimizer.INSTANCE
					.mergeOverlappedSegmentsAtEdges(copyTrack);
			long copyRound = System.nanoTime() - started;

			assertEquals("segments", javaResult.getSegments().size(), copyResult.getSegments().size());
			assertEquals("points", count(javaResult), count(copyResult));
			if (round >= WARMUP_ROUNDS) {
				java = Math.min(java, javaRound);
				copy = Math.min(copy, copyRound);
				points = count(javaResult);
			}
		}
		System.out.printf("join the pieces of a route   java %5.1f ms   copy %5.1f ms   %d points%n",
				java / 1e6, copy / 1e6, points);
	}

	private static int count(Track track) {
		int points = 0;
		for (TrkSegment segment : track.getSegments()) {
			points += segment.getPoints().size();
		}
		return points;
	}

	/** One long route as the indexer leaves it: several pieces, overlapping near each cut. */
	private static Track routeCutIntoPieces() {
		List<WptPt> whole = new ArrayList<>(POINTS);
		for (int i = 0; i < POINTS; i++) {
			whole.add(new WptPt(48.0 + i * 0.0001, 17.0 + i * 0.00005));
		}
		Track track = new Track();
		List<TrkSegment> segments = new ArrayList<>();
		int per = POINTS / PIECES;
		for (int piece = 0; piece < PIECES; piece++) {
			int from = Math.max(0, piece * per - OVERLAP);
			int to = Math.min(POINTS, (piece + 1) * per);
			TrkSegment segment = new TrkSegment();
			for (int i = from; i < to; i++) {
				segment.getPoints().add(new WptPt(whole.get(i).getLat(), whole.get(i).getLon()));
			}
			segments.add(segment);
		}
		// the pieces arrive in the order the files were read, not in the order of the route
		Collections.reverse(segments);
		track.setSegments(segments);
		return track;
	}
}
