package net.osmand.plus.myplaces;

import android.content.Intent;
import android.os.AsyncTask;
import android.text.Html;
import android.text.Spanned;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.data.FavouritePoint;
import net.osmand.plus.myplaces.FavouritesDbHelper.FavoriteGroup;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class ShareFavoritesAsyncTask extends AsyncTask<Void, Void, Void> {

	private static final Log log = PlatformUtil.getLog(ShareFavoritesAsyncTask.class);

	private static final int MAX_CHARS_IN_DESCRIPTION = 100000;

	private final OsmandApplication app;
	private final FavouritesDbHelper helper;

	private final FavoriteGroup group;
	private final File srcFile;
	private final File destFile;

	private Spanned pointsDescription;
	private final ShareFavoritesListener listener;

	public ShareFavoritesAsyncTask(@NonNull OsmandApplication app,
	                               @Nullable FavoriteGroup group,
	                               @Nullable ShareFavoritesListener listener) {
		this.app = app;
		this.group = group;
		this.listener = listener;
		helper = app.getFavorites();

		File dir = new File(app.getCacheDir(), "share");
		if (!dir.exists()) {
			dir.mkdir();
		}
		srcFile = group == null ? helper.getExternalFile() : null;
		destFile = new File(dir, srcFile != null ? srcFile.getName() : FavouritesDbHelper.FILE_TO_SAVE);
	}

	@Override
	protected void onPreExecute() {
		if (listener != null) {
			listener.shareFavoritesStarted();
		}
	}

	@Override
	protected Void doInBackground(Void... params) {
		List<FavoriteGroup> groups;
		if (group != null) {
			helper.saveFile(group.getPoints(), destFile);
			groups = new ArrayList<>();
			groups.add(group);
		} else {
			groups = app.getFavorites().getFavoriteGroups();
		}
		pointsDescription = Html.fromHtml(generateHtmlPrint(groups));
		try {
			if (srcFile != null && destFile != null) {
				Algorithms.fileCopy(srcFile, destFile);
			}
		} catch (IOException e) {
			log.error(e);
			app.showToastMessage("Error sharing favorites: " + e.getMessage());
		}
		return null;
	}

	@Override
	protected void onPostExecute(Void res) {
		if (listener != null) {
			listener.shareFavoritesFinished();
		}
		if (destFile.exists()) {
			Intent intent = new Intent();
			intent.setAction(Intent.ACTION_SEND)
					.putExtra(Intent.EXTRA_SUBJECT, app.getString(R.string.share_fav_subject))
					.putExtra(Intent.EXTRA_TEXT, pointsDescription)
					.putExtra(Intent.EXTRA_STREAM, AndroidUtils.getUriForFile(app, destFile))
					.setType("text/plain")
					.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			Intent chooserIntent = Intent.createChooser(intent, app.getString(R.string.share_fav_subject));
			AndroidUtils.startActivityIfSafe(app, chooserIntent);
		}
	}

	private String generateHtmlPrint(List<FavoriteGroup> groups) {
		StringBuilder html = new StringBuilder();
		StringBuilder buffer = new StringBuilder();
		html.append("<h1>My Favorites</h1>");

		for (FavoriteGroup group : groups) {
			buffer.setLength(0);
			buffer.append("<h3>").append(group.getDisplayName(app)).append("</h3>");
			if (buffer.length() + html.length() > MAX_CHARS_IN_DESCRIPTION) {
				return html.append("<p>...</p>").toString();
			}

			html.append(buffer);
			boolean reachedLimit = generateHtmlForGroup(group.getPoints(), html);
			if (reachedLimit) {
				return html.append("<p>...</p>").toString();
			}
		}
		return html.toString();
	}

	private boolean generateHtmlForGroup(List<FavouritePoint> points, StringBuilder html) {
		StringBuilder buffer = new StringBuilder();
		for (FavouritePoint fp : points) {
			buffer.setLength(0);

			float lat = (float) fp.getLatitude();
			float lon = (float) fp.getLongitude();
			String url = "geo:" + lat + "," + lon + "?m=" + fp.getName();
			buffer.append("<p>")
					.append(fp.getDisplayName(app))
					.append(" - <a href=\"")
					.append(url)
					.append("\">geo:")
					.append(lat).append(",").append(lon)
					.append("</a><br></p>");

			if (buffer.length() + html.length() > MAX_CHARS_IN_DESCRIPTION) {
				return true;
			}
			html.append(buffer);
		}
		return false;
	}

	public interface ShareFavoritesListener {

		void shareFavoritesStarted();

		void shareFavoritesFinished();
	}
}
