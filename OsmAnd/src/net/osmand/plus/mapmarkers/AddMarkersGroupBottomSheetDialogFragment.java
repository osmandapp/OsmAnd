package net.osmand.plus.mapmarkers;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.MapMarkersHelper.MarkersSyncGroup;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.mapmarkers.adapters.GroupsAdapter;

public abstract class AddMarkersGroupBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = "AddMarkersGroupBottomSheetDialogFragment";

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

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.fragment_marker_add_group_bottom_sheet_dialog, container);

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
				MarkersSyncGroup group = createMapMarkersSyncGroup(position);
				mapMarkersHelper.addMarkersSyncGroup(group);
				mapMarkersHelper.syncGroup(group);
				if (listener != null) {
					listener.onGroupAdded();
				}
				dismiss();
			}
		});
		recyclerView.setAdapter(adapter);

		mainView.findViewById(R.id.close_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismiss();
			}
		});

		setupHeightAndBackground(mainView, R.id.groups_recycler_view);

		return mainView;
	}

	protected abstract void createAdapter();

	protected abstract MarkersSyncGroup createMapMarkersSyncGroup(int position);

	public interface AddGroupListener {
		void onGroupAdded();
	}
}
