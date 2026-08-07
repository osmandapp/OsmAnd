package net.osmand.plus.myplaces.favorites;

import static net.osmand.IndexConstants.GPX_FILE_EXT;

import android.os.AsyncTask;
import android.os.Build;
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
		this(activity, groups, null, listener);
	}

	public ShareFavoritesAsyncTask(@NonNull FragmentActivity activity,
	                               @NonNull List<FavoriteGroup> groups,
	                               @Nullable String folderPath,
	                               @Nullable ShareFavoritesListener listener) {
		this.app = AndroidUtils.getApp(activity);
		this.groups = groups;
		this.listener = listener;
		this.favouritesHelper = app.getFavoritesHelper();
		this.activityRef = new WeakReference<>(activity);
		destFile = createDestinationFile(app, groups, folderPath);
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
		pointsDescription = buildPointsDescription(app, groups);
		return null;
	}

	@NonNull
	public static File createDestinationFile(@NonNull OsmandApplication app,
	                                         @NonNull List<FavoriteGroup> groups,
	                                         @Nullable String folderPath) {
		File dir = new File(app.getCacheDir(), "share");
		if (!dir.exists()) {
			dir.mkdirs();
		}
		if (folderPath != null) {
			return new File(dir, FavouritesFileHelper.getFolderFileName(folderPath) + GPX_FILE_EXT);
		}
		if (groups.size() == 1) {
			File file = app.getFavoritesHelper().getFileHelper().getExternalFile(groups.get(0));
			return new File(dir, file.getName());
		}
		return new File(dir, FavouritesFileHelper.FAV_FILE_PREFIX + GPX_FILE_EXT);
	}

	@NonNull
	public static Spanned buildPointsDescription(@NonNull OsmandApplication app,
	                                             @NonNull List<FavoriteGroup> groups) {
		return Html.fromHtml(generateHtmlPrint(app, groups));
	}

	@NonNull
	private static String generateHtmlPrint(@NonNull OsmandApplication app,
	                                        @NonNull List<FavoriteGroup> groups) {
		StringBuilder html = new StringBuilder();
		StringBuilder buffer = new StringBuilder();
		html.append("<h1>My Favorites</h1>");

		for (FavoriteGroup group : groups) {
			buffer.setLength(0);
			buffer.append("<h3>").append(FavoriteFolderFormatter.getBreadcrumb(app, group.getName())).append("</h3>");
			if (buffer.length() + html.length() > MAX_CHARS_IN_DESCRIPTION) {
				return html.append("<p>...</p>").toString();
			}

			html.append(buffer);
			boolean reachedLimit = generateHtmlForGroup(app, group.getPoints(), html);
			if (reachedLimit) {
				return html.append("<p>...</p>").toString();
			}
		}
		return html.toString();
	}

	private static boolean generateHtmlForGroup(@NonNull OsmandApplication app,
	                                            @NonNull List<FavouritePoint> points,
	                                            @NonNull StringBuilder html) {
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
			shareFavorites(app, activity, destFile, pointsDescription);
		}
	}

	public static void shareFavorites(@NonNull OsmandApplication app,
			@NonNull FragmentActivity activity, @NonNull File destFile,
			@NonNull CharSequence pointsDescription) {
		String type = "text/plain";
		String extraText = String.valueOf(pointsDescription);
		String extraSubject = app.getString(R.string.share_fav_subject);

		NativeShareDialogBuilder builder = new NativeShareDialogBuilder()
				.addFileWithSaveAction(destFile, app, activity, extraText, true)
				.setChooserTitle(extraSubject)
				.setExtraStream(AndroidUtils.getUriForFile(app, destFile))
				.setType(type);

		if (Build.VERSION.SDK_INT < 34) {
			builder.setExtraSubject(extraSubject);
			builder.setExtraText(extraText);
		}

		builder.build(app);
	}

	public interface ShareFavoritesListener {

		void shareFavoritesStarted();

		void shareFavoritesFinished(@NonNull File destFile, @NonNull Spanned pointsDescription);
	}
}
