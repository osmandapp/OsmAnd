package net.osmand.plus.widgets.ctxmenu.callback;

import androidx.annotation.NonNull;

public interface OnDataChangeUiAdapter {
	void onDataSetChanged();
	void onDataSetInvalidated();
	void onRefreshItem(@NonNull String itemId);
}
