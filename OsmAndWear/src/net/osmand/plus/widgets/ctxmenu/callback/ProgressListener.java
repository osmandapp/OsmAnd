package net.osmand.plus.widgets.ctxmenu.callback;

import android.widget.ArrayAdapter;

import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;

public interface ProgressListener {
	boolean onProgressChanged(Object progressObject, int progress,
	                          ArrayAdapter<ContextMenuItem> adapter,
	                          int itemId, int position);
}
