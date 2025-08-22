package net.osmand.plus.mapcontextmenu.builders.rows;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.mapcontextmenu.CollapsableView;
import net.osmand.util.Algorithms;

public class AmenityInfoRow {

	public final String key;
	public final Drawable icon;
	public final int iconId;
	public final String textPrefix;
	public final String text;
	public final String hiddenUrl;
	public final CollapsableView collapsableView;
	public final boolean collapsable;
	public final int textColor;
	public final boolean isWiki;
	public final boolean isText;
	public final boolean needLinks;
	public final boolean isPhoneNumber;
	public final boolean isUrl;
	public final int order;
	public final String name;
	public final boolean matchWidthDivider;
	public final int textLinesLimit;

	private AmenityInfoRow(@NonNull Builder builder) {
		this.key = builder.key;
		this.icon = builder.icon;
		this.iconId = builder.iconId;
		this.textPrefix = builder.textPrefix;
		this.text = builder.text;
		this.hiddenUrl = builder.hiddenUrl;
		this.collapsableView = builder.collapsableView;
		this.collapsable = builder.collapsableView != null;
		this.textColor = builder.textColor;
		this.isWiki = builder.isWiki;
		this.isText = builder.isText;
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
		private Drawable icon;
		private int iconId;
		private String textPrefix = "";
		private String text;
		private String hiddenUrl;
		private CollapsableView collapsableView;
		private int textColor;
		private boolean isWiki;
		private boolean isText;
		private boolean needLinks;
		private boolean isPhoneNumber;
		private boolean isUrl;
		private int order;
		private String name;
		private boolean matchWidthDivider;
		private int textLinesLimit = 0;


		public Builder(@NonNull String key) { this.key = key; }

		// Setters
		public Builder setIcon(Drawable icon) { this.icon = icon; return this; }
		public Builder setIconId(int iconId) { this.iconId = iconId; return this; }
		public Builder setTextPrefix(String textPrefix) { this.textPrefix = textPrefix; return this; }
		public Builder setTextPrefixIfNotPresent(String textPrefix) { if (!hasTextPrefix()) setTextPrefix(textPrefix); return this; }
		public Builder setText(String text) { this.text = text; return this; }
		public Builder setTextIfNotPresent(String text) { if (!hasText()) setText(text); return this; }
		public Builder setHiddenUrl(String hiddenUrl) { this.hiddenUrl = hiddenUrl; return this; }
		public Builder setCollapsableView(CollapsableView view) { this.collapsableView = view; return this; }
		public Builder setTextColor(int color) { this.textColor = color; return this; }
		public Builder setIsWiki(boolean wiki) { this.isWiki = wiki; return this; }
		public Builder setIsText(boolean textFlag) { this.isText = textFlag; return this; }
		public Builder setNeedLinks(boolean needLinks) { this.needLinks = needLinks; return this; }
		public Builder setIsPhoneNumber(boolean isPhoneNumber) { this.isPhoneNumber = isPhoneNumber; return this; }
		public Builder setIsUrl(boolean isUrl) { this.isUrl = isUrl; return this; }
		public Builder setOrder(int order) { this.order = order; return this; }
		public Builder setName(String name) { this.name = name; return this; }
		public Builder setMatchWidthDivider(boolean match) { this.matchWidthDivider = match; return this; }
		public Builder setTextLinesLimit(int limit) { this.textLinesLimit = limit; return this; }

		// Getters
		public String getKey() { return key; }
		public Drawable getIcon() { return icon; }
		public int getIconId() { return iconId; }
		public boolean hasIcon() { return iconId != 0 || icon != null; }
		public String getTextPrefix() { return textPrefix; }
		public boolean hasTextPrefix() { return !Algorithms.isEmpty(textPrefix); }
		public String getText() { return text; }
		public boolean hasText() { return !Algorithms.isEmpty(text); }
		public String getHiddenUrl() { return hiddenUrl; }
		public boolean hasHiddenUrl() { return !Algorithms.isEmpty(hiddenUrl); }
		public CollapsableView getCollapsableView() { return collapsableView; }
		public boolean isCollapsable() {return collapsableView != null;}
		public int getTextColor() { return textColor; }
		public boolean isWiki() { return isWiki; }
		public boolean isText() { return isText; }
		public boolean isNeedLinks() { return needLinks && collapsableView == null; }
		public boolean isPhoneNumber() { return isPhoneNumber; }
		public boolean isUrl() { return isUrl; }
		public int getOrder() { return order; }
		public String getName() { return name; }
		public boolean isDescription() { return isText && iconId == R.drawable.ic_action_info_dark; }
		public int getTextLinesLimit() { return textLinesLimit; }

		public AmenityInfoRow build() { return new AmenityInfoRow(this); }
	}
}