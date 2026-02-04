package net.osmand.plus.myplaces.favorites.dialogs;

import static net.osmand.plus.myplaces.favorites.dialogs.FavoriteListSortMode.*;

import androidx.annotation.NonNull;

import net.osmand.Collator;
import net.osmand.OsmAndCollator;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.configmap.tracks.TrackTab;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.shared.data.KLatLon;
import net.osmand.shared.util.KMapUtils;

import java.util.Comparator;

public class FavoriteComparator implements Comparator<Object> {

	public final KLatLon latLon;
	public final TrackTab trackTab;
	public final FavoriteListSortMode sortMode;
	public final Collator collator = OsmAndCollator.primaryCollator();
	private final OsmandApplication app;

	public FavoriteComparator(@NonNull FavoriteListSortMode sortMode, @NonNull LatLon latLon, OsmandApplication app) {
		this.trackTab = null;
		this.sortMode = sortMode;
		this.latLon = SharedUtil.kLatLon(latLon);
		this.app = app;
	}

	@Override
	public int compare(Object o1, Object o2) {
		if (o1 instanceof Integer) {
			return o2 instanceof Integer ? Integer.compare((Integer) o1, (Integer) o2) : -1;
		}
		if (o2 instanceof Integer) {
			return 1;
		}
		if (o1 instanceof FavoriteGroup group1) {
			if (o2 instanceof FavoriteGroup group2) {
				if (group1.isVisible() != group2.isVisible()) {
					return group1.isVisible() ? -1 : 1;
				}
				return compareFavoriteGroups(group1, group2);
			}
			return -1;
		}
		if (o1 instanceof FavouritePoint point1 && o2 instanceof FavouritePoint point2) {
			return compareFavoritePoints(point1, point2);
		}
		return 0;
	}

	private int compareFavoritePoints(@NonNull FavouritePoint point1,
	                                  @NonNull FavouritePoint point2) {
		int multiplier;
		switch (sortMode) {
			case NAME_ASCENDING, NAME_DESCENDING -> {
				multiplier = sortMode == NAME_ASCENDING ? 1 : -1;
				return multiplier * compareFavoritePointNames(point1, point2);
			}
			case NEAREST, FARTHEST -> {
				multiplier = sortMode == NEAREST ? 1 : -1;
				return multiplier * compareNearestPoints(point1, point2);
			}
			case DATE_ASCENDING, DATE_DESCENDING -> {
				multiplier = sortMode == DATE_DESCENDING ? -1 : 1;
				return multiplier * compareFolderFilesByLastModified(point1, point2);
			}
		}
		return 0;
	}

	private int compareFolderFilesByLastModified(@NonNull FavouritePoint point1, @NonNull FavouritePoint point2) {
		long lastModified1 = point1.getTimestamp();
		long lastModified2 = point2.getTimestamp();

		if (lastModified1 == lastModified2) {
			return compareFavoritePointNames(point1, point2);
		}
		return compareFilesByLastModified(lastModified1, lastModified2);
	}


	private int compareNearestPoints(@NonNull FavouritePoint point1, @NonNull FavouritePoint point2) {
		double distance1 = KMapUtils.INSTANCE.getDistance(latLon, new KLatLon(point1.getLatitude(), point1.getLongitude()));
		double distance2 = KMapUtils.INSTANCE.getDistance(latLon, new KLatLon(point2.getLatitude(), point2.getLongitude()));
		return Double.compare(distance1, distance2);
	}

	private int compareFavoriteGroups(@NonNull FavoriteGroup group1,
	                                  @NonNull FavoriteGroup group2) {
		int multiplier;
		return switch (sortMode) {
			case NAME_ASCENDING, NAME_DESCENDING -> {
				multiplier = sortMode == NAME_ASCENDING ? 1 : -1;
				yield multiplier * compareTrackFolderNames(group1, group2);
			}
			case LAST_MODIFIED, DATE_ASCENDING, DATE_DESCENDING -> {
				multiplier = sortMode == DATE_DESCENDING ? -1 : 1;
				yield multiplier * compareFolderFilesByLastModified(group1, group2);
			}

			default -> 0;
		};
	}

	private int compareFolderFilesByLastModified(@NonNull FavoriteGroup group1,
	                                             @NonNull FavoriteGroup group2) {
		long lastModified1 = group1.getTimeModified();
		long lastModified2 = group2.getTimeModified();

		if (lastModified1 == lastModified2) {
			return compareTrackFolderNames(group1, group2);
		}
		return compareFilesByLastModified(lastModified1, lastModified2);
	}

	private int compareFilesByLastModified(long lastModified1, long lastModified2) {
		return -Long.compare(lastModified1, lastModified2);
	}

	private int compareTrackFolderNames(@NonNull FavoriteGroup folder1,
	                                    @NonNull FavoriteGroup folder2) {
		return compareNames(folder1.getDisplayName(app), folder2.getDisplayName(app));
	}

	private int compareFavoritePointNames(@NonNull FavouritePoint point1,
	                                      @NonNull FavouritePoint point2) {
		return compareNames(point1.getDisplayName(app), point2.getDisplayName(app));
	}

	private int compareNames(@NonNull String item1, @NonNull String item2) {
		return collator.compare(item1, item2);
	}
}
