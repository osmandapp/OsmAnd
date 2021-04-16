package net.osmand.plus.download;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IndexConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.LocalIndexInfo;
import net.osmand.plus.helpers.enums.MetricsConstants;
import net.osmand.util.Algorithms;

import java.io.File;
import java.text.DateFormat;
import java.util.Collections;
import java.util.List;

import static net.osmand.IndexConstants.BINARY_SRTM_MAP_INDEX_EXT;
import static net.osmand.IndexConstants.BINARY_SRTM_MAP_INDEX_EXT_ZIP;
import static net.osmand.plus.activities.LocalIndexHelper.LocalIndexType.SRTM_DATA;
import static net.osmand.plus.download.DownloadActivityType.SRTM_COUNTRY_FILE;

public class SrtmDownloadItem extends DownloadItem {

	private final List<IndexItem> indexes;
	private boolean useMeters;

	public SrtmDownloadItem(List<IndexItem> indexes,
	                        boolean useMeters) {
		super(SRTM_COUNTRY_FILE);
		this.indexes = indexes;
		this.useMeters = useMeters;
	}

	public void setUseMeters(boolean useMeters) {
		this.useMeters = useMeters;
	}

	public boolean isUseMeters() {
		return useMeters;
	}

	@Nullable
	public IndexItem getIndexItem() {
		for (IndexItem index : indexes) {
			if (useMeters && isMetersItem(index) || !useMeters && !isMetersItem(index)) {
				return index;
			}
		}
		return null;
	}

	@Override
	protected double getSizeToDownloadInMb() {
		return getIndexItem().getSizeToDownloadInMb();
	}

	@Override
	public double getArchiveSizeMB() {
		return getIndexItem().getArchiveSizeMB();
	}

	@Override
	public boolean isOutdated() {
		for (DownloadItem item : indexes) {
			if (item.isOutdated()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isDownloaded() {
		for (DownloadItem item : indexes) {
			if (item.isDownloaded()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean hasActualDataToDownload() {
		// may be check only downloaded items if any downloaded
		for (IndexItem item : indexes) {
			if (item.hasActualDataToDownload()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isDownloading(@NonNull DownloadIndexesThread thread) {
		for (IndexItem item : indexes) {
			if (thread.isDownloading(item)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String getFileName() {
		// may be check only downloaded items if any downloaded
		return getIndexItem().getFileName();
	}

	@NonNull
	@Override
	public List<File> getDownloadedFiles(@NonNull OsmandApplication app) {
		// may be check both indexes files
		List<File> result;
		for (IndexItem index : indexes) {
			result = index.getDownloadedFiles(app);
			if (!Algorithms.isEmpty(result)) {
				return result;
			}
		}
		return Collections.emptyList();
	}

	public String getDate(@NonNull DateFormat dateFormat, boolean remote) {
		// may be check only downloaded items if any downloaded
		return getIndexItem().getDate(dateFormat, remote);
	}

	@Override
	public boolean isUseAbbreviation() {
		return true;
	}

	@Override
	public String getAbbreviationInScopes(Context ctx) {
		return getAbbreviationInScopes(ctx, this);
	}

	public static boolean shouldUseMetersByDefault(@NonNull OsmandApplication app) {
		MetricsConstants metricSystem = app.getSettings().METRIC_SYSTEM.get();
		return metricSystem != MetricsConstants.MILES_AND_FEET;
	}

	@NonNull
	public static String getAbbreviationInScopes(Context ctx, Object obj) {
		return "(" + getAbbreviation(ctx, obj) + ")";
	}

	@NonNull
	public static String getAbbreviation(Context context, Object obj) {
		return context.getString(isMetersItem(obj) ? R.string.m : R.string.foot);
	}

	public static boolean isMetersItem(Object item) {
		if (item instanceof IndexItem) {
			return ((IndexItem) item).getFileName().endsWith(BINARY_SRTM_MAP_INDEX_EXT_ZIP);
		} else if (item instanceof LocalIndexInfo) {
			return ((LocalIndexInfo) item).getFileName().endsWith(BINARY_SRTM_MAP_INDEX_EXT);
		} else if (item instanceof SrtmDownloadItem) {
			return ((SrtmDownloadItem) item).useMeters;
		} else if (item instanceof MultipleDownloadItem) {
			for (DownloadItem downloadItem : ((MultipleDownloadItem) item).getAllItems()) {
				return isMetersItem(downloadItem);
			}
		}
		return false;
	}

	public static boolean containsSrtmExtension(@NonNull String fileName) {
		return fileName.contains(IndexConstants.BINARY_SRTM_MAP_INDEX_EXT)
				|| fileName.contains(IndexConstants.BINARY_SRTM_FEET_MAP_INDEX_EXT);
	}

	public static boolean isSrtmFile(@NonNull String fileName) {
		return fileName.endsWith(IndexConstants.BINARY_SRTM_MAP_INDEX_EXT)
				|| fileName.endsWith(IndexConstants.BINARY_SRTM_FEET_MAP_INDEX_EXT);
	}

	@NonNull
	public static String getExtension(IndexItem indexItem) {
		return isMetersItem(indexItem) ?
				IndexConstants.BINARY_SRTM_MAP_INDEX_EXT :
				IndexConstants.BINARY_SRTM_FEET_MAP_INDEX_EXT;
	}

	public static boolean isSRTMItem(Object item) {
		if (item instanceof IndexItem) {
			return ((IndexItem) item).getType() == SRTM_COUNTRY_FILE;
		} else if (item instanceof DownloadItem) {
			return ((DownloadItem) item).getType() == SRTM_COUNTRY_FILE;
		} else if (item instanceof LocalIndexInfo) {
			return ((LocalIndexInfo) item).getType() == SRTM_DATA;
		}
		return false;
	}

}
