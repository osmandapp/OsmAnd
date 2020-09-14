package net.osmand.map;

import java.io.IOException;

public interface ITileSource {

	public int getMaximumZoomSupported();

	public String getName();

	public int getTileSize();

	public String getUrlToLoad(int x, int y, int zoom);

	public String getUrlTemplate();

	public byte[] getBytes(int x, int y, int zoom, String dirWithTiles) throws IOException;

	public int getMinimumZoomSupported();

	public String getTileFormat();

	public int getBitDensity();

	public boolean isEllipticYTile();

	public boolean couldBeDownloadedFromInternet();

	public long getExpirationTimeMillis();

	public int getExpirationTimeMinutes();

	public String getReferer();

	public String getUserAgent();

	public void deleteTiles(String path);

	public int getAvgSize();

	public String getRule();

	public String getRandoms();

	public boolean isInvertedYTile();

	public boolean isTimeSupported();

	public boolean getInversiveZoom();
}
