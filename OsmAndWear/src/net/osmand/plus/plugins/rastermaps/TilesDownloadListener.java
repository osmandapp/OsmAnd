package net.osmand.plus.plugins.rastermaps;

public interface TilesDownloadListener {

	void onTileDownloaded(long tileNumber, long cumulativeTilesSize);

	void onSuccessfulFinish();

	void onDownloadFailed();
}