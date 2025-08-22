package net.osmand.plus.views.mapwidgets.widgetinterfaces;

public interface ISupportMultiRow {
	default void updateValueAlign(boolean fullRow) {}
	default void updateHiddenNameText() {}
	void updateFullRowState(int widgetsCount);
}
