package net.osmand.plus.api;

import java.io.File;
import java.util.List;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager.TileSourceTemplate;

public interface InternalToDoAPI {

	public BinaryMapIndexReader[] getRoutingMapFiles();
	
	public ITileSource newSqliteTileSource(File dir, List<TileSourceTemplate> knownTemplates);


}
