package net.osmand.search.core;

public class SearchExportSettings {
	private boolean exportEmptyCities;
	private boolean exportBuildings;

	public SearchExportSettings() {
		exportEmptyCities = true;
		exportBuildings = true;
	}

	public SearchExportSettings(boolean exportEmptyCities, boolean exportBuildings) {
		this.exportEmptyCities = exportEmptyCities;
		this.exportBuildings = exportBuildings;
	}

	public boolean isExportEmptyCities() {
		return exportEmptyCities;
	}

	public void setExportEmptyCities(boolean exportEmptyCities) {
		this.exportEmptyCities = exportEmptyCities;
	}

	public boolean isExportBuildings() {
		return exportBuildings;
	}

	public void setExportBuildings(boolean exportBuildings) {
		this.exportBuildings = exportBuildings;
	}
}
