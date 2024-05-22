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
import android.widget.ImageButton;
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
import net.osmand.plus.base.containers.ScreenItem;
import net.osmand.plus.helpers.RequestMapThemeParams;
import net.osmand.plus.keyevent.commands.KeyEventCommand;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.List;

class EditKeyAssignmentAdapter extends RecyclerView.Adapter<ViewHolder> {

	static final int CARD_TOP_DIVIDER = 1;
	static final int HEADER_ITEM = 2;
	static final int ADD_ACTION_ITEM = 3;
	static final int ASSIGNED_ACTION_ITEM = 4;
	static final int CARD_DIVIDER = 5;
	static final int ASSIGNED_KEY_ITEM = 6;
	static final int ADD_KEY_ITEM = 7;
	static final int CARD_BOTTOM_SHADOW = 8;
	static final int SPACE = 9;

	private final OsmandApplication app;
	private final ApplicationMode appMode;
	private ViewGroup parent;
	private Context context;

	private List<ScreenItem> screenItems = new ArrayList<>();
	private final boolean usedOnMap;
	private final EditKeyAssignmentController controller;

	public EditKeyAssignmentAdapter(@NonNull OsmandApplication app, @NonNull ApplicationMode appMode,
	                                @NonNull EditKeyAssignmentController controller, boolean usedOnMap) {
		setHasStableIds(true);
		this.app = app;
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
			case CARD_DIVIDER:
				return new CardDividerViewHolder(inflate(R.layout.divider));
			case CARD_BOTTOM_SHADOW:
				return new CardBottomShadowViewHolder(inflate(R.layout.card_bottom_divider));
			case SPACE:
				return new SpaceViewHolder(new View(context), getDimen(R.dimen.fab_margin_bottom_big));
			default:
				throw new IllegalArgumentException("Unsupported view type");
		}
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		int itemType = getItemViewType(position);
		if (!equalsToAny(itemType, HEADER_ITEM, ADD_ACTION_ITEM, ASSIGNED_ACTION_ITEM, ADD_KEY_ITEM, ASSIGNED_KEY_ITEM)) {
			return;
		}
		ScreenItem screenItem = screenItems.get(position);
		if (itemType == HEADER_ITEM) {
			HeaderViewHolder h = (HeaderViewHolder) holder;
			h.title.setText(getString((Integer) screenItem.getValue()));
			return;

		}
		ActionItemViewHolder h = (ActionItemViewHolder) holder;
		if (itemType == ADD_ACTION_ITEM) {
			h.actionButton.setImageDrawable(getAddIcon());
			h.title.setText(R.string.key_assignment_add_action);
			h.icon.setVisibility(View.GONE);
			h.summaryContainer.setVisibility(View.GONE);
			h.buttonView.setOnClickListener(v -> controller.askAddAction());

		} else if (itemType == ASSIGNED_ACTION_ITEM) {
			KeyEventCommand command = (KeyEventCommand) screenItem.getValue();
			h.actionButton.setImageDrawable(getDeleteIcon());
			h.title.setText(command.toHumanString(app));
			h.icon.setVisibility(View.VISIBLE);
			h.icon.setImageResource(command.getIconId());
			h.summaryContainer.setVisibility(View.GONE);
			h.actionButton.setOnClickListener(v -> controller.askDeleteAction());

		} else if (itemType == ADD_KEY_ITEM) {
			h.actionButton.setImageDrawable(getAddIcon());
			h.title.setText(R.string.key_assignment_add_key);
			h.icon.setVisibility(View.GONE);
			h.summaryContainer.setVisibility(View.GONE);
			h.buttonView.setOnClickListener(v -> controller.askAddKeyCode());

		} else if (itemType == ASSIGNED_KEY_ITEM) {
			int keyCode = (int) screenItem.getValue();
			String keyName = getKeySymbol(app, keyCode);
			h.actionButton.setImageDrawable(getDeleteIcon());
			h.actionButton.setOnClickListener(v -> controller.askDeleteKeyCode(keyCode));
			h.title.setText(app.getString(R.string.key_name_pattern, keyName));
			h.icon.setVisibility(View.GONE);
			h.summaryContainer.setVisibility(View.VISIBLE);
			h.summary.setText(keyName);

		}
//		int color = appMode.getProfileColor(isNightMode());
//		setupSelectableBackground(h.buttonView, color);
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
		return app.getDaynightHelper().isNightMode(usedOnMap, new RequestMapThemeParams().setAppMode(appMode));
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

	static class ActionItemViewHolder extends ViewHolder {

		public View buttonView;
		public ImageButton actionButton;
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
			summary = summaryContainer.findViewById(R.id.description);
		}
	}

}
