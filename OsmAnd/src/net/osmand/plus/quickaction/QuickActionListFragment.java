package net.osmand.plus.quickaction;

import static net.osmand.plus.utils.UiUtilities.CompoundButtonType.TOOLBAR;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.quickaction.ConfirmationBottomSheet.OnConfirmButtonClickListener;
import net.osmand.plus.quickaction.QuickActionRegistry.QuickActionUpdatesListener;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback.OnItemMoveCallback;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback.UnmovableItem;
import net.osmand.plus.views.layers.MapQuickActionLayer;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Created by okorsun on 20.12.16.
 */

public class QuickActionListFragment extends BaseOsmAndFragment implements QuickActionUpdatesListener,
		OnConfirmButtonClickListener {

	public static final String TAG = QuickActionListFragment.class.getSimpleName();

	public static final String ACTIONS_TO_DELETE_KEY = "actions_to_delete";
	public static final String SCREEN_TYPE_KEY = "screen_type";

	private static final int SCREEN_TYPE_REORDER = 0;
	private static final int SCREEN_TYPE_DELETE = 1;

	private static final int ITEMS_IN_GROUP = 6;

	private FloatingActionButton fab;
	private View bottomPanel;
	private Toolbar toolbar;
	private View toolbarSwitchContainer;
	private ImageView navigationIcon;
	private View deleteIconContainer;

	private QuickActionAdapter adapter;
	private ItemTouchHelper touchHelper;
	private QuickActionRegistry quickActionRegistry;
	private final ArrayList<Long> actionsToDelete = new ArrayList<>();
	private int screenType = SCREEN_TYPE_REORDER;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			long[] array = savedInstanceState.getLongArray(ACTIONS_TO_DELETE_KEY);
			if (array != null) {
				for (long id : array) {
					actionsToDelete.add(id);
				}
			}
			screenType = savedInstanceState.getInt(SCREEN_TYPE_KEY, SCREEN_TYPE_REORDER);
		}
		requireMyActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			public void handleOnBackPressed() {
				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null) {
					OsmandMapTileView mapView = mapActivity.getMapView();
					MapQuickActionLayer layer = mapView.getLayerByClass(MapQuickActionLayer.class);
					if (isVisible()) {
						layer.onBackPressed();
					} else if (layer.onBackPressed()) {
						return;
					}
					FragmentManager fragmentManager = mapActivity.getSupportFragmentManager();
					if (!fragmentManager.isStateSaved()) {
						fragmentManager.popBackStackImmediate();
					}
				}
			}
		});
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = themedInflater.inflate(R.layout.quick_action_list, container, false);

		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		fab = view.findViewById(R.id.fabButton);
		fab.setOnClickListener(v -> showAddQuickActionDialog());

		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);

		bottomPanel = view.findViewById(R.id.bottom_panel);
		View btnSelectAll = bottomPanel.findViewById(R.id.select_all);
		View btnDelete = bottomPanel.findViewById(R.id.delete);

		btnSelectAll.setOnClickListener(view1 -> {
			actionsToDelete.clear();
			for (QuickAction action : adapter.getQuickActions()) {
				actionsToDelete.add(action.id);
			}
			updateListItems();
			updateToolbarTitle();
		});

		btnDelete.setOnClickListener(view12 -> showConfirmDeleteActionsBottomSheet(getMapActivity()));
		UiUtilities.setupDialogButton(nightMode, btnDelete,
				DialogButtonType.SECONDARY, R.string.shared_string_delete);

		quickActionRegistry = app.getQuickActionRegistry();

		toolbar = view.findViewById(R.id.toolbar);
		navigationIcon = toolbar.findViewById(R.id.close_button);
		deleteIconContainer = toolbar.findViewById(R.id.action_button);
		toolbarSwitchContainer = toolbar.findViewById(R.id.toolbar_switch_container);
		setUpToolbar();

		adapter = new QuickActionAdapter(viewHolder -> touchHelper.startDrag(viewHolder));
		recyclerView.setAdapter(adapter);
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

		ItemTouchHelper.Callback touchHelperCallback = new ReorderItemTouchHelperCallback(adapter);
		touchHelper = new ItemTouchHelper(touchHelperCallback);
		touchHelper.attachToRecyclerView(recyclerView);

		recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
				if (screenType == SCREEN_TYPE_REORDER) {
					if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
						fab.hide();
					} else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
						fab.show();
					}
				}
			}
		});
		updateListItems();
		updateVisibility();
		return view;
	}

	private void setUpToolbar() {
		TextView tvTitle = toolbar.findViewById(R.id.toolbar_title);
		tvTitle.setTextColor(ColorUtilities.getActiveButtonsAndLinksTextColor(app, nightMode));
		boolean isWidgetVisibleOnMap = app.getQuickActionRegistry().isQuickActionOn();
		updateToolbarTitle();
		updateToolbarNavigationIcon();
		updateToolbarActionButton();
		updateToolbarSwitch(isWidgetVisibleOnMap);
	}

	private void updateToolbarNavigationIcon() {
		int activeButtonsColorResId = ColorUtilities.getActiveButtonsAndLinksTextColorId(nightMode);
		if (screenType == SCREEN_TYPE_REORDER) {
			Drawable icBack = app.getUIUtilities().getIcon(
					AndroidUtils.getNavigationIconResId(app),
					activeButtonsColorResId);
			navigationIcon.setImageDrawable(icBack);
			navigationIcon.setOnClickListener(view -> {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					activity.onBackPressed();
				}
			});
		} else if (screenType == SCREEN_TYPE_DELETE) {
			Drawable icClose = getIcon(R.drawable.ic_action_close, activeButtonsColorResId);
			navigationIcon.setImageDrawable(icClose);
			navigationIcon.setOnClickListener(view -> changeScreenType(SCREEN_TYPE_REORDER));
		}
	}

	private void updateListItems() {
		MapActivity ma = getMapActivity();
		OsmandApplication app = ma.getMyApplication();
		List<QuickAction> actions = quickActionRegistry.getQuickActions();

		updateToolbarActionButton();
		List<ListItem> items = new ArrayList<>();
		if (actions.size() > 0) {
			items.add(new ListItem(ItemType.LIST_DIVIDER));
			int screen = 0;
			for (int i = 0; i < actions.size(); i++) {
				if (i % ITEMS_IN_GROUP == 0) {
					items.add(new ListItem(ItemType.HEADER, ++screen));
				}
				items.add(new ListItem(ItemType.ACTION, actions.get(i)));
			}
		}
		if (screenType == SCREEN_TYPE_REORDER) {
			items.add(new ListItem(ItemType.LIST_DIVIDER));
			String promo = app.getString(R.string.export_import_quick_actions_with_profiles_promo);
			items.add(new ListItem(ItemType.DESCRIPTION, promo));
			items.add(new ListItem(ItemType.BUTTON,
					new ControlButton(getString(R.string.quick_action_new_action),
							R.drawable.ic_action_plus,
							view -> showAddQuickActionDialog())));
			items.add(new ListItem(ItemType.BUTTON,
					new ControlButton(getString(R.string.shared_string_delete_all),
							R.drawable.ic_action_delete_dark,
							view -> {
								actionsToDelete.clear();
								for (ListItem item : adapter.items) {
									if (item.type == ItemType.ACTION) {
										QuickAction action = (QuickAction) item.value;
										actionsToDelete.add(action.id);
									}
								}
								showConfirmDeleteActionsBottomSheet(ma);
							})));
		}
		items.add(new ListItem(ItemType.BOTTOM_SHADOW));
		adapter.setItems(items);
	}

	private void updateVisibility() {
		if (screenType == SCREEN_TYPE_REORDER) {
			fab.setVisibility(View.VISIBLE);
			bottomPanel.setVisibility(View.GONE);
			toolbarSwitchContainer.setVisibility(View.VISIBLE);
			deleteIconContainer.setVisibility(View.VISIBLE);
		} else if (screenType == SCREEN_TYPE_DELETE) {
			fab.setVisibility(View.GONE);
			bottomPanel.setVisibility(View.VISIBLE);
			toolbarSwitchContainer.setVisibility(View.GONE);
			deleteIconContainer.setVisibility(View.GONE);
		}
	}

	private void changeScreenType(int screenType) {
		this.screenType = screenType;
		actionsToDelete.clear();
		updateToolbarTitle();
		updateToolbarNavigationIcon();
		updateListItems();
		updateVisibility();
	}

	private void updateToolbarActionButton() {
		View deleteIconContainer = toolbar.findViewById(R.id.action_button);
		ImageView deletingModeIcon = toolbar.findViewById(R.id.action_button_icon);
		int activeButtonsColorResId = ColorUtilities.getActiveButtonsAndLinksTextColorId(nightMode);
		boolean hasActiveQuickActions = quickActionRegistry.getQuickActions().size() > 0;
		int activeColor = ContextCompat.getColor(app, activeButtonsColorResId);
		int deleteIconColor = hasActiveQuickActions ? activeColor :
				ColorUtilities.getColorWithAlpha(activeColor, 0.25f);
		Drawable deleteIcon = getPaintedContentIcon(
				R.drawable.ic_action_delete_dark, deleteIconColor);
		deletingModeIcon.setImageDrawable(deleteIcon);
		deleteIconContainer.setOnClickListener(view -> changeScreenType(SCREEN_TYPE_DELETE));
		deleteIconContainer.setEnabled(hasActiveQuickActions);
	}

	private void updateToolbarTitle() {
		if (toolbar != null) {
			TextView tvTitle = toolbar.findViewById(R.id.toolbar_title);
			if (screenType == SCREEN_TYPE_REORDER) {
				tvTitle.setText(getString(R.string.configure_screen_quick_action));
			} else if (screenType == SCREEN_TYPE_DELETE) {
				int selectedCount = actionsToDelete.size();
				String title = String.format(
						getString(R.string.ltr_or_rtl_combine_via_colon),
						getString(R.string.shared_string_selected),
						selectedCount);
				tvTitle.setText(title);
			}
		}
	}

	private void updateToolbarSwitch(boolean isChecked) {
		ApplicationMode appMode = app.getSettings().getApplicationMode();
		int color = isChecked ? appMode.getProfileColor(nightMode) : ContextCompat.getColor(app, R.color.preference_top_switch_off);
		AndroidUtils.setBackground(toolbarSwitchContainer, new ColorDrawable(color));

		SwitchCompat switchView = toolbarSwitchContainer.findViewById(R.id.switchWidget);
		switchView.setChecked(isChecked);
		UiUtilities.setupCompoundButton(switchView, nightMode, TOOLBAR);

		toolbarSwitchContainer.setOnClickListener(view -> {
			boolean visible = !isChecked;
			updateToolbarSwitch(visible);
			setWidgetVisibilityOnMap(visible);
		});

		TextView title = toolbarSwitchContainer.findViewById(R.id.switchButtonText);
		title.setText(isChecked ? R.string.shared_string_enabled : R.string.shared_string_disabled);
	}

	private void setWidgetVisibilityOnMap(boolean visible) {
		app.getQuickActionRegistry().setQuickActionFabState(visible);

		MapActivity activity = getMapActivity();
		if (activity != null) {
			MapQuickActionLayer mil = activity.getMapLayers().getMapQuickActionLayer();
			if (mil != null) {
				mil.refreshLayer();
			}
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		getMapActivity().disableDrawer();
		quickActionRegistry.addUpdatesListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		getMapActivity().enableDrawer();
		quickActionRegistry.removeUpdatesListener(this);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		long[] array = new long[actionsToDelete.size()];
		for (int i = 0; i < actionsToDelete.size(); i++) {
			array[i] = actionsToDelete.get(i);
		}
		outState.putLongArray(ACTIONS_TO_DELETE_KEY, array);
		outState.putInt(SCREEN_TYPE_KEY, screenType);
	}

	@Override
	public int getStatusBarColorId() {
		return ColorUtilities.getStatusBarColorId(nightMode);
	}

	private MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}

	private void saveQuickActions() {
		quickActionRegistry.updateQuickActions(adapter.getQuickActions());
	}

	@Override
	public void onActionsUpdated() {
		updateListItems();
	}

	@Override
	public void onConfirmButtonClick() {
		if (adapter != null) {
			adapter.deleteItems(actionsToDelete);
			actionsToDelete.clear();
			if (screenType == SCREEN_TYPE_DELETE) {
				changeScreenType(SCREEN_TYPE_REORDER);
			} else if (screenType == SCREEN_TYPE_REORDER) {
				updateToolbarActionButton();
			}
		}
	}

	private class QuickActionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements OnItemMoveCallback {

		private List<ListItem> items = new ArrayList<>();
		private final OnStartDragListener onStartDragListener;

		public QuickActionAdapter(OnStartDragListener onStartDragListener) {
			this.onStartDragListener = onStartDragListener;
		}

		@NotNull
		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			Context themedCtx = UiUtilities.getThemedContext(parent.getContext(), nightMode);
			LayoutInflater inflater = UiUtilities.getInflater(themedCtx, nightMode);
			int listBgColor = ContextCompat.getColor(themedCtx,
					AndroidUtils.resolveAttribute(themedCtx, R.attr.bg_color));
			ItemType type = viewType < ItemType.values().length ? ItemType.values()[viewType] : ItemType.LIST_DIVIDER;
			View itemView;
			switch (type) {
				case ACTION:
					itemView = inflater.inflate(R.layout.quick_action_list_item, parent, false);
					return new QuickActionVH(itemView);
				case HEADER:
					itemView = inflater.inflate(R.layout.quick_action_list_header, parent, false);
					return new QuickActionHeaderVH(itemView);
				case DESCRIPTION:
					itemView = inflater.inflate(R.layout.bottom_sheet_item_description_with_padding, parent, false);
					itemView.setBackgroundColor(listBgColor);
					return new DescriptionVH(itemView);
				case LIST_DIVIDER:
					itemView = inflater.inflate(R.layout.list_item_divider, parent, false);
					return new ListDividerVH(itemView);
				case BUTTON:
					itemView = inflater.inflate(R.layout.preference_button, parent, false);
					FrameLayout frame = new FrameLayout(themedCtx);
					frame.setLayoutParams(new ViewGroup.LayoutParams(
							ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
					frame.addView(itemView);
					frame.setBackgroundColor(listBgColor);
					return new ButtonVH(frame);
				case BOTTOM_SHADOW:
					itemView = inflater.inflate(R.layout.card_bottom_divider, parent, false);
					int spaceHeight = getResources()
							.getDimensionPixelSize(R.dimen.bottom_sheet_big_item_height);
					itemView.setMinimumHeight(spaceHeight);
					return new BottomShadowVH(itemView);
				default:
					throw new IllegalArgumentException("Unsupported view type");
			}
		}

		@Override
		public void onBindViewHolder(@NotNull RecyclerView.ViewHolder holder, int position) {
			ListItem item = items.get(position);
			int activeColorResId = ColorUtilities.getActiveColorId(nightMode);

			if (holder instanceof QuickActionVH) {
				QuickActionVH h = (QuickActionVH) holder;
				QuickAction action = (QuickAction) item.value;

				if (screenType == SCREEN_TYPE_REORDER) {
					h.moveButton.setVisibility(View.VISIBLE);
					h.deleteIcon.setVisibility(View.VISIBLE);
					h.checkbox.setVisibility(View.GONE);

					h.moveButton.setOnTouchListener((v, event) -> {
						if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
							onStartDragListener.onStartDrag(h);
						}
						return false;
					});

					h.deleteBtn.setOnClickListener(v -> {
						actionsToDelete.clear();
						actionsToDelete.add(action.id);
						showConfirmDeleteAnActionBottomSheet(getActivity(),
								QuickActionListFragment.this, action, nightMode);
					});

					h.deleteBtn.setClickable(true);
					h.deleteBtn.setFocusable(true);
					h.itemContainer.setOnClickListener(view -> {
						FragmentManager manager = getFragmentManager();
						if (manager != null) {
							CreateEditActionDialog.showInstance(manager, action);
						}
					});
				} else if (screenType == SCREEN_TYPE_DELETE) {
					h.moveButton.setVisibility(View.GONE);
					h.deleteIcon.setVisibility(View.GONE);
					h.checkbox.setVisibility(View.VISIBLE);

					h.checkbox.setClickable(false);

					h.deleteBtn.setClickable(false);
					h.deleteBtn.setFocusable(false);
					h.itemContainer.setOnClickListener(view -> {
						boolean isChecked = actionsToDelete.contains(action.id);
						h.checkbox.setChecked(!isChecked);
						if (!isChecked) {
							actionsToDelete.add(action.id);
						} else {
							actionsToDelete.remove(action.id);
						}
						updateToolbarTitle();
					});
					h.checkbox.setChecked(actionsToDelete.contains(action.id));
				}

				List<QuickAction> actions = getQuickActions();
				int actionGlobalPosition = actions.indexOf(action);
				int actionPosition = actionGlobalPosition % ITEMS_IN_GROUP + 1;
				String name = action.getName(app);
				if (action.getActionNameRes() != 0 && !name.contains(getString(action.getActionNameRes()))) {
					String prefAction = getString(action.getActionNameRes());
					h.title.setText(getString(R.string.ltr_or_rtl_combine_via_dash, prefAction, action.getName(app)));
				} else {
					h.title.setText(name);
				}
				h.subTitle.setText(getResources().getString(R.string.quick_action_item_action, actionPosition));
				h.icon.setImageDrawable(getContentIcon(action.getIconRes(app)));

				boolean lastActionInList = actionGlobalPosition == actions.size() - 1;
				boolean lastOnScreen = actionPosition == ITEMS_IN_GROUP;
				if (!lastActionInList) {
					if (lastOnScreen) {
						h.itemDivider.setVisibility(View.GONE);
						h.longDivider.setVisibility(View.VISIBLE);
					} else {
						h.itemDivider.setVisibility(View.VISIBLE);
						h.longDivider.setVisibility(View.GONE);
					}
				} else {
					h.itemDivider.setVisibility(View.GONE);
					h.longDivider.setVisibility(View.GONE);
				}
			} else if (holder instanceof QuickActionHeaderVH) {
				QuickActionHeaderVH h = (QuickActionHeaderVH) holder;
				String title = String.format(
						getResources().getString(R.string.quick_action_item_screen),
						position / (ITEMS_IN_GROUP + 1) + 1);
				h.headerName.setText(title);
			} else if (holder instanceof ButtonVH) {
				ControlButton buttonInfo = (ControlButton) item.value;
				ButtonVH h = (ButtonVH) holder;
				h.container.setOnClickListener(buttonInfo.listener);
				h.title.setText(buttonInfo.title);
				int iconColor = ContextCompat.getColor(app, activeColorResId);
				int titleColor = ContextCompat.getColor(app, activeColorResId);
				if (buttonInfo.iconRes == R.drawable.ic_action_delete_dark) {
					boolean hasActiveActions = getQuickActions().size() > 0;
					h.container.setEnabled(hasActiveActions);
					int deleteAllIconColor = ContextCompat.getColor(app,
							R.color.color_osm_edit_delete);
					iconColor = hasActiveActions ? deleteAllIconColor :
							ColorUtilities.getColorWithAlpha(deleteAllIconColor, 0.25f);
					titleColor = hasActiveActions ? titleColor :
							ColorUtilities.getColorWithAlpha(titleColor, 0.25f);
					h.divider.setVisibility(View.GONE);
				} else {
					h.container.setEnabled(true);
					h.divider.setVisibility(View.VISIBLE);
				}
				h.icon.setImageDrawable(getPaintedContentIcon(buttonInfo.iconRes, iconColor));
				h.title.setTextColor(titleColor);
				Drawable background = UiUtilities.getColoredSelectableDrawable(app,
						ContextCompat.getColor(app, activeColorResId), 0.3f);
				View selectableView = h.container.findViewById(R.id.selectable_list_item);
				AndroidUtils.setBackground(selectableView, background);
			} else if (holder instanceof DescriptionVH) {
				String description = (String) item.value;
				((DescriptionVH) holder).tvDescription.setText(description);
			}
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

		public void deleteItems(List<Long> actionsToDelete) {
			if (actionsToDelete != null && actionsToDelete.size() > 0) {
				Iterator<ListItem> it = items.iterator();
				while (it.hasNext()) {
					ListItem item = it.next();
					if (item.type == ItemType.ACTION) {
						for (Long actionId : actionsToDelete) {
							QuickAction qa = (QuickAction) item.value;
							if (actionId != null) {
								if (actionId.equals(qa.getId())) {
									it.remove();
									break;
								}
							}
						}
					}
				}
				saveQuickActions();
				updateListItems();
			}
		}

		public List<QuickAction> getQuickActions() {
			List<QuickAction> result = new ArrayList<>();
			for (ListItem item : items) {
				if (item.type == ItemType.ACTION) {
					result.add((QuickAction) item.value);
				}
			}

			return result;
		}

		@Override
		public boolean onItemMove(int selectedPosition, int targetPosition) {
			Log.v(TAG, "selected: " + selectedPosition + ", target: " + targetPosition);

			Collections.swap(items, selectedPosition, targetPosition);
			if (selectedPosition - targetPosition < -1) {
				notifyItemMoved(selectedPosition, targetPosition);
				notifyItemMoved(targetPosition - 1, selectedPosition);
			} else if (selectedPosition - targetPosition > 1) {
				notifyItemMoved(selectedPosition, targetPosition);
				notifyItemMoved(targetPosition + 1, selectedPosition);
			} else {
				notifyItemMoved(selectedPosition, targetPosition);
			}
			notifyItemChanged(selectedPosition);
			notifyItemChanged(targetPosition);
			return true;
		}

		@Override
		public void onItemDismiss(@NonNull ViewHolder holder) {
			saveQuickActions();
		}

		public void setItems(List<ListItem> items) {
			this.items = items;
			notifyDataSetChanged();
		}

		private class QuickActionVH extends RecyclerView.ViewHolder {
			public TextView title;
			public TextView subTitle;
			public ImageView icon;
			public View itemDivider;
			public View longDivider;
			public View moveButton;
			public ImageView moveIcon;
			public View deleteBtn;
			public ImageView deleteIcon;
			public CompoundButton checkbox;
			public View itemContainer;

			public QuickActionVH(View itemView) {
				super(itemView);
				title = itemView.findViewById(R.id.title);
				subTitle = itemView.findViewById(R.id.subtitle);
				icon = itemView.findViewById(R.id.imageView);
				itemDivider = itemView.findViewById(R.id.item_divider);
				longDivider = itemView.findViewById(R.id.long_divider);
				deleteBtn = itemView.findViewById(R.id.action_button);
				deleteIcon = itemView.findViewById(R.id.action_icon);
				moveButton = itemView.findViewById(R.id.move_button);
				moveIcon = itemView.findViewById(R.id.move_icon);
				checkbox = itemView.findViewById(R.id.checkbox);
				itemContainer = itemView.findViewById(R.id.searchListItemLayout);

				deleteIcon.setImageDrawable(app.getUIUtilities()
						.getIcon(R.drawable.ic_action_delete_item, R.color.color_osm_edit_delete));
				moveIcon.setImageDrawable(app.getUIUtilities()
						.getThemedIcon(R.drawable.ic_action_item_move));
				UiUtilities.setupCompoundButton(checkbox, nightMode,
						UiUtilities.CompoundButtonType.GLOBAL);
			}
		}

		private class QuickActionHeaderVH extends RecyclerView.ViewHolder implements UnmovableItem {

			public TextView headerName;

			public QuickActionHeaderVH(View itemView) {
				super(itemView);
				headerName = itemView.findViewById(R.id.header);
			}

			@Override
			public boolean isMovingDisabled() {
				return true;
			}
		}

		private class DescriptionVH extends RecyclerView.ViewHolder implements UnmovableItem {

			private final TextView tvDescription;

			public DescriptionVH(View itemView) {
				super(itemView);
				tvDescription = itemView.findViewById(R.id.description);
			}

			@Override
			public boolean isMovingDisabled() {
				return true;
			}
		}

		private class ListDividerVH extends RecyclerView.ViewHolder implements UnmovableItem {

			public ListDividerVH(View itemView) {
				super(itemView);
			}

			@Override
			public boolean isMovingDisabled() {
				return true;
			}
		}

		private class BottomShadowVH extends RecyclerView.ViewHolder implements UnmovableItem {

			public BottomShadowVH(View itemView) {
				super(itemView);
			}

			@Override
			public boolean isMovingDisabled() {
				return true;
			}
		}

		private class ButtonVH extends RecyclerView.ViewHolder implements UnmovableItem {

			private final View container;
			private final ImageView icon;
			private final TextView title;
			private final View divider;

			public ButtonVH(View itemView) {
				super(itemView);
				container = itemView;
				icon = itemView.findViewById(android.R.id.icon);
				title = itemView.findViewById(android.R.id.title);
				divider = itemView.findViewById(R.id.divider);
			}

			@Override
			public boolean isMovingDisabled() {
				return true;
			}
		}
	}

	protected enum ItemType {
		ACTION,
		HEADER,
		DESCRIPTION,
		LIST_DIVIDER,
		BOTTOM_SHADOW,
		BUTTON
	}

	private static class ListItem {
		ItemType type;
		Object value;

		public ListItem(ItemType type) {
			this.type = type;
		}

		public ListItem(ItemType type, Object value) {
			this.type = type;
			this.value = value;
		}
	}

	protected static class ControlButton {
		private final String title;
		private final int iconRes;
		private final View.OnClickListener listener;

		public ControlButton(String title, int iconRes, View.OnClickListener listener) {
			this.title = title;
			this.iconRes = iconRes;
			this.listener = listener;
		}
	}

	public interface OnStartDragListener {
		void onStartDrag(RecyclerView.ViewHolder viewHolder);
	}

	private void showAddQuickActionDialog() {
		FragmentManager manager = getFragmentManager();
		if (manager != null) {
			AddQuickActionDialog.showInstance(manager, false);
		}
	}

	private void showConfirmDeleteActionsBottomSheet(MapActivity ma) {
		String message = String.format(
				getString(R.string.delete_all_actions_message_q),
				actionsToDelete.size());
		ConfirmationBottomSheet.showInstance(ma.getSupportFragmentManager(),
				this,
				getString(R.string.shared_string_delete_all_q),
				message, R.string.shared_string_delete, false);
	}

	static void showConfirmDeleteAnActionBottomSheet(FragmentActivity ctx, Fragment target,
	                                                 QuickAction action, boolean usedOnMap) {
		String actionName = action.getName(ctx);
		String message = String.format(ctx.getString(
				R.string.quick_actions_delete_text), actionName);
		SpannableString styledMessage = UiUtilities.createSpannableString(
				message, Typeface.BOLD, actionName);

		ConfirmationBottomSheet.showInstance(ctx.getSupportFragmentManager(), target,
				ctx.getString(R.string.quick_actions_delete), styledMessage,
				R.string.shared_string_delete, usedOnMap);
	}

	public static void showInstance(@NonNull FragmentActivity activity) {
		FragmentManager manager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			QuickActionListFragment fragment = new QuickActionListFragment();
			manager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}