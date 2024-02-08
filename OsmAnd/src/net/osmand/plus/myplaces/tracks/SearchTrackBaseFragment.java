package net.osmand.plus.myplaces.tracks;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.ColorInt;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.Location;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.configmap.tracks.SearchTracksAdapter;
import net.osmand.plus.configmap.tracks.SortByBottomSheet;
import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.plus.configmap.tracks.TrackItemsContainer;
import net.osmand.plus.configmap.tracks.viewholders.EmptyTracksViewHolder.EmptyTracksListener;
import net.osmand.plus.configmap.tracks.viewholders.SortTracksViewHolder.SortTracksListener;
import net.osmand.plus.configmap.tracks.viewholders.TrackViewHolder;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.myplaces.tracks.ItemsSelectionHelper.SelectionHelperProvider;
import net.osmand.plus.settings.enums.TracksSortMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.widgets.tools.SimpleTextWatcher;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class SearchTrackBaseFragment extends BaseOsmAndDialogFragment implements OsmAndCompassListener,
		OsmAndLocationListener, TrackItemsContainer, SortTracksListener {

	protected final ItemsSelectionHelper<TrackItem> selectionHelper = new ItemsSelectionHelper<>();

	protected SearchTracksAdapter adapter;
	protected View clearSearchQuery;
	protected EditText searchEditText;

	protected boolean usedOnMap;
	protected boolean selectionMode;

	private Location location;
	private Float heading;
	private boolean locationUpdateStarted;
	private boolean compassUpdateAllowed = true;

	@Override
	protected boolean isUsedOnMap() {
		return usedOnMap;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!selectionHelper.hasAnyItems()) {
			setupSelectionHelper();
		}
	}

	@LayoutRes
	protected abstract int getLayoutId();

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = themedInflater.inflate(getLayoutId(), container, false);
		view.setBackgroundColor(ContextCompat.getColor(app, nightMode ? R.color.activity_background_color_dark : R.color.list_background_color_light));

		Fragment fragment = getTargetFragment();
		List<TrackItem> trackItems = new ArrayList<>(selectionHelper.getAllItems());
		adapter = createAdapter(view.getContext(), trackItems);
		adapter.setTracksSortMode(getTracksSortMode());
		adapter.setSortTracksListener(this);
		adapter.setSelectionListener(getTrackSelectionListener());
		if (fragment instanceof EmptyTracksListener) {
			adapter.setImportTracksListener((EmptyTracksListener) fragment);
		}

		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(app));
		recyclerView.setItemAnimator(null);
		recyclerView.setAdapter(adapter);
		recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
				super.onScrollStateChanged(recyclerView, newState);
				compassUpdateAllowed = newState == RecyclerView.SCROLL_STATE_IDLE;
			}
		});

		setupToolbar(view);
		setupSearch(view);
		setupFragment(view);
		return view;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		updateSearchQuery();
	}

	@NonNull
	protected SearchTracksAdapter createAdapter(@NonNull Context context, List<TrackItem> trackItems) {
		TracksSearchFilter filter = new TracksSearchFilter(app, trackItems);
		return new SearchTracksAdapter(context, trackItems, nightMode, selectionMode, filter);
	}

	protected abstract void setupFragment(View view);

	@Override
	public void onResume() {
		super.onResume();
		searchEditText.requestFocus();
		AndroidUtils.showSoftKeyboard(requireActivity(), searchEditText);
		startLocationUpdate();
		setupFilterCallback();
	}

	protected void setupFilterCallback() {
		adapter.setFilterCallback(filteredItems -> {
			updateAdapterWithFilteredItems(filteredItems);
			return true;
		});
	}

	protected void updateAdapterWithFilteredItems(List<TrackItem> filteredItems) {
		updateSearchQuery();
		adapter.updateFilteredItems(filteredItems);
		updateButtonsState();
	}

	private void updateSearchQuery() {
		searchEditText.setText(adapter.getCurrentSearchQuery());
		searchEditText.setSelection(searchEditText.length());
	}

	public void setupSelectionHelper() {
		Fragment fragment = getTargetFragment();
		if (fragment instanceof ItemsSelectionHelper.SelectionHelperProvider) {
			SelectionHelperProvider<TrackItem> helperProvider = (SelectionHelperProvider<TrackItem>) fragment;
			ItemsSelectionHelper<TrackItem> originalHelper = helperProvider.getSelectionHelper();
			selectionHelper.syncWith(originalHelper);
		}
	}

	protected boolean areAllTracksSelected() {
		return selectionHelper.isItemsSelected(adapter.getFilteredItems());
	}

	protected abstract void updateButtonsState();

	protected void setupToolbar(@NonNull View view) {
		View appbar = view.findViewById(R.id.appbar);
		ViewCompat.setElevation(appbar, 5.0f);
		appbar.setBackgroundColor(ContextCompat.getColor(app, nightMode ? R.color.app_bar_main_dark : R.color.app_bar_main_light));
		setStatusBarBackgroundColor(ContextCompat.getColor(app, nightMode ? R.color.status_bar_main_dark : R.color.status_bar_main_light));
	}

	protected void setStatusBarBackgroundColor(@ColorInt int color) {
		Window window = requireDialog().getWindow();
		if (window != null) {
			AndroidUiHelper.setStatusBarContentColor(window.getDecorView(), true);
			window.setStatusBarColor(color);
		}
	}

	protected void setupSearch(@NonNull View view) {
		View searchContainer = view.findViewById(R.id.search_container);
		clearSearchQuery = searchContainer.findViewById(R.id.clearButton);
		clearSearchQuery.setVisibility(View.GONE);
		ImageButton backButton = view.findViewById(R.id.back_button);
		backButton.setVisibility(View.VISIBLE);
		backButton.setOnClickListener((v) -> dismiss());
		searchEditText = searchContainer.findViewById(R.id.searchEditText);
		searchEditText.setHint(R.string.search_track_by_name);
		searchEditText.setTextColor(ContextCompat.getColor(app, R.color.card_and_list_background_light));
		searchEditText.setHintTextColor(ContextCompat.getColor(app, R.color.white_50_transparent));
		searchEditText.addTextChangedListener(new SimpleTextWatcher() {
			@Override
			public void afterTextChanged(Editable query) {
				filterTracks(query.toString());
				AndroidUiHelper.updateVisibility(clearSearchQuery, query.length() > 0);
				adapter.notifyItemChanged(0);
			}
		});
		clearSearchQuery.setOnClickListener((v) -> resetSearchQuery());
	}

	protected void resetSearchQuery() {
		filterTracks(null);
		searchEditText.setText(null);
	}

	protected void filterTracks(@Nullable String query) {
		adapter.filter(query);
	}

	@Override
	public void updateItems(@NonNull Set<TrackItem> trackItems) {
		adapter.notifyDataSetChanged();
		updateButtonsState();
	}

	@Override
	public void updateContent() {
		adapter.notifyDataSetChanged();
		updateButtonsState();
		updateLocationUi();
	}

	@Override
	public void onPause() {
		super.onPause();
		stopLocationUpdate();
	}

	@Override
	public void updateLocation(Location location) {
		if (!MapUtils.areLatLonEqual(this.location, location)) {
			this.location = location;
			updateLocationUi();
		}
	}

	@Override
	public void updateCompassValue(float value) {
		// 99 in next line used to one-time initialize arrows (with reference vs. fixed-north direction)
		// on non-compass devices
		float lastHeading = heading != null ? heading : 99;
		heading = value;
		if (Math.abs(MapUtils.degreesDiff(lastHeading, heading)) > 5) {
			updateLocationUi();
		} else {
			heading = lastHeading;
		}
	}

	private void updateLocationUi() {
		if (compassUpdateAllowed && adapter != null) {
			app.runInUIThread(() -> {
				if (location == null) {
					location = app.getLocationProvider().getLastKnownLocation();
				}
				adapter.notifyDataSetChanged();
			});
		}
	}

	public void startLocationUpdate() {
		if (!locationUpdateStarted) {
			locationUpdateStarted = true;
			OsmAndLocationProvider locationProvider = app.getLocationProvider();
			locationProvider.removeCompassListener(locationProvider.getNavigationInfo());
			locationProvider.addCompassListener(this);
			locationProvider.addLocationListener(this);
			updateLocationUi();
		}
	}

	public void stopLocationUpdate() {
		if (locationUpdateStarted) {
			locationUpdateStarted = false;
			OsmAndLocationProvider locationProvider = app.getLocationProvider();
			locationProvider.removeLocationListener(this);
			locationProvider.removeCompassListener(this);
			locationProvider.addCompassListener(locationProvider.getNavigationInfo());
		}
	}

	@Override
	public void showSortByDialog() {
		FragmentManager manager = getFragmentManager();
		if (manager != null) {
			SortByBottomSheet.showInstance(manager, getTracksSortMode(), this, isUsedOnMap());
		}
	}

	@NonNull
	@Override
	public TracksSortMode getTracksSortMode() {
		return settings.SEARCH_TRACKS_SORT_MODE.get();
	}

	@Override
	public void setTracksSortMode(@NonNull TracksSortMode sortMode, boolean sortSubFolders) {
		settings.SEARCH_TRACKS_SORT_MODE.set(sortMode);
		adapter.setTracksSortMode(getTracksSortMode());
	}

	@NonNull
	protected abstract TrackViewHolder.TrackSelectionListener getTrackSelectionListener();
}
