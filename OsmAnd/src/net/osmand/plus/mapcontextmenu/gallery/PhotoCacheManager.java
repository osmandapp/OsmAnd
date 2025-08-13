package net.osmand.plus.mapcontextmenu.gallery;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

public class PhotoCacheManager {
	private static final Log LOG = PlatformUtil.getLog(PhotoCacheManager.class);

	private static final String CACHE_DIR = "online_photos_list_cache";
	private static final int MAX_CACHE_ITEMS = 100;

	private final File cacheDir;

	public PhotoCacheManager(@NonNull Context context) {
		cacheDir = new File(context.getCacheDir(), CACHE_DIR);
		if (!cacheDir.exists()) {
			cacheDir.mkdirs();
		}
	}

	public void save(@NonNull String keyRaw, @NonNull String json) {
		String fileName = hashKey(keyRaw) + ".json";
		if (Algorithms.isEmpty(fileName)) {
			return;
		}
		File file = new File(cacheDir, fileName);
		try (FileWriter writer = new FileWriter(file)) {
			writer.write(json);
			cleanupIfNeeded();
		} catch (IOException e) {
			LOG.error("Error trying to save json photos list: " + e);
		}
	}

	@Nullable
	public String load(String keyRaw) {
		String fileName = hashKey(keyRaw);
		if (Algorithms.isEmpty(fileName)) {
			return null;
		}
		File file = new File(cacheDir, fileName + ".json");
		if (!file.exists()) return null;
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			return reader.lines().collect(Collectors.joining());
		} catch (IOException e) {
			LOG.error("Error trying to load cached json photos list: " + e);
			return null;
		}
	}

	public boolean exists(String keyRaw) {
		String fileName = hashKey(keyRaw);
		if (Algorithms.isEmpty(fileName)) {
			return false;
		}
		File file = new File(cacheDir, fileName + ".json");
		return file.exists();
	}

	private void cleanupIfNeeded() {
		File[] files = cacheDir.listFiles();
		if (files != null && files.length > MAX_CACHE_ITEMS) {
			Arrays.sort(files, Comparator.comparingLong(File::lastModified));
			for (int i = 0; i < files.length - MAX_CACHE_ITEMS; i++) {
				files[i].delete();
			}
		}
	}

	@Nullable
	private String hashKey(String key) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] messageDigest = md.digest(key.getBytes(StandardCharsets.UTF_8));
			BigInteger bigInteger = new BigInteger(1, messageDigest);
			StringBuilder hashText = new StringBuilder(bigInteger.toString(16));
			while (hashText.length() < 32) hashText.insert(0, "0");
			return hashText.toString();
		} catch (NoSuchAlgorithmException e) {
			LOG.error("Error trying to get hash for key: " + key + " exception: " + e);
			return null;
		}
	}

	@NonNull
	public static String buildRawKey(String wikidataId, String wikiCategory, String wikiTitle) {
		StringBuilder builder = new StringBuilder();
		if (wikidataId != null && !wikidataId.isEmpty()) {
			builder.append("article=").append(wikidataId).append("&");
		}
		if (wikiCategory != null && !wikiCategory.isEmpty()) {
			builder.append("category=").append(wikiCategory).append("&");
		}
		if (wikiTitle != null && !wikiTitle.isEmpty()) {
			builder.append("wiki=").append(wikiTitle);
		}
		return builder.toString();
	}
}