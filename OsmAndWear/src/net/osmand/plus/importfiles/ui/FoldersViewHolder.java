package net.osmand.plus.importfiles.ui;

import static net.osmand.plus.measurementtool.adapter.FolderListAdapter.VIEW_TYPE_ADD;
import static net.osmand.plus.measurementtool.adapter.FolderListAdapter.getFolders;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.IndexConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.importfiles.ui.ImportTracksAdapter.ImportTracksListener;
import net.osmand.plus.measurementtool.adapter.FolderListAdapter.FolderListAdapterListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.List;

class FoldersViewHolder extends ViewHolder {

	final OsmandApplication app;
	final FoldersAdapter adapter;
	final ImportTracksListener listener;

	final View button;
	final View selectableItem;
	final ImageView groupIcon;
	final TextView buttonTitle;
	final RecyclerView recyclerView;

	final boolean nightMode;

	FoldersViewHolder(@NonNull View view, @Nullable ImportTracksListener listener, boolean nightMode) {
		super(view);
		this.app = (OsmandApplication) view.getContext().getApplicationContext();
		this.listener = listener;
		this.nightMode = nightMode;
		this.adapter = getAdapter();

		recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setAdapter(adapter);
		recyclerView.setLayoutManager(new LinearLayoutManager(app, RecyclerView.HORIZONTAL, false));

		button = view.findViewById(R.id.list_groups_button);
		groupIcon = button.findViewById(android.R.id.icon);
		buttonTitle = button.findViewById(android.R.id.title);
		selectableItem = button.findViewById(R.id.selectable_list_item);
	}

	public void bindView(@Nullable String selectedFolder) {
		adapter.setSelectedItem(selectedFolder);
		updateFolderItems();

		buttonTitle.setText(R.string.list_of_groups);
		button.setOnClickListener(v -> {
			if (listener != null) {
				listener.onFoldersListSelected();
			}
		});
		int color = ColorUtilities.getActiveColorId(nightMode);
		AndroidUtils.setBackground(selectableItem, UiUtilities.getSelectableDrawable(app));
		groupIcon.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_group_list, color));
	}

	@NonNull
	private FoldersAdapter getAdapter() {
		FoldersAdapter adapter = new FoldersAdapter(app, null, nightMode);
		adapter.setItems(getItems());
		adapter.setListener(new FolderListAdapterListener() {
			@Override
			public void onItemSelected(String item) {
				if (listener != null) {
					listener.onFolderSelected(item);
				}
			}

			@Override
			public void onAddNewItemSelected() {
				if (listener != null) {
					listener.onAddFolderSelected();
				}
			}
		});
		return adapter;
	}

	private void updateFolderItems() {
		List<Object> newItems = getItems();
		List<Object> items = adapter.getItems();

		if (!newItems.equals(items)) {
			newItems.removeAll(items);
			items.addAll(1, newItems);
			adapter.notifyDataSetChanged();
		}
	}

	@NonNull
	private List<Object> getItems() {
		List<Object> items = new ArrayList<>();
		items.add(VIEW_TYPE_ADD);
		items.addAll(getFolders(app.getAppPath(IndexConstants.GPX_INDEX_DIR)));
		return items;
	}
}
