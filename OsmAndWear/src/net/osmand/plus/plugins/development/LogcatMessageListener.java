package net.osmand.plus.plugins.development;

import java.util.List;

public interface LogcatMessageListener {
	void onLogcatLogs(String filterLevel, List<String> logs);
}
