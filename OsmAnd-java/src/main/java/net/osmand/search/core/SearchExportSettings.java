package net.osmand.search.core;

public class SearchExportSettings {
	private boolean exportEmptyCities;
	private boolean exportBuildings;
	private double maxDistance;

	public SearchExportSettings() {
		exportEmptyCities = true;
		exportBuildings = true;
		maxDistance = -1;
	}

	public SearchExportSettings(boolean exportEmptyCities, boolean exportBuildings, double maxDistance) {
		this.exportEmptyCities = exportEmptyCities;
		this.exportBuildings = exportBuildings;
		this.maxDistance = maxDistance;
	}

	public boolean isExportEmptyCities() {
		return exportEmptyCities;
	}

	public boolean isExportBuildings() {
		return exportBuildings;
	}

	public double getMaxDistance() {
		return maxDistance;
	}
}
