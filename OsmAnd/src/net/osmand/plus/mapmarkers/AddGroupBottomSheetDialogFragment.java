package net.osmand.plus.mapmarkers;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ContextThemeWrapper;
import android.view.View;

import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.MapMarkersHelper.MarkersSyncGroup;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.mapmarkers.adapters.GroupsAdapter;

public abstract class AddGroupBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = "AddGroupBottomSheetDialogFragment";

	private AddGroupListener listener;
	protected View mainView;
	protected GroupsAdapter adapter;
	protected MapMarkersHelper mapMarkersHelper;

	public void setListener(AddGroupListener listener) {
		this.listener = listener;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mapMarkersHelper = getMyApplication().getMapMarkersHelper();
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes),
				R.layout.fragment_marker_add_group_bottom_sheet_dialog, null);

		final RecyclerView recyclerView = mainView.findViewById(R.id.groups_recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		createAdapter();
		adapter.setAdapterListener(new GroupsAdapter.GroupsAdapterListener() {
			@Override
			public void onItemClick(View view) {
				int position = recyclerView.getChildAdapterPosition(view);
				if (position == RecyclerView.NO_POSITION) {
					return;
				}
				showProgressBar();
				MarkersSyncGroup group = createMapMarkersSyncGroup(position);
				mapMarkersHelper.addMarkersSyncGroup(group);
				mapMarkersHelper.syncGroupAsync(group, new MapMarkersHelper.OnGroupSyncedListener() {
					@Override
					public void onSyncDone() {
						if (listener != null) {
							listener.onGroupAdded();
						}
						dismiss();
					}
				});
			}
		});
		recyclerView.setAdapter(adapter);

		items.add(new BaseBottomSheetItem.Builder().setCustomView(mainView).create());
	}

	@Override
	public void onDestroyView() {
		Dialog dialog = getDialog();
		if (dialog != null && getRetainInstance()) {
			dialog.setDismissMessage(null);
		}
		super.onDestroyView();
	}

	@Override
	protected boolean useScrollableItemsContainer() {
		return false;
	}

	private void showProgressBar() {
		mainView.findViewById(R.id.groups_recycler_view).setVisibility(View.GONE);
		mainView.findViewById(R.id.progress_bar).setVisibility(View.VISIBLE);
	}

	protected abstract void createAdapter();

	protected abstract MarkersSyncGroup createMapMarkersSyncGroup(int position);

	public interface AddGroupListener {
		void onGroupAdded();
	}
}
