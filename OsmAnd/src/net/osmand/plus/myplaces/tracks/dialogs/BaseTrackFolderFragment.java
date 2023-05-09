package net.osmand.plus.myplaces.tracks.dialogs;

import static net.osmand.plus.configmap.tracks.TracksFragment.OPEN_TRACKS_TAB;
import static net.osmand.plus.myplaces.MyPlacesActivity.TAB_ID;
import static net.osmand.plus.myplaces.MyPlacesActivity.TRACKS_TAB;
import static net.osmand.plus.myplaces.tracks.dialogs.TrackFoldersAdapter.TYPE_SORT_TRACKS;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.configmap.tracks.SortByBottomSheet;
import net.osmand.plus.configmap.tracks.TrackTabType;
import net.osmand.plus.configmap.tracks.viewholders.SortTracksViewHolder.SortTracksListener;
import net.osmand.plus.configmap.tracks.viewholders.TrackViewHolder.TrackSelectionListener;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.myplaces.tracks.GpxActionsHelper;
import net.osmand.plus.myplaces.tracks.dialogs.viewholders.TrackFolderViewHolder.FolderSelectionListener;
import net.osmand.plus.myplaces.tracks.dialogs.viewholders.VisibleTracksViewHolder.VisibleTracksListener;
import net.osmand.plus.myplaces.tracks.tasks.DeleteGpxFilesTask.GpxFilesDeletionListener;
import net.osmand.plus.settings.enums.TracksSortMode;
import net.osmand.plus.track.data.GPXInfo;
import net.osmand.plus.track.data.TrackFolder;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseTrackFolderFragment extends BaseOsmAndDialogFragment implements SortTracksListener,
		TrackSelectionListener, FolderSelectionListener, VisibleTracksListener, GpxFilesDeletionListener {

	protected GpxActionsHelper gpxActionsHelper;
	protected TrackFolder trackFolder;

	protected TrackFoldersAdapter adapter;

	@ColorRes
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return ColorUtilities.getStatusBarColorId(nightMode);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		gpxActionsHelper = new GpxActionsHelper(requireActivity(), nightMode);
		gpxActionsHelper.setTargetFragment(this);
		gpxActionsHelper.setDeletionListener(this);
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		Activity activity = requireActivity();
		int themeId = nightMode ? R.style.OsmandDarkTheme_DarkActionbar : R.style.OsmandLightTheme_DarkActionbar_LightStatusBar;
		Dialog dialog = new Dialog(activity, themeId) {
			@Override
			public void onBackPressed() {
				dismiss();
			}
		};
		Window window = dialog.getWindow();
		if (window != null) {
			if (!settings.DO_NOT_USE_ANIMATIONS.get()) {
				window.getAttributes().windowAnimations = R.style.Animations_Alpha;
			}
			window.setStatusBarColor(ContextCompat.getColor(app, getStatusBarColorId()));
		}
		return dialog;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		FragmentActivity activity = requireActivity();
		LayoutInflater themedInflater = UiUtilities.getInflater(activity, nightMode);
		View view = themedInflater.inflate(R.layout.track_folder_fragment, container, false);
		view.setBackgroundColor(ColorUtilities.getListBgColor(app, nightMode));

		setupToolbar(view);

		adapter = new TrackFoldersAdapter(app, nightMode);
		adapter.setSortTracksListener(this);
		adapter.setVisibleTracksListener(this);
		adapter.setTrackSelectionListener(this);
		adapter.setFolderSelectionListener(this);

		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(app));
		recyclerView.setItemAnimator(null);
		recyclerView.setAdapter(adapter);

		if (trackFolder != null) {
			updateAdapter();
		}

		return view;
	}

	@NonNull
	protected List<Object> getAdapterItems() {
		List<Object> items = new ArrayList<>();
		items.add(TYPE_SORT_TRACKS);
		items.addAll(trackFolder.getSubFolders());
		items.addAll(trackFolder.getTrackItems());
		return items;
	}

	protected void setupToolbar(@NonNull View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);

		TextView toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
		toolbarTitle.setText(trackFolder.getName());

		ImageView closeButton = toolbar.findViewById(R.id.close_button);
		closeButton.setImageDrawable(getIcon(AndroidUtils.getNavigationIconResId(view.getContext())));
		closeButton.setOnClickListener(v -> dismiss());
		ViewCompat.setElevation(view.findViewById(R.id.appbar), 5.0f);
	}

	protected void updateAdapter() {
		adapter.updateItems(getAdapterItems());
	}

	@Override
	public void showSortByDialog() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			SortByBottomSheet.showInstance(activity.getSupportFragmentManager(), this);
		}
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
	public void showTracksDialog() {
		Bundle bundle = new Bundle();
		bundle.putString(OPEN_TRACKS_TAB, TrackTabType.ON_MAP.name());

		Bundle prevIntentParams = new Bundle();
		prevIntentParams.putInt(TAB_ID, TRACKS_TAB);

		MapActivity.launchMapActivityMoveToTop(requireActivity(), prevIntentParams, null, bundle);
	}

	@Override
	public void onGpxFilesDeletionStarted() {

	}

	@Override
	public void onGpxFilesDeleted(GPXInfo... values) {

	}

	@Override
	public void onGpxFilesDeletionFinished(boolean shouldUpdateFolders) {

	}
}
