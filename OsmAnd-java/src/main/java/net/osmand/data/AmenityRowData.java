package net.osmand.data;

import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.util.Algorithms;

import java.util.List;

public class AmenityRowData {

	public enum CollapsableRowType {
		NONE, PLAIN, POI_TYPE_GROUP, ELEVATION_PILLS, OPENING_HOURS
	}

	public final String key;
	public final String value;
	public final int iconId;
	public final String iconName;
	public final String textPrefix;
	public final String text;
	public final String hiddenUrl;
	public final List<AmenityRowData> collapsableRows;
	public final CollapsableRowType collapsableRowType;
	public final List<PoiType> collapsablePoiTypes;
	public final PoiCategory collapsableCategory;
	public final boolean poiAdditional;
	public final boolean collapsable;
	public final int textColor;
	public final boolean isWiki;
	public final boolean isText;
	public final boolean isDescription;
	public final boolean needLinks;
	public final boolean isPhoneNumber;
	public final boolean isUrl;
	public final int order;
	public final String name;
	public final boolean matchWidthDivider;
	public final int textLinesLimit;

	private AmenityRowData(Builder builder) {
		this.key = builder.key;
		this.value = builder.value;
		this.iconId = builder.iconId;
		this.iconName = builder.iconName;
		this.textPrefix = builder.textPrefix;
		this.text = builder.text;
		this.hiddenUrl = builder.hiddenUrl;
		this.collapsableRows = builder.collapsableRows;
		this.collapsableRowType = builder.collapsableRowType;
		this.collapsablePoiTypes = builder.collapsablePoiTypes;
		this.collapsableCategory = builder.collapsableCategory;
		this.poiAdditional = builder.poiAdditional;
		this.collapsable = builder.collapsableRowType != CollapsableRowType.NONE;
		this.textColor = builder.textColor;
		this.isWiki = builder.isWiki;
		this.isText = builder.isText;
		this.isDescription = builder.isDescription;
		this.needLinks = builder.needLinks;
		this.isPhoneNumber = builder.isPhoneNumber;
		this.isUrl = builder.isUrl;
		this.order = builder.order;
		this.name = builder.name;
		this.matchWidthDivider = builder.matchWidthDivider;
		this.textLinesLimit = builder.textLinesLimit;
	}

	public static class Builder {
		private final String key;
		private String value;
		private int iconId;
		private String iconName;
		private String textPrefix = "";
		private String text;
		private String hiddenUrl;
		private List<AmenityRowData> collapsableRows;
		private CollapsableRowType collapsableRowType = CollapsableRowType.NONE;
		private List<PoiType> collapsablePoiTypes;
		private PoiCategory collapsableCategory;
		private boolean poiAdditional;
		private int textColor;
		private boolean isWiki;
		private boolean isText;
		private boolean isDescription;
		private boolean needLinks;
		private boolean isPhoneNumber;
		private boolean isUrl;
		private int order;
		private String name;
		private boolean matchWidthDivider;
		private int textLinesLimit = 0;

		public Builder(String key) {
			this.key = key;
		}

		public Builder setValue(String value) { this.value = value; return this; }
		public Builder setIconId(int iconId) { this.iconId = iconId; return this; }
		public Builder setIconName(String iconName) { this.iconName = iconName; return this; }
		public Builder setTextPrefix(String textPrefix) { this.textPrefix = textPrefix; return this; }
		public Builder setText(String text) { this.text = text; return this; }
		public Builder setHiddenUrl(String hiddenUrl) { this.hiddenUrl = hiddenUrl; return this; }

		public Builder setCollapsableRows(List<AmenityRowData> collapsableRows) {
			this.collapsableRows = collapsableRows;
			if (collapsableRowType == CollapsableRowType.NONE && collapsableRows != null) {
				collapsableRowType = CollapsableRowType.PLAIN;
			}
			return this;
		}
		public Builder setCollapsableRowType(CollapsableRowType collapsableRowType) { this.collapsableRowType = collapsableRowType; return this; }
		public Builder setCollapsablePoiTypes(List<PoiType> collapsablePoiTypes) { this.collapsablePoiTypes = collapsablePoiTypes; return this; }
		public Builder setCollapsableCategory(PoiCategory collapsableCategory) { this.collapsableCategory = collapsableCategory; return this; }
		public Builder setPoiAdditional(boolean poiAdditional) { this.poiAdditional = poiAdditional; return this; }

		public Builder setTextColor(int color) { this.textColor = color; return this; }
		public Builder setIsWiki(boolean wiki) { this.isWiki = wiki; return this; }
		public Builder setIsText(boolean textFlag) { this.isText = textFlag; return this; }
		public Builder setIsDescription(boolean isDescription) { this.isDescription = isDescription; return this; }
		public Builder setNeedLinks(boolean needLinks) { this.needLinks = needLinks; return this; }
		public Builder setIsPhoneNumber(boolean isPhoneNumber) { this.isPhoneNumber = isPhoneNumber; return this; }
		public Builder setIsUrl(boolean isUrl) { this.isUrl = isUrl; return this; }
		public Builder setOrder(int order) { this.order = order; return this; }
		public Builder setName(String name) { this.name = name; return this; }
		public Builder setMatchWidthDivider(boolean match) { this.matchWidthDivider = match; return this; }
		public Builder setTextLinesLimit(int limit) { this.textLinesLimit = limit; return this; }

		public Builder setTextIfNotPresent(String text) { if (!hasText()) setText(text); return this; }
		public Builder setTextPrefixIfNotPresent(String textPrefix) { if (!hasTextPrefix()) setTextPrefix(textPrefix); return this; }

		public String getKey() { return key; }
		public String getValue() { return value; }
		public int getIconId() { return iconId; }
		public String getIconName() { return iconName; }
		public boolean hasIcon() { return iconId != 0; }
		public String getTextPrefix() { return textPrefix; }
		public boolean hasTextPrefix() { return !Algorithms.isEmpty(textPrefix); }
		public String getText() { return text; }
		public boolean hasText() { return !Algorithms.isEmpty(text); }
		public String getHiddenUrl() { return hiddenUrl; }
		public boolean hasHiddenUrl() { return !Algorithms.isEmpty(hiddenUrl); }
		public CollapsableRowType getCollapsableRowType() { return collapsableRowType; }
		public boolean isWiki() { return isWiki; }
		public boolean isText() { return isText; }
		public boolean isDescription() { return isDescription; }
		public boolean isNeedLinks() { return needLinks && collapsableRowType == CollapsableRowType.NONE; }

		public AmenityRowData build() {
			return new AmenityRowData(this);
		}
	}
}
