package net.osmand.plus.views.mapwidgets.widgetinterfaces;

public interface ISupportMultiRow {
	default void updateValueAlign(boolean fullRow) {}
	void updateFullRowState(int widgetsCount);
}
