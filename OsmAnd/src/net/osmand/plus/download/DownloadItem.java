package net.osmand.plus.download;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.map.OsmandRegions;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

import java.io.File;
import java.text.DateFormat;
import java.util.List;
import java.util.Locale;

public abstract class DownloadItem {

	protected DownloadActivityType type;
	protected DownloadResourceGroup relatedGroup;

	public DownloadItem(@NonNull DownloadActivityType type) {
		this.type = type;
	}

	@NonNull
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
	public String getSizeDescription(@NonNull Context ctx) {
		return getFormattedMb(ctx, getSizeToDownloadInMb());
	}

	public String getVisibleName(@NonNull Context ctx, OsmandRegions osmandRegions) {
		return getVisibleName(ctx, osmandRegions, true);
	}

	public String getVisibleName(@NonNull Context ctx, OsmandRegions osmandRegions, boolean includingParent) {
		return getVisibleName(ctx, osmandRegions, includingParent, false);
	}

	public String getVisibleName(@NonNull Context ctx, OsmandRegions osmandRegions, boolean includingParent, boolean useShortName) {
		return type.getVisibleName(this, ctx, osmandRegions, includingParent, useShortName);
	}

	public String getVisibleDescription(@NonNull OsmandApplication app) {
		return type.getVisibleDescription(this, app);
	}

	@NonNull
	public String getBasename() {
		return type.getBasename(this);
	}

	public boolean isDownloading(@NonNull OsmandApplication app) {
		return isDownloading(app.getDownloadThread());
	}

	@NonNull
	public abstract List<File> getDownloadedFiles(@NonNull OsmandApplication app);

	@Nullable
	public abstract String getAdditionalDescription(Context ctx);

	public abstract double getSizeToDownloadInMb();

	public abstract double getArchiveSizeMB();

	public abstract boolean isDownloaded();

	public abstract boolean isOutdated();

	public abstract boolean hasActualDataToDownload();

	public abstract boolean isDownloading(@NonNull DownloadIndexesThread thread);

	public abstract String getFileName();

	public abstract boolean isFree();

	public abstract String getFreeMessage();

	public abstract String getDate(@NonNull DateFormat dateFormat, boolean remote);

	@NonNull
	public static String getFormattedMb(@NonNull Context ctx, double sizeInMb) {
		String size = String.format(Locale.US, "%.2f", sizeInMb);
		return ctx.getString(R.string.ltr_or_rtl_combine_via_space, size, "MB");
	}

	@NonNull
	@Override
	public String toString() {
		return getFileName();
	}
}