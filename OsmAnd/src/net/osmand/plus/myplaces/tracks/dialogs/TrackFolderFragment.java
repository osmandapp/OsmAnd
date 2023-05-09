package net.osmand.plus.myplaces.tracks.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.configmap.tracks.SearchTrackItemsFragment;
import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.plus.myplaces.tracks.GpxActionsHelper;
import net.osmand.plus.settings.enums.TracksSortMode;
import net.osmand.plus.track.data.GPXInfo;
import net.osmand.plus.track.data.TrackFolder;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;

public class TrackFolderFragment extends BaseTrackFolderFragment {

	public static final String TAG = TrackFolderFragment.class.getSimpleName();


	@Override
	protected void setupToolbar(@NonNull View view) {
		super.setupToolbar(view);

		ViewGroup container = view.findViewById(R.id.actions_container);
		container.removeAllViews();

		LayoutInflater inflater = UiUtilities.getInflater(view.getContext(), nightMode);
		setupSearchButton(inflater, container);
		setupMenuButton(inflater, container);
	}

	private void setupSearchButton(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
		ImageButton button = (ImageButton) inflater.inflate(R.layout.action_button, container, false);
		button.setImageDrawable(getIcon(R.drawable.ic_action_search_dark));
		button.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				SearchTrackItemsFragment.showInstance(getChildFragmentManager());
			}
		});
		container.addView(button);
	}

	private void setupMenuButton(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
		ImageButton button = (ImageButton) inflater.inflate(R.layout.action_button, container, false);
		button.setImageDrawable(getIcon(R.drawable.ic_overflow_menu_white));
		button.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				TracksSelectionFragment.showInstance(activity.getSupportFragmentManager(), trackFolder);
			}
		});
		container.addView(button);
	}

	@NonNull
	@Override
	public TracksSortMode getTracksSortMode() {
		return TracksSortMode.getDefaultSortMode();
	}

	@Override
	public void setTracksSortMode(@NonNull TracksSortMode sortMode) {

	}

	@Override
	public void onFolderSelected(@NonNull TrackFolder folder) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			showInstance(activity.getSupportFragmentManager(), folder);
		}
	}

	@Override
	public void onTrackItemOptionsSelected(@NonNull View view, @NonNull TrackItem trackItem) {
		GPXInfo gpxInfo = new GPXInfo(trackItem.getName(), trackItem.getFile());
		gpxActionsHelper.openPopUpMenu(view, gpxInfo);
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull TrackFolder folder) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			TrackFolderFragment fragment = new TrackFolderFragment();
			fragment.trackFolder = folder;
			fragment.setRetainInstance(true);
			fragment.show(manager, TAG);
		}
	}
}