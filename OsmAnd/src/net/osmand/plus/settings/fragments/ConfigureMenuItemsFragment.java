package net.osmand.plus.settings.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.ContextMenuItemsPreference;
import net.osmand.plus.settings.backend.ContextMenuItemsSettings;
import net.osmand.plus.settings.backend.MainContextMenuItemsSettings;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.dialogs.ConfigureMapMenu;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.profiles.SelectCopyAppModeBottomSheet;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.bottomsheets.ChangeGeneralProfilesPrefBottomSheet;
import net.osmand.plus.settings.fragments.ConfigureMenuRootFragment.ScreenType;
import net.osmand.plus.settings.fragments.RearrangeMenuItemsAdapter.MenuItemsAdapterListener;
import net.osmand.plus.settings.fragments.RearrangeMenuItemsAdapter.RearrangeMenuAdapterItem;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.APP_PROFILES_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_CONFIGURE_PROFILE_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_SWITCH_PROFILE_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_MORE_ID;
import static net.osmand.plus.settings.fragments.RearrangeMenuItemsAdapter.AdapterItemType.BUTTON;
import static net.osmand.plus.settings.fragments.RearrangeMenuItemsAdapter.AdapterItemType.DESCRIPTION;
import static net.osmand.plus.settings.fragments.RearrangeMenuItemsAdapter.AdapterItemType.DIVIDER;
import static net.osmand.plus.settings.fragments.RearrangeMenuItemsAdapter.AdapterItemType.HEADER;
import static net.osmand.plus.settings.fragments.RearrangeMenuItemsAdapter.AdapterItemType.MENU_ITEM;

public class ConfigureMenuItemsFragment extends BaseOsmAndFragment
		implements SelectCopyAppModeBottomSheet.CopyAppModePrefsListener {

	public static final String TAG = ConfigureMenuItemsFragment.class.getName();
	public static final int MAIN_BUTTONS_QUANTITY = 4;
	private static final Log LOG = PlatformUtil.getLog(ConfigureMenuItemsFragment.class.getName());
	private static final String APP_MODE_KEY = "app_mode_key";
	private static final String ITEM_TYPE_KEY = "item_type_key";
	private static final String ITEMS_ORDER_KEY = "items_order_key";
	private static final String HIDDEN_ITEMS_KEY = "hidden_items_key";
	private static final String MAIN_ACTIONS_ITEMS_KEY = "main_actions_items_key";
	private static final String CONFIGURE_MENU_ITEMS_TAG = "configure_menu_items_tag";
	private static final String IS_CHANGED_KEY = "is_changed_key";
	private RearrangeMenuItemsAdapter rearrangeAdapter;
	private HashMap<String, Integer> menuItemsOrder;
	private ContextMenuAdapter contextMenuAdapter;
	private List<String> hiddenMenuItems;
	private List<String> mainActionItems;
	private ApplicationMode appMode;
	private LayoutInflater mInflater;
	private OsmandApplication app;
	private ScreenType screenType;
	private boolean nightMode;
	private boolean wasReset = false;
	private boolean isChanged = false;
	private FragmentActivity activity;
	private RecyclerView recyclerView;

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putStringArrayList(HIDDEN_ITEMS_KEY, new ArrayList<>(hiddenMenuItems));
		outState.putSerializable(ITEMS_ORDER_KEY, menuItemsOrder);
		outState.putSerializable(ITEM_TYPE_KEY, screenType);
		outState.putString(APP_MODE_KEY, appMode.getStringKey());
		outState.putBoolean(IS_CHANGED_KEY, isChanged);
		if (screenType == ScreenType.CONTEXT_MENU_ACTIONS) {
			outState.putStringArrayList(MAIN_ACTIONS_ITEMS_KEY, new ArrayList<>(mainActionItems));
		}
	}

	public static ConfigureMenuItemsFragment showInstance(
			@NonNull FragmentManager fm,
			@NonNull ApplicationMode appMode,
			@NonNull ScreenType type) {
		ConfigureMenuItemsFragment fragment = new ConfigureMenuItemsFragment();
		fragment.setScreenType(type);
		fragment.setAppMode(appMode);
		fm.beginTransaction()
				.replace(R.id.fragmentContainer, fragment, TAG)
				.addToBackStack(CONFIGURE_MENU_ITEMS_TAG)
				.commitAllowingStateLoss();
		return fragment;
	}

	public void setAppMode(ApplicationMode appMode) {
		this.appMode = appMode;
	}

	public ApplicationMode getAppMode() {
		return appMode != null ? appMode : app.getSettings().getApplicationMode();
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
		if (savedInstanceState != null) {
			appMode = ApplicationMode.valueOfStringKey(savedInstanceState.getString(APP_MODE_KEY), null);
			screenType = (ScreenType) savedInstanceState.getSerializable(ITEM_TYPE_KEY);
			hiddenMenuItems = savedInstanceState.getStringArrayList(HIDDEN_ITEMS_KEY);
			menuItemsOrder = (HashMap<String, Integer>) savedInstanceState.getSerializable(ITEMS_ORDER_KEY);
			isChanged = savedInstanceState.getBoolean(IS_CHANGED_KEY);
			if (screenType == ScreenType.CONTEXT_MENU_ACTIONS) {
				mainActionItems = savedInstanceState.getStringArrayList(MAIN_ACTIONS_ITEMS_KEY);
			}
		} else {
			initSavedIds(appMode);
		}
		nightMode = !app.getSettings().isLightContentForMode(appMode);
		mInflater = UiUtilities.getInflater(app, nightMode);
		mainActionItems = new ArrayList<>();
		activity = requireActivity();
		activity.getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				exitFragment();
			}
		});
	}

	private void initSavedIds(ApplicationMode appMode) {
		initSavedIds(appMode, false);
	}

	private void initSavedIds(ApplicationMode appMode, boolean useDefaultValue) {
		ContextMenuItemsSettings settings = getMenuItemsSettings(appMode, useDefaultValue);
		hiddenMenuItems = new ArrayList<>(settings.getHiddenIds());
		menuItemsOrder = new HashMap<>();
		List<String> orderIds = settings.getOrderIds();
		for (int i = 0; i < orderIds.size(); i++) {
			menuItemsOrder.put(orderIds.get(i), i);
		}
	}

	private void initMainActionsIds(ApplicationMode appMode) {
		initMainActionsIds(appMode, false);
	}

	private void initMainActionsIds(ApplicationMode appMode, boolean useDefaultValue) {
		List<ContextMenuItem> defItems = getCustomizableDefaultItems(contextMenuAdapter.getDefaultItems());
		ContextMenuItemsSettings pref = getMenuItemsSettings(appMode, useDefaultValue);
		if (pref instanceof MainContextMenuItemsSettings) {
			mainActionItems = new ArrayList<>(((MainContextMenuItemsSettings) pref).getMainIds());
			if (mainActionItems.isEmpty()) {
				for (int i = 0; i < MAIN_BUTTONS_QUANTITY && i < defItems.size(); i++) {
					mainActionItems.add(defItems.get(i).getId());
				}
			}
		}
	}

	public static List<ContextMenuItem> getCustomizableDefaultItems(List<ContextMenuItem> defItems) {
		List<ContextMenuItem> items = new ArrayList<>();
		for (ContextMenuItem item : defItems) {
			if (!APP_PROFILES_ID.equals(item.getId())
					&& !DRAWER_CONFIGURE_PROFILE_ID.equals(item.getId())
					&& !DRAWER_SWITCH_PROFILE_ID.equals(item.getId())) {
				items.add(item);
			}
		}
		return items;
	}

	private void instantiateContextMenuAdapter() {
		if (activity instanceof MapActivity) {
			switch (screenType) {
				case DRAWER:
					contextMenuAdapter = ((MapActivity) activity).getMapActions().createMainOptionsMenu();
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
		AppBarLayout appbar = root.findViewById(R.id.appbar);
		View toolbar = mInflater.inflate(R.layout.global_preference_toolbar, container, false);
		TextView toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
		ImageButton toolbarButton = toolbar.findViewById(R.id.close_button);
		toolbar.setBackgroundColor(nightMode
				? getResources().getColor(R.color.list_background_color_dark)
				: getResources().getColor(R.color.list_background_color_light));
		toolbarTitle.setTextColor(nightMode
				? getResources().getColor(R.color.text_color_primary_dark)
				: getResources().getColor(R.color.list_background_color_dark));
		toolbarButton.setImageDrawable(getPaintedContentIcon(
				AndroidUtils.getNavigationIconResId(app),
				getResources().getColor(R.color.text_color_secondary_light)));
		toolbarTitle.setText(screenType.titleRes);
		toolbarButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				exitFragment();
			}
		});
		appbar.addView(toolbar);
		recyclerView = root.findViewById(R.id.profiles_list);
		View cancelButton = root.findViewById(R.id.dismiss_button);
		root.findViewById(R.id.buttons_divider).setVisibility(View.VISIBLE);
		UiUtilities.setupDialogButton(nightMode, cancelButton, UiUtilities.DialogButtonType.SECONDARY, R.string.shared_string_cancel);
		View applyButton = root.findViewById(R.id.right_bottom_button);
		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentActivity fragmentActivity = getActivity();
				if (fragmentActivity != null) {
					fragmentActivity.onBackPressed();
				}
			}
		});
		UiUtilities.setupDialogButton(nightMode, applyButton, UiUtilities.DialogButtonType.PRIMARY, R.string.shared_string_apply);
		applyButton.setVisibility(View.VISIBLE);
		applyButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				List<ContextMenuItem> defItems = getCustomizableDefaultItems(contextMenuAdapter.getDefaultItems());
				List<String> ids = new ArrayList<>();
				if (!menuItemsOrder.isEmpty()) {
					sortByCustomOrder(defItems, menuItemsOrder);
					for (ContextMenuItem item : defItems) {
						ids.add(item.getId());
					}
				}
				FragmentManager fm = getFragmentManager();
				final ContextMenuItemsSettings prefToSave;
				if (screenType == ScreenType.CONTEXT_MENU_ACTIONS) {
					prefToSave = new MainContextMenuItemsSettings(mainActionItems, hiddenMenuItems, ids);
				} else {
					prefToSave = new ContextMenuItemsSettings(hiddenMenuItems, ids);
				}
				if (fm != null) {
					ChangeGeneralProfilesPrefBottomSheet.showInstance(fm,
							getSettingForScreen().getId(),
							prefToSave,
							getTargetFragment(),
							false,
							R.string.back_to_editing,
							appMode,
							new ChangeGeneralProfilesPrefBottomSheet.OnChangeSettingListener() {
								@Override
								public void onApplied(boolean profileOnly) {
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

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		instantiateContextMenuAdapter();
		if (menuItemsOrder.isEmpty()) {
			for (ContextMenuItem item : getCustomizableDefaultItems(contextMenuAdapter.getDefaultItems())) {
				menuItemsOrder.put(item.getId(), item.getOrder());
			}
		}
		if (screenType == ScreenType.CONTEXT_MENU_ACTIONS) {
			initMainActionsIds(appMode);
		}
		recyclerView.setPadding(0, 0, 0, (int) app.getResources().getDimension(R.dimen.dialog_button_ex_min_width));
		rearrangeAdapter = new RearrangeMenuItemsAdapter(app, getAdapterItems(), nightMode);
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
				if (screenType == ScreenType.CONTEXT_MENU_ACTIONS) {
					mainActionItems = rearrangeAdapter.getMainActionsIds();
				}
				toPosition = holder.getAdapterPosition();
				if (toPosition >= 0 && fromPosition >= 0 && toPosition != fromPosition) {
					rearrangeAdapter.notifyDataSetChanged();
				}
			}

			@Override
			public void onButtonClicked(int position) {
				RearrangeMenuAdapterItem rearrangeMenuAdapterItem = rearrangeAdapter.getItem(position);
				if (rearrangeMenuAdapterItem.getValue() instanceof ContextMenuItem) {
					ContextMenuItem menuItemBase = (ContextMenuItem) rearrangeMenuAdapterItem.getValue();
					menuItemBase.setHidden(!menuItemBase.isHidden());
					String id = menuItemBase.getId();
					if (menuItemBase.isHidden()) {
						hiddenMenuItems.add(id);
						if (screenType == ScreenType.CONTEXT_MENU_ACTIONS) {
							mainActionItems.remove(id);
						}
					} else {
						hiddenMenuItems.remove(id);
						if (screenType == ScreenType.CONTEXT_MENU_ACTIONS && mainActionItems.size() < MAIN_BUTTONS_QUANTITY) {
							mainActionItems.add(id);
						}
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
	}

	@Override
	public void onResume() {
		super.onResume();
		if (activity instanceof MapActivity) {
			((MapActivity) activity).disableDrawer();
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		if (activity instanceof MapActivity) {
			((MapActivity) activity).enableDrawer();
		}
	}

	private List<RearrangeMenuAdapterItem> getAdapterItems() {
		List<RearrangeMenuAdapterItem> items = new ArrayList<>();
		items.add(new RearrangeMenuAdapterItem(DESCRIPTION, screenType));

		List<RearrangeMenuAdapterItem> visible = getItemsForRearrangeAdapter(hiddenMenuItems, menuItemsOrder, false);
		List<RearrangeMenuAdapterItem> hiddenItems = getItemsForRearrangeAdapter(hiddenMenuItems, menuItemsOrder, true);
		if (screenType == ScreenType.CONTEXT_MENU_ACTIONS) {
			int buttonMoreIndex = MAIN_BUTTONS_QUANTITY - 1;
			for (int i = 0; i < visible.size(); i++) {
				ContextMenuItem value = (ContextMenuItem) visible.get(i).getValue();
				if (value.getId() != null && value.getId().equals(MAP_CONTEXT_MENU_MORE_ID) && i > buttonMoreIndex) {
					RearrangeMenuAdapterItem third = visible.get(buttonMoreIndex);
					visible.set(buttonMoreIndex, visible.get(i));
					visible.set(i, third);
					value.setOrder(buttonMoreIndex);
					((ContextMenuItem) third.getValue()).setOrder(i);
					break;
				}
			}

			List<RearrangeMenuAdapterItem> main = new ArrayList<>();
			List<RearrangeMenuAdapterItem> additional = new ArrayList<>();
			for (RearrangeMenuAdapterItem adapterItem : visible) {
				if (mainActionItems.contains(((ContextMenuItem) adapterItem.getValue()).getId())) {
					main.add(adapterItem);
				} else {
					additional.add(adapterItem);
				}
			}
			items.add(new RearrangeMenuAdapterItem(HEADER, new RearrangeMenuItemsAdapter.HeaderItem(R.string.main_actions, R.string.main_actions_descr)));
			items.addAll(main);
			items.add(new RearrangeMenuAdapterItem(HEADER, new RearrangeMenuItemsAdapter.HeaderItem(R.string.additional_actions, R.string.additional_actions_descr)));
			if (!additional.isEmpty()) {
				items.addAll(additional);
			}
		} else {
			items.addAll(visible);
		}
		if (!hiddenItems.isEmpty()) {
			items.add(new RearrangeMenuAdapterItem(HEADER,
					new RearrangeMenuItemsAdapter.HeaderItem(R.string.shared_string_hidden,
							screenType == ScreenType.CONFIGURE_MAP ? R.string.reset_items_descr : R.string.hidden_items_descr)));
			items.addAll(hiddenItems);
		}
		items.add(new RearrangeMenuAdapterItem(DIVIDER, 1));
		items.add(new RearrangeMenuAdapterItem(BUTTON, new RearrangeMenuItemsAdapter.ButtonItem(
				R.string.reset_to_default,
				R.drawable.ic_action_reset_to_default_dark,
				new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						resetToDefault();
					}
				})));
		items.add(new RearrangeMenuAdapterItem(BUTTON, new RearrangeMenuItemsAdapter.ButtonItem(
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

	public void resetToDefault() {
		hiddenMenuItems.clear();
		menuItemsOrder.clear();
		wasReset = true;
		isChanged = true;
		if (screenType == ScreenType.CONTEXT_MENU_ACTIONS) {
			mainActionItems.clear();
		}
		instantiateContextMenuAdapter();
		initSavedIds(appMode, true);
		initMainActionsIds(appMode, true);
		rearrangeAdapter.updateItems(getAdapterItems());
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
			isChanged = true;
			initSavedIds(appMode);
			if (screenType == ScreenType.CONTEXT_MENU_ACTIONS) {
				initMainActionsIds(appMode);
			}
			rearrangeAdapter.updateItems(getAdapterItems());
		}
	}

	public ContextMenuItemsSettings getMenuItemsSettings(ApplicationMode appMode,
	                                                     boolean useDefaultValue) {
		ContextMenuItemsPreference preference = getSettingForScreen();
		if (useDefaultValue) {
			return preference.getProfileDefaultValue(appMode);
		} else {
			return preference.getModeValue(appMode);
		}
	}

	public ContextMenuItemsPreference getSettingForScreen() {
		return getSettingForScreen(app, screenType);
	}

	public static ContextMenuItemsPreference getSettingForScreen(OsmandApplication app, ScreenType screenType) throws IllegalArgumentException {
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

	private List<RearrangeMenuAdapterItem> getItemsForRearrangeAdapter(List<String> hiddenItemsIds, HashMap<String, Integer> itemsOrderIds, boolean hidden) {
		List<ContextMenuItem> defItems = getCustomizableDefaultItems(contextMenuAdapter.getDefaultItems());
		if (!itemsOrderIds.isEmpty()) {
			sortByCustomOrder(defItems, itemsOrderIds);
		}
		List<RearrangeMenuAdapterItem> visibleItems = new ArrayList<>();
		List<RearrangeMenuAdapterItem> hiddenItems = new ArrayList<>();
		for (ContextMenuItem item : defItems) {
			String id = item.getId();
			if (hiddenItemsIds != null && hiddenItemsIds.contains(id)) {
				item.setHidden(true);
				hiddenItems.add(new RearrangeMenuAdapterItem(MENU_ITEM, item));
			} else {
				item.setHidden(false);
				visibleItems.add(new RearrangeMenuAdapterItem(MENU_ITEM, item));
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

	private void resetHiddenItems(boolean profileOnly) {
		for (ContextMenuItem item : contextMenuAdapter.getItems()) {
			if (hiddenMenuItems.contains(item.getId())) {
				if (item.getItemDeleteAction() != null) {
					item.getItemDeleteAction().itemWasDeleted(appMode, profileOnly);
				}
			}
		}
	}
}
