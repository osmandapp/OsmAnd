package net.osmand.plus.download;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IndexConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.LocalIndexInfo;
import net.osmand.plus.helpers.enums.MetricsConstants;

import java.io.File;
import java.text.DateFormat;
import java.util.List;

import static net.osmand.IndexConstants.BINARY_SRTM_MAP_INDEX_EXT;
import static net.osmand.IndexConstants.BINARY_SRTM_MAP_INDEX_EXT_ZIP;
import static net.osmand.plus.activities.LocalIndexHelper.LocalIndexType.SRTM_DATA;
import static net.osmand.plus.download.DownloadActivityType.SRTM_COUNTRY_FILE;

public class SrtmDownloadItem extends DownloadItem {

	private List<IndexItem> indexes;
	private IndexItem meter;
	private IndexItem feet;

	private boolean shouldUseMeters;

	public SrtmDownloadItem(List<IndexItem> indexes,
	                        boolean shouldUseMeters) {
		super(SRTM_COUNTRY_FILE);
		this.indexes = indexes;
		this.shouldUseMeters = shouldUseMeters;
	}

	public boolean isShouldUseMeters() {
		return shouldUseMeters;
	}

	public void setShouldUseMeters(boolean shouldUseMeters) {
		this.shouldUseMeters = shouldUseMeters;
	}

	public IndexItem getIndexItem() {
		return shouldUseMeters ? getMeterItem() : getFeetItem();
	}

	@Nullable
	public IndexItem getMeterItem() {
		if (meter == null && indexes != null) {
			for (IndexItem index : indexes) {
				if (isMetersItem(index)) {
					meter = index;
				}
			}
		}
		return meter;
	}

	@Nullable
	public IndexItem getFeetItem() {
		if (feet == null && indexes != null) {
			for (IndexItem index : indexes) {
				if (!isMetersItem(index)) {
					feet = index;
				}
			}
		}
		return feet;
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
	public boolean isDownloaded() {
		return meter.isDownloaded() || feet.isDownloaded();
	}

	@Override
	public boolean isOutdated() {
		return meter.isOutdated() || feet.isOutdated();
	}

	@Override
	public boolean hasActualDataToDownload() {
		return getIndexItem().hasActualDataToDownload();
	}

	@Override
	public boolean isDownloading(@NonNull DownloadIndexesThread thread) {
		return getMeterItem().isDownloading(thread) || getFeetItem().isDownloading(thread);
	}

	@Override
	public String getFileName() {
		return getIndexItem().getFileName();
	}

	@NonNull
	@Override
	public List<File> getDownloadedFiles(@NonNull OsmandApplication app) {
		return getIndexItem().getDownloadedFiles(app);
	}

	public String getDate(@NonNull DateFormat dateFormat, boolean remote) {
		return getIndexItem().getDate(dateFormat, remote);
	}

	public static boolean shouldUseMetersByDefault(@NonNull OsmandApplication app) {
		return app.getSettings().METRIC_SYSTEM.get() != MetricsConstants.MILES_AND_FEET;
	}

	@NonNull
	public static String getAbbreviationInScopes(Context ctx, boolean base) {
		return "(" + getAbbreviation(ctx, base) + ")";
	}

	@NonNull
	public static String getAbbreviation(Context context, boolean base) {
		return context.getString(base ? R.string.m : R.string.foot);
	}

	public static boolean isMetersItem(Object item) {
		if (item instanceof IndexItem) {
			return ((IndexItem) item).getFileName().endsWith(BINARY_SRTM_MAP_INDEX_EXT_ZIP);
		} else if (item instanceof LocalIndexInfo) {
			return ((LocalIndexInfo) item).getFileName().endsWith(BINARY_SRTM_MAP_INDEX_EXT);
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
