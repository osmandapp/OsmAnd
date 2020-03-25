package net.osmand.view;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.settings.UiCustomizationFragment;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback;

import java.util.List;

public abstract class BaseMoveItemAdapter
		extends RecyclerView.Adapter<RecyclerView.ViewHolder>
		implements ReorderItemTouchHelperCallback.OnItemMoveCallback {

//	private List<Object>


	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		return null;
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

	}

//	private class ListItem {
//		UiCustomizationFragment.AdapterItemType type;
//		Object value;
//
//		public ListItem(UiCustomizationFragment.AdapterItemType type, Object value) {
//			this.type = type;
//			this.value = value;
//		}
//	}

	public enum ItemType {
		DESCRIPTION,
		UI_ITEM,
		DIVIDER,
		HEADER,
		BUTTON
	}
}
