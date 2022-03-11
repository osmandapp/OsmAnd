package net.osmand.plus.widgets.cmadapter.callback;

import android.widget.ArrayAdapter;

import net.osmand.plus.widgets.cmadapter.ContextMenuItem;

public interface ProgressListener {
	boolean onProgressChanged(Object progressObject, int progress,
	                          ArrayAdapter<ContextMenuItem> adapter,
	                          int itemId, int position);
}
