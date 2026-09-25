package net.osmand.plus.mapcontextmenu.builders;

import android.graphics.drawable.Drawable;

import net.osmand.plus.mapcontextmenu.CollapsableView;

public class AmenityInfoRow {

	public final String key;
	public Drawable icon;
	public int iconId;
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

	public AmenityInfoRow(String key, Drawable icon, String textPrefix, String text,
	                      String hiddenUrl, boolean collapsable,
	                      CollapsableView collapsableView, int textColor, boolean isWiki,
	                      boolean isText, boolean needLinks, int order, String name,
	                      boolean isPhoneNumber, boolean isUrl,
	                      boolean matchWidthDivider, int textLinesLimit) {
		this.key = key;
		this.icon = icon;
		this.textPrefix = textPrefix;
		this.text = text;
		this.hiddenUrl = hiddenUrl;
		this.collapsable = collapsable;
		this.collapsableView = collapsableView;
		this.textColor = textColor;
		this.isWiki = isWiki;
		this.isText = isText;
		this.needLinks = needLinks;
		this.order = order;
		this.name = name;
		this.isPhoneNumber = isPhoneNumber;
		this.isUrl = isUrl;
		this.matchWidthDivider = matchWidthDivider;
		this.textLinesLimit = textLinesLimit;
	}

	public AmenityInfoRow(String key, int iconId, String textPrefix, String text,
	                      String hiddenUrl, boolean collapsable,
	                      CollapsableView collapsableView, int textColor, boolean isWiki,
	                      boolean isText, boolean needLinks, int order, String name,
	                      boolean isPhoneNumber, boolean isUrl,
	                      boolean matchWidthDivider, int textLinesLimit) {
		this.key = key;
		this.iconId = iconId;
		this.textPrefix = textPrefix;
		this.text = text;
		this.hiddenUrl = hiddenUrl;
		this.collapsable = collapsable;
		this.collapsableView = collapsableView;
		this.textColor = textColor;
		this.isWiki = isWiki;
		this.isText = isText;
		this.needLinks = needLinks;
		this.order = order;
		this.name = name;
		this.isPhoneNumber = isPhoneNumber;
		this.isUrl = isUrl;
		this.matchWidthDivider = matchWidthDivider;
		this.textLinesLimit = textLinesLimit;
	}
}
