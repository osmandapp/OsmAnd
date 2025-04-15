package net.osmand.render;

import net.osmand.util.Algorithms;

public class RenderingClass {

	private final String title;
	private final String description;
	private final String category;
	private final String legendObject;
	private final String innerLegendObject;
	private final String innerTitle;
	private final String innerDescription;
	private final String innerCategory;
	private final String innerNames;
	private final boolean enabledByDefault;
	private final String name;

	public RenderingClass(String title, String description, String category, String legendObject,
			String innerLegendObject, String innerTitle, String innerDescription,
			String innerCategory, String innerNames, boolean enabledByDefault, String name) {
		this.title = title;
		this.description = description;
		this.category = category;
		this.legendObject = legendObject;
		this.innerLegendObject = innerLegendObject;
		this.innerTitle = innerTitle;
		this.innerDescription = innerDescription;
		this.innerCategory = innerCategory;
		this.innerNames = innerNames;
		this.enabledByDefault = enabledByDefault;
		this.name = name;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public String getCategory() {
		return category;
	}

	public String getLegendObject() {
		return legendObject;
	}

	public String getInnerLegendObject() {
		return innerLegendObject;
	}

	public String getInnerTitle() {
		return innerTitle;
	}

	public String getInnerDescription() {
		return innerDescription;
	}

	public String getInnerCategory() {
		return innerCategory;
	}

	public String getInnerNames() {
		return innerNames;
	}

	public boolean isEnabledByDefault() {
		return enabledByDefault;
	}

	public String getName() {
		return name;
	}

	public String getParentName() {
		return Algorithms.getFileNameWithoutExtension(name);
	}

	@Override
	public String toString() {
		return name;
	}
}