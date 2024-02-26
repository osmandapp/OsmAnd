package net.osmand.plus.myplaces.favorites;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.FavouritePoint;

import java.util.Set;

public class DeleteFavoritesTask extends AsyncTask<Void, Object, Void> {

	private final FavouritesHelper helper;
	private final Set<FavoriteGroup> groups;
	private final Set<FavouritePoint> points;
	private final DeleteFavoritesListener listener;

	public DeleteFavoritesTask(@NonNull FavouritesHelper helper, @Nullable Set<FavoriteGroup> groups,
	                           @Nullable Set<FavouritePoint> points, @Nullable DeleteFavoritesListener listener) {
		this.helper = helper;
		this.groups = groups;
		this.points = points;
		this.listener = listener;
	}

	@Override
	protected void onPreExecute() {
		if (listener != null) {
			listener.onDeletingStarted();
		}
	}

	@Override
	protected Void doInBackground(Void... params) {
		helper.delete(groups, points);
		return null;
	}

	@Override
	protected void onPostExecute(Void result) {
		if (listener != null) {
			listener.onDeletingFinished();
		}
	}

	public interface DeleteFavoritesListener {

		void onDeletingStarted();

		void onDeletingFinished();
	}
}