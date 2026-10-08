package net.osmand.plus.plugins.rastermaps;

public interface TilesDownloadListener {

	void onTileDownloaded(long tileNumber, long cumulativeTilesSize);

	void onTileFailed(long failedTilesNumber);

	void onWaitingForServer(boolean waiting);

	void onSuccessfulFinish();

	void onDownloadFailed();
}