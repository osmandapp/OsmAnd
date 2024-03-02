package net.osmand.plus.views.mapwidgets.configure.reorder;

import static net.osmand.plus.utils.UiUtilities.getColoredSelectableDrawable;
import static net.osmand.plus.views.mapwidgets.WidgetType.isComplexWidget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback.OnItemMoveCallback;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.SimpleWidgetInfo;
import net.osmand.plus.views.mapwidgets.WidgetGroup;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.configure.WidgetIconsHelper;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.ActionButtonViewHolder;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.ActionButtonViewHolder.ActionButtonInfo;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.AddPageButtonViewHolder;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.AddedWidgetViewHolder;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.AddedWidgetViewHolder.AddedWidgetUiInfo;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.AddedWidgetViewHolder.ItemMovableCallback;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.AvailableItemViewHolder;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.AvailableItemViewHolder.AvailableWidgetUiInfo;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.DividerViewHolder;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.HeaderViewHolder;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.PageViewHolder;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.PageViewHolder.PageUiInfo;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.SpaceViewHolder;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReorderWidgetsAdapter extends Adapter<ViewHolder> implements OnItemMoveCallback, ItemMovableCallback {

	private final OsmandApplication app;
	private final ReorderWidgetsAdapterHelper reorderHelper;
	private final List<ListItem> items = new ArrayList<>();
	private List<ListItem> itemsBeforeMove = new ArrayList<>();
	private final WidgetsPanel panel;

	private WidgetAdapterDragListener dragListener;
	private WidgetsAdapterActionsListener actionsListener;
	private final boolean nightMode;

	private final WidgetIconsHelper iconsHelper;
	private final int profileColor;

	public enum ItemType {
		HEADER,
		PAGE,
		ADDED_WIDGET,
		AVAILABLE_GROUP,
		AVAILABLE_WIDGET,
		ADD_PAGE_BUTTON,
		CARD_DIVIDER,
		CARD_TOP_DIVIDER,
		CARD_BOTTOM_DIVIDER,
		SPACE,
		ACTION_BUTTON
	}

	public ReorderWidgetsAdapter(@NonNull OsmandApplication app, @NonNull WidgetsDataHolder dataHolder, boolean nightMode) {
		setHasStableIds(true);
		this.app = app;
		this.nightMode = nightMode;
		this.reorderHelper = new ReorderWidgetsAdapterHelper(app, this, dataHolder, items, nightMode);
		this.panel = dataHolder.getSelectedPanel();

		ApplicationMode mode = app.getSettings().getApplicationMode();
		profileColor = mode.getProfileColor(nightMode);
		iconsHelper = new WidgetIconsHelper(app, profileColor, nightMode);
	}

	@NonNull
	public List<ListItem> getItems() {
		return items;
	}

	@SuppressLint("NotifyDataSetChanged")
	public void setItems(@NonNull List<ListItem> items) {
		this.items.clear();
		this.items.addAll(items);
		notifyDataSetChanged();
	}

	public void addWidget(@NonNull MapWidgetInfo widgetInfo) {
		reorderHelper.addWidget(widgetInfo);
	}

	public void setDragListener(@NonNull WidgetAdapterDragListener dragListener) {
		this.dragListener = dragListener;
	}

	public void setActionsListener(@NonNull WidgetsAdapterActionsListener actionsListener) {
		this.actionsListener = actionsListener;
	}

	public void restorePage(int page, int position) {
		reorderHelper.restorePage(page, position);
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		Context ctx = parent.getContext();
		LayoutInflater inflater = UiUtilities.getInflater(ctx, nightMode);

		switch (ItemType.values()[viewType]) {
			case HEADER:
				return new HeaderViewHolder(inflater.inflate(R.layout.configure_screen_list_item_header, parent, false));
			case PAGE:
				View itemView = inflater.inflate(R.layout.configure_screen_list_item_page_reorder, parent, false);
				return new PageViewHolder(itemView);
			case ADDED_WIDGET:
				itemView = inflater.inflate(R.layout.configure_screen_list_item_widget_reorder, parent, false);
				return new AddedWidgetViewHolder(itemView, this);
			case ADD_PAGE_BUTTON:
				itemView = inflater.inflate(R.layout.configure_screen_list_item_add_page, parent, false);
				return new AddPageButtonViewHolder(itemView, profileColor);
			case AVAILABLE_GROUP:
			case AVAILABLE_WIDGET:
				itemView = inflater.inflate(R.layout.configure_screen_list_item_available_item_reorder, parent, false);
				return new AvailableItemViewHolder(itemView);
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
			case ACTION_BUTTON:
				return new ActionButtonViewHolder(inflater.inflate(R.layout.configure_screen_list_item_button, parent, false));
			default:
				throw new IllegalArgumentException("Unsupported view type");
		}
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
		ListItem item = items.get(position);
		if (viewHolder instanceof PageViewHolder) {
			bindPageViewHolder(((PageViewHolder) viewHolder), position);
		} else if (viewHolder instanceof AddedWidgetViewHolder) {
			bindAddedWidgetViewHolder(((AddedWidgetViewHolder) viewHolder), position);
		} else if (viewHolder instanceof AvailableItemViewHolder) {
			if (item.type == ItemType.AVAILABLE_GROUP) {
				bindAvailableGroupViewHolder(((AvailableItemViewHolder) viewHolder), position);
			} else {
				bindAvailableWidgetViewHolder(((AvailableItemViewHolder) viewHolder), position);
			}
		} else if (viewHolder instanceof HeaderViewHolder) {
			HeaderViewHolder holder = (HeaderViewHolder) viewHolder;
			holder.title.setText((String) item.value);
		} else if (viewHolder instanceof ActionButtonViewHolder) {
			ActionButtonInfo actionButtonInfo = (ActionButtonInfo) item.value;

			ActionButtonViewHolder holder = (ActionButtonViewHolder) viewHolder;
			holder.buttonView.setOnClickListener(actionButtonInfo.listener);
			holder.icon.setImageResource(actionButtonInfo.iconRes);
			holder.title.setText(actionButtonInfo.title);

			AndroidUtils.setBackground(holder.buttonView, getColoredSelectableDrawable(app, profileColor, 0.3f));
		} else if (viewHolder instanceof SpaceViewHolder) {
			SpaceViewHolder holder = (SpaceViewHolder) viewHolder;
			holder.setHeight((int) item.value);
		} else if (viewHolder instanceof AddPageButtonViewHolder) {
			AddPageButtonViewHolder holder = (AddPageButtonViewHolder) viewHolder;
			holder.textView.setText(panel.isPanelVertical() ? R.string.add_row : R.string.add_page);
			holder.buttonContainer.setOnClickListener(v -> reorderHelper.addPage());
		}
	}

	@SuppressLint("ClickableViewAccessibility")
	private void bindPageViewHolder(@NonNull PageViewHolder viewHolder, int position) {
		int pageIndex = ((PageUiInfo) items.get(position).value).index;

		OnClickListener deletePageListener;
		Drawable deleteIcon;
		boolean firstPage = pageIndex == 0;

		if (firstPage) {
			deletePageListener = null;
			deleteIcon = getDeleteIcon(true);
		} else if (panel.isPanelVertical()) {
			boolean rowHasComplexWidget = rowHasComplexWidget(position);
			boolean previousRowHasComplexWidget = rowHasComplexWidget(getPreviousRowPosition(position));
			boolean isRowEmpty = getRowWidgetIds(position).isEmpty();

			if (rowHasComplexWidget && !isRowEmpty) {
				deletePageListener = v -> app.showToastMessage(app.getString(R.string.remove_widget_first));
				deleteIcon = getDeleteIcon(true);
			} else if (previousRowHasComplexWidget && !isRowEmpty) {
				deletePageListener = v -> app.showToastMessage(app.getString(R.string.previous_row_has_complex_widget));
				deleteIcon = getDeleteIcon(true);
			} else{
				deletePageListener = v -> deletePage(viewHolder.getAdapterPosition(), pageIndex);
				deleteIcon = getDeleteIcon(false);
			}
		} else {
			deletePageListener = v -> deletePage(viewHolder.getAdapterPosition(), pageIndex);
			deleteIcon = getDeleteIcon(false);
		}
		viewHolder.deletePageButton.setOnClickListener(deletePageListener);
		viewHolder.deletePageButton.setImageDrawable(deleteIcon);

		AndroidUiHelper.updateVisibility(viewHolder.topDivider, !firstPage);
		viewHolder.pageText.setText(app.getString(panel.isPanelVertical() ? R.string.row_number : R.string.page_number, String.valueOf(pageIndex + 1)));

		boolean hideMoveIcon = firstPage || (panel.isPanelVertical() && rowHasComplexWidget(position));
		AndroidUiHelper.updateVisibility(viewHolder.moveIcon, !hideMoveIcon);
		viewHolder.moveIcon.setOnTouchListener((v, event) -> {
			if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
				if (panel.isPanelVertical()) {
					itemsBeforeMove.clear();
					itemsBeforeMove.addAll(items);
				}
				dragListener.onDragStarted(viewHolder);
			}
			return false;
		});
	}

	private ArrayList<AddedWidgetUiInfo> getRowWidgetIds(int rowPosition) {
		ArrayList<AddedWidgetUiInfo> rowWidgetIds = new ArrayList<>();
		ListItem item = items.get(++rowPosition);

		while (rowPosition < items.size() && item.type == ItemType.ADDED_WIDGET) {
			item = items.get(rowPosition);
			if (item.type == ItemType.ADDED_WIDGET) {
				rowWidgetIds.add((AddedWidgetUiInfo) item.value);
			}
			rowPosition++;
		}
		return rowWidgetIds;
	}

	private int getPreviousRowPosition(int currentRowPosition) {
		currentRowPosition--;
		while (currentRowPosition >= 0) {
			if (items.get(currentRowPosition).type == ItemType.PAGE) {
				return currentRowPosition;
			}
			currentRowPosition--;
		}
		return -1;
	}

	private boolean rowHasComplexWidget(int rowPosition) {
		ArrayList<AddedWidgetUiInfo> rowWidgetIds = getRowWidgetIds(rowPosition);
		for (AddedWidgetUiInfo widgetUiInfo : rowWidgetIds) {
			if (isComplexWidget(widgetUiInfo.key)) {
				return true;
			}
		}
		return false;
	}

	@Nullable
	private String getComplexWidgetName(int rowPosition){
		ArrayList<AddedWidgetUiInfo> rowWidgetIds = getRowWidgetIds(rowPosition);
		for (AddedWidgetUiInfo widgetUiInfo : rowWidgetIds) {
			if (isComplexWidget(widgetUiInfo.key)) {
				return widgetUiInfo.title;
			}
		}
		return null;
	}

	private void deletePage(int position, int pageToDelete) {
		reorderHelper.deletePage(position, pageToDelete);
		if (actionsListener != null) {
			actionsListener.onPageDeleted(pageToDelete, position);
		}
	}

	@SuppressLint("ClickableViewAccessibility")
	private void bindAddedWidgetViewHolder(@NonNull AddedWidgetViewHolder viewHolder, int position) {
		AddedWidgetUiInfo widgetInfo = (AddedWidgetUiInfo) items.get(position).value;

		viewHolder.deleteWidgetButton.setImageDrawable(getDeleteIcon(false));
		viewHolder.deleteWidgetButton.setOnClickListener(v -> {
			int pos = viewHolder.getAdapterPosition();
			reorderHelper.deleteWidget(widgetInfo, pos);
		});

		viewHolder.title.setText(widgetInfo.title);
		viewHolder.moveIcon.setOnTouchListener((view, event) -> {
			if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
				if (panel.isPanelVertical()) {
					itemsBeforeMove.clear();
					itemsBeforeMove.addAll(items);
				}
				dragListener.onDragStarted(viewHolder);
			}
			return false;
		});
		viewHolder.icon.setImageResource(widgetInfo.iconId);
		iconsHelper.updateWidgetIcon(viewHolder.icon, widgetInfo.info);
	}

	private void bindAvailableGroupViewHolder(@NonNull AvailableItemViewHolder viewHolder, int position) {
		WidgetGroup widgetGroup = ((WidgetGroup) items.get(position).value);
		OnClickListener showInfoListener = v -> {
			if (actionsListener != null) {
				actionsListener.showWidgetGroupInfo(widgetGroup, Collections.emptyList());
			}
		};

		viewHolder.addButton.setImageDrawable(getAddIcon());
		viewHolder.addButton.setOnClickListener(showInfoListener);

		viewHolder.icon.setImageResource(widgetGroup.getIconId(nightMode));
		viewHolder.title.setText(AvailableItemViewHolder.getGroupTitle(widgetGroup, app, nightMode));

		viewHolder.infoButton.setOnClickListener(showInfoListener);
		viewHolder.itemView.setOnClickListener(v -> viewHolder.infoButton.callOnClick());
		AndroidUtils.setBackground(viewHolder.container, getColoredSelectableDrawable(app, profileColor, 0.3f));

		updateAvailableItemDivider(viewHolder, position);
	}

	private void bindAvailableWidgetViewHolder(@NonNull AvailableItemViewHolder viewHolder, int position) {
		AvailableWidgetUiInfo widgetInfo = (AvailableWidgetUiInfo) items.get(position).value;

		viewHolder.addButton.setImageDrawable(getAddIcon());
		viewHolder.addButton.setOnClickListener(v -> {
			int pos = viewHolder.getAdapterPosition();
			AvailableWidgetUiInfo widgetUiInfo = ((AvailableWidgetUiInfo) items.get(pos).value);
			addWidget(widgetUiInfo.info);
		});

		iconsHelper.updateWidgetIcon(viewHolder.icon, widgetInfo.info);
		viewHolder.title.setText(widgetInfo.title);

		viewHolder.infoButton.setOnClickListener(v -> showWidgetInfoIfPossible(widgetInfo));
		viewHolder.itemView.setOnClickListener(v -> viewHolder.infoButton.callOnClick());
		AndroidUtils.setBackground(viewHolder.container, getColoredSelectableDrawable(app, profileColor, 0.3f));

		updateAvailableItemDivider(viewHolder, position);
	}

	private void showWidgetInfoIfPossible(@NonNull AvailableWidgetUiInfo widgetInfo) {
		if (actionsListener != null) {
			WidgetType widgetType = WidgetType.getById(widgetInfo.key);
			if (widgetType != null) {
				actionsListener.showWidgetInfo(widgetType);
			} else {
				String externalProviderPackage = widgetInfo.info.getExternalProviderPackage();
				if (!Algorithms.isEmpty(externalProviderPackage)) {
					actionsListener.showExternalWidgetIndo(widgetInfo.key, externalProviderPackage);
				}
			}
		}
	}

	private void updateAvailableItemDivider(@NonNull AvailableItemViewHolder viewHolder, int position) {
		ItemType nextItemType = position + 1 == items.size() ? null : items.get(position + 1).type;
		boolean lastAvailableItem = nextItemType != ItemType.AVAILABLE_GROUP && nextItemType != ItemType.AVAILABLE_WIDGET;
		AndroidUiHelper.updateVisibility(viewHolder.bottomDivider, !lastAvailableItem);
	}

	@Override
	public boolean onItemMove(int from, int to) {
		return reorderHelper.swapItemsIfAllowed(from, to);
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
	public void onItemDismiss(@NonNull ViewHolder holder) {
		if (panel.isPanelVertical()) {
			for (int index = 0; index < items.size(); index++) {
				ListItem item = items.get(index);
				if (item.type == ItemType.PAGE && rowHasComplexWidget(index) && getRowWidgetIds(index).size() > 1) {
					app.showToastMessage(app.getString(R.string.complex_widget_alert, getComplexWidgetName(index)));
					items.clear();
					items.addAll(itemsBeforeMove);
					break;
				}
			}
		}
		dragListener.onDragOrSwipeEnded(holder);
	}

	@Override
	public long getItemId(int position) {
		ListItem item = items.get(position);
		if (item.value instanceof PageUiInfo) {
			int page = ((PageUiInfo) item.value).index;
			return app.getString(panel.isPanelVertical() ? R.string.row_number : R.string.page_number, String.valueOf(page)).hashCode();
		} else if (item.value instanceof AddedWidgetUiInfo) {
			return ((AddedWidgetUiInfo) item.value).key.hashCode();
		} else if (item.type == ItemType.ADD_PAGE_BUTTON) {
			return panel.isPanelVertical() ? R.string.add_row : R.string.add_page;
		} else if (item.value instanceof ActionButtonInfo) {
			return ((ActionButtonInfo) item.value).title.hashCode();
		} else if (item.value instanceof WidgetGroup) {
			return ((WidgetGroup) item.value).titleId;
		} else if (item.value instanceof AvailableWidgetUiInfo) {
			return ((AvailableWidgetUiInfo) item.value).key.hashCode();
		} else if (item.value != null) {
			return item.value.hashCode();
		}
		return (item.type.name() + position).hashCode();
	}

	@Override
	public boolean isListItemMovable(int position) {
		Object itemValue = items.get(position).value;
		return itemValue instanceof PageUiInfo || itemValue instanceof AddedWidgetUiInfo;
	}

	@NonNull
	private Drawable getDeleteIcon(boolean disabled) {
		return disabled
				? app.getUIUtilities().getIcon(R.drawable.ic_action_remove, nightMode)
				: app.getUIUtilities().getIcon(R.drawable.ic_action_remove, R.color.color_osm_edit_delete);
	}

	@NonNull
	private Drawable getAddIcon() {
		return app.getUIUtilities().getIcon(R.drawable.ic_action_add, R.color.color_osm_edit_create);
	}

	public static class ListItem {

		public final ItemType type;
		public final Object value;

		public ListItem(@NonNull ItemType type, @Nullable Object value) {
			this.type = type;
			this.value = value;
		}
	}

	public interface WidgetAdapterDragListener {

		void onDragStarted(ViewHolder holder);

		void onDragOrSwipeEnded(ViewHolder holder);
	}

	public interface WidgetsAdapterActionsListener {

		void onPageDeleted(int page, int position);

		void showWidgetInfo(@NonNull WidgetType widget);

		void showWidgetGroupInfo(@NonNull WidgetGroup widgetGroup, @NonNull List<String> addedGroupWidgetsIds);

		void showExternalWidgetIndo(@NonNull String widgetId, @NonNull String externalProviderPackage);
	}
}