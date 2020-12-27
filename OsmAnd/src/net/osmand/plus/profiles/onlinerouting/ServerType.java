package net.osmand.plus.profiles.onlinerouting;

public enum ServerType {
	GRAPHHOPER("Graphhoper", "https://graphhopper.com/api/1/route?"),
	OSRM("OSRM", "https://zlzk.biz/route/v1/");

	ServerType(String title, String baseUrl) {
		this.title = title;
		this.baseUrl = baseUrl;
	}

	private String title;
	private String baseUrl;

	public String getTitle() {
		return title;
	}

	public String getBaseUrl() {
		return baseUrl;
	}
}
