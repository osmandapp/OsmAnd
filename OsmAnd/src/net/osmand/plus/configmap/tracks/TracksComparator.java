package net.osmand.plus.configmap.tracks;

import static com.jwetherell.openmap.common.LatLonPoint.EQUIVALENT_TOLERANCE;
import static net.osmand.plus.settings.enums.TracksSortMode.DATE_DESCENDING;
import static net.osmand.plus.settings.enums.TracksSortMode.DISTANCE_ASCENDING;
import static net.osmand.plus.settings.enums.TracksSortMode.DURATION_ASCENDING;
import static net.osmand.plus.settings.enums.TracksSortMode.LAST_MODIFIED;
import static net.osmand.plus.settings.enums.TracksSortMode.NAME_ASCENDING;
import static net.osmand.plus.settings.enums.TracksSortMode.NAME_DESCENDING;
import static net.osmand.plus.settings.enums.TracksSortMode.VALUE_ASCENDING;
import static net.osmand.shared.gpx.GpxParameter.FILE_CREATION_TIME;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Collator;
import net.osmand.OsmAndCollator;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.plus.myplaces.tracks.VisibleTracksGroup;
import net.osmand.plus.settings.enums.TracksSortMode;
import net.osmand.shared.gpx.data.ComparableTracksGroup;
import net.osmand.shared.data.KLatLon;
import net.osmand.shared.gpx.GpxDataItem;
import net.osmand.shared.gpx.GpxTrackAnalysis;
import net.osmand.shared.gpx.TrackItem;
import net.osmand.shared.gpx.filters.TrackFolderAnalysis;
import net.osmand.shared.io.KFile;
import net.osmand.shared.util.KMapUtils;
import net.osmand.util.CollectionUtils;

import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Map;

public class TracksComparator implements Comparator<Object> {

	public final KLatLon latLon;
	public final TrackTab trackTab;
	public final TracksSortMode sortMode;
	public final Collator collator = OsmAndCollator.primaryCollator();
	private boolean useSubdirs = false;

	// Sorting keys have to stay constant for the whole sort, otherwise TimSort throws
	// "Comparison method violates its general contract!". Track folders read them from the
	// file system and recalculate them lazily, so they are cached per comparator instance.
	private final Map<ComparableTracksGroup, Long> lastModifiedCache = new IdentityHashMap<>();
	private final Map<ComparableTracksGroup, TrackFolderAnalysis> analysisCache = new IdentityHashMap<>();

	public TracksComparator(@NonNull TrackTab trackTab, @NonNull OsmandApplication app) {
		this.trackTab = trackTab;
		this.sortMode = trackTab.getSortMode();
		this.latLon = SharedUtil.kLatLon(TrackSortModesHelper.getReferenceLocation(app, sortMode));
	}

	public TracksComparator(@NonNull TracksSortMode sortMode,
	                        @NonNull OsmandApplication app, boolean useSubdirs) {
		this(sortMode, app);
		this.useSubdirs = useSubdirs;
	}

	public TracksComparator(@NonNull TracksSortMode sortMode, @NonNull OsmandApplication app) {
		this.trackTab = null;
		this.sortMode = sortMode;
		this.latLon = SharedUtil.kLatLon(TrackSortModesHelper.getReferenceLocation(app, sortMode));
	}

	@Override
	public int compare(Object o1, Object o2) {
		if (o1 instanceof Integer) {
			return o2 instanceof Integer ? Integer.compare((Integer) o1, (Integer) o2) : -1;
		}
		if (o2 instanceof Integer) {
			return 1;
		}
		boolean currentTrack1 = o1 instanceof TrackItem && ((TrackItem) o1).isShowCurrentTrack();
		boolean currentTrack2 = o2 instanceof TrackItem && ((TrackItem) o2).isShowCurrentTrack();
		if (currentTrack1 || currentTrack2) {
			return currentTrack1 && currentTrack2 ? 0 : (currentTrack1 ? -1 : 1);
		}
		boolean visibleGroup1 = o1 instanceof VisibleTracksGroup;
		boolean visibleGroup2 = o2 instanceof VisibleTracksGroup;
		if (visibleGroup1 || visibleGroup2) {
			return visibleGroup1 && visibleGroup2 ? 0 : (visibleGroup1 ? -1 : 1);
		}
		if (o1 instanceof ComparableTracksGroup folder1) {
			if (o2 instanceof ComparableTracksGroup folder2) {
				int predefinedOrder1 = folder1.getDefaultOrder();
				int predefinedOrder2 = folder2.getDefaultOrder();
				if (predefinedOrder1 != predefinedOrder2) {
					return Integer.compare(predefinedOrder1, predefinedOrder2);
				}
				return compareTrackGroups(folder1, folder2);
			}
			return -1;
		}
		if (o2 instanceof ComparableTracksGroup) {
			return 1;
		}
		boolean trackItem1 = o1 instanceof TrackItem;
		boolean trackItem2 = o2 instanceof TrackItem;
		if (trackItem1 && trackItem2) {
			return compareTrackItems((TrackItem) o1, (TrackItem) o2);
		}
		// Any other row (folder statistics for example) is kept after the tracks. Reporting it
		// as equal to every track would make the comparator intransitive.
		if (trackItem1 || trackItem2) {
			return trackItem1 ? -1 : 1;
		}
		return 0;
	}

	private int compareTrackGroups(@NonNull ComparableTracksGroup group1,
	                               @NonNull ComparableTracksGroup group2) {
		int multiplier;
		switch (sortMode) {
			case NAME_ASCENDING, NAME_DESCENDING: {
				multiplier = sortMode == NAME_ASCENDING ? 1 : -1;
				return multiplier * compareTrackFolderNames(group1, group2);
			}

			case VALUE_ASCENDING, VALUE_DESCENDING: {
				double v1 = group1.getComparisonValue();
				double v2 = group2.getComparisonValue();
				if (v1 != v2) {
					multiplier = sortMode == VALUE_ASCENDING ? 1 : -1;
					return multiplier * Double.compare(v1, v2);
				}
			}

			case LAST_MODIFIED, DATE_ASCENDING, DATE_DESCENDING: {
				multiplier = sortMode == DATE_DESCENDING ? -1 : 1;
				return multiplier * compareFolderFilesByLastModified(group1, group2);
			}

			case DISTANCE_ASCENDING, DISTANCE_DESCENDING: {
				float dist1 = getFolderAnalysis(group1).getTotalDistance();
				float dist2 = getFolderAnalysis(group2).getTotalDistance();
				if (Math.abs(dist1 - dist2) >= EQUIVALENT_TOLERANCE) {
					multiplier = sortMode == DISTANCE_ASCENDING ? 1 : -1;
					return multiplier * Float.compare(dist1, dist2);
				}
			}

			case DURATION_ASCENDING, DURATION_DESCENDING: {
				int timeSpan1 = getFolderAnalysis(group1).getTimeSpan();
				int timeSpan2 = getFolderAnalysis(group2).getTimeSpan();
				if (timeSpan1 != timeSpan2) {
					multiplier = sortMode == DURATION_ASCENDING ? 1 : -1;
					return multiplier * Long.compare(timeSpan1, timeSpan2);
				}
			}
		}
		return compareTrackFolderNames(group1, group2);
	}

	private int compareTrackItems(@NonNull TrackItem item1, @NonNull TrackItem item2) {
		Integer currentTrack = checkCurrentTrack(item1, item2);
		if (currentTrack != null) {
			return currentTrack;
		}

		GpxDataItem dataItem1 = item1.getDataItem();
		GpxDataItem dataItem2 = item2.getDataItem();
		GpxTrackAnalysis analysis1;
		GpxTrackAnalysis analysis2;

		if (shouldCheckAnalysis()) {
			analysis1 = dataItem1 != null ? dataItem1.getAnalysis() : null;
			analysis2 = dataItem2 != null ? dataItem2.getAnalysis() : null;
			Integer analysis = checkItemsAnalysis(item1, item2, analysis1, analysis2);
			if (analysis != null) {
				return analysis;
			}
		}

		switch (sortMode) {
			case NEAREST, NEAREST_TO_MAP_CENTER:
				analysis1 = dataItem1 != null ? dataItem1.getAnalysis() : null;
				analysis2 = dataItem2 != null ? dataItem2.getAnalysis() : null;
				return compareNearestItems(item1, item2, analysis1, analysis2);
			case NAME_ASCENDING:
				return compareTrackItemNames(item1, item2);
			case NAME_DESCENDING:
				return -compareTrackItemNames(item1, item2);
			case DATE_ASCENDING:
				analysis1 = dataItem1 != null ? dataItem1.getAnalysis() : null;
				analysis2 = dataItem2 != null ? dataItem2.getAnalysis() : null;
				long startTime1_asc = analysis1 == null ? 0 : analysis1.getStartTime();
				long startTime2_asc = analysis2 == null ? 0 : analysis2.getStartTime();
				long time1_asc = dataItem1 == null ? startTime1_asc : (long) dataItem1.getParameter(FILE_CREATION_TIME);
				long time2_asc = dataItem2 == null ? startTime2_asc : (long) dataItem2.getParameter(FILE_CREATION_TIME);
				if (time1_asc == time2_asc || time1_asc < 10 && time2_asc < 10) {
					return compareTrackItemNames(item1, item2);
				}
				if (time1_asc < 10) {
					return 1;
				} else if (time2_asc < 10) {
					return -1;
				}
				return -Long.compare(time1_asc, time2_asc);
			case DATE_DESCENDING:
				analysis1 = dataItem1 != null ? dataItem1.getAnalysis() : null;
				analysis2 = dataItem2 != null ? dataItem2.getAnalysis() : null;
				long startTime1_desc = analysis1 == null ? 0 : analysis1.getStartTime();
				long startTime2_desc = analysis2 == null ? 0 : analysis2.getStartTime();
				long time1_desc = dataItem1 == null ? startTime1_desc : (long) dataItem1.getParameter(FILE_CREATION_TIME);
				long time2_desc = dataItem2 == null ? startTime2_desc : (long) dataItem2.getParameter(FILE_CREATION_TIME);
				if (time1_desc == time2_desc || time1_desc < 10 && time2_desc < 10) {
					return compareTrackItemNames(item1, item2);
				}
				if (time1_desc < 10) {
					return 1;
				} else if (time2_desc < 10) {
					return -1;
				}
				return Long.compare(time1_desc, time2_desc);
			case LAST_MODIFIED:
				return compareItemFilesByLastModified(item1, item2);
			case DISTANCE_DESCENDING:
				analysis1 = dataItem1 != null ? dataItem1.getAnalysis() : null;
				analysis2 = dataItem2 != null ? dataItem2.getAnalysis() : null;
				if (Math.abs(analysis1.getTotalDistance() - analysis2.getTotalDistance()) < EQUIVALENT_TOLERANCE) {
					return compareTrackItemNames(item1, item2);
				}
				return -Float.compare(analysis1.getTotalDistance(), analysis2.getTotalDistance());
			case DISTANCE_ASCENDING:
				analysis1 = dataItem1 != null ? dataItem1.getAnalysis() : null;
				analysis2 = dataItem2 != null ? dataItem2.getAnalysis() : null;
				if (Math.abs(analysis1.getTotalDistance() - analysis2.getTotalDistance()) < EQUIVALENT_TOLERANCE) {
					return compareTrackItemNames(item1, item2);
				}
				return Float.compare(analysis1.getTotalDistance(), analysis2.getTotalDistance());
			case DURATION_DESCENDING:
				analysis1 = dataItem1 != null ? dataItem1.getAnalysis() : null;
				analysis2 = dataItem2 != null ? dataItem2.getAnalysis() : null;
				if (analysis1.getDurationInSeconds() == analysis2.getDurationInSeconds()) {
					return compareTrackItemNames(item1, item2);
				}
				return -Long.compare(analysis1.getDurationInSeconds(), analysis2.getDurationInSeconds());
			case DURATION_ASCENDING:
				analysis1 = dataItem1 != null ? dataItem1.getAnalysis() : null;
				analysis2 = dataItem2 != null ? dataItem2.getAnalysis() : null;
				if (analysis1.getDurationInSeconds() == analysis2.getDurationInSeconds()) {
					return compareTrackItemNames(item1, item2);
				}
				return Long.compare(analysis1.getDurationInSeconds(), analysis2.getDurationInSeconds());
		}
		return 0;
	}

	private boolean shouldCheckAnalysis() {
		return !CollectionUtils.equalsToAny(sortMode, NAME_ASCENDING, NAME_DESCENDING, LAST_MODIFIED);
	}

	@Nullable
	private Integer checkCurrentTrack(@NonNull TrackItem item1, @NonNull TrackItem item2) {
		if (item1.isShowCurrentTrack()) {
			return -1;
		}
		if (item2.isShowCurrentTrack()) {
			return 1;
		}
		return null;
	}

	@Nullable
	private Integer checkItemsAnalysis(@NonNull TrackItem item1, @NonNull TrackItem item2,
	                                   @Nullable GpxTrackAnalysis analysis1, @Nullable GpxTrackAnalysis analysis2) {
		if (analysis1 == null) {
			return analysis2 == null ? compareTrackItemNames(item1, item2) : 1;
		}
		if (analysis2 == null) {
			return -1;
		}
		return null;
	}

	private int compareNearestItems(@NonNull TrackItem item1, @NonNull TrackItem item2,
	                                @NonNull GpxTrackAnalysis analysis1, @NonNull GpxTrackAnalysis analysis2) {
		if (analysis1.getLatLonStart() == null) {
			return analysis2.getLatLonStart() == null ? compareTrackItemNames(item1, item2) : 1;
		}
		if (analysis2.getLatLonStart() == null) {
			return -1;
		}
		if (analysis1.getLatLonStart().equals(analysis2.getLatLonStart())) {
			return compareTrackItemNames(item1, item2);
		}
		double distance1 = KMapUtils.INSTANCE.getDistance(latLon, analysis1.getLatLonStart());
		double distance2 = KMapUtils.INSTANCE.getDistance(latLon, analysis2.getLatLonStart());
		return Double.compare(distance1, distance2);
	}

	private int compareItemFilesByLastModified(@NonNull TrackItem item1, @NonNull TrackItem item2) {
		KFile file1 = item1.getFile();
		KFile file2 = item2.getFile();

		if (file1 == null) {
			return file2 == null ? compareTrackItemNames(item1, item2) : 1;
		}
		if (file2 == null) {
			return -1;
		}
		// Deliberately not file.lastModified(): reading the file system during the sort makes the
		// comparison result change when tracks are deleted in background (a missing file reports 0).
		long lastModified1 = item1.getLastModified();
		long lastModified2 = item2.getLastModified();
		if (lastModified1 == lastModified2) {
			return compareTrackItemNames(item1, item2);
		}
		return compareFilesByLastModified(lastModified1, lastModified2);
	}

	private int compareFolderFilesByLastModified(@NonNull ComparableTracksGroup folder1, @NonNull ComparableTracksGroup folder2) {
		long lastModified1 = getLastModified(folder1);
		long lastModified2 = getLastModified(folder2);

		if (lastModified1 == lastModified2) {
			return compareTrackFolderNames(folder1, folder2);
		}
		return compareFilesByLastModified(lastModified1, lastModified2);
	}

	private int compareFilesByLastModified(long lastModified1, long lastModified2) {
		return -Long.compare(lastModified1, lastModified2);
	}

	private long getLastModified(@NonNull ComparableTracksGroup group) {
		Long lastModified = lastModifiedCache.get(group);
		if (lastModified == null) {
			lastModified = group.lastModified();
			lastModifiedCache.put(group, lastModified);
		}
		return lastModified;
	}

	@NonNull
	private TrackFolderAnalysis getFolderAnalysis(@NonNull ComparableTracksGroup group) {
		TrackFolderAnalysis analysis = analysisCache.get(group);
		if (analysis == null) {
			analysis = group.getFolderAnalysis();
			analysisCache.put(group, analysis);
		}
		return analysis;
	}

	private int compareTrackItemNames(@NonNull TrackItem item1, @NonNull TrackItem item2) {
		return compareNames(item1.getName(), item2.getName());
	}

	private int compareTrackFolderNames(@NonNull ComparableTracksGroup folder1,
	                                    @NonNull ComparableTracksGroup folder2) {
		return compareNames(folder1.getDirName(useSubdirs), folder2.getDirName(useSubdirs));
	}

	private int compareNames(@NonNull String item1, @NonNull String item2) {
		return collator.compare(item1, item2);
	}
}
