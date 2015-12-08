package net.osmand.plus.download.ui;

import net.osmand.plus.activities.LocalIndexInfo;

public interface AbstractLoadLocalIndexTask {
	void loadFile(LocalIndexInfo... loaded);
}
