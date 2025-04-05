package net.osmand.plus.plugins.rastermaps;

import net.osmand.map.TileSourceManager;

import java.util.List;
import java.util.Optional;

public class TileSourceTemplatesProvider {

	private final TileSourceTemplatesDownloader tileSourceTemplatesDownloader;
	private Optional<List<TileSourceManager.TileSourceTemplate>> cache = Optional.empty();
	private boolean cacheEnabled;

	public TileSourceTemplatesProvider(final TileSourceTemplatesDownloader tileSourceTemplatesDownloader,
									   final boolean cacheEnabled) {
		this.tileSourceTemplatesDownloader = tileSourceTemplatesDownloader;
		this.cacheEnabled = cacheEnabled;
	}

	public List<TileSourceManager.TileSourceTemplate> getTileSourceTemplates() {
		if (cache.isEmpty() || !cacheEnabled) {
			cache = Optional.of(tileSourceTemplatesDownloader.downloadTileSourceTemplates());
		}
		return cache.orElseThrow();
	}

	public void disableCache() {
		this.cacheEnabled = false;
	}

	public void enableCache() {
		this.cacheEnabled = true;
	}
}
