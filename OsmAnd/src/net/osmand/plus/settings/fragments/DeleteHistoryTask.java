package net.osmand.plus.settings.fragments;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.base.BaseLoadAsyncTask;
import net.osmand.plus.search.history.SearchHistoryHelper;
import net.osmand.plus.helpers.TargetPoint;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.search.core.SearchResult;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DeleteHistoryTask extends BaseLoadAsyncTask<Void, Void, Void> {

	private final Set<Object> selectedItems;
	private final DeleteHistoryListener listener;

	public DeleteHistoryTask(@NonNull FragmentActivity activity, @NonNull Set<Object> selectedItems,
	                         @Nullable DeleteHistoryListener listener) {
		super(activity);
		this.selectedItems = selectedItems;
		this.listener = listener;
		setShouldShowProgress(true);
	}

	protected String getProgressTitle() {
		return app.getString(R.string.deleting);
	}

	protected String getProgressDescription() {
		return app.getString(R.string.deleting_history);
	}

	@Override
	protected Void doInBackground(Void... voids) {
		SearchHistoryHelper historyHelper = app.getSearchHistoryHelper();
		boolean clearBackupPoints = false;
		List<MapMarker> mapMarkers = new ArrayList<>();

		for (Object item : selectedItems) {
			if (item instanceof SearchResult searchResult) {
				historyHelper.remove(searchResult);
			} else if (item instanceof TargetPoint) {
				clearBackupPoints = true;
			} else if (item instanceof MapMarker) {
				mapMarkers.add((MapMarker) item);
			}
		}

		if (clearBackupPoints) {
			app.getTargetPointsHelper().clearBackupPoints();
		}
		if (!Algorithms.isEmpty(mapMarkers)) {
			app.getMapMarkersHelper().removeMarkers(mapMarkers);
		}

		return null;
	}

	@Override
	protected void onPostExecute(Void unused) {
		hideProgress();
		if (listener != null) {
			listener.onDeletionComplete();
		}
	}

	public interface DeleteHistoryListener {
		void onDeletionComplete();
	}
}