package net.osmand.plus.importfiles.ui;


import android.app.Dialog;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.Location;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseFullScreenDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.importfiles.ui.ExitImportBottomSheet.OnExitConfirmedListener;
import net.osmand.plus.importfiles.ui.TrackPointsAdapter.OnItemSelectedListener;
import net.osmand.plus.settings.fragments.BaseSettingsListFragment;
import net.osmand.plus.track.helpers.GpxDisplayGroup;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SelectPointsFragment extends BaseFullScreenDialogFragment implements OnExitConfirmedListener,
		OnItemSelectedListener, OsmAndCompassListener, OsmAndLocationListener {

	public static final String TAG = ImportTracksFragment.class.getSimpleName();

	private ImportTrackItem trackItem;
	private final List<WptPt> points = new ArrayList<>();
	private final Set<WptPt> selectedPoints = new HashSet<>();

	private TrackPointsAdapter adapter;

	private DialogButton applyButton;
	private DialogButton selectAllButton;
	private TextView toolbarTitle;
	private ExpandableListView listView;

	private Float heading;
	private Location location;
	private boolean locationUpdateStarted;

	@Override
	protected boolean isUsedOnMap() {
		return true;
	}

	@NonNull
	@Override
	public Dialog createDialog(Bundle savedInstanceState) {
		return new Dialog(requireContext(), getTheme()) {
			@Override
			public void onBackPressed() {
				if (selectedPointsChanged()) {
					dismiss();
				} else {
					showSkipSelectionDialog();
				}
			}
		};
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = inflate(R.layout.select_track_points_fragment, container, false);

		setupToolbar(view);
		setupButtons(view);
		setupListView(view);

		updateToolbar();
		updateButtonsState();
		expandAllGroups();

		return view;
	}

	protected void setupListView(@NonNull View view) {
		GpxFile gpxFile = trackItem.selectedGpxFile.getGpxFile();
		GpxDisplayGroup group = app.getGpxDisplayHelper().buildPointsDisplayGroup(gpxFile, points, trackItem.name);

		adapter = new TrackPointsAdapter(view.getContext(), selectedPoints, nightMode);
		adapter.setListener(this);
		adapter.synchronizeGroups(Collections.singletonList(group));

		listView = view.findViewById(R.id.list);
		listView.setAdapter(adapter);

		if (!Algorithms.isEmpty(trackItem.suggestedPoints) && listView.getHeaderViewsCount() == 0) {
			listView.addHeaderView(getHeaderView());
		}
		BaseSettingsListFragment.setupListView(listView);
	}

	@NonNull
	private View getHeaderView() {
		LayoutInflater inflater = UiUtilities.getInflater(requireContext(), nightMode);
		View view = inflater.inflate(R.layout.suggested_points_header, null);

		TextView title = view.findViewById(R.id.title);
		title.setText(getHeaderText());

		view.findViewById(R.id.selectable_list_item).setOnClickListener(v -> {
			selectedPoints.clear();
			selectedPoints.addAll(trackItem.suggestedPoints);

			updateToolbar();
			updateButtonsState();
			adapter.notifyDataSetChanged();
		});
		return view;
	}

	@NonNull
	private CharSequence getHeaderText() {
		String text = getString(R.string.selected_waypoints_descr, trackItem.name);
		int start = text.indexOf(trackItem.name);
		int end = start + trackItem.name.length();

		SpannableString spannable = new SpannableString(text);
		spannable.setSpan(new CustomTypefaceSpan(FontCache.getMediumFont()), start, end, 0);
		spannable.setSpan(new ForegroundColorSpan(ColorUtilities.getActiveColor(app, nightMode)), start, end, 0);
		return spannable;
	}

	private void expandAllGroups() {
		for (int i = 0; i < adapter.getGroupCount(); i++) {
			listView.expandGroup(i);
		}
	}

	protected void setupToolbar(@NonNull View view) {
		View appbar = view.findViewById(R.id.appbar);
		ViewCompat.setElevation(appbar, 5.0f);

		toolbarTitle = appbar.findViewById(R.id.toolbar_title);

		ImageView closeButton = appbar.findViewById(R.id.close_button);
		closeButton.setOnClickListener(v -> {
			if (selectedPointsChanged()) {
				dismiss();
			} else {
				showSkipSelectionDialog();
			}
		});
		closeButton.setImageDrawable(getIcon(AndroidUtils.getNavigationIconResId(view.getContext())));
	}

	protected void updateToolbar() {
		String selected = getString(R.string.shared_string_selected);
		String count = getString(R.string.ltr_or_rtl_combine_via_slash, String.valueOf(selectedPoints.size()), String.valueOf(points.size()));
		toolbarTitle.setText(getString(R.string.ltr_or_rtl_combine_via_colon, selected, count));
	}

	private void setupButtons(@NonNull View view) {
		View buttonsContainer = view.findViewById(R.id.control_buttons);
		View container = buttonsContainer.findViewById(R.id.bottom_buttons_container);
		container.setBackgroundColor(ColorUtilities.getListBgColor(app, nightMode));

		applyButton = container.findViewById(R.id.right_bottom_button);
		applyButton.setOnClickListener(v -> applySelectedPoints());

		selectAllButton = container.findViewById(R.id.dismiss_button);
		selectAllButton.setOnClickListener(v -> {
			if (selectedPoints.containsAll(points)) {
				selectedPoints.clear();
			} else {
				selectedPoints.addAll(points);
			}
			updateToolbar();
			updateButtonsState();
			adapter.notifyDataSetChanged();
		});
		AndroidUiHelper.updateVisibility(applyButton, true);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.buttons_divider), true);

		TextView textView = selectAllButton.findViewById(R.id.button_text);
		FrameLayout.LayoutParams params = (LayoutParams) textView.getLayoutParams();
		params.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
		textView.setLayoutParams(params);
	}

	protected void updateButtonsState() {
		applyButton.setTitleId(R.string.shared_string_apply);
		applyButton.setButtonType(DialogButtonType.PRIMARY);

		boolean allSelected = selectedPoints.containsAll(points);
		String selectAllText = getString(allSelected ? R.string.shared_string_deselect_all : R.string.shared_string_select_all);
		selectAllButton.setButtonType(DialogButtonType.SECONDARY_ACTIVE);
		selectAllButton.setTitle(selectAllText);
		selectAllButton.setIconId(R.drawable.ic_action_deselect_all);

		TextView textView = selectAllButton.findViewById(R.id.button_text);
		textView.setCompoundDrawablePadding(AndroidUtils.dpToPx(app, 12));
	}

	private void applySelectedPoints() {
		Fragment target = getTargetFragment();
		if (target instanceof PointsSelectionListener) {
			((PointsSelectionListener) target).onPointsSelected(trackItem, selectedPoints);
		}
		dismiss();
	}

	private boolean selectedPointsChanged() {
		return selectedPoints.size() == trackItem.selectedPoints.size()
				&& selectedPoints.containsAll(trackItem.selectedPoints);
	}

	private void showSkipSelectionDialog() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			SkipPointsSelectionBottomSheet.showInstance(activity.getSupportFragmentManager(), this, true);
		}
	}

	@Override
	public void onItemSelected(WptPt point, boolean selected) {
		if (selected) {
			selectedPoints.add(point);
		} else {
			selectedPoints.remove(point);
		}
		updateToolbar();
		updateButtonsState();
	}

	@Override
	public void onCategorySelected(List<WptPt> points, boolean selected) {
		if (selected) {
			selectedPoints.addAll(points);
		} else {
			selectedPoints.removeAll(points);
		}
		updateToolbar();
		updateButtonsState();
	}

	@Override
	public void onExitConfirmed() {
		dismiss();
	}

	@Override
	public void onResume() {
		super.onResume();
		startLocationUpdate();
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
		if (adapter != null) {
			app.runInUIThread(() -> {
				if (location == null) {
					location = app.getLocationProvider().getLastKnownLocation();
				}
				adapter.notifyDataSetChanged();
			});
		}
	}

	private void startLocationUpdate() {
		if (!locationUpdateStarted) {
			locationUpdateStarted = true;
			OsmAndLocationProvider locationProvider = app.getLocationProvider();
			locationProvider.removeCompassListener(locationProvider.getNavigationInfo());
			locationProvider.addCompassListener(this);
			locationProvider.addLocationListener(this);
			updateLocationUi();
		}
	}

	private void stopLocationUpdate() {
		if (locationUpdateStarted) {
			locationUpdateStarted = false;
			OsmAndLocationProvider locationProvider = app.getLocationProvider();
			locationProvider.removeLocationListener(this);
			locationProvider.removeCompassListener(this);
			locationProvider.addCompassListener(locationProvider.getNavigationInfo());
		}
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull ImportTrackItem trackItem,
	                                @NonNull List<WptPt> points, @Nullable Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			SelectPointsFragment fragment = new SelectPointsFragment();
			fragment.trackItem = trackItem;
			fragment.points.addAll(points);
			fragment.selectedPoints.addAll(trackItem.selectedPoints);
			fragment.setRetainInstance(true);
			fragment.setTargetFragment(target, 0);
			fragment.show(manager, TAG);
		}
	}

	public interface PointsSelectionListener {

		void onPointsSelected(@NonNull ImportTrackItem trackItem, @NonNull Set<WptPt> folder);
	}
}