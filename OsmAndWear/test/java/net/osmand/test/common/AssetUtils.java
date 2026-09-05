package net.osmand.test.common;

import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.FileUtils;

import java.io.File;
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

}
