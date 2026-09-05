package net.osmand.plus.widgets.ctxmenu;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.widgets.ctxmenu.callback.OnDataChangeUiAdapter;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class ContextMenuListAdapter extends ArrayAdapter<ContextMenuItem> implements OnDataChangeUiAdapter {

	private final ViewCreator viewCreator;

	public ContextMenuListAdapter(@NonNull Activity context,
	                              @NonNull ViewCreator viewCreator,
	                              @NonNull List<ContextMenuItem> objects) {
		super(context, viewCreator.getDefaultLayoutId(), R.id.title, objects);
		this.viewCreator = viewCreator;
		viewCreator.setUiAdapter(this);
	}

	@Override
	public boolean isEnabled(int position) {
		ContextMenuItem item = getItem(position);
		if (item != null) {
			return !item.isCategory() && item.isClickable() && item.getLayout() != R.layout.drawer_divider;
		}
		return true;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		return viewCreator.getView(getItem(position), convertView);
	}

	@Override
	public void onDataSetChanged() {
		notifyDataSetChanged();
	}

	@Override
	public void onDataSetInvalidated() {
		notifyDataSetInvalidated();
	}

	@Override
	public void onRefreshItem(@NonNull String itemId) {
		ContextMenuItem item = getItemById(itemId);
		if (item != null) {
			item.refreshWithActualData();
			onDataSetChanged();
		}
	}

	@Nullable
	public ContextMenuItem getItemById(@NonNull String itemId) {
		for (int i = 0; i < getCount(); i++) {
			ContextMenuItem item = getItem(i);
			if (Objects.equals(itemId, item.getId())) {
				return item;
			}
		}
		return null;
	}

}
