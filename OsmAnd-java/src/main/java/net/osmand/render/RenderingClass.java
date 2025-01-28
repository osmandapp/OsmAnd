package net.osmand.render;

public class RenderingClass {

	private final String name;
	private final String title;
	private final boolean enabledByDefault;

	public RenderingClass(String name, String title, boolean enabledByDefault) {
		this.name = name;
		this.title = title;
		this.enabledByDefault = enabledByDefault;
	}

	public String getName() {
		return name;
	}

	public String getTitle() {
		return title;
	}

	public boolean isEnabledByDefault() {
		return enabledByDefault;
	}

	@Override
	public String toString() {
		return name;
	}
}