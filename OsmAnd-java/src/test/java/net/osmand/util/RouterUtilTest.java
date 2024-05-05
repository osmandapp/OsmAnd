package net.osmand.util;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class RouterUtilTest {

	private static final String ROAD_INFO_DELIMITER = ":";
	public static boolean LOCAL_ANDROID_BUILD = true;

	public static long getRoadId(String roadInfo) {
		if (roadInfo.contains(ROAD_INFO_DELIMITER)) {
			return Long.parseLong(roadInfo.split(ROAD_INFO_DELIMITER)[0]);
		}
		return Long.parseLong(roadInfo);
	}

	public static int getRoadStartPoint(String roadInfo) {
		if (roadInfo.contains(ROAD_INFO_DELIMITER)) {
			return Integer.parseInt(roadInfo.split(ROAD_INFO_DELIMITER)[1]);
		}
		return -1;
	}

	public static Set<Long> getExpectedIdSet(Map<String, String> expectedResults) {
		Set<Long> expectedSegments = new HashSet<>();
		for (String roadInfo : expectedResults.keySet()) {
			expectedSegments.add(RouterUtilTest.getRoadId(roadInfo));
		}
		return expectedSegments;
	}

	static boolean isAndroidEnvironment() {
		try {
			Class.forName("android.os.Build");
			return true;
		} catch (ClassNotFoundException e) {
		}
		return "true".equals(System.getProperty("isAndroidBuild"));
	}

	public static String getNativeLibPath() {
		if (isAndroidEnvironment()) {
			return null;
		}
		Path path = FileSystems.getDefault().getPath("../../core-legacy/binaries");
		if (Files.exists(path)) {
			File nativeLibPath = path.normalize().toAbsolutePath().toFile();
			for (final File f1 : Objects.requireNonNull(nativeLibPath.listFiles())) {
				if (f1.isDirectory()) {
					for (final File f2 : Objects.requireNonNull(f1.listFiles())) {
						if (f2.isDirectory()) {
							File libDir = getLatestLib(f2.listFiles());
							return libDir == null ? f2.getAbsolutePath() : libDir.getAbsolutePath();
						}
					}
				}
			}
		}
		return null;
	}

	private static File getLatestLib(File[] f3) {
		File libDir = null;
		for (File f4 : Objects.requireNonNull(f3)) {
			if (f4.isDirectory() && (f4.getName().equals("Release") || f4.getName().equals("Debug"))) {
				if (libDir == null) {
					libDir = f4.getAbsoluteFile();
				} else {
					if (libDir.lastModified() < f4.lastModified()) {
						libDir = f4;
					}
				}
			}
		}
		return libDir;
	}
}
