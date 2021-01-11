package net.osmand.plus.onlinerouting;

public enum EngineType {

	GRAPHHOPER("Graphhoper", "https://graphhopper.com/api/1/route"),
	OSRM("OSRM", "https://router.project-osrm.org/route/v1/"),
	ORS("Openroute Service", "https://api.openrouteservice.org/v2/directions/");

	private String title;
	private String standardUrl;

	EngineType(String title, String standardUrl) {
		this.title = title;
		this.standardUrl = standardUrl;
	}

	public String getTitle() {
		return title;
	}

	public String getStandardUrl() {
		return standardUrl;
	}
}
