package net.osmand.plus.download;

public interface DisplayItem {

	DownloadActivityType getType();

	boolean isDownloaded();

	boolean isOutdated();

	double getContentSizeMB();

	double getArchiveSizeMB();

	String getBasename();

	boolean isDownloading(DownloadIndexesThread thread);

	DownloadResourceGroup getRelatedGroup();

	void setRelatedGroup(DownloadResourceGroup relatedGroup);

}
