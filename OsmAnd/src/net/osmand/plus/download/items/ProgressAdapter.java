package net.osmand.plus.download.items;

import net.osmand.plus.base.BasicProgressAsyncTask;

/**
 * Created by GaidamakUA on 10/16/15.
 */
public interface ProgressAdapter {
	void setProgress(BasicProgressAsyncTask<?, ?, ?> task, Object tag);
}
