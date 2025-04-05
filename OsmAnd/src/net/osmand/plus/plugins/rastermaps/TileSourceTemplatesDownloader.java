package net.osmand.plus.plugins.rastermaps;

import net.osmand.map.TileSourceManager;

import java.util.List;

public class TileSourceTemplatesDownloader {

	private final String versionAsURLParam;

	public TileSourceTemplatesDownloader(final String versionAsURLParam) {
		this.versionAsURLParam = versionAsURLParam;
	}

	public List<TileSourceManager.TileSourceTemplate> downloadTileSourceTemplates() {
		return TileSourceManager.downloadTileSourceTemplates(versionAsURLParam, true);
	}
}
