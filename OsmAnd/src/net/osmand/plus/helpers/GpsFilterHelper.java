package net.osmand.plus.helpers;

import android.os.AsyncTask;

import net.osmand.GPXUtilities.Author;
import net.osmand.GPXUtilities.Copyright;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.GPXTrackAnalysis;
import net.osmand.GPXUtilities.Metadata;
import net.osmand.GPXUtilities.Track;
import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.FilteredSelectedGpxFile;
import net.osmand.plus.FilteredSelectedGpxFile.AltitudeFilter;
import net.osmand.plus.FilteredSelectedGpxFile.HdopFilter;
import net.osmand.plus.FilteredSelectedGpxFile.SmoothingFilter;
import net.osmand.plus.FilteredSelectedGpxFile.SpeedFilter;
import net.osmand.plus.GPXDatabase.GpxDataItem;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayGroup;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.Version;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class GpsFilterHelper {

	private final OsmandApplication app;
	private final Set<GpsFilterListener> listeners = new HashSet<>();

	private final Executor singleThreadExecutor = Executors.newSingleThreadExecutor();
	private GpsFilterTask gpsFilterTask = null;

	public GpsFilterHelper(@NonNull OsmandApplication app) {
		this.app = app;
	}

	public void addListener(@NonNull GpsFilterListener listener) {
		listeners.add(listener);
	}

	public void removeListener(@NonNull GpsFilterListener listener) {
		listeners.remove(listener);
	}

	public void clearListeners() {
		listeners.clear();
	}

	public void filterGpxFile(@NonNull FilteredSelectedGpxFile filteredSelectedGpxFile) {
		if (gpsFilterTask != null) {
			gpsFilterTask.cancel(false);
		}
		gpsFilterTask = new GpsFilterTask(app, filteredSelectedGpxFile, listeners);
		gpsFilterTask.executeOnExecutor(singleThreadExecutor);
	}

	@SuppressWarnings("deprecation")
	private static class GpsFilterTask extends AsyncTask<Void, Void, Boolean> {

		private final OsmandApplication app;
		private final FilteredSelectedGpxFile filteredSelectedGpxFile;
		private final Set<GpsFilterListener> listeners;

		private GPXFile filteredGpxFile;
		private GPXTrackAnalysis trackAnalysis;
		private List<GpxDisplayGroup> displayGroups;

		public GpsFilterTask(@NonNull OsmandApplication app,
		                     @NonNull FilteredSelectedGpxFile filteredGpx,
		                     @NonNull Set<GpsFilterListener> listeners) {
			this.app = app;
			this.filteredSelectedGpxFile = filteredGpx;
			this.listeners = listeners;
		}

		@Override
		protected Boolean doInBackground(Void... voids) {
			SelectedGpxFile sourceSelectedGpxFile = filteredSelectedGpxFile.getSourceSelectedGpxFile();
			GPXFile sourceGpx = sourceSelectedGpxFile.getGpxFile();

			filteredGpxFile = copyGpxFile(app, sourceGpx);
			filteredGpxFile.tracks.clear();

			int pointsCount = 0;
			for (Track track : sourceGpx.tracks) {

				Track filteredTrack = new Track();
				filteredTrack.name = track.name;
				filteredTrack.desc = track.desc;

				for (TrkSegment segment : track.segments) {

					if (segment.generalSegment) {
						continue;
					}

					TrkSegment filteredSegment = new TrkSegment();
					filteredSegment.name = segment.name;

					double cumulativeDistance = 0;
					WptPt previousPoint = null;
					List<WptPt> points = segment.points;

					for (int i = 0; i < points.size(); i++) {

						if (isCancelled()) {
							return false;
						}

						WptPt point = points.get(i);

						if (previousPoint != null) {
							cumulativeDistance += MapUtils.getDistance(previousPoint.lat, previousPoint.lon,
									point.lat, point.lon);
						}
						boolean firstOrLast = i == 0 || i + 1 == points.size();

						if (acceptPoint(point, pointsCount, cumulativeDistance, firstOrLast)) {
							filteredSegment.points.add(new WptPt(point));
							cumulativeDistance = 0;
						}

						pointsCount++;
						previousPoint = point;
					}

					if (filteredSegment.points.size() != 0) {
						filteredTrack.segments.add(filteredSegment);
					}
				}

				if (filteredTrack.segments.size() != 0) {
					filteredGpxFile.tracks.add(filteredTrack);
				}
			}

			if (filteredSelectedGpxFile.isJoinSegments()) {
				filteredGpxFile.addGeneralTrack();
			}

			trackAnalysis = filteredGpxFile.getAnalysis(System.currentTimeMillis());
			displayGroups = processSplit(filteredGpxFile);

			return true;
		}

		private boolean acceptPoint(@NonNull WptPt point, int pointIndex, double cumulativeDistance, boolean firstOrLast) {
			SpeedFilter speedFilter = filteredSelectedGpxFile.getSpeedFilter();
			AltitudeFilter altitudeFilter = filteredSelectedGpxFile.getAltitudeFilter();
			HdopFilter hdopFilter = filteredSelectedGpxFile.getHdopFilter();
			SmoothingFilter smoothingFilter = filteredSelectedGpxFile.getSmoothingFilter();

			return speedFilter.acceptPoint(point, pointIndex, cumulativeDistance)
					&& altitudeFilter.acceptPoint(point, pointIndex, cumulativeDistance)
					&& hdopFilter.acceptPoint(point, pointIndex, cumulativeDistance)
					&& (firstOrLast || smoothingFilter.acceptPoint(point, pointIndex, cumulativeDistance));
		}

		@Nullable
		private List<GpxDisplayGroup> processSplit(@NonNull GPXFile gpxFile) {
			List<GpxDataItem> dataItems = app.getGpxDbHelper().getSplitItems();
			for (GpxDataItem dataItem : dataItems) {
				if (dataItem.getFile().getAbsolutePath().equals(gpxFile.path)) {
					return GpxSelectionHelper.processSplit(app, dataItem, gpxFile);
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(@NonNull Boolean successfulFinish) {
			if (successfulFinish && !isCancelled()) {
				filteredSelectedGpxFile.updateGpxFile(filteredGpxFile);
				filteredSelectedGpxFile.setTrackAnalysis(trackAnalysis);
				if (displayGroups != null) {
					filteredSelectedGpxFile.setDisplayGroups(displayGroups);
				}
				for (GpsFilterListener listener : listeners) {
					listener.onFinishFiltering();
				}
			}
		}
	}

	@NonNull
	public static GPXFile copyGpxFile(@NonNull OsmandApplication app, @NonNull GPXFile source) {
		GPXFile copy = new GPXFile(Version.getFullVersion(app));
		copy.author = source.author;
		if (source.metadata != null) {
			copy.metadata = copyMetadata(source.metadata);
		}
		copy.tracks = copyTracks(source.tracks);
		copy.addPoints(source.getPoints());
		copy.routes = new ArrayList<>(source.routes);
		copy.path = source.path;
		copy.hasAltitude = source.hasAltitude;
		copy.modifiedTime = System.currentTimeMillis();
		copy.copyExtensions(source);
		return copy;
	}

	@NonNull
	private static List<Track> copyTracks(@NonNull List<Track> sourceTracks) {
		List<Track> copiedTracks = new ArrayList<>(sourceTracks.size());
		for (Track sourceTrack : sourceTracks) {

			Track trackCopy = new Track();
			trackCopy.name = sourceTrack.name;
			trackCopy.desc = sourceTrack.desc;
			trackCopy.generalTrack = sourceTrack.generalTrack;
			copiedTracks.add(trackCopy);

			for (TrkSegment sourceSegment : sourceTrack.segments) {

				TrkSegment segmentCopy = new TrkSegment();
				segmentCopy.name = sourceSegment.name;
				segmentCopy.generalSegment = sourceSegment.generalSegment;
				trackCopy.segments.add(segmentCopy);

				for (WptPt sourcePoint : sourceSegment.points) {
					segmentCopy.points.add(new WptPt(sourcePoint));
				}
			}
		}

		return copiedTracks;
	}

	@NonNull
	private static Metadata copyMetadata(@NonNull Metadata source) {
		Metadata copy = new Metadata();
		copy.name = source.name;
		copy.desc = source.desc;
		copy.link = source.link;
		copy.keywords = source.keywords;
		copy.time = source.time;

		if (source.author != null) {
			Author author = new Author();
			author.name = source.author.name;
			author.email = source.author.email;
			author.link = source.author.link;
			author.copyExtensions(source.author);
			copy.author = author;
		}

		if (source.copyright != null) {
			Copyright copyright = new Copyright();
			copyright.author = source.copyright.author;
			copyright.year = source.copyright.year;
			copyright.license = source.copyright.license;
			copyright.copyExtensions(source.copyright);
			copy.copyright = copyright;
		}

		copy.copyExtensions(source);

		return copy;
	}

	public interface GpsFilterListener {

		void onFinishFiltering();
	}
}