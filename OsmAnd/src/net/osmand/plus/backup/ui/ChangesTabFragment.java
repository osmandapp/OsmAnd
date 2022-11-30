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

public class ChangesTabFragment extends BaseOsmAndFragment {
	protected OsmandApplication app;
	private boolean nightMode;

	private ChangesAdapter adapter;
	private List<SettingsItem> changeList;
	private ChangesTabType tabType;

	public ChangesTabFragment(OsmandApplication app) {
		this.app = app;
		nightMode = !app.getSettings().isLightContent();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		LayoutInflater themedInflater = UiUtilities.getInflater(app, nightMode);
		View view = themedInflater.inflate(R.layout.fragment_changes_tab, container, false);

		adapter = new ChangesAdapter(app.getOsmandMap().getMapView().getMapActivity(), changeList, nightMode, tabType);

		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
		recyclerView.setAdapter(adapter);
		recyclerView.setItemAnimator(null);
		recyclerView.setLayoutAnimation(null);

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		updateAdapter();

	}

	@Override
	public void onPause() {
		super.onPause();
	}

	public void setChangeList(List<SettingsItem> changeList) {
		this.changeList = changeList;
	}

	public void setTabType(ChangesTabType tabType) {
		this.tabType = tabType;
	}

	public ChangesTabType getTabType() {
		return tabType;
	}

	public String getTitle(){
		return app.getString(tabType.resId);
	}

	private void updateAdapter() {
		if (adapter != null) {
			adapter.updateItems();
		}
	}
}
