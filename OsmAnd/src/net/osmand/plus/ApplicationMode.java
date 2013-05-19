package net.osmand.plus;


public enum ApplicationMode {
	/*
	 * DEFAULT("Default"), CAR("Car"), BICYCLE("Bicycle"), PEDESTRIAN("Pedestrian");
	 */
	DEFAULT(R.string.app_mode_default), 
	CAR(R.string.app_mode_car), 
	BICYCLE(R.string.app_mode_bicycle), 
	PEDESTRIAN(R.string.app_mode_pedestrian);

	private final int key;

	ApplicationMode(int key) {
		this.key = key;
	}

	public String toHumanString(ClientContext ctx) {
		return ctx.getString(key);
	}

}