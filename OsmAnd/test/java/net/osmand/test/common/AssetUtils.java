package net.osmand.test.common;

import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.FileUtils;
import net.osmand.util.Algorithms;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class AssetUtils {

	@NonNull
	public static Uri getUriFromAssetFile(@NonNull OsmandApplication app, @NonNull String assetPath, @NonNull String assetName) throws IOException {
		File cacheDir = new File(FileUtils.getTempDir(app), "assets/" + assetPath);
		if (!cacheDir.exists())
			cacheDir.mkdirs();
		File fileToShare = new File(cacheDir, assetName);

		AssetManager assetManager = InstrumentationRegistry.getInstrumentation().getContext().getAssets();
		InputStream in = assetManager.open(assetPath + "/" + assetName);
		OutputStream out = Files.newOutputStream(fileToShare.toPath());

		byte[] buffer = new byte[4096];
		int read;
		while ((read = in.read(buffer)) != -1) {
			out.write(buffer, 0, read);
		}
		in.close();
		out.flush();
		out.close();

		return AndroidUtils.getUriForFile(app, fileToShare);
	}

	public static void copyAssetToFile(@NonNull Context testContext, String assetName, File destFile) throws IOException {
		if(destFile.exists()) {
			destFile.delete();
		}
		try (InputStream in = testContext.getAssets().open(assetName);
		     FileOutputStream out = new FileOutputStream(destFile)) {
			Algorithms.streamCopy(in, out);
		}
	}

	public static List<String> listAssetFiles(Context context, String path) {
		List<String> fileList = new ArrayList<>();
		AssetManager assetManager = context.getAssets();
		try {
			String[] list = assetManager.list(path);
			if (list != null) {
				for (String item : list) {
					String fullPath = path.isEmpty() ? item : path + "/" + item;
					try {
						String[] subList = assetManager.list(fullPath);
						if (subList != null && subList.length > 0) {
							fileList.addAll(listAssetFiles(context, fullPath));
						} else {
							fileList.add(fullPath);
						}
					} catch (IOException e) {
						fileList.add(fullPath);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return fileList;
	}
}
