package net.osmand.map;

import java.io.IOException;

public interface ITileSource {

	public int getMaximumZoomSupported();

	public String getName();

	public int getTileSize();

	public String getUrlToLoad(int x, int y, int zoom);

	public byte[] getBytes(int x, int y, int zoom, String dirWithTiles) throws IOException;

	public int getMinimumZoomSupported();

	public String getTileFormat();

	public int getBitDensity();

	public boolean isEllipticYTile();

	public boolean couldBeDownloadedFromInternet();

	public int getExpirationTimeMillis();

	public int getExpirationTimeMinutes();
	
	public String getReferer();

	public void deleteTiles(String path);

}
