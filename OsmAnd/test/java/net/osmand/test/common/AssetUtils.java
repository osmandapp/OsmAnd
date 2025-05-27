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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

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
		String[] files = testContext.getAssets().list("");
		Log.d("TAG", "*copyAssetToFile: List " + files.length);
		System.out.println("copyAssetToFile: List " + files.length);

		for (int i = 0; i < files.length; i++) {
			Log.d("TAG", "*copyAssetToFile: List " + files[0]);
			System.out.println("copyAssetToFile: List " + files[0]);
		}
		String[] files2 = testContext.getAssets().list("");

		Log.d("TAG", "*copyAssetToFile2: List " + files2.length);
		System.out.println("copyAssetToFile2: List " + files2.length);
		for (int i = 0; i < files2.length; i++) {
			Log.d("TAG", "*copyAssetToFile: List " + files2[0]);
			System.out.println("copyAssetToFile: List " + files2[0]);
		}
		try (InputStream in = testContext.getAssets().open(assetName);
		     FileOutputStream out = new FileOutputStream(destFile)) {
			byte[] buffer = new byte[4096];
			int length;
			while ((length = in.read(buffer)) > 0) {
				out.write(buffer, 0, length);
			}
		}
	}

}
