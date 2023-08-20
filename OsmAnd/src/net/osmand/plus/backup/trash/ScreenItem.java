package net.osmand.plus.backup.trash;

public class ScreenItem {

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
