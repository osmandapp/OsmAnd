package net.osmand.plus.settings;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivityActions;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.dialogs.ConfigureMapMenu;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.profiles.SelectCopyAppModeBottomSheet;
import net.osmand.plus.settings.ConfigureMenuRootFragment.ScreenType;
import net.osmand.plus.settings.bottomsheets.ChangeGeneralProfilesPrefBottomSheet;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback;
import net.osmand.plus.settings.RearrangeMenuItemsAdapter.AdapterItem;
import net.osmand.plus.settings.RearrangeMenuItemsAdapter.MenuItemsAdapterListener;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_MORE_ID;
import static net.osmand.plus.OsmandSettings.ContextMenuItemsPreference.HIDDEN;
import static net.osmand.plus.OsmandSettings.ContextMenuItemsPreference.ORDER;
import static net.osmand.plus.settings.RearrangeMenuItemsAdapter.AdapterItemType.BUTTON;
import static net.osmand.plus.settings.RearrangeMenuItemsAdapter.AdapterItemType.DESCRIPTION;
import static net.osmand.plus.settings.RearrangeMenuItemsAdapter.AdapterItemType.DIVIDER;
import static net.osmand.plus.settings.RearrangeMenuItemsAdapter.AdapterItemType.HEADER;
import static net.osmand.plus.settings.RearrangeMenuItemsAdapter.AdapterItemType.MENU_ITEM;

public class ConfigureMenuItemsFragment extends BaseOsmAndFragment
		implements SelectCopyAppModeBottomSheet.CopyAppModePrefsListener {

	public static final String TAG = ConfigureMenuItemsFragment.class.getName();
	private static final Log LOG = PlatformUtil.getLog(ConfigureMenuItemsFragment.class.getName());
	private static final String ITEM_TYPE_KEY = "item_type_key";
	private static final String ITEMS_ORDER_KEY = "items_order_key";
	private static final String HIDDEN_ITEMS_KEY = "hidden_items_key";
	private static final String CONFIGURE_MENU_ITEMS_TAG = "configure_menu_items_tag";
	private RearrangeMenuItemsAdapter rearrangeAdapter;
	private HashMap<String, Integer> menuItemsOrder;
	private ContextMenuAdapter contextMenuAdapter;
	private List<String> hiddenMenuItems;
	private ApplicationMode appMode;
	private LayoutInflater mInflater;
	private OsmandApplication app;
	private ScreenType screenType;
	private boolean nightMode;
	private boolean wasReset = false;
	private boolean isChanged = false;

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putStringArrayList(HIDDEN_ITEMS_KEY, new ArrayList<>(hiddenMenuItems));
		outState.putSerializable(ITEMS_ORDER_KEY, menuItemsOrder);
		outState.putSerializable(ITEM_TYPE_KEY, screenType);
	}

	public static ConfigureMenuItemsFragment showInstance(@NonNull FragmentManager fm, @NonNull ScreenType type) {
		ConfigureMenuItemsFragment fragment = new ConfigureMenuItemsFragment();
		fragment.setScreenType(type);
		fm.beginTransaction()
				.replace(R.id.fragmentContainer, fragment, TAG)
				.addToBackStack(CONFIGURE_MENU_ITEMS_TAG)
				.commitAllowingStateLoss();
		return fragment;
	}


	@Override
	public int getStatusBarColorId() {
		View view = getView();
		if (view != null && Build.VERSION.SDK_INT >= 23 && !nightMode) {
			view.setSystemUiVisibility(view.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
		}
		return nightMode ? R.color.activity_background_dark : R.color.activity_background_light;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		appMode = app.getSettings().getApplicationMode();
		nightMode = !app.getSettings().isLightContent();
		mInflater = UiUtilities.getInflater(app, nightMode);
		instantiateContextMenuAdapter();
		if (savedInstanceState != null
				&& savedInstanceState.containsKey(ITEM_TYPE_KEY)
				&& savedInstanceState.containsKey(HIDDEN_ITEMS_KEY)
				&& savedInstanceState.containsKey(ITEMS_ORDER_KEY)) {
			screenType = (ScreenType) savedInstanceState.getSerializable(ITEM_TYPE_KEY);
			hiddenMenuItems = savedInstanceState.getStringArrayList(HIDDEN_ITEMS_KEY);
			menuItemsOrder = (HashMap<String, Integer>) savedInstanceState.getSerializable(ITEMS_ORDER_KEY);
		} else {
			hiddenMenuItems = getSettingForScreen(app, screenType).getHiddenIds();
			menuItemsOrder = new HashMap<>();
			List<String> orderIds = getSettingForScreen(app, screenType).getOrderIds();
			for (int i = 0; i < orderIds.size(); i++) {
				menuItemsOrder.put(orderIds.get(i), i);
			}
		}
	}

	private void instantiateContextMenuAdapter() {
		Activity activity = getActivity();
		if (activity instanceof MapActivity) {
			switch (screenType) {
				case DRAWER:
					MapActivityActions mapActivityActions = new MapActivityActions((MapActivity) activity);
					contextMenuAdapter = mapActivityActions.createMainOptionsMenu();
					break;
				case CONFIGURE_MAP:
					ConfigureMapMenu configureMapMenu = new ConfigureMapMenu();
					contextMenuAdapter = configureMapMenu.createListAdapter((MapActivity) activity);
					break;
				case CONTEXT_MENU_ACTIONS:
					MapContextMenu menu = ((MapActivity) activity).getContextMenu();
					contextMenuAdapter = menu.getActionsContextMenuAdapter(true);
					break;
			}
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable Bundle savedInstanceState) {
		View root = mInflater.inflate(R.layout.edit_arrangement_list_fragment, container, false);
		Toolbar toolbar = root.findViewById(R.id.toolbar);
		TextView toolbarTitle = root.findViewById(R.id.toolbar_title);
		ImageButton toolbarButton = root.findViewById(R.id.close_button);
		RecyclerView recyclerView = root.findViewById(R.id.profiles_list);
		recyclerView.setPadding(0, 0, 0, AndroidUtils.dpToPx(app, 72));
		toolbar.setBackgroundColor(nightMode
				? getResources().getColor(R.color.list_background_color_dark)
				: getResources().getColor(R.color.list_background_color_light));
		toolbarTitle.setTextColor(nightMode
				? getResources().getColor(R.color.text_color_primary_dark)
				: getResources().getColor(R.color.list_background_color_dark));
		toolbarButton.setImageDrawable(getPaintedContentIcon(R.drawable.ic_arrow_back, getResources().getColor(R.color.text_color_secondary_light)));
		toolbarTitle.setText(screenType.titleRes);
		toolbarButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				exitFragment();
			}
		});


		rearrangeAdapter = new RearrangeMenuItemsAdapter(app, getAdapterItems());
		recyclerView.setLayoutManager(new LinearLayoutManager(app));
		final ItemTouchHelper touchHelper = new ItemTouchHelper(new ReorderItemTouchHelperCallback(rearrangeAdapter));
		touchHelper.attachToRecyclerView(recyclerView);
		MenuItemsAdapterListener listener = new MenuItemsAdapterListener() {
			private int fromPosition;
			private int toPosition;

			@Override
			public void onDragStarted(RecyclerView.ViewHolder holder) {
				fromPosition = holder.getAdapterPosition();
				touchHelper.startDrag(holder);
			}

			@Override
			public void onDragOrSwipeEnded(RecyclerView.ViewHolder holder) {
				toPosition = holder.getAdapterPosition();
				if (toPosition >= 0 && fromPosition >= 0 && toPosition != fromPosition) {
					rearrangeAdapter.notifyDataSetChanged();
				}
			}

			@Override
			public void onButtonClicked(int position) {
				AdapterItem adapterItem = rearrangeAdapter.getItem(position);
				if (adapterItem.getValue() instanceof ContextMenuItem) {
					ContextMenuItem menuItemBase = (ContextMenuItem) adapterItem.getValue();
					menuItemBase.setHidden(!menuItemBase.isHidden());
					if (menuItemBase.isHidden()) {
						hiddenMenuItems.add(menuItemBase.getId());
					} else {
						hiddenMenuItems.remove(menuItemBase.getId());
					}
					wasReset = false;
					isChanged = true;
					rearrangeAdapter.updateItems(getAdapterItems());
				}
			}

			@Override
			public void onItemMoved(String id, int position) {
				menuItemsOrder.put(id, position);
				wasReset = false;
				isChanged = true;
			}
		};
		rearrangeAdapter.setListener(listener);
		recyclerView.setAdapter(rearrangeAdapter);
		View cancelButton = root.findViewById(R.id.dismiss_button);
		UiUtilities.setupDialogButton(nightMode, cancelButton, UiUtilities.DialogButtonType.SECONDARY, R.string.shared_string_cancel);
		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentActivity fragmentActivity = getActivity();
				if (fragmentActivity != null) {
					fragmentActivity.onBackPressed();
				}
			}
		});
		root.findViewById(R.id.buttons_divider).setVisibility(View.VISIBLE);
		View applyButton = root.findViewById(R.id.right_bottom_button);
		UiUtilities.setupDialogButton(nightMode, applyButton, UiUtilities.DialogButtonType.PRIMARY, R.string.shared_string_apply);
		applyButton.setVisibility(View.VISIBLE);
		applyButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				List<ContextMenuItem> defItems = contextMenuAdapter.getDefaultItems();
				sortByCustomOrder(defItems, menuItemsOrder);
				List<String> ids = new ArrayList<>();
				for (ContextMenuItem item : defItems) {
					ids.add(item.getId());
				}
				FragmentManager fm = getFragmentManager();
				String stringToSave = convertToJsonString(hiddenMenuItems, ids, getSettingForScreen(app, screenType).getId());
				if (fm != null) {
					ChangeGeneralProfilesPrefBottomSheet.showInstance(fm,
							getSettingForScreen(app, screenType).getId(),
							stringToSave,
							getTargetFragment(),
							false,
							appMode,
							new ChangeGeneralProfilesPrefBottomSheet.OnChangeSettingListener() {
								@Override
								public void onApplied() {
									dismissFragment();
								}

								@Override
								public void onDiscard() {

								}
							});
					}
			}
		});
		if (Build.VERSION.SDK_INT >= 21) {
			AndroidUtils.addStatusBarPadding21v(app, root);
		}
		return root;
	}

	private String convertToJsonString(List<String> hidden, List<String> order, String id) {
		try {
			JSONObject json = new JSONObject();
			JSONObject items = new JSONObject();
			JSONArray hiddenItems = new JSONArray();
			JSONArray orderItems = new JSONArray();
			addIdsToJsonArray(hiddenItems, hidden);
			addIdsToJsonArray(orderItems, order);
			items.put(HIDDEN, hiddenItems);
			items.put(ORDER, orderItems);
			json.put(id, items);
			return json.toString();
		} catch (JSONException e) {
			LOG.error("Error converting to json string: " + e);
		}
		return "";
	}

	private void addIdsToJsonArray(@NonNull JSONArray jsonArray, List<String> ids) {
		if (ids != null && !ids.isEmpty()) {
			for (String id : ids) {
				jsonArray.put(id);
			}
		}
	}

	private List<AdapterItem> getAdapterItems() {
		List<AdapterItem> items = new ArrayList<>();
		items.add(new AdapterItem(DESCRIPTION, screenType));

		List<AdapterItem> visible = getItemsForRearrangeAdapter(hiddenMenuItems, wasReset ? null : menuItemsOrder, false);
		List<AdapterItem> hiddenItems = getItemsForRearrangeAdapter(hiddenMenuItems, wasReset ? null : menuItemsOrder, true);
		if (screenType == ScreenType.CONTEXT_MENU_ACTIONS) {
			for (int i =0; i<visible.size(); i++){
				ContextMenuItem value = (ContextMenuItem) visible.get(i).getValue();
				if (value.getId() != null && value.getId().equals(MAP_CONTEXT_MENU_MORE_ID)){
					if (i >3){
						AdapterItem third = visible.get(3);
						visible.set(3,visible.get(i));
						menuItemsOrder.put(((ContextMenuItem) third.getValue()).getId(),i);
						menuItemsOrder.put(((ContextMenuItem) visible.get(i).getValue()).getId(),3);
					}
				}
			}

			List<AdapterItem> main = new ArrayList<>();
			int actionsIndex = Math.min(4, visible.size());
			for (int i = 0; i < actionsIndex; i++) {
				main.add(visible.get(i));
			}
			items.add(new AdapterItem(HEADER, new RearrangeMenuItemsAdapter.HeaderItem(R.string.main_actions, R.string.main_actions_descr)));
			items.addAll(main);
			items.add(new AdapterItem(HEADER, new RearrangeMenuItemsAdapter.HeaderItem(R.string.additional_actions, R.string.additional_actions_descr)));
			List<AdapterItem> additional = new ArrayList<>();
			for (int i = 4; i < visible.size(); i++) {
				additional.add(visible.get(i));
			}
			items.addAll(additional);
		} else {
			items.addAll(visible);
		}
		if (!hiddenItems.isEmpty()) {
			items.add(new AdapterItem(HEADER, new RearrangeMenuItemsAdapter.HeaderItem(R.string.shared_string_hidden, R.string.hidden_items_descr)));
			items.addAll(hiddenItems);
		}
		items.add(new AdapterItem(DIVIDER, 1));
		items.add(new AdapterItem(BUTTON, new RearrangeMenuItemsAdapter.ButtonItem(
				R.string.reset_to_default,
				R.drawable.ic_action_reset_to_default_dark,
				new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						hiddenMenuItems.clear();
						menuItemsOrder.clear();
						wasReset = true;
						isChanged = true;
						instantiateContextMenuAdapter();
						rearrangeAdapter.updateItems(getAdapterItems());
					}
				})));
		items.add(new AdapterItem(BUTTON, new RearrangeMenuItemsAdapter.ButtonItem(
				R.string.copy_from_other_profile,
				R.drawable.ic_action_copy,
				new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						FragmentManager fm = getFragmentManager();
						if (fm != null) {
							SelectCopyAppModeBottomSheet.showInstance(
									fm,
									ConfigureMenuItemsFragment.this,
									false,
									appMode
							);
						}
					}
				})));
		return items;
	}

	public void exitFragment() {
		if (isChanged) {
			showExitDialog();
		} else {
			dismissFragment();
		}
	}

	public void showExitDialog() {
		Context themedContext = UiUtilities.getThemedContext(getActivity(), nightMode);
		AlertDialog.Builder dismissDialog = new AlertDialog.Builder(themedContext);
		dismissDialog.setTitle(getString(R.string.shared_string_dismiss));
		dismissDialog.setMessage(getString(R.string.exit_without_saving));
		dismissDialog.setNegativeButton(R.string.shared_string_cancel, null);
		dismissDialog.setPositiveButton(R.string.shared_string_exit, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dismissFragment();
			}
		});
		dismissDialog.show();
	}

	private void dismissFragment() {
		FragmentManager fm = getFragmentManager();
		if (fm != null && !fm.isStateSaved()) {
			getFragmentManager().popBackStack(CONFIGURE_MENU_ITEMS_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
		}
	}

	private void setScreenType(@NonNull ScreenType screenType) {
		this.screenType = screenType;
	}

	@Override
	public void copyAppModePrefs(ApplicationMode appMode) {
		if (appMode != null) {
			List<OsmandSettings.OsmandPreference> prefs = new ArrayList<>();
			prefs.add(getSettingForScreen(app, screenType));
			app.getSettings().copyProfilePreferences(appMode, this.appMode, prefs);
			dismissFragment();
		}
	}

	public static OsmandSettings.ContextMenuItemsPreference getSettingForScreen(OsmandApplication app, ScreenType screenType) {
		switch (screenType) {
			case DRAWER:
				return app.getSettings().DRAWER_ITEMS;
			case CONFIGURE_MAP:
				return app.getSettings().CONFIGURE_MAP_ITEMS;
			case CONTEXT_MENU_ACTIONS:
				return app.getSettings().CONTEXT_MENU_ACTIONS_ITEMS;
			default:
				throw new IllegalArgumentException("Unsupported screen type");
		}
	}

	private void initDefaultOrders(@NonNull List<ContextMenuItem> items) {
		for (int i = 0; i < items.size(); i++) {
			items.get(i).setOrder(i);
		}
	}

	public List<AdapterItem> getItemsForRearrangeAdapter(@Nullable List<String> hiddenItemsIds, @Nullable HashMap<String, Integer> itemsOrderIds, boolean hidden) {
		List<ContextMenuItem> defItems = contextMenuAdapter.getDefaultItems();
		if (itemsOrderIds == null || itemsOrderIds.isEmpty()) {
			initDefaultOrders(defItems);
		} else {
			sortByCustomOrder(defItems, itemsOrderIds);
		}
		List<AdapterItem> visibleItems = new ArrayList<>();
		List<AdapterItem> hiddenItems = new ArrayList<>();
		for (ContextMenuItem item : defItems) {
			String id = item.getId();
			if (hiddenItemsIds != null && hiddenItemsIds.contains(id)) {
				item.setHidden(true);
				hiddenItems.add(new AdapterItem(MENU_ITEM, item));
			} else {
				item.setHidden(false);
				visibleItems.add(new AdapterItem(MENU_ITEM, item));
			}
		}
		return hidden ? hiddenItems : visibleItems;
	}

	private void sortByCustomOrder(List<ContextMenuItem> defItems, HashMap<String, Integer> itemsOrderIds) {
		for (ContextMenuItem item : defItems) {
			Integer order = itemsOrderIds.get(item.getId());
			if (order != null) {
				item.setOrder(order);
			}
		}
		Collections.sort(defItems, new Comparator<ContextMenuItem>() {
			@Override
			public int compare(ContextMenuItem item1, ContextMenuItem item2) {
				int order1 = item1.getOrder();
				int order2 = item2.getOrder();
				return (order1 < order2) ? -1 : ((order1 == order2) ? 0 : 1);
			}
		});
	}
}
