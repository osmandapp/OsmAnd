package net.osmand.plus.download.ui;

import net.osmand.plus.download.local.LocalItem;

public interface AbstractLoadLocalIndexTask {
	void loadFile(LocalItem... loaded);
}
