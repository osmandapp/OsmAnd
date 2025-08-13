package net.osmand.plus.views.mapwidgets.configure.panel;

import static net.osmand.plus.views.mapwidgets.configure.panel.SearchWidgetsFragment.PAYLOAD_SEPARATOR_UPDATE;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.ApplicationMode;
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
	private final ApplicationMode selectedAppMode;
	private final WidgetIconsHelper iconsHelper;

	public SearchWidgetsAdapter(@NonNull OsmandApplication app, @NonNull ApplicationMode selectedAppMode, @NonNull SearchWidgetListener listener, @NonNull List<Object> items,
	                            @NonNull WidgetIconsHelper iconsHelper, boolean nightMode) {
		this.items = items;
		this.nightMode = nightMode;
		this.listener = listener;
		this.iconsHelper = iconsHelper;
		this.app = app;
		this.selectedAppMode = selectedAppMode;
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
		return new SearchWidgetViewHolder(view, app);
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
		if (holder instanceof SearchWidgetViewHolder viewHolder) {
			boolean showDivider = position != items.size() - 1;
			Object item = items.get(position);
			if (item instanceof WidgetType) {
				viewHolder.bind(selectedAppMode, listener, (WidgetType) item, nightMode, showDivider);
			} else if (item instanceof GroupItem) {
				viewHolder.bind(selectedAppMode, listener, (GroupItem) item, nightMode, showDivider);
			} else if (item instanceof MapWidgetInfo) {
				viewHolder.bind(selectedAppMode, iconsHelper, listener, (MapWidgetInfo) item, nightMode, showDivider);
			}
		}
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads) {
		if (holder instanceof SearchWidgetViewHolder viewHolder) {
			if (!payloads.isEmpty() && payloads.contains(PAYLOAD_SEPARATOR_UPDATE)) {
				viewHolder.updateDivider(position != getItemCount() - 1);
			} else {
				super.onBindViewHolder(holder, position, payloads);
			}
		}
	}

	@Override
	public int getItemCount() {
		return items.size();
	}
}