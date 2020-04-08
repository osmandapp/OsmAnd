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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_MORE_ID;
import static net.osmand.plus.settings.RearrangeMenuItemsAdapter.AdapterItemType.BUTTON;
import static net.osmand.plus.settings.RearrangeMenuItemsAdapter.AdapterItemType.DESCRIPTION;
import static net.osmand.plus.settings.RearrangeMenuItemsAdapter.AdapterItemType.DIVIDER;
import static net.osmand.plus.settings.RearrangeMenuItemsAdapter.AdapterItemType.HEADER;

public class ConfigureMenuItemsFragment extends BaseOsmAndFragment
		implements SelectCopyAppModeBottomSheet.CopyAppModePrefsListener {

	public static final String TAG = ConfigureMenuItemsFragment.class.getName();
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
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		appMode = app.getSettings().getApplicationMode();
		nightMode = !app.getSettings().isLightContent();
		mInflater = UiUtilities.getInflater(app, nightMode);
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
					contextMenuAdapter = menu.getAdapter();
					break;
			}
		}
		if (savedInstanceState != null
				&& savedInstanceState.containsKey(ITEM_TYPE_KEY)
				&& savedInstanceState.containsKey(HIDDEN_ITEMS_KEY)
				&& savedInstanceState.containsKey(ITEMS_ORDER_KEY)) {
			screenType = (ScreenType) savedInstanceState.getSerializable(ITEM_TYPE_KEY);
			hiddenMenuItems = savedInstanceState.getStringArrayList(HIDDEN_ITEMS_KEY);
			menuItemsOrder = (HashMap<String, Integer>) savedInstanceState.getSerializable(ITEMS_ORDER_KEY);
		} else {
			hiddenMenuItems = getSettingForScreen(app, screenType).getHiddenIds();
			menuItemsOrder = contextMenuAdapter.getMenuItemsOrder(getSettingForScreen(app, screenType).getOrderIds());
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
				contextMenuAdapter.reorderMenuItems(defItems, menuItemsOrder);
				List<String> ids = new ArrayList<>();
				for (ContextMenuItem item : defItems) {
					ids.add(item.getId());
				}
				FragmentManager fm = getFragmentManager();
				String stringToSave = getSettingForScreen(app, screenType).convertToJsonString(hiddenMenuItems, ids);
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

	private List<AdapterItem> getAdapterItems() {
		List<AdapterItem> items = new ArrayList<>();
		items.add(new AdapterItem(DESCRIPTION, screenType));

		List<AdapterItem> visible = contextMenuAdapter.getItemsForRearrangeAdapter(hiddenMenuItems, wasReset ? null : menuItemsOrder, false);
		List<AdapterItem> hiddenItems = contextMenuAdapter.getItemsForRearrangeAdapter(hiddenMenuItems, wasReset ? null : menuItemsOrder, true);
		if (screenType == ScreenType.CONTEXT_MENU_ACTIONS) {
			List<AdapterItem> main = new ArrayList<>();
			int actionsIndex = 3;
			for (int i = 0; i < visible.size(); i++) {
				if (((ContextMenuItem) visible.get(i).getValue()).getId().equals(MAP_CONTEXT_MENU_MORE_ID)) {
					actionsIndex = i;
					break;
				}
			}
			for (int i = 0; i < actionsIndex + 1; i++) {
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

	public static OsmandSettings.MenuItemConfigPreference getSettingForScreen(OsmandApplication app, ScreenType screenType) {
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
}
