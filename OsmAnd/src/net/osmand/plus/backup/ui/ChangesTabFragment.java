package net.osmand.plus.backup.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.backup.ui.ChangesFragment.ChangesTabType;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.plus.utils.UiUtilities;

import java.util.List;

public abstract class ChangesTabFragment extends BaseOsmAndFragment {

	protected OsmandApplication app;

	private List<SettingsItem> settingsItems;

	private ChangesTabType tabType;
	private ChangesAdapter adapter;

	protected boolean nightMode;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		nightMode = isNightMode(false);
		tabType = getChangesTabType();
		settingsItems = getSettingsItems();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		LayoutInflater themedInflater = UiUtilities.getInflater(app, nightMode);
		View view = themedInflater.inflate(R.layout.fragment_changes_tab, container, false);

		adapter = new ChangesAdapter(settingsItems, nightMode, tabType, requireActivity().getSupportFragmentManager());

		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
		recyclerView.setAdapter(adapter);
		recyclerView.setItemAnimator(null);
		recyclerView.setLayoutAnimation(null);

		return view;
	}

	public abstract ChangesTabType getChangesTabType();

	public abstract List<SettingsItem> getSettingsItems();

	@Override
	public void onResume() {
		super.onResume();
		updateAdapter();
	}

	private void updateAdapter() {
		if (adapter != null) {
			adapter.updateItems();
		}
	}
}