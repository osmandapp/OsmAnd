package net.osmand.plus.download;

import android.content.Context;

import androidx.annotation.NonNull;

import net.osmand.map.OsmandRegions;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

import java.io.File;
import java.util.List;
import java.util.Locale;

public abstract class DownloadItem {

	protected DownloadActivityType type;
	protected DownloadResourceGroup relatedGroup;

	public DownloadItem(DownloadActivityType type) {
		this.type = type;
	}

	public DownloadActivityType getType() {
		return type;
	}

	public void setRelatedGroup(DownloadResourceGroup relatedGroup) {
		this.relatedGroup = relatedGroup;
	}

	public DownloadResourceGroup getRelatedGroup() {
		return relatedGroup;
	}

	@NonNull
	public String getSizeDescription(Context ctx) {
		String size = String.format(Locale.US, "%.2f", getSizeToDownloadInMb());
		return ctx.getString(R.string.ltr_or_rtl_combine_via_space, size, "MB");
	}

	public String getVisibleName(Context ctx, OsmandRegions osmandRegions) {
		return type.getVisibleName(this, ctx, osmandRegions, true);
	}

	public String getVisibleName(Context ctx, OsmandRegions osmandRegions, boolean includingParent) {
		return type.getVisibleName(this, ctx, osmandRegions, includingParent);
	}

	public String getVisibleDescription(OsmandApplication ctx) {
		return type.getVisibleDescription(this, ctx);
	}

	public String getBasename() {
		return type.getBasename(this);
	}

	protected abstract double getSizeToDownloadInMb();

	public abstract double getArchiveSizeMB();

	public abstract boolean isDownloaded();

	public abstract boolean isOutdated();

	public abstract boolean hasActualDataToDownload();

	public abstract boolean isDownloading(DownloadIndexesThread thread);

	public abstract String getFileName();

	@NonNull
	public abstract List<File> getDownloadedFiles(OsmandApplication app);

}
