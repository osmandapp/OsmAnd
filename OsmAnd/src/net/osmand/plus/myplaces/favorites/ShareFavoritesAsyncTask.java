package net.osmand.plus.myplaces.favorites;

import static net.osmand.IndexConstants.GPX_FILE_EXT;

import android.os.AsyncTask;
import android.text.Html;
import android.text.Spanned;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.data.FavouritePoint;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.mapcontextmenu.other.ShareMenu.NativeShareDialogBuilder;
import net.osmand.plus.utils.AndroidUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;

public class ShareFavoritesAsyncTask extends AsyncTask<Void, Void, Void> {

	private static final int MAX_CHARS_IN_DESCRIPTION = 100000;

	private final OsmandApplication app;
	private final FavouritesHelper favouritesHelper;
	private final WeakReference<FragmentActivity> activityRef;

	private final List<FavoriteGroup> groups;
	private final File destFile;

	private Spanned pointsDescription;
	private final ShareFavoritesListener listener;

	public ShareFavoritesAsyncTask(@NonNull FragmentActivity activity,
	                               @Nullable FavoriteGroup group,
	                               @Nullable ShareFavoritesListener listener) {
		this(activity, group != null
				? Collections.singletonList(group) : AndroidUtils.getApp(activity).getFavoritesHelper().getFavoriteGroups(), listener);
	}

	public ShareFavoritesAsyncTask(@NonNull FragmentActivity activity,
	                               @NonNull List<FavoriteGroup> groups,
	                               @Nullable ShareFavoritesListener listener) {
		this.app = AndroidUtils.getApp(activity);
		this.groups = groups;
		this.listener = listener;
		this.favouritesHelper = app.getFavoritesHelper();
		this.activityRef = new WeakReference<>(activity);

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

	@Override
	protected void onPostExecute(Void res) {
		if (listener != null) {
			listener.shareFavoritesFinished(destFile, pointsDescription);
		}
		FragmentActivity activity = activityRef.get();
		if (AndroidUtils.isActivityNotDestroyed(activity) && destFile.exists()) {
			shareFavorites(activity, destFile);
		}
	}

	private void shareFavorites(@NonNull FragmentActivity activity, @NonNull File destFile) {
		String type = "text/plain";
		String extraText = String.valueOf(pointsDescription);
		String extraSubject = app.getString(R.string.share_fav_subject);

		new NativeShareDialogBuilder()
				.addFileWithSaveAction(destFile, app, activity, true)
				.setChooserTitle(extraSubject)
				.setExtraSubject(extraSubject)
				.setExtraText(extraText)
				.setExtraStream(AndroidUtils.getUriForFile(app, destFile))
				.setType(type)
				.build(app);
	}

	public interface ShareFavoritesListener {

		void shareFavoritesStarted();

		void shareFavoritesFinished(@NonNull File destFile, @NonNull Spanned pointsDescription);
	}
}
