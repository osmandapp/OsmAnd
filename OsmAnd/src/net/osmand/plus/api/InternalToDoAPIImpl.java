package net.osmand.plus.api;

import java.io.File;
import java.util.List;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.SQLiteTileSource;

public class InternalToDoAPIImpl implements InternalToDoAPI {

	private OsmandApplication app;

	public InternalToDoAPIImpl(OsmandApplication app) {
		this.app = app;
	}

	@Override
	public BinaryMapIndexReader[] getRoutingMapFiles() {
		return app.getResourceManager().getRoutingMapFiles();
	}

	@Override
	public ITileSource newSqliteTileSource(File dir, List<TileSourceTemplate> knownTemplates) {
		return new SQLiteTileSource(app, dir, knownTemplates);
	}

}
