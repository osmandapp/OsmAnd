package net.osmand.plus.search;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.plus.track.data.GPXInfo;
import net.osmand.shared.gpx.GpxDataItem;
import net.osmand.shared.gpx.GpxDbHelper;
import net.osmand.shared.io.KFile;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class SearchTrackDataResolver {

	private static final long REFRESH_DELAY_MS = 150;

	public interface TrackDataListener {
		void onTrackDataResolved();
	}

	private final OsmandApplication app;
	@Nullable
	private final TrackDataListener listener;

	private final Set<String> requestedPaths = new HashSet<>();
	private final AtomicBoolean refreshScheduled = new AtomicBoolean();
	private volatile boolean released;

	public SearchTrackDataResolver(@NonNull OsmandApplication app, @Nullable TrackDataListener listener) {
		this.app = app;
		this.listener = listener;
	}

	@NonNull
	public SearchTrackData resolve(@Nullable GPXInfo gpxInfo) {
		File file = gpxInfo != null ? gpxInfo.getFile() : null;
		if (file == null || released) {
			return SearchTrackData.UNRESOLVED;
		}
		KFile kFile = SharedUtil.kFile(file);
		GpxDbHelper gpxDbHelper = app.getGpxDbHelper();

		GpxDataItem item;
		if (requestedPaths.add(kFile.path())) {
			item = gpxDbHelper.getItem(kFile, this::onTrackDataReady);
		} else {
			item = gpxDbHelper.getItem(kFile, false);
		}
		return item != null ? SearchTrackData.create(app, item) : SearchTrackData.UNRESOLVED;
	}

	private void onTrackDataReady(@NonNull GpxDataItem item) {
		if (!released) {
			scheduleRefresh();
		}
	}

	private void scheduleRefresh() {
		if (listener != null && refreshScheduled.compareAndSet(false, true)) {
			app.runInUIThread(() -> {
				refreshScheduled.set(false);
				if (!released && listener != null) {
					listener.onTrackDataResolved();
				}
			}, REFRESH_DELAY_MS);
		}
	}

	public void release() {
		released = true;
		requestedPaths.clear();
	}
}