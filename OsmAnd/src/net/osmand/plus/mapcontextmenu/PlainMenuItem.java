package net.osmand.plus.mapcontextmenu;

import android.view.View;

public class PlainMenuItem {

	private int iconId;
	private String buttonText;
	private String text;
	private boolean needLinks;
	private boolean url;
	private boolean collapsable;
	private CollapsableView collapsableView;
	private View.OnClickListener onClickListener;

	public PlainMenuItem(int iconId, String buttonText, String text, boolean needLinks, boolean url,
	                     boolean collapsable, CollapsableView collapsableView,
	                     View.OnClickListener onClickListener) {
		this.iconId = iconId;
		this.buttonText = buttonText;
		this.text = text;
		this.needLinks = needLinks;
		this.url = url;
		this.collapsable = collapsable;
		this.collapsableView = collapsableView;
		this.onClickListener = onClickListener;
	}

	public int getIconId() {
		return iconId;
	}

	public String getButtonText() {
		return buttonText;
	}

	public String getText() {
		return text;
	}

	public boolean isNeedLinks() {
		return needLinks;
	}

	public boolean isUrl() {
		return url;
	}

	public boolean isCollapsable() {
		return collapsable;
	}

	public CollapsableView getCollapsableView() {
		return collapsableView;
	}

	public View.OnClickListener getOnClickListener() {
		return onClickListener;
	}
}