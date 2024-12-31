package net.osmand.plus.keyevent.fragments.keyassignments;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static net.osmand.plus.keyevent.KeySymbolMapper.getKeySymbol;
import static net.osmand.plus.utils.AndroidUtils.setBackground;
import static net.osmand.plus.utils.UiUtilities.getColoredSelectableDrawable;

import android.content.Context;
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
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.containers.ScreenItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.keyevent.assignment.KeyAssignment;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButton;

import java.util.ArrayList;
import java.util.List;

class KeyAssignmentsAdapter extends RecyclerView.Adapter<ViewHolder> {

	static final int EMPTY_STATE = 1;
	static final int CARD_TOP_DIVIDER = 2;
	static final int CARD_DIVIDER = 3;
	static final int HEADER = 4;
	static final int KEY_ASSIGNMENT_ITEM = 5;
	static final int EDIT_KEY_ASSIGNMENT_ITEM = 6;
	static final int CARD_BOTTOM_SHADOW = 7;
	static final int SPACE = 8;

	private final OsmandApplication app;
	private final ApplicationMode appMode;
	private ViewGroup parent;
	private Context context;

	private List<ScreenItem> screenItems = new ArrayList<>();
	private boolean editable;
	private final KeyAssignmentsController controller;

	public KeyAssignmentsAdapter(@NonNull OsmandApplication app, @NonNull ApplicationMode appMode,
	                             @NonNull KeyAssignmentsController controller) {
		this.app = app;
		this.appMode = appMode;
		this.controller = controller;
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		this.parent = parent;
		context = parent.getContext();
		switch (viewType) {
			case EMPTY_STATE:
				return new EmptyStateViewHolder(inflate(R.layout.card_empty_assignments_banner));
			case CARD_TOP_DIVIDER:
				return new CardTopDividerViewHolder(inflate(R.layout.list_item_divider));
			case CARD_DIVIDER:
				return new CardDividerViewHolder(inflate(R.layout.list_item_divider));
			case HEADER:
				return new HeaderViewHolder(inflate(R.layout.list_item_header_48dp));
			case KEY_ASSIGNMENT_ITEM:
			case EDIT_KEY_ASSIGNMENT_ITEM:
				return new ActionItemViewHolder(inflate(R.layout.list_item_external_input_device_key_assignment_item));
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
		boolean editMode = controller.isInEditMode();
		ScreenItem item = screenItems.get(position);
		if (holder instanceof EmptyStateViewHolder) {
			EmptyStateViewHolder h = (EmptyStateViewHolder) holder;
			h.btnAdd.setOnClickListener(v -> controller.askAddAssignment(h.btnAdd));

		} else if (holder instanceof HeaderViewHolder) {
			HeaderViewHolder h = (HeaderViewHolder) holder;
			h.title.setText(R.string.shared_string_action);
			h.summary.setText(R.string.shared_string_key);

		} else if (holder instanceof ActionItemViewHolder) {
			ActionItemViewHolder h = (ActionItemViewHolder) holder;
			KeyAssignment assignment = (KeyAssignment) item.getValue();

			if (isEditable() && !editMode) {
				boolean nightMode = controller.isNightMode();
				int color = appMode.getProfileColor(nightMode);
				setupSelectableBackground(h.buttonView, color);
			}
			h.icon.setImageResource(assignment.getIconId(context));
			h.icon.setVisibility(View.VISIBLE);
			h.actionButton.setVisibility(editMode ? View.VISIBLE : View.GONE);
			h.actionButton.setOnClickListener(v -> {
				if (editMode) {
					controller.askRemoveAssignment(assignment);
				}
			});
			h.extraIconPadding.setVisibility(editMode ? View.GONE : View.VISIBLE);

			h.buttonView.setClickable(isEditable());
			h.buttonView.setFocusable(isEditable());
			h.buttonView.setOnClickListener(isEditable()? v -> {
				if (!editMode) {
					controller.askEditAssignment(assignment, v);
				}
			}: null);
			h.buttonView.setTransitionName("transition_" + assignment.getId());
			h.actionName.setText(assignment.getName(app));

			h.assignedKeys.removeAllViews();
			List<Integer> keyCodes = assignment.getKeyCodes();
			int keyCodesCount = keyCodes.size();
			int visibleKeyCodesCount = 0;
			int visibleKeyCodesLimit = editMode ? 2 : 3;
			for (Integer keyCode : keyCodes) {
				h.assignedKeys.addView(createKeycodeView(keyCode));
				visibleKeyCodesCount++;
				if (visibleKeyCodesCount < keyCodesCount) {
					h.assignedKeys.addView(inflate(R.layout.item_key_assignment_element_space));
					if (visibleKeyCodesCount == visibleKeyCodesLimit) {
						h.assignedKeys.addView(inflate(R.layout.item_key_assignment_element_more));
						break;
					}
				}
			}

			ScreenItem nextItem = position < screenItems.size() - 1 ? screenItems.get(position + 1) : null;
			int nextItemType = nextItem != null ? nextItem.getType() : -1;
			boolean dividerNeeded = nextItemType == KEY_ASSIGNMENT_ITEM || nextItemType == EDIT_KEY_ASSIGNMENT_ITEM;
			AndroidUiHelper.updateVisibility(h.divider, dividerNeeded);
		}
	}

	@NonNull
	private View createKeycodeView(@NonNull Integer keyCode) {
		View view = inflate(R.layout.item_key_assignment_button_small);
		TextView title = view.findViewById(R.id.description);
		title.setText(getKeySymbol(app, keyCode));
		return view;
	}

	public void setScreenData(@NonNull List<ScreenItem> screenItems, boolean isDeviceTypeEditable) {
		this.screenItems = screenItems;
		this.editable = isDeviceTypeEditable;
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

	private View inflate(@LayoutRes int layoutResId) {
		LayoutInflater inflater = UiUtilities.getInflater(context, controller.isNightMode());
		return inflater.inflate(layoutResId, parent, false);
	}

	private void setupSelectableBackground(@NonNull View view, @ColorInt int color) {
		setBackground(view, getColoredSelectableDrawable(view.getContext(), color, 0.3f));
	}

	private boolean isEditable() {
		return editable;
	}

	private int getDimen(@DimenRes int resId) {
		return app.getResources().getDimensionPixelSize(resId);
	}

	static class EmptyStateViewHolder extends ViewHolder {

		public DialogButton btnAdd;

		public EmptyStateViewHolder(@NonNull View itemView) {
			super(itemView);
			btnAdd = itemView.findViewById(R.id.add_button);
		}
	}

	static class CardBottomShadowViewHolder extends ViewHolder {
		public CardBottomShadowViewHolder(@NonNull View itemView) {
			super(itemView);
		}
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

	static class CardDividerViewHolder extends ViewHolder {
		public CardDividerViewHolder(@NonNull View itemView) {
			super(itemView);
		}
	}

	static class HeaderViewHolder extends RecyclerView.ViewHolder {

		public TextView title;
		public TextView summary;

		public HeaderViewHolder(@NonNull View itemView) {
			super(itemView);
			title = itemView.findViewById(R.id.title);
			summary = itemView.findViewById(R.id.count);
			AndroidUiHelper.updateVisibility(summary, true);
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
		public View actionButton;
		public View extraIconPadding;
		public ImageView icon;
		public TextView actionName;
		public ViewGroup assignedKeys;
		public View divider;

		public ActionItemViewHolder(@NonNull View itemView) {
			super(itemView);
			buttonView = itemView.findViewById(R.id.selectable_list_item);
			actionButton = itemView.findViewById(R.id.action_button);
			extraIconPadding = itemView.findViewById(R.id.extra_space);
			icon = itemView.findViewById(R.id.icon);
			actionName = itemView.findViewById(R.id.title);
			assignedKeys = itemView.findViewById(R.id.assigned_keys);
			divider = itemView.findViewById(R.id.bottom_divider);
		}
	}
}
