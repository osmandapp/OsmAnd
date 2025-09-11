package net.osmand.plus.keyevent.fragments.editassignment;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static net.osmand.plus.keyevent.KeySymbolMapper.getKeySymbol;
import static net.osmand.plus.utils.AndroidUtils.setBackground;
import static net.osmand.plus.utils.UiUtilities.getColoredSelectableDrawable;
import static net.osmand.util.CollectionUtils.equalsToAny;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.DimenRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.containers.ScreenItem;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.FlowLayout;

import java.util.ArrayList;
import java.util.List;

class EditKeyAssignmentAdapter extends RecyclerView.Adapter<ViewHolder> {

	static final int CARD_TOP_DIVIDER = 1;
	static final int HEADER_ITEM = 2;
	static final int ADD_ACTION_ITEM = 3;
	static final int ASSIGNED_ACTION_ITEM = 4;
	static final int LIST_DIVIDER = 5;
	static final int ASSIGNED_KEY_ITEM = 6;
	static final int ADD_KEY_ITEM = 7;
	static final int CARD_BOTTOM_SHADOW = 8;
	static final int SPACE = 9;
	static final int CARD_DIVIDER = 10;
	static final int ASSIGNED_ACTION_OVERVIEW = 11;
	static final int ASSIGNED_KEYS_OVERVIEW = 12;

	private final OsmandApplication app;
	private final MapActivity mapActivity;
	private final ApplicationMode appMode;
	private ViewGroup parent;
	private Context context;

	private List<ScreenItem> screenItems = new ArrayList<>();
	private final boolean usedOnMap;
	private final EditKeyAssignmentController controller;

	public EditKeyAssignmentAdapter(@NonNull MapActivity mapActivity, @NonNull ApplicationMode appMode,
	                                @NonNull EditKeyAssignmentController controller, boolean usedOnMap) {
		this.mapActivity = mapActivity;
		this.app = mapActivity.getApp();
		this.appMode = appMode;
		this.usedOnMap = usedOnMap;
		this.controller = controller;
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		this.parent = parent;
		context = parent.getContext();
		switch (viewType) {
			case CARD_TOP_DIVIDER:
				return new CardTopDividerViewHolder(inflate(R.layout.list_item_divider));
			case HEADER_ITEM:
				return new HeaderViewHolder(inflate(R.layout.list_item_key_assignment_header));
			case ADD_ACTION_ITEM:
			case ASSIGNED_ACTION_ITEM:
			case ADD_KEY_ITEM:
			case ASSIGNED_KEY_ITEM:
				return new ActionItemViewHolder(inflate(R.layout.list_item_edit_key_assignment));
			case LIST_DIVIDER:
				return new CardDividerViewHolder(inflate(R.layout.divider));
			case CARD_DIVIDER:
				return new CardDividerViewHolder(inflate(R.layout.list_item_divider));
			case CARD_BOTTOM_SHADOW:
				return new CardBottomShadowViewHolder(inflate(R.layout.card_bottom_divider));
			case SPACE:
				return new SpaceViewHolder(new View(context), getDimen(R.dimen.fab_margin_bottom_big));
			case ASSIGNED_ACTION_OVERVIEW:
				return new ActionItemViewHolder(inflate(R.layout.list_item_key_assignment_action_simple));
			case ASSIGNED_KEYS_OVERVIEW:
				return new AssignedKeysViewHolder(inflate(R.layout.list_item_assigned_keys_overview));
			default:
				throw new IllegalArgumentException("Unsupported view type");
		}
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		int itemType = getItemViewType(position);
		ScreenItem screenItem = screenItems.get(position);
		if (itemType == HEADER_ITEM) {
			HeaderViewHolder h = (HeaderViewHolder) holder;
			h.title.setText(getString((Integer) screenItem.getValue()));
			return;
		} else if (itemType == ASSIGNED_ACTION_OVERVIEW) {
			ActionItemViewHolder h = (ActionItemViewHolder) holder;
			QuickAction action = (QuickAction) screenItem.getValue();
			h.icon.setImageResource(action.getIconRes(app));
			h.title.setText(action.getName(app));
			return;
		} else if (itemType == ASSIGNED_KEYS_OVERVIEW) {
			AssignedKeysViewHolder h = (AssignedKeysViewHolder) holder;
			h.flowLayout.removeAllViews();
			List<Integer> keyCodes = (List<Integer>) screenItem.getValue();
			for (Integer keycode : keyCodes) {
				h.flowLayout.addView(createKeycodeView(keycode));
			}
			return;
		}
		if (!equalsToAny(itemType, ADD_ACTION_ITEM, ASSIGNED_ACTION_ITEM, ADD_KEY_ITEM, ASSIGNED_KEY_ITEM)) {
			return;
		}
		ActionItemViewHolder h = (ActionItemViewHolder) holder;
		if (itemType == ADD_ACTION_ITEM) {
			h.actionButton.setImageDrawable(getAddIcon());
			h.title.setText(R.string.key_assignment_add_action);
			h.icon.setVisibility(View.GONE);
			h.summaryContainer.setVisibility(View.GONE);
			h.buttonView.setOnClickListener(v -> controller.askAddAction(mapActivity));

		} else if (itemType == ASSIGNED_ACTION_ITEM) {
			QuickAction command = (QuickAction) screenItem.getValue();
			h.actionButton.setImageDrawable(getDeleteIcon());
			h.title.setText(command.getName(app));
			h.icon.setVisibility(View.VISIBLE);
			h.icon.setImageResource(command.getIconRes(app));
			h.summaryContainer.setVisibility(View.GONE);
			h.actionButton.setOnClickListener(v -> controller.askDeleteAction());
			h.buttonView.setOnClickListener(v -> controller.askEditAction(mapActivity));

		} else if (itemType == ADD_KEY_ITEM) {
			h.actionButton.setImageDrawable(getAddIcon());
			h.title.setText(R.string.key_assignment_add_key);
			h.icon.setVisibility(View.GONE);
			h.summaryContainer.setVisibility(View.GONE);
			h.buttonView.setOnClickListener(v -> controller.askAddKeyCode(mapActivity));

		} else if (itemType == ASSIGNED_KEY_ITEM) {
			int keyCode = (int) screenItem.getValue();
			String keyName = getKeySymbol(app, keyCode);
			h.actionButton.setImageDrawable(getDeleteIcon());
			h.actionButton.setOnClickListener(v -> controller.askDeleteKeyCode(keyCode));
			h.title.setText(app.getString(R.string.key_name_pattern, keyName));
			h.icon.setVisibility(View.GONE);
			h.summaryContainer.setVisibility(View.VISIBLE);
			h.summaryContainer.setOnClickListener(v -> controller.askChangeKeyCode(mapActivity, keyCode));
			h.summary.setText(keyName);
		}
		int color = appMode.getProfileColor(isNightMode());
		setupSelectableBackground(h.buttonView, color);
	}

	@NonNull
	private View createKeycodeView(@NonNull Integer keyCode) {
		View view = inflate(R.layout.item_key_assignment_button);
		TextView title = view.findViewById(R.id.description);
		title.setText(getKeySymbol(app, keyCode));
		return view;
	}

	public void setScreenData(@NonNull List<ScreenItem> screenItems) {
		this.screenItems = screenItems;
		notifyDataSetChanged();
	}

	@Override
	public int getItemCount() {
		return screenItems.size();
	}

	@Override
	public int getItemViewType(int position) {
		return screenItems.get(position).getType();
	}

	@Override
	public long getItemId(int position) {
		return screenItems.get(position).getId();
	}

	@NonNull
	private Drawable getDeleteIcon() {
		return app.getUIUtilities().getIcon(R.drawable.ic_action_remove, R.color.color_osm_edit_delete);
	}

	@NonNull
	private Drawable getAddIcon() {
		return app.getUIUtilities().getIcon(R.drawable.ic_action_add, R.color.color_osm_edit_create);
	}

	private View inflate(@LayoutRes int layoutResId) {
		LayoutInflater inflater = UiUtilities.getInflater(context, controller.isNightMode());
		return inflater.inflate(layoutResId, parent, false);
	}

	private void setupSelectableBackground(@NonNull View view, @ColorInt int color) {
		setBackground(view, getColoredSelectableDrawable(view.getContext(), color, 0.3f));
	}

	private boolean isNightMode() {
		return app.getDaynightHelper().isNightMode(appMode, ThemeUsageContext.valueOf(usedOnMap));
	}

	private int getDimen(@DimenRes int resId) {
		return app.getResources().getDimensionPixelSize(resId);
	}

	@NonNull
	private String getString(@StringRes int resId) {
		return app.getString(resId);
	}

	static class CardTopDividerViewHolder extends ViewHolder {
		public CardTopDividerViewHolder(@NonNull View itemView) {
			super(itemView);
			View shadowToHide = itemView.findViewById(R.id.bottomShadowView);
			if (shadowToHide != null) {
				shadowToHide.setVisibility(View.INVISIBLE);
			}
		}
	}

	static class HeaderViewHolder extends ViewHolder {
		private final TextView title;

		public HeaderViewHolder(@NonNull View itemView) {
			super(itemView);
			title = itemView.findViewById(R.id.title);
		}
	}

	static class CardDividerViewHolder extends ViewHolder {
		public CardDividerViewHolder(@NonNull View itemView) {
			super(itemView);
		}
	}

	static class CardBottomShadowViewHolder extends ViewHolder {
		public CardBottomShadowViewHolder(@NonNull View itemView) {
			super(itemView);
		}
	}

	static class SpaceViewHolder extends ViewHolder {
		public SpaceViewHolder(@NonNull View itemView, int hSpace) {
			super(itemView);
			itemView.setLayoutParams(new LayoutParams(MATCH_PARENT, hSpace));
		}
	}

	static class AssignedKeysViewHolder extends ViewHolder {
		public FlowLayout flowLayout;

		public AssignedKeysViewHolder(@NonNull View itemView) {
			super(itemView);
			flowLayout = itemView.findViewById(R.id.flow_layout);
		}
	}

	static class ActionItemViewHolder extends ViewHolder {

		public View buttonView;
		public ImageView actionButton;
		public ImageView icon;
		public TextView title;
		public View summaryContainer;
		public TextView summary;

		public ActionItemViewHolder(@NonNull View itemView) {
			super(itemView);
			buttonView = itemView.findViewById(R.id.selectable_list_item);
			actionButton = itemView.findViewById(R.id.action_button);
			icon = itemView.findViewById(R.id.icon);
			title = itemView.findViewById(R.id.title);
			summaryContainer = itemView.findViewById(R.id.assigned_key);
			if (summaryContainer != null) {
				summary = summaryContainer.findViewById(R.id.description);
			}
		}
	}

}
