package net.osmand.plus.settings.fragments;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_MORE_ID;
import static net.osmand.plus.settings.fragments.RearrangeMenuItemsAdapter.AdapterItemType.BUTTON;
import static net.osmand.plus.settings.fragments.RearrangeMenuItemsAdapter.AdapterItemType.DESCRIPTION;
import static net.osmand.plus.settings.fragments.RearrangeMenuItemsAdapter.AdapterItemType.DIVIDER;
import static net.osmand.plus.settings.fragments.RearrangeMenuItemsAdapter.AdapterItemType.HEADER;
import static net.osmand.plus.settings.fragments.RearrangeMenuItemsAdapter.AdapterItemType.MENU_ITEM;

import android.content.Context;
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

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.chooseplan.button.PurchasingUtils;
import net.osmand.plus.configmap.ConfigureMapMenu;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.profiles.SelectCopyAppModeBottomSheet;
import net.osmand.plus.profiles.SelectCopyAppModeBottomSheet.CopyAppModePrefsListener;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.menuitems.ContextMenuItemsSettings;
import net.osmand.plus.settings.backend.menuitems.DrawerMenuItemsSettings;
import net.osmand.plus.settings.backend.menuitems.MainContextMenuItemsSettings;
import net.osmand.plus.settings.backend.preferences.ContextMenuItemsPreference;
import net.osmand.plus.settings.bottomsheets.ChangeGeneralProfilesPrefBottomSheet;
import net.osmand.plus.settings.fragments.ConfigureMenuRootFragment.ScreenType;
import net.osmand.plus.settings.fragments.RearrangeMenuItemsAdapter.MenuItemsAdapterListener;
import net.osmand.plus.settings.fragments.RearrangeMenuItemsAdapter.RearrangeMenuAdapterItem;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.CtxMenuUtils;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.plus.widgets.dialogbutton.DialogButton;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class ConfigureMenuItemsFragment extends BaseOsmAndFragment
		implements CopyAppModePrefsListener {

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
	private ScreenType screenType;
	private boolean nightMode;
	private boolean wasReset;
	private boolean isChanged;
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

	public static void showInstance(@NonNull FragmentManager fragmentManager,
	                                @NonNull ApplicationMode appMode,
	                                @NonNull ScreenType type) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			ConfigureMenuItemsFragment fragment = new ConfigureMenuItemsFragment();
			fragment.setScreenType(type);
			fragment.setAppMode(appMode);
			fragmentManager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(CONFIGURE_MENU_ITEMS_TAG)
					.commitAllowingStateLoss();
		}
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
		if (view != null && !nightMode) {
			AndroidUiHelper.setStatusBarContentColor(view, view.getSystemUiVisibility(), true);
		}
		return nightMode ? R.color.activity_background_dark : R.color.activity_background_light;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			appMode = ApplicationMode.valueOfStringKey(savedInstanceState.getString(APP_MODE_KEY), null);
			screenType = AndroidUtils.getSerializable(savedInstanceState, ITEM_TYPE_KEY, ScreenType.class);
			hiddenMenuItems = savedInstanceState.getStringArrayList(HIDDEN_ITEMS_KEY);
			menuItemsOrder = (HashMap<String, Integer>) AndroidUtils.getSerializable(savedInstanceState, ITEMS_ORDER_KEY, HashMap.class);
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
		List<ContextMenuItem> items = CtxMenuUtils.getCustomizableItems(contextMenuAdapter);
		ContextMenuItemsSettings pref = getMenuItemsSettings(appMode, useDefaultValue);
		if (pref instanceof MainContextMenuItemsSettings) {
			mainActionItems = new ArrayList<>(((MainContextMenuItemsSettings) pref).getMainIds());
			if (mainActionItems.isEmpty()) {
				for (int i = 0; i < MAIN_BUTTONS_QUANTITY && i < items.size(); i++) {
					mainActionItems.add(items.get(i).getId());
				}
			}
		}
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
			PurchasingUtils.removePromoItems(contextMenuAdapter);
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		Context ctx = requireContext();
		View root = mInflater.inflate(R.layout.edit_arrangement_list_fragment, container, false);
		AppBarLayout appbar = root.findViewById(R.id.appbar);
		View toolbar = mInflater.inflate(R.layout.global_preference_toolbar, container, false);
		TextView toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
		ImageButton toolbarButton = toolbar.findViewById(R.id.close_button);
		toolbar.setBackgroundColor(ColorUtilities.getListBgColor(ctx, nightMode));
		toolbarTitle.setTextColor(getColor(nightMode ? R.color.text_color_primary_dark : R.color.list_background_color_dark));
		toolbarButton.setImageDrawable(getPaintedContentIcon(
				AndroidUtils.getNavigationIconResId(app),
				getColor(R.color.text_color_secondary_light)));
		toolbarTitle.setText(screenType.titleRes);
		toolbarButton.setOnClickListener(view -> exitFragment());
		appbar.addView(toolbar);
		recyclerView = root.findViewById(R.id.profiles_list);

		DialogButton cancelButton = root.findViewById(R.id.dismiss_button);
		root.findViewById(R.id.buttons_divider).setVisibility(View.VISIBLE);
		cancelButton.setButtonType(DialogButtonType.SECONDARY);
		cancelButton.setTitleId(R.string.shared_string_cancel);
		cancelButton.setOnClickListener(v -> {
			FragmentActivity fragmentActivity = getActivity();
			if (fragmentActivity != null) {
				fragmentActivity.onBackPressed();
			}
		});

		DialogButton applyButton = root.findViewById(R.id.right_bottom_button);
		applyButton.setButtonType(DialogButtonType.PRIMARY);
		applyButton.setTitleId(R.string.shared_string_apply);
		applyButton.setVisibility(View.VISIBLE);
		applyButton.setOnClickListener(v -> {
			List<ContextMenuItem> defItems = CtxMenuUtils.getCustomizableItems(contextMenuAdapter);
			List<String> ids = new ArrayList<>();
			if (!menuItemsOrder.isEmpty()) {
				sortByCustomOrder(defItems, menuItemsOrder);
				for (ContextMenuItem item : defItems) {
					ids.add(item.getId());
				}
			}
			FragmentManager fm = getFragmentManager();
			ContextMenuItemsSettings prefToSave;
			if (screenType == ScreenType.DRAWER) {
				prefToSave = new DrawerMenuItemsSettings(hiddenMenuItems, ids);
			} else if (screenType == ScreenType.CONTEXT_MENU_ACTIONS) {
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
		});
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), root);
		return root;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		instantiateContextMenuAdapter();
		if (menuItemsOrder.isEmpty()) {
			for (ContextMenuItem item : CtxMenuUtils.getCustomizableItems(contextMenuAdapter)) {
				menuItemsOrder.put(item.getId(), item.getOrder());
			}
		}
		if (screenType == ScreenType.CONTEXT_MENU_ACTIONS) {
			initMainActionsIds(appMode);
		}
		recyclerView.setPadding(0, 0, 0, (int) app.getResources().getDimension(R.dimen.dialog_button_ex_min_width));
		rearrangeAdapter = new RearrangeMenuItemsAdapter(app, getAdapterItems(), nightMode);
		recyclerView.setLayoutManager(new LinearLayoutManager(app));
		ItemTouchHelper touchHelper = new ItemTouchHelper(new ReorderItemTouchHelperCallback(rearrangeAdapter));
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
				view -> resetToDefault())));
		items.add(new RearrangeMenuAdapterItem(BUTTON, new RearrangeMenuItemsAdapter.ButtonItem(
				R.string.copy_from_other_profile,
				R.drawable.ic_action_copy,
				view -> showSelectCopyAppModeDialog())));
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
		dismissDialog.setPositiveButton(R.string.shared_string_exit, (dialog, which) -> dismissFragment());
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

	private void showSelectCopyAppModeDialog() {
		FragmentManager manager = getFragmentManager();
		if (manager != null) {
			SelectCopyAppModeBottomSheet.showInstance(manager, this, appMode);
		}
	}

	@Override
	public void copyAppModePrefs(@NonNull ApplicationMode appMode) {
		isChanged = true;
		initSavedIds(appMode);
		if (screenType == ScreenType.CONTEXT_MENU_ACTIONS) {
			initMainActionsIds(appMode);
		}
		rearrangeAdapter.updateItems(getAdapterItems());
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
		List<ContextMenuItem> defItems = CtxMenuUtils.getCustomizableItems(contextMenuAdapter);
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
		Collections.sort(defItems, (item1, item2) -> {
			int order1 = item1.getOrder();
			int order2 = item2.getOrder();
			return Integer.compare(order1, order2);
		});
	}

}
