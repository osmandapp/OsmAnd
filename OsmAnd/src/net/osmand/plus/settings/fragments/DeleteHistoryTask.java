package net.osmand.plus.settings.fragments;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.base.BaseLoadAsyncTask;
import net.osmand.plus.helpers.SearchHistoryHelper;
import net.osmand.plus.helpers.TargetPoint;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.search.core.SearchResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DeleteHistoryTask extends BaseLoadAsyncTask<Void, Void, Void> {

	private final Set<Object> selectedItems;
	private final DeleteHistoryType historyType;
	private final DeleteHistoryListener listener;

	public DeleteHistoryTask(@NonNull FragmentActivity activity, @NonNull DeleteHistoryType historyType,
	                         @NonNull Set<Object> selectedItems, @Nullable DeleteHistoryListener listener) {
		super(activity);
		this.selectedItems = selectedItems;
		this.historyType = historyType;
		this.listener = listener;
	}

	protected String getProgressTitle() {
		return app.getString(R.string.deleting);
	}

	protected String getProgressDescription() {
		return app.getString(R.string.deleting_history);
	}

	@Override
	protected Void doInBackground(Void... voids) {
		if (historyType == DeleteHistoryType.SEARCH) {
			deleteSearchHistory();
		} else if (historyType == DeleteHistoryType.NAVIGATION) {
			deleteNavigationHistory();
		} else {
			deleteMarkerHistory();
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

	private void deleteSearchHistory() {
		SearchHistoryHelper helper = SearchHistoryHelper.getInstance(app);
		for (Object item : selectedItems) {
			if (item instanceof SearchResult searchResult) {
				helper.remove(searchResult.object);
			}
		}
	}

	private void deleteNavigationHistory() {
		boolean clearBackupPoints = false;
		SearchHistoryHelper historyHelper = SearchHistoryHelper.getInstance(app);
		for (Object item : selectedItems) {
			if (item instanceof SearchResult searchResult) {
				historyHelper.remove(searchResult.object);
			} else if (item instanceof TargetPoint) {
				clearBackupPoints = true;
			}
		}
		if (clearBackupPoints) {
			app.getTargetPointsHelper().clearBackupPoints();
		}
	}

	private void deleteMarkerHistory() {
		List<MapMarker> mapMarkers = new ArrayList<>();
		for (Object item : selectedItems) {
			if (item instanceof MapMarker) {
				mapMarkers.add((MapMarker) item);
			}
		}
		app.getMapMarkersHelper().removeMarkers(mapMarkers);
	}

	public interface DeleteHistoryListener {
		void onDeletionComplete();
	}

	public enum DeleteHistoryType {
		MARKER,
		SEARCH,
		NAVIGATION
	}
}