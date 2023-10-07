package net.osmand.plus.keyevent.ui.keybindings;

class ScreenItem {

	public ScreenItemType type;
	public Object value;

	public ScreenItem(ScreenItemType type) {
		this(type, null);
	}

	public ScreenItem(ScreenItemType type, Object value) {
		this.type = type;
		this.value = value;
	}

}
