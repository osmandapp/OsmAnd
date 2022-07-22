package net.osmand.map;

import java.io.IOException;
import java.util.Map;

public interface ITileSource {

	int getMaximumZoomSupported();

	String getName();

	int getTileSize();

	String getUrlToLoad(int x, int y, int zoom);

	String getUrlTemplate();

	byte[] getBytes(int x, int y, int zoom, String dirWithTiles) throws IOException;

	int getMinimumZoomSupported();

	String getTileFormat();

	int getBitDensity();

	boolean isEllipticYTile();

	boolean couldBeDownloadedFromInternet();

	long getExpirationTimeMillis();

	int getExpirationTimeMinutes();

	long getTileModifyTime(int x, int y, int zoom, String dirWithTiles);

	String getReferer();

	String getUserAgent();

	void deleteTiles(String path);

	int getAvgSize();

	String getRule();

	String getRandoms();

	boolean isInvertedYTile();

	boolean isTimeSupported();

	boolean getInversiveZoom();

	ParameterType getParamType();

	long getParamMin();

	long getParamStep();

	long getParamMax();

	Map<String, String> getUrlParameters();

	String getUrlParameter(String name);

	void setUrlParameter(String name, String value);

	void resetUrlParameter(String name);

	void resetUrlParameters();
}
