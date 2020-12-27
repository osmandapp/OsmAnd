package net.osmand.plus.profiles.onlinerouting;

public enum VehicleType {
	CAR("car", "Car"),
	BIKE("bike", "Bike"),
	FOOT("foot", "Foot"),
	DRIVING("driving", "Driving"),
	CUSTOM("custom", "Custom");

	VehicleType(String key, String title) {
		this.key = key;
		this.title = title;
	}

	private String key;
	private String title;

	public String getKey() {
		return key;
	}

	public String getTitle() {
		return title;
	}
}