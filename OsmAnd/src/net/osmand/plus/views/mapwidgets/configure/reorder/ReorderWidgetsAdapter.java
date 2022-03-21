package net.osmand.plus.views.mapwidgets.configure.reorder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.ButtonViewHolder;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.ButtonViewHolder.ButtonInfo;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.DividerViewHolder;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.HeaderViewHolder;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.SpaceViewHolder;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.WidgetViewHolder;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.WidgetViewHolder.CheckItemIsMovable;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.WidgetViewHolder.WidgetUiInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReorderWidgetsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
		implements ReorderItemTouchHelperCallback.OnItemMoveCallback, CheckItemIsMovable {

	private final OsmandApplication app;
	private DataHolder dataHolder;
	private WidgetAdapterListener listener;

	private List<ListItem> items = new ArrayList<>();
	private final boolean nightMode;

	public enum ItemType {
		HEADER,
		WIDGET,
		CARD_DIVIDER,
		CARD_TOP_DIVIDER,
		CARD_BOTTOM_DIVIDER,
		SPACE,
		BUTTON
	}

	public ReorderWidgetsAdapter(@NonNull OsmandApplication app,
	                             @NonNull DataHolder dataHolder,
	                             boolean nightMode) {
		setHasStableIds(true);
		this.app = app;
		this.nightMode = nightMode;
		this.dataHolder = dataHolder;
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		Context ctx = parent.getContext();
		LayoutInflater inflater = UiUtilities.getInflater(ctx, nightMode);
		ItemType type = viewType < ItemType.values().length ? ItemType.values()[viewType] : ItemType.CARD_DIVIDER;
		View itemView;
		switch (type) {
			case WIDGET:
				itemView = inflater.inflate(R.layout.configure_screen_list_item_widget_reorder, parent, false);
				return new WidgetViewHolder(itemView, ReorderWidgetsAdapter.this);
			case HEADER:
				itemView = inflater.inflate(R.layout.configure_screen_list_item_header, parent, false);
				return new HeaderViewHolder(itemView);
			case CARD_DIVIDER:
				itemView = inflater.inflate(R.layout.list_item_divider, parent, false);
				return new DividerViewHolder(itemView);
			case CARD_TOP_DIVIDER:
				itemView = inflater.inflate(R.layout.list_item_divider, parent, false);
				View bottomShadow = itemView.findViewById(R.id.bottomShadowView);
				bottomShadow.setVisibility(View.INVISIBLE);
				return new DividerViewHolder(itemView);
			case CARD_BOTTOM_DIVIDER:
				itemView = inflater.inflate(R.layout.card_bottom_divider, parent, false);
				return new DividerViewHolder(itemView);
			case SPACE:
				itemView = new View(ctx);
				return new SpaceViewHolder(itemView);
			case BUTTON:
				itemView = inflater.inflate(R.layout.configure_screen_list_item_button, parent, false);
				return new ButtonViewHolder(itemView);
			default:
				throw new IllegalArgumentException("Unsupported view type");
		}
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder,
	                             int position) {
		ListItem item = items.get(position);
		if (holder instanceof WidgetViewHolder) {
			WidgetViewHolder h = (WidgetViewHolder) holder;
			WidgetUiInfo widgetInfo = (WidgetUiInfo) item.value;
			h.title.setText(widgetInfo.title);
			h.icon.setImageResource(widgetInfo.iconId);
			setImageFilter(h.icon, !widgetInfo.isActive);
			h.moveIcon.setOnTouchListener((view, event) -> {
				if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
					listener.onDragStarted(holder);
				}
				return false;
			});
		} else if (holder instanceof HeaderViewHolder) {
			HeaderViewHolder h = (HeaderViewHolder) holder;
			String header = (String) item.value;
			h.title.setText(header);
		} else if (holder instanceof ButtonViewHolder) {
			ButtonInfo buttonInfo = (ButtonInfo) item.value;
			ButtonViewHolder h = (ButtonViewHolder) holder;
			h.buttonView.setOnClickListener(buttonInfo.listener);
			h.icon.setImageResource(buttonInfo.iconRes);
			h.title.setText(buttonInfo.title);
			ApplicationMode appMode = app.getSettings().getApplicationMode();
			int profileColor = appMode.getProfileColor(nightMode);
			Drawable drawable = UiUtilities.getColoredSelectableDrawable(app, profileColor, 0.3f);
			AndroidUtils.setBackground(h.buttonView, drawable);
		} else if (holder instanceof SpaceViewHolder) {
			float space = (float) item.value;
			((SpaceViewHolder) holder).setHeight((int) space);
		}
	}

	public void setItems(List<ListItem> items) {
		this.items = items;
		notifyDataSetChanged();
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
	public boolean onItemMove(int from, int to) {
		Object itemFrom = items.get(from).value;
		Object itemTo = items.get(to).value;
		if (itemFrom instanceof WidgetUiInfo && itemTo instanceof WidgetUiInfo) {
			dataHolder.orderModified();
			WidgetUiInfo widgetFrom = (WidgetUiInfo) itemFrom;
			WidgetUiInfo widgetTo = (WidgetUiInfo) itemTo;

			int orderFrom = widgetFrom.order;
			int orderTo = widgetTo.order;

			widgetFrom.order = orderTo;
			widgetTo.order = orderFrom;

			dataHolder.getOrders().put(widgetFrom.title, orderTo);
			dataHolder.getOrders().put(widgetTo.title, orderFrom);

			Collections.swap(items, from, to);
			notifyItemMoved(from, to);
			return true;
		}
		return false;
	}

	@Override
	public long getItemId(int position) {
		ListItem item = items.get(position);
		if (item.value instanceof WidgetUiInfo) {
			return ((WidgetUiInfo) item.value).title.hashCode();
		} else if (item.value instanceof ButtonInfo) {
			return ((ButtonInfo) item.value).title.hashCode();
		} else if (item.value != null) {
			return item.value.hashCode();
		}
		return item.hashCode();
	}

	@Override
	public void onItemDismiss(ViewHolder holder) {
		listener.onDragOrSwipeEnded(holder);
	}

	public void setListener(@NonNull WidgetAdapterListener listener) {
		this.listener = listener;
	}

	private void setImageFilter(@NonNull ImageView ivImage, boolean applyFilter) {
		if (applyFilter) {
			ColorMatrix matrix = new ColorMatrix();
			matrix.setSaturation(0);
			ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
			ivImage.setColorFilter(filter);
		} else {
			ivImage.clearColorFilter();
		}
	}

	@Override
	public boolean isListItemMovable(int position) {
		ListItem item = items.get(position);
		return item.value instanceof WidgetUiInfo;
	}


	public static class ListItem {
		ItemType type;
		Object value;

		public ListItem(@NonNull ItemType type, @NonNull Object value) {
			this.type = type;
			this.value = value;
		}
	}

	public interface WidgetAdapterListener {
		void onDragStarted(RecyclerView.ViewHolder holder);
		void onDragOrSwipeEnded(RecyclerView.ViewHolder holder);
	}

}
