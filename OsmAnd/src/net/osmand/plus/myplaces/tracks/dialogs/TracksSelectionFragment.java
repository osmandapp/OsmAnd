package net.osmand.plus.myplaces.tracks.dialogs;

import static net.osmand.plus.myplaces.MyPlacesActivity.GPX_TAB;
import static net.osmand.plus.myplaces.MyPlacesActivity.TAB_ID;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.myplaces.tracks.TracksSelectionHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.osmedit.oauth.OsmOAuthHelper.OsmAuthorizationListener;
import net.osmand.plus.settings.enums.TracksSortMode;
import net.osmand.plus.track.data.TrackFolder;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class TracksSelectionFragment extends BaseTrackFolderFragment implements OsmAuthorizationListener {

	public static final String TAG = TrackFolderFragment.class.getSimpleName();

	private TracksSelectionHelper selectionHelper;

	private TextView toolbarTitle;
	private ImageButton selectionButton;

	@Override
	@ColorRes
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return ColorUtilities.getActiveColorId(nightMode);
	}

	@NonNull
	public List<TrackItem> getSelectedTrackItems() {
		return selectionHelper.getSelectedTrackItems();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		selectionHelper = new TracksSelectionHelper(app, trackFolder.getFlattenedTrackItems());

		View view = super.onCreateView(inflater, container, savedInstanceState);
		updateSelection();
		adapter.setSelectionMode(true);

		return view;
	}

	@Override
	protected void setupToolbar(@NonNull View view) {
		super.setupToolbar(view);

		Toolbar toolbar = view.findViewById(R.id.toolbar);
		toolbar.setBackgroundColor(ColorUtilities.getActiveColor(app, nightMode));

		toolbarTitle = toolbar.findViewById(R.id.toolbar_title);

		ImageView closeButton = toolbar.findViewById(R.id.close_button);
		closeButton.setImageDrawable(getIcon(R.drawable.ic_action_close));

		setupToolbarActions(view);
		ViewCompat.setElevation(view.findViewById(R.id.appbar), 5.0f);
	}

	private void setupToolbarActions(@NonNull View view) {
		ViewGroup container = view.findViewById(R.id.actions_container);
		container.removeAllViews();

		LayoutInflater inflater = UiUtilities.getInflater(view.getContext(), nightMode);
		setupSelectionButton(inflater, container);
		setupMenuButton(inflater, container);
	}

	private void setupSelectionButton(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
		selectionButton = (ImageButton) inflater.inflate(R.layout.action_button, container, false);
		selectionButton.setOnClickListener(v -> {
			if (selectionHelper.isAllItemsSelected()) {
				selectionHelper.clearSelectedItems();
			} else {
				selectionHelper.selectAllItems();
			}
			updateSelection();
			adapter.notifyDataSetChanged();
		});
		updateSelection();
		container.addView(selectionButton);
	}

	private void updateSelection() {
		toolbarTitle.setText(String.valueOf(selectionHelper.getSelectedItemsCount()));

		boolean selected = selectionHelper.isAllItemsSelected();
		int iconId = selected ? R.drawable.ic_action_deselect_all : R.drawable.ic_action_select_all;
		selectionButton.setImageDrawable(getIcon(iconId));
	}

	private void setupMenuButton(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
		ImageButton button = (ImageButton) inflater.inflate(R.layout.action_button, container, false);
		button.setImageDrawable(getIcon(R.drawable.ic_overflow_menu_white));
		button.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				openPopupMenu(v);
			}
		});
		container.addView(button);
	}

	private void openPopupMenu(@NonNull View view) {
		List<PopUpMenuItem> items = new ArrayList<>();

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_show_on_map)
				.setIcon(getContentIcon(R.drawable.ic_show_on_map))
				.setOnClickListener(v -> {
					selectionHelper.saveTracksVisibility();
					dismiss();
				})
				.create()
		);
		PluginsHelper.onOptionsMenuActivity(getActivity(), this, items);

		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = view;
		displayData.menuItems = items;
		displayData.nightMode = nightMode;
		PopUpMenu.show(displayData);
	}

	@Override
	public boolean isTrackItemSelected(@NonNull TrackItem trackItem) {
		return selectionHelper.isTrackItemSelected(trackItem);
	}

	@Override
	public void onTrackItemsSelected(@NonNull Set<TrackItem> trackItems, boolean selected) {
		selectionHelper.onTrackItemsSelected(trackItems, selected);
		adapter.onItemsSelected(trackItems);
		updateSelection();
	}

	@Override
	public boolean isFolderSelected(@NonNull TrackFolder folder) {
		return selectionHelper.isFolderSelected(folder);
	}

	@Override
	public void onFolderSelected(@NonNull TrackFolder folder) {
		selectionHelper.onFolderSelected(folder);
		adapter.onItemsSelected(Collections.singleton(folder));
		updateSelection();
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
	public void authorizationCompleted() {
		Bundle bundle = new Bundle();
		bundle.putInt(TAB_ID, GPX_TAB);

		Intent intent = new Intent(app, app.getAppCustomization().getMyPlacesActivity());
		intent.putExtra(MapActivity.INTENT_PARAMS, bundle);
		intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);

		app.startActivity(intent);
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull TrackFolder folder) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			TracksSelectionFragment fragment = new TracksSelectionFragment();
			fragment.trackFolder = folder;
			fragment.setRetainInstance(true);
			fragment.show(manager, TAG);
		}
	}
}
