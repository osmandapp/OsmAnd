package net.osmand.gpx.flyover;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Picks the popular places shown as photo cards during a flyover: close to the track,
 * the most popular first, spread out along the track so that cards do not overlap.
 */
public class FlyoverPlaceSelector {

	public static class Candidate {
		public final double lat;
		public final double lon;
		public final int rank;
		public final String name;
		public final String imageUrl;
		/** Kind of place, for the log: POI category and subtype. */
		public final String kind;

		public Candidate(double lat, double lon, int rank, String name, String imageUrl) {
			this(lat, lon, rank, name, imageUrl, "");
		}

		public Candidate(double lat, double lon, int rank, String name, String imageUrl, String kind) {
			this.lat = lat;
			this.lon = lon;
			this.rank = rank;
			this.name = name;
			this.imageUrl = imageUrl;
			this.kind = kind;
		}
	}

	public static class Selection {
		public final Candidate candidate;
		/** Meters from the start of the track to the point closest to the place. */
		public final double distanceAlong;
		/** 0..1 along the track. */
		public final double progress;

		Selection(Candidate candidate, double distanceAlong, double progress) {
			this.candidate = candidate;
			this.distanceAlong = distanceAlong;
			this.progress = progress;
		}
	}

	/** Places this close to the track are preferred, meters. */
	public static final double NEAR_CORRIDOR_METERS = 100;
	/** Then places this close, meters. */
	public static final double MID_CORRIDOR_METERS = 500;
	/** Nothing further from the track than this is shown, meters. */
	public static final double FAR_CORRIDOR_METERS = 1000;
	/** Two places closer than this along the track are the same spot; only the more popular one is shown. */
	public static final double MIN_SPACING_METERS = 50;

	/**
	 * Selects up to {@code maxPlaces} places spread along the whole track. The track is divided into
	 * as many stretches as places, and each stretch gets the best place near it: within 100 m of the
	 * track first, then 500 m, then 1000 m, the most popular within each. Stretches with nothing
	 * nearby stay empty, and the remaining capacity goes to the best places left anywhere.
	 */
	public static List<Selection> selectSpread(FlyoverTrackGeometry geometry, List<Candidate> candidates, int maxPlaces) {
		if (maxPlaces <= 0) {
			return new ArrayList<>();
		}
		double length = geometry.getLength();
		List<Selection> nearby = new ArrayList<>();
		List<Double> offsets = new ArrayList<>();
		for (Candidate candidate : candidates) {
			if (isBlank(candidate.name) || !isPhotograph(candidate.imageUrl)) {
				continue;
			}
			double[] projection = geometry.project(candidate.lat, candidate.lon);
			if (projection[1] > FAR_CORRIDOR_METERS) {
				continue;
			}
			nearby.add(new Selection(candidate, projection[0], length > 0 ? projection[0] / length : 0));
			offsets.add(projection[1]);
		}
		final List<Selection> all = nearby;
		final List<Double> allOffsets = offsets;
		Integer[] order = new Integer[all.size()];
		for (int i = 0; i < order.length; i++) {
			order[i] = i;
		}
		java.util.Arrays.sort(order, new Comparator<Integer>() {
			@Override
			public int compare(Integer a, Integer b) {
				int byTier = Integer.compare(corridorTier(allOffsets.get(a)), corridorTier(allOffsets.get(b)));
				if (byTier != 0) {
					return byTier;
				}
				int byRank = Integer.compare(all.get(b).candidate.rank, all.get(a).candidate.rank);
				return byRank != 0 ? byRank : Double.compare(all.get(a).distanceAlong, all.get(b).distanceAlong);
			}
		});

		List<Selection> chosen = new ArrayList<>();
		Set<String> names = new HashSet<>();
		Set<String> images = new HashSet<>();
		boolean[] stretchTaken = new boolean[maxPlaces];
		List<Selection> leftover = new ArrayList<>();
		for (Integer index : order) {
			Selection selection = all.get(index);
			if (!accepts(selection, chosen, names, images)) {
				continue;
			}
			int stretch = Math.min(maxPlaces - 1, (int) (selection.progress * maxPlaces));
			if (stretchTaken[stretch]) {
				leftover.add(selection);
				continue;
			}
			stretchTaken[stretch] = true;
			add(selection, chosen, names, images);
		}
		for (Selection selection : leftover) {
			if (chosen.size() >= maxPlaces) {
				break;
			}
			if (accepts(selection, chosen, names, images)) {
				add(selection, chosen, names, images);
			}
		}
		Collections.sort(chosen, new Comparator<Selection>() {
			@Override
			public int compare(Selection a, Selection b) {
				return Double.compare(a.distanceAlong, b.distanceAlong);
			}
		});
		return chosen;
	}

	private static int corridorTier(double offsetMeters) {
		return offsetMeters <= NEAR_CORRIDOR_METERS ? 0 : offsetMeters <= MID_CORRIDOR_METERS ? 1 : 2;
	}

	private static boolean accepts(Selection selection, List<Selection> chosen, Set<String> names, Set<String> images) {
		if (names.contains(selection.candidate.name.trim().toLowerCase())
				|| images.contains(imageFile(selection.candidate.imageUrl))) {
			return false;
		}
		for (Selection other : chosen) {
			if (Math.abs(other.distanceAlong - selection.distanceAlong) < MIN_SPACING_METERS) {
				return false;
			}
		}
		return true;
	}

	private static void add(Selection selection, List<Selection> chosen, Set<String> names, Set<String> images) {
		names.add(selection.candidate.name.trim().toLowerCase());
		images.add(imageFile(selection.candidate.imageUrl));
		chosen.add(selection);
	}

	public static List<Selection> select(FlyoverTrackGeometry geometry, List<Candidate> candidates, int maxPlaces,
	                                     double corridorMeters, double minSpacingMeters) {
		List<Selection> nearby = new ArrayList<>();
		double length = geometry.getLength();
		for (Candidate candidate : candidates) {
			if (isBlank(candidate.name) || !isPhotograph(candidate.imageUrl)) {
				continue;
			}
			double[] projection = geometry.project(candidate.lat, candidate.lon);
			if (projection[1] > corridorMeters) {
				continue;
			}
			double progress = length > 0 ? projection[0] / length : 0;
			nearby.add(new Selection(candidate, projection[0], progress));
		}
		Collections.sort(nearby, new Comparator<Selection>() {
			@Override
			public int compare(Selection a, Selection b) {
				int byRank = Integer.compare(b.candidate.rank, a.candidate.rank);
				return byRank != 0 ? byRank : Double.compare(a.distanceAlong, b.distanceAlong);
			}
		});

		List<Selection> chosen = new ArrayList<>();
		Set<String> names = new HashSet<>();
		Set<String> images = new HashSet<>();
		for (Selection selection : nearby) {
			if (chosen.size() >= maxPlaces) {
				break;
			}
			String name = selection.candidate.name.trim().toLowerCase();
			// A town and its church often share one photograph; show it once, for the more popular of the two.
			String image = imageFile(selection.candidate.imageUrl);
			if (names.contains(name) || images.contains(image)) {
				continue;
			}
			boolean tooClose = false;
			for (Selection other : chosen) {
				if (Math.abs(other.distanceAlong - selection.distanceAlong) < minSpacingMeters) {
					tooClose = true;
					break;
				}
			}
			if (tooClose) {
				continue;
			}
			names.add(name);
			images.add(image);
			chosen.add(selection);
		}
		Collections.sort(chosen, new Comparator<Selection>() {
			@Override
			public int compare(Selection a, Selection b) {
				return Double.compare(a.distanceAlong, b.distanceAlong);
			}
		});
		return chosen;
	}

	/**
	 * Whether the image is a photograph rather than a map, a diagram or a coat of arms: Wikimedia
	 * photographs are JPEGs, while maps and drawings are PNG or SVG.
	 */
	public static boolean isPhotograph(String imageUrl) {
		if (isBlank(imageUrl)) {
			return false;
		}
		String path = imageUrl;
		int query = path.indexOf('?');
		if (query >= 0) {
			path = path.substring(0, query);
		}
		String lower = path.toLowerCase();
		return lower.endsWith(".jpg") || lower.endsWith(".jpeg");
	}

	/** The image path without host and size parameters, so that the same file in different sizes matches. */
	static String imageFile(String imageUrl) {
		String path = imageUrl;
		int query = path.indexOf('?');
		if (query >= 0) {
			path = path.substring(0, query);
		}
		int slash = path.lastIndexOf('/');
		return (slash >= 0 ? path.substring(slash + 1) : path).toLowerCase();
	}

	private static boolean isBlank(String s) {
		return s == null || s.trim().isEmpty();
	}
}
