package net.osmand.plus.myplaces.favorites.dialogs;

import static net.osmand.plus.settings.enums.FavoriteListSortMode.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Collator;
import net.osmand.OsmAndCollator;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.configmap.tracks.TrackTab;
import net.osmand.plus.myplaces.favorites.FavoriteFolder;
import net.osmand.plus.myplaces.favorites.FavoriteFolderFormatter;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.settings.enums.FavoriteListSortMode;
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
		if (o1 == o2) return 0;
		if (o1 == null) return 1;
		if (o2 == null) return -1;

		int r1 = rank(o1);
		int r2 = rank(o2);
		if (r1 != r2) {
			return Integer.compare(r1, r2);
		}

		if (o1 instanceof Integer i1 && o2 instanceof Integer i2) {
			return Integer.compare(i1, i2);
		}

		if (isFavoriteFolder(o1) && isFavoriteFolder(o2)) {
			boolean pinned1 = isPinnedFolder(o1);
			boolean pinned2 = isPinnedFolder(o2);
			if (pinned1 != pinned2) return pinned1 ? -1 : 1;

			boolean visible1 = isVisibleFolder(o1);
			boolean visible2 = isVisibleFolder(o2);
			if (visible1 != visible2) return visible1 ? -1 : 1;

			return compareFavoriteFolders(o1, o2);
		}

		if (o1 instanceof FavouritePoint p1 && o2 instanceof FavouritePoint p2) {
			return compareFavoritePoints(p1, p2);
		}

		return Integer.compare(System.identityHashCode(o1), System.identityHashCode(o2));
	}

	private int rank(Object o) {
		if (o instanceof Integer) return 0;
		if (isFavoriteFolder(o)) return 1;
		if (o instanceof FavouritePoint) return 2;
		return 3;
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

	private int compareFilesByLastModified(long lastModified1, long lastModified2) {
		return -Long.compare(lastModified1, lastModified2);
	}

	private int compareFavoriteFolders(@NonNull Object folder1, @NonNull Object folder2) {
		int multiplier;
		return switch (sortMode) {
			case NAME_ASCENDING, NAME_DESCENDING -> {
				multiplier = sortMode == NAME_ASCENDING ? 1 : -1;
				yield multiplier * compareFolderTitles(folder1, folder2);
			}
			case LAST_MODIFIED, DATE_ASCENDING, DATE_DESCENDING -> {
				multiplier = sortMode == DATE_DESCENDING ? -1 : 1;
				yield multiplier * compareFoldersByLastModified(folder1, folder2);
			}
			default -> 0;
		};
	}

	private int compareFoldersByLastModified(@NonNull Object folder1, @NonNull Object folder2) {
		long lastModified1 = getFolderLastModified(folder1);
		long lastModified2 = getFolderLastModified(folder2);

		if (lastModified1 == lastModified2) {
			return compareFolderTitles(folder1, folder2);
		}
		return compareFilesByLastModified(lastModified1, lastModified2);
	}

	private boolean isFavoriteFolder(@NonNull Object object) {
		return object instanceof FavoriteGroup || object instanceof FavoriteFolder;
	}

	private boolean isPinnedFolder(@NonNull Object object) {
		FavoriteGroup group = getFavoriteGroup(object);
		return group != null && group.isPinned();
	}

	private boolean isVisibleFolder(@NonNull Object object) {
		FavoriteGroup group = getFavoriteGroup(object);
		return group == null || group.isVisible();
	}

	@Nullable
	private FavoriteGroup getFavoriteGroup(@NonNull Object object) {
		if (object instanceof FavoriteGroup group) {
			return group;
		} else if (object instanceof FavoriteFolder folder) {
			return folder.getGroup();
		}
		return null;
	}

	@NonNull
	private String getFolderTitle(@NonNull Object object) {
		if (object instanceof FavoriteFolder folder) {
			return FavoriteFolderFormatter.getDisplayName(app, folder.getFullPath());
		} else if (object instanceof FavoriteGroup group) {
			return FavoriteFolderFormatter.getDisplayName(app, group.getName());
		}
		return "";
	}

	private long getFolderLastModified(@NonNull Object object) {
		if (object instanceof FavoriteFolder folder) {
			return folder.getSubtreeLastModified();
		} else if (object instanceof FavoriteGroup group) {
			return group.getTimeModified();
		}
		return 0;
	}

	private int compareFolderTitles(@NonNull Object folder1, @NonNull Object folder2) {
		return compareNames(getFolderTitle(folder1), getFolderTitle(folder2));
	}

	private int compareFavoritePointNames(@NonNull FavouritePoint point1,
	                                      @NonNull FavouritePoint point2) {
		return compareNames(point1.getDisplayName(app), point2.getDisplayName(app));
	}

	private int compareNames(@NonNull String item1, @NonNull String item2) {
		return collator.compare(item1, item2);
	}
}
