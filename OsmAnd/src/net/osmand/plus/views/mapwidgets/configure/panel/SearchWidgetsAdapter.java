package net.osmand.plus.views.mapwidgets.configure.panel;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.configure.WidgetIconsHelper;
import net.osmand.plus.views.mapwidgets.configure.panel.SearchWidgetsFragment.GroupItem;
import net.osmand.plus.views.mapwidgets.configure.panel.holders.SearchWidgetViewHolder;

import java.util.List;

public class SearchWidgetsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	private static final int TYPE_WIDGET = 0;
	private static final int TYPE_GROUP = 1;

	private List<Object> items;
	boolean nightMode;
	private final SearchWidgetListener listener;
	private final OsmandApplication app;
	private final WidgetIconsHelper iconsHelper;


	public SearchWidgetsAdapter(@NonNull OsmandApplication app, @NonNull SearchWidgetListener listener, @NonNull List<Object> items,
	                            @NonNull WidgetIconsHelper iconsHelper, boolean nightMode) {
		this.items = items;
		this.nightMode = nightMode;
		this.listener = listener;
		this.iconsHelper = iconsHelper;
		this.app = app;
	}

	public void setItems(@NonNull List<Object> items) {
		this.items.clear();
		this.items = items;
	}

	@NonNull
	public List<Object> getItems() {
		return items;
	}

	@Override
	public int getItemViewType(int position) {
		return (items.get(position) instanceof WidgetType) ? TYPE_WIDGET : TYPE_GROUP;
	}

	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		LayoutInflater inflater = LayoutInflater.from(parent.getContext());
		View view = inflater.inflate(R.layout.item_widget, parent, false);
		return new SearchWidgetViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
		if (holder instanceof SearchWidgetViewHolder viewHolder) {

			Object item = items.get(position);
			if (item instanceof WidgetType) {
				viewHolder.bind(app, listener, (WidgetType) item, nightMode);
			} else if (item instanceof GroupItem) {
				viewHolder.bind(listener, (GroupItem) item, nightMode);
			} else if (item instanceof MapWidgetInfo) {
				viewHolder.bind(app, iconsHelper, listener, (MapWidgetInfo) item);
			}
		}
	}

	@Override
	public int getItemCount() {
		return items.size();
	}
}