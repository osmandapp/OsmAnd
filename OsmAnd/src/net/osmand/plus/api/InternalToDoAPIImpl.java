package net.osmand.plus.api;

import java.io.File;
import java.util.List;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings.DayNightMode;
import net.osmand.plus.SQLiteTileSource;
import net.osmand.plus.activities.search.SearchHistoryHelper;

public class InternalToDoAPIImpl implements InternalToDoAPI {

	private OsmandApplication app;

	public InternalToDoAPIImpl(OsmandApplication app) {
		this.app = app;
	}

	@Override
	public void addNewItemToHistory(double latitude, double longitude, String historyDescription) {
		SearchHistoryHelper.getInstance().addNewItemToHistory(latitude, longitude, historyDescription, app);
	}

	@Override
	public void forceMapRendering() {
		app.getResourceManager().getRenderer().clearCache();
	}

	@Override
	public BinaryMapIndexReader[] getRoutingMapFiles() {
		return app.getResourceManager().getRoutingMapFiles();
	}

	@Override
	public void setDayNightMode(DayNightMode val) {
		app.getDaynightHelper().setDayNightMode(val);
	}

	@Override
	public ITileSource newSqliteTileSource(File dir, List<TileSourceTemplate> knownTemplates) {
		return new SQLiteTileSource(dir, knownTemplates);
	}

}
