package net.osmand.data;

import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.util.Algorithms;

import java.util.List;

public class AmenityTagEntry {

	public enum CollapsableEntryType {
		NONE, PLAIN, POI_TYPE_GROUP, ELEVATION_PILLS, OPENING_HOURS
	}

	public final String key;
	public final String value;
	public final AdditionalInfoBundle.ResolvedPoiType resolvedType;
	public final int iconId;
	public final List<String> iconNameCandidates;
	public final int fallbackIconId;
	public final String textPrefix;
	public final String text;
	public final String hiddenUrl;
	public final List<AmenityTagEntry> collapsableEntries;
	public final CollapsableEntryType collapsableEntryType;
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

	private AmenityTagEntry(Builder builder) {
		this.key = builder.key;
		this.value = builder.value;
		this.resolvedType = builder.resolvedType;
		this.iconId = builder.iconId;
		this.iconNameCandidates = builder.iconNameCandidates;
		this.fallbackIconId = builder.fallbackIconId;
		this.textPrefix = builder.textPrefix;
		this.text = builder.text;
		this.hiddenUrl = builder.hiddenUrl;
		this.collapsableEntries = builder.collapsableEntries;
		this.collapsableEntryType = builder.collapsableEntryType;
		this.collapsablePoiTypes = builder.collapsablePoiTypes;
		this.collapsableCategory = builder.collapsableCategory;
		this.poiAdditional = builder.poiAdditional;
		this.collapsable = builder.collapsableEntryType != CollapsableEntryType.NONE;
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
		private AdditionalInfoBundle.ResolvedPoiType resolvedType;
		private int iconId;
		private List<String> iconNameCandidates = List.of();
		private int fallbackIconId;
		private String textPrefix = "";
		private String text;
		private String hiddenUrl;
		private List<AmenityTagEntry> collapsableEntries;
		private CollapsableEntryType collapsableEntryType = CollapsableEntryType.NONE;
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

		public static Builder from(AmenityTagEntry entry) {
			return new Builder(entry.key)
					.setValue(entry.value)
					.setResolvedType(entry.resolvedType)
					.setIconId(entry.iconId)
					.setIconNameCandidates(entry.iconNameCandidates)
					.setFallbackIconId(entry.fallbackIconId)
					.setTextPrefix(entry.textPrefix)
					.setText(entry.text)
					.setHiddenUrl(entry.hiddenUrl)
					.setCollapsableEntries(entry.collapsableEntries)
					.setCollapsableEntryType(entry.collapsableEntryType)
					.setCollapsablePoiTypes(entry.collapsablePoiTypes)
					.setCollapsableCategory(entry.collapsableCategory)
					.setPoiAdditional(entry.poiAdditional)
					.setTextColor(entry.textColor)
					.setIsWiki(entry.isWiki)
					.setIsText(entry.isText)
					.setIsDescription(entry.isDescription)
					.setNeedLinks(entry.needLinks)
					.setIsPhoneNumber(entry.isPhoneNumber)
					.setIsUrl(entry.isUrl)
					.setOrder(entry.order)
					.setName(entry.name)
					.setMatchWidthDivider(entry.matchWidthDivider)
					.setTextLinesLimit(entry.textLinesLimit);
		}

		public Builder setValue(String value) {
			this.value = value;
			return this;
		}

		public Builder setResolvedType(AdditionalInfoBundle.ResolvedPoiType resolvedType) {
			this.resolvedType = resolvedType;
			return this;
		}

		public Builder setIconId(int iconId) {
			this.iconId = iconId;
			return this;
		}

		public Builder setIconNameCandidates(List<String> iconNameCandidates) {
			this.iconNameCandidates = iconNameCandidates;
			return this;
		}

		public Builder setFallbackIconId(int fallbackIconId) {
			this.fallbackIconId = fallbackIconId;
			return this;
		}

		public Builder setTextPrefix(String textPrefix) {
			this.textPrefix = textPrefix;
			return this;
		}

		public Builder setText(String text) {
			this.text = text;
			return this;
		}

		public Builder setHiddenUrl(String hiddenUrl) {
			this.hiddenUrl = hiddenUrl;
			return this;
		}

		public Builder setCollapsableEntries(List<AmenityTagEntry> collapsableEntries) {
			this.collapsableEntries = collapsableEntries;
			if (collapsableEntryType == CollapsableEntryType.NONE && collapsableEntries != null) {
				collapsableEntryType = CollapsableEntryType.PLAIN;
			}
			return this;
		}

		public Builder setCollapsableEntryType(CollapsableEntryType collapsableEntryType) {
			this.collapsableEntryType = collapsableEntryType;
			return this;
		}

		public Builder setCollapsablePoiTypes(List<PoiType> collapsablePoiTypes) {
			this.collapsablePoiTypes = collapsablePoiTypes;
			return this;
		}

		public Builder setCollapsableCategory(PoiCategory collapsableCategory) {
			this.collapsableCategory = collapsableCategory;
			return this;
		}

		public Builder setPoiAdditional(boolean poiAdditional) {
			this.poiAdditional = poiAdditional;
			return this;
		}

		public Builder setTextColor(int color) {
			this.textColor = color;
			return this;
		}

		public Builder setIsWiki(boolean wiki) {
			this.isWiki = wiki;
			return this;
		}

		public Builder setIsText(boolean textFlag) {
			this.isText = textFlag;
			return this;
		}

		public Builder setIsDescription(boolean isDescription) {
			this.isDescription = isDescription;
			return this;
		}

		public Builder setNeedLinks(boolean needLinks) {
			this.needLinks = needLinks;
			return this;
		}

		public Builder setIsPhoneNumber(boolean isPhoneNumber) {
			this.isPhoneNumber = isPhoneNumber;
			return this;
		}

		public Builder setIsUrl(boolean isUrl) {
			this.isUrl = isUrl;
			return this;
		}

		public Builder setOrder(int order) {
			this.order = order;
			return this;
		}

		public Builder setName(String name) {
			this.name = name;
			return this;
		}

		public Builder setMatchWidthDivider(boolean match) {
			this.matchWidthDivider = match;
			return this;
		}

		public Builder setTextLinesLimit(int limit) {
			this.textLinesLimit = limit;
			return this;
		}

		public Builder setTextIfNotPresent(String text) {
			if (!hasText()) setText(text);
			return this;
		}

		public Builder setTextPrefixIfNotPresent(String textPrefix) {
			if (!hasTextPrefix()) setTextPrefix(textPrefix);
			return this;
		}

		public String getKey() {
			return key;
		}

		public String getValue() {
			return value;
		}

		public int getIconId() {
			return iconId;
		}

		public boolean hasIcon() {
			return iconId != 0;
		}

		public String getTextPrefix() {
			return textPrefix;
		}

		public boolean hasTextPrefix() {
			return !Algorithms.isEmpty(textPrefix);
		}

		public String getText() {
			return text;
		}

		public boolean hasText() {
			return !Algorithms.isEmpty(text);
		}

		public String getHiddenUrl() {
			return hiddenUrl;
		}

		public boolean hasHiddenUrl() {
			return !Algorithms.isEmpty(hiddenUrl);
		}

		public CollapsableEntryType getCollapsableEntryType() {
			return collapsableEntryType;
		}

		public boolean isWiki() {
			return isWiki;
		}

		public boolean isText() {
			return isText;
		}

		public boolean isDescription() {
			return isDescription;
		}

		public boolean isNeedLinks() {
			return needLinks && collapsableEntryType == CollapsableEntryType.NONE;
		}

		public AmenityTagEntry build() {
			return new AmenityTagEntry(this);
		}
	}
}
