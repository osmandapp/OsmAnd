package net.osmand.plus.views.mapwidgets.configure.reorder;

import static net.osmand.plus.utils.UiUtilities.getColoredSelectableDrawable;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback.OnItemMoveCallback;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.ButtonViewHolder;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.ButtonViewHolder.ButtonInfo;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.DividerViewHolder;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.HeaderViewHolder;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.SpaceViewHolder;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.WidgetViewHolder;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.WidgetViewHolder.ItemMovableCallback;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.WidgetViewHolder.WidgetUiInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReorderWidgetsAdapter extends Adapter<ViewHolder> implements OnItemMoveCallback, ItemMovableCallback {

	private final OsmandApplication app;
	private final WidgetsDataHolder dataHolder;
	private final List<ListItem> items = new ArrayList<>();

	private WidgetAdapterListener listener;
	private final boolean nightMode;
	private final int profileColor;
	private final int defaultIconColor;

	public enum ItemType {
		HEADER,
		WIDGET,
		CARD_DIVIDER,
		CARD_TOP_DIVIDER,
		CARD_BOTTOM_DIVIDER,
		SPACE,
		BUTTON
	}

	public ReorderWidgetsAdapter(@NonNull OsmandApplication app, @NonNull WidgetsDataHolder dataHolder, boolean nightMode) {
		setHasStableIds(true);
		this.app = app;
		this.nightMode = nightMode;
		this.dataHolder = dataHolder;

		ApplicationMode mode = app.getSettings().getApplicationMode();
		profileColor = mode.getProfileColor(nightMode);
		defaultIconColor = ColorUtilities.getDefaultIconColor(app, nightMode);
	}

	@NonNull
	public List<ListItem> getItems() {
		return items;
	}

	public void setItems(@NonNull List<ListItem> items) {
		this.items.clear();
		this.items.addAll(items);
		notifyDataSetChanged();
	}

	public void setListener(@NonNull WidgetAdapterListener listener) {
		this.listener = listener;
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		Context ctx = parent.getContext();
		LayoutInflater inflater = UiUtilities.getInflater(ctx, nightMode);

		switch (ItemType.values()[viewType]) {
			case WIDGET:
				View itemView = inflater.inflate(R.layout.configure_screen_list_item_widget_reorder, parent, false);
				return new WidgetViewHolder(itemView, ReorderWidgetsAdapter.this);
			case HEADER:
				return new HeaderViewHolder(inflater.inflate(R.layout.configure_screen_list_item_header, parent, false));
			case CARD_DIVIDER:
				return new DividerViewHolder(inflater.inflate(R.layout.list_item_divider, parent, false));
			case CARD_TOP_DIVIDER:
				itemView = inflater.inflate(R.layout.list_item_divider, parent, false);
				View bottomShadow = itemView.findViewById(R.id.bottomShadowView);
				bottomShadow.setVisibility(View.INVISIBLE);
				return new DividerViewHolder(itemView);
			case CARD_BOTTOM_DIVIDER:
				return new DividerViewHolder(inflater.inflate(R.layout.card_bottom_divider, parent, false));
			case SPACE:
				return new SpaceViewHolder(new View(ctx));
			case BUTTON:
				return new ButtonViewHolder(inflater.inflate(R.layout.configure_screen_list_item_button, parent, false));
			default:
				throw new IllegalArgumentException("Unsupported view type");
		}
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
		ListItem item = items.get(position);
		if (viewHolder instanceof WidgetViewHolder) {
			WidgetUiInfo widgetInfo = (WidgetUiInfo) item.value;
			WidgetViewHolder holder = (WidgetViewHolder) viewHolder;

			holder.title.setText(widgetInfo.title);
			holder.moveIcon.setOnTouchListener((view, event) -> {
				if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
					listener.onDragStarted(viewHolder);
				}
				return false;
			});
			holder.icon.setImageResource(widgetInfo.iconId);
			WidgetViewHolder.updateWidgetIcon(holder.icon, widgetInfo.info, profileColor, defaultIconColor, widgetInfo.isActive, nightMode);
		} else if (viewHolder instanceof HeaderViewHolder) {
			HeaderViewHolder holder = (HeaderViewHolder) viewHolder;
			holder.title.setText((String) item.value);
		} else if (viewHolder instanceof ButtonViewHolder) {
			ButtonInfo buttonInfo = (ButtonInfo) item.value;

			ButtonViewHolder holder = (ButtonViewHolder) viewHolder;
			holder.buttonView.setOnClickListener(buttonInfo.listener);
			holder.icon.setImageResource(buttonInfo.iconRes);
			holder.title.setText(buttonInfo.title);

			ApplicationMode appMode = app.getSettings().getApplicationMode();
			int profileColor = appMode.getProfileColor(nightMode);
			AndroidUtils.setBackground(holder.buttonView, getColoredSelectableDrawable(app, profileColor, 0.3f));
		} else if (viewHolder instanceof SpaceViewHolder) {
			SpaceViewHolder holder = (SpaceViewHolder) viewHolder;
			holder.setHeight((int) item.value);
		}
	}

	@Override
	public boolean onItemMove(int from, int to) {
		Object itemTo = items.get(to).value;
		Object itemFrom = items.get(from).value;
		if (itemFrom instanceof WidgetUiInfo && itemTo instanceof WidgetUiInfo) {
			WidgetUiInfo widgetTo = (WidgetUiInfo) itemTo;
			WidgetUiInfo widgetFrom = (WidgetUiInfo) itemFrom;

			int orderTo = widgetTo.order;
			int orderFrom = widgetFrom.order;

			widgetTo.order = orderFrom;
			widgetFrom.order = orderTo;

			dataHolder.getOrders().put(widgetTo.title, orderFrom);
			dataHolder.getOrders().put(widgetFrom.title, orderTo);

			Collections.swap(items, from, to);
			notifyItemMoved(from, to);
			return true;
		}
		return false;
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	@Override
	public int getItemViewType(int position) {
		ListItem item = items.get(position);
		return item.type.ordinal();
	}

	@Override
	public void onItemDismiss(ViewHolder holder) {
		listener.onDragOrSwipeEnded(holder);
	}

	@Override
	public long getItemId(int position) {
		ListItem item = items.get(position);
		if (item.value instanceof WidgetUiInfo) {
			return ((WidgetUiInfo) item.value).key.hashCode();
		} else if (item.value instanceof ButtonInfo) {
			return ((ButtonInfo) item.value).title.hashCode();
		} else if (item.value != null) {
			return item.value.hashCode();
		}
		return item.hashCode();
	}

	@Override
	public boolean isListItemMovable(int position) {
		ListItem item = items.get(position);
		return item.value instanceof WidgetUiInfo;
	}

	public static class ListItem {

		public final ItemType type;
		public final Object value;

		public ListItem(@NonNull ItemType type, @Nullable Object value) {
			this.type = type;
			this.value = value;
		}
	}

	public interface WidgetAdapterListener {

		void onDragStarted(ViewHolder holder);

		void onDragOrSwipeEnded(ViewHolder holder);
	}
}