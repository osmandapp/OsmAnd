package net.osmand.plus.download.ui;

import net.osmand.plus.download.LocalIndexInfo;

public interface AbstractLoadLocalIndexTask {
	void loadFile(LocalIndexInfo... loaded);
}
