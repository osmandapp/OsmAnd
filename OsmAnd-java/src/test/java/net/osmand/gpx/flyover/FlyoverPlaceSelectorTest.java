package net.osmand.gpx.flyover;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.osmand.gpx.flyover.FlyoverPlaceSelector.Candidate;
import net.osmand.gpx.flyover.FlyoverPlaceSelector.Selection;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class FlyoverPlaceSelectorTest {

	private static final double LAT = 50.0;
	private static final double LON = 10.0;
	private static final double LON_PER_METER = 1 / (111_320d * Math.cos(Math.toRadians(LAT)));
	private static final double LAT_PER_METER = 1 / 111_320d;
	private static final String IMAGE = "https://example.org/photo.jpg";

	/** 10 km straight track heading east. */
	private static FlyoverTrackGeometry track() {
		return FlyoverCameraPathTest.eastwardTrack(101, 100);
	}

	/** {@code IMAGE} stands for "a photograph of this place": each place gets its own file. */
	private static Candidate at(double alongMeters, double offsetMeters, int rank, String name, String image) {
		String url = IMAGE.equals(image) ? "https://example.org/" + name.replace(' ', '_') + ".jpg" : image;
		return new Candidate(LAT + offsetMeters * LAT_PER_METER, LON + alongMeters * LON_PER_METER, rank, name, url);
	}

	@Test
	public void projectionFindsDistanceAlongAndFromTrack() {
		double[] projection = track().project(LAT + 50 * LAT_PER_METER, LON + 2000 * LON_PER_METER);
		assertEquals(2000, projection[0], 2);
		assertEquals(50, projection[1], 1);
		double[] beyondEnd = track().project(LAT, LON + 12_000 * LON_PER_METER);
		assertEquals(10_000, beyondEnd[0], 2);
		assertEquals(2000, beyondEnd[1], 5);
	}

	@Test
	public void keepsNearbyRankedPlacesSpreadAlongTheTrack() {
		List<Candidate> candidates = new ArrayList<>();
		candidates.add(at(7000, 100, 500, "Castle", IMAGE));
		candidates.add(at(2000, 50, 900, "Cathedral", IMAGE));
		candidates.add(at(2100, 20, 800, "Old Bridge", IMAGE));       // too close to the cathedral
		candidates.add(at(5000, 3000, 999, "Far Away Hill", IMAGE));  // outside the corridor
		candidates.add(at(8000, 10, 950, "No Photo Park", null));     // nothing to show
		candidates.add(at(9000, 10, 700, "cathedral", IMAGE));        // same name again

		List<Selection> result = FlyoverPlaceSelector.select(track(), candidates, 10, 300, 500);

		assertEquals(2, result.size());
		assertEquals("Cathedral", result.get(0).candidate.name);
		assertEquals("Castle", result.get(1).candidate.name);
		assertEquals(0.2, result.get(0).progress, 0.001);
		assertEquals(0.7, result.get(1).progress, 0.001);
	}

	@Test
	public void limitsTheNumberOfPlacesToTheMostPopular() {
		List<Candidate> candidates = new ArrayList<>();
		for (int i = 0; i < 12; i++) {
			candidates.add(at(400 + i * 800, 30, 100 + i, "Place " + i, IMAGE));
		}
		List<Selection> result = FlyoverPlaceSelector.select(track(), candidates, 10, 300, 500);
		assertEquals(10, result.size());
		double previous = -1;
		for (Selection selection : result) {
			assertTrue("sorted along the track", selection.distanceAlong > previous);
			previous = selection.distanceAlong;
		}
		// the two least popular places are the ones dropped
		for (Selection selection : result) {
			assertTrue(selection.candidate.rank >= 102);
		}
	}

	@Test
	public void spacingPrefersTheMorePopularOfTwoNeighbours() {
		List<Candidate> candidates = new ArrayList<>();
		candidates.add(at(3000, 10, 100, "Minor", IMAGE));
		candidates.add(at(3200, 10, 900, "Major", IMAGE));
		List<Selection> result = FlyoverPlaceSelector.select(track(), candidates, 10, 300, 500);
		assertEquals(1, result.size());
		assertEquals("Major", result.get(0).candidate.name);
	}

	@Test
	public void placesAreSpreadAlongTheWholeTrack() {
		List<Candidate> candidates = new ArrayList<>();
		// ten popular places within 100 m, all on the first third of the track
		for (int i = 0; i < 10; i++) {
			candidates.add(at(300 + i * 300, 50, 500 + i, "Near " + i, IMAGE));
		}
		// two modest places further from the track on the last third, and one too far
		candidates.add(at(7500, 400, 100, "Mid", IMAGE));
		candidates.add(at(9500, 900, 100, "Far", IMAGE));
		candidates.add(at(8500, 1200, 999, "Too far", IMAGE));
		List<Selection> result = FlyoverPlaceSelector.selectSpread(track(), candidates, 6);
		assertEquals(6, result.size());
		// the end of the track is covered even though its places are further away and less popular
		assertEquals("Mid", result.get(4).candidate.name);
		assertEquals("Far", result.get(5).candidate.name);
		// the remaining capacity goes to the most popular near places
		assertEquals("Near 9", result.get(3).candidate.name);
		double previous = -1;
		for (Selection selection : result) {
			assertTrue("sorted along the track", selection.distanceAlong > previous);
			previous = selection.distanceAlong;
		}
	}

	@Test
	public void nearerPlacesWinWithinAStretch() {
		List<Candidate> candidates = new ArrayList<>();
		candidates.add(at(2000, 80, 100, "Close and modest", IMAGE));
		candidates.add(at(2600, 400, 900, "Further and famous", IMAGE));
		List<Selection> result = FlyoverPlaceSelector.selectSpread(track(), candidates, 2);
		// both fit into two stretches; with one stretch the closer place is taken first
		assertEquals(2, result.size());
		result = FlyoverPlaceSelector.selectSpread(track(), candidates, 1);
		assertEquals(1, result.size());
		assertEquals("Close and modest", result.get(0).candidate.name);
	}

	@Test
	public void mapsAndDrawingsAreNotPhotographs() {
		assertTrue(FlyoverPlaceSelector.isPhotograph("https://example.org/images/a/ab/Church.jpg?width=1280"));
		assertTrue(FlyoverPlaceSelector.isPhotograph("https://example.org/images/a/ab/Hill.JPEG"));
		assertFalse(FlyoverPlaceSelector.isPhotograph("https://example.org/images/7/7f/Wy%C5%BCyna_Krakowsko-Cz%C4%99stochowska.png?width=1280"));
		assertFalse(FlyoverPlaceSelector.isPhotograph("https://example.org/images/c/cd/Coat_of_arms.svg"));
		assertFalse(FlyoverPlaceSelector.isPhotograph(null));
		List<Candidate> candidates = new ArrayList<>();
		candidates.add(at(2000, 50, 900, "Region", "https://example.org/map.png"));
		candidates.add(at(4000, 50, 100, "Chapel", IMAGE));
		List<Selection> result = FlyoverPlaceSelector.select(track(), candidates, 10, 300, 50);
		assertEquals(1, result.size());
		assertEquals("Chapel", result.get(0).candidate.name);
	}

	@Test
	public void onePhotographIsShownOnce() {
		List<Candidate> candidates = new ArrayList<>();
		candidates.add(at(2000, 50, 900, "Skała (miasto)", "https://a.example/images-1280/0/05/Skala_kosciol.jpg?width=1280"));
		candidates.add(at(2300, 50, 800, "Kościół św. Mikołaja w Skale", "https://a.example/images-480/0/05/Skala_kosciol.jpg?width=480"));
		candidates.add(at(6000, 50, 100, "Cave", IMAGE));
		List<Selection> result = FlyoverPlaceSelector.select(track(), candidates, 10, 300, 50);
		assertEquals(2, result.size());
		assertEquals("Skała (miasto)", result.get(0).candidate.name);
		assertEquals("Cave", result.get(1).candidate.name);
	}

	@Test
	public void nearbyPlacesAreNotThinnedOutBySpacing() {
		List<Candidate> candidates = new ArrayList<>();
		for (int i = 0; i < 9; i++) {
			candidates.add(at(500 + i * 200, 30, 100 + i, "Place " + i, IMAGE));
		}
		List<Selection> result = FlyoverPlaceSelector.selectSpread(track(), candidates, 9);
		assertEquals(9, result.size());
	}
}
