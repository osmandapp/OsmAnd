package net.osmand.plus.myplaces.favorites;

import android.content.Intent;
import android.os.AsyncTask;
import android.text.Html;
import android.text.Spanned;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.FavouritePoint;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;

import java.io.File;
import java.util.Collections;
import java.util.List;

import static net.osmand.IndexConstants.GPX_FILE_EXT;

public class ShareFavoritesAsyncTask extends AsyncTask<Void, Void, Void> {

	private static final int MAX_CHARS_IN_DESCRIPTION = 100000;

	private final OsmandApplication app;
	private final FavouritesHelper favouritesHelper;

	private final List<FavoriteGroup> groups;
	private final File destFile;

	private Spanned pointsDescription;
	private final ShareFavoritesListener listener;

	public ShareFavoritesAsyncTask(@NonNull OsmandApplication app,
	                               @Nullable FavoriteGroup group,
	                               @Nullable ShareFavoritesListener listener) {
		this.app = app;
		this.groups = group != null
				? Collections.singletonList(group) : app.getFavoritesHelper().getFavoriteGroups();
		this.listener = listener;
		this.favouritesHelper = app.getFavoritesHelper();

		File dir = new File(app.getCacheDir(), "share");
		if (!dir.exists()) {
			dir.mkdir();
		}
		if (this.groups.size() == 1) {
			File file = app.getFavoritesHelper().getFileHelper().getExternalFile(this.groups.get(0));
			destFile = new File(dir, file.getName());
		} else {
			destFile = new File(dir, FavouritesFileHelper.FAV_FILE_PREFIX + GPX_FILE_EXT);
		}
	}

	@Override
	protected void onPreExecute() {
		if (listener != null) {
			listener.shareFavoritesStarted();
		}
	}

	@Override
	protected Void doInBackground(Void... params) {
		favouritesHelper.getFileHelper().saveFile(groups, destFile);
		pointsDescription = Html.fromHtml(generateHtmlPrint(groups));
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
