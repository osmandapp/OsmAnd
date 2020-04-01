package net.osmand.plus.settings;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
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
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.settings.ConfigureMenuRootFragment.ScreenType;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback;
import net.osmand.plus.settings.MenuItemsAdapter.AdapterItem;
import net.osmand.plus.settings.MenuItemsAdapter.HeaderItem;
import net.osmand.plus.settings.MenuItemsAdapter.ButtonItem;
import net.osmand.plus.settings.MenuItemsAdapter.MenuItemBase;
import net.osmand.plus.settings.MenuItemsAdapter.MenuItemsAdapterListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import static net.osmand.plus.settings.MenuItemsAdapter.AdapterItemType.HEADER;
import static net.osmand.plus.settings.MenuItemsAdapter.AdapterItemType.DIVIDER;
import static net.osmand.plus.settings.MenuItemsAdapter.AdapterItemType.DESCRIPTION;
import static net.osmand.plus.settings.MenuItemsAdapter.AdapterItemType.BUTTON;
import static net.osmand.plus.settings.MenuItemsAdapter.AdapterItemType.MENU_ITEM;

public class ConfigureMenuItemsFragment extends BaseOsmAndFragment {

	public static final String TAG = ConfigureMenuItemsFragment.class.getName();
	private static final String ITEM_TYPE_KEY = "item_type_key";
	private static String ITEMS_ORDER_KEY = "items_order_key";
	private static String HIDDEN_ITEMS_KEY = "hidden_items_key";
	private static String CONFIGURE_MENU_ITEMS_TAG = "configure_menu_items_tag";
	private OsmandApplication app;
	private boolean nightMode;
	private ScreenType type;
	private LayoutInflater mInflater;
	private MenuItemsManager menuItemsManager;
	private HashMap<String, Integer> menuItemsOrder = new LinkedHashMap<>();
	private List<String> hiddenMenuItems = new ArrayList<>();

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(ITEM_TYPE_KEY, type);
		outState.putStringArrayList(HIDDEN_ITEMS_KEY, new ArrayList<>(hiddenMenuItems));
		outState.putSerializable(ITEMS_ORDER_KEY, menuItemsOrder);
	}

	public static ConfigureMenuItemsFragment showInstance(@NonNull FragmentManager fm, @NonNull ScreenType type) {
		ConfigureMenuItemsFragment fragment = new ConfigureMenuItemsFragment();
		fragment.setType(type);
		fm.beginTransaction()
				.replace(R.id.fragmentContainer, fragment, TAG)
				.addToBackStack(CONFIGURE_MENU_ITEMS_TAG)
				.commitAllowingStateLoss();
		return fragment;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null
				&& savedInstanceState.containsKey(ITEM_TYPE_KEY)
				&& savedInstanceState.containsKey(HIDDEN_ITEMS_KEY)
				&& savedInstanceState.containsKey(ITEMS_ORDER_KEY)) {
			type = (ScreenType) savedInstanceState.getSerializable(ITEM_TYPE_KEY);
			hiddenMenuItems = savedInstanceState.getStringArrayList(HIDDEN_ITEMS_KEY);
			menuItemsOrder = (HashMap<String,Integer>) savedInstanceState.getSerializable(ITEMS_ORDER_KEY);
		}
		app = requireMyApplication();
		nightMode = !app.getSettings().isLightContent();
		mInflater = UiUtilities.getInflater(app, nightMode);
		menuItemsManager = new MenuItemsManager(app);

		List<String> ids = app.getSettings().HIDDEN_DRAWER_ITEMS.getStringsList();
		hiddenMenuItems = new ArrayList<>();
		if (ids != null) {
			hiddenMenuItems.addAll(ids);
		}
		menuItemsOrder = menuItemsManager.getDrawerItemsSavedOrder();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View root = mInflater.inflate(R.layout.edit_arrangement_list_fragment, container, false);
		Toolbar toolbar = root.findViewById(R.id.toolbar);
		TextView toolbarTitle = root.findViewById(R.id.toolbar_title);
		ImageButton toolbarButton = root.findViewById(R.id.close_button);
		RecyclerView recyclerView = root.findViewById(R.id.profiles_list);
		toolbar.setBackgroundColor(nightMode
				? getResources().getColor(R.color.list_background_color_dark)
				: getResources().getColor(R.color.list_background_color_light));
		toolbarTitle.setTextColor(nightMode
				? getResources().getColor(R.color.text_color_primary_dark)
				: getResources().getColor(R.color.list_background_color_dark));
		toolbarButton.setImageDrawable(getPaintedContentIcon(R.drawable.ic_arrow_back, getResources().getColor(R.color.text_color_secondary_light)));
		toolbarTitle.setText(type.titleRes);
		toolbarButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				showExitDialog();
			}
		});

		recyclerView.setLayoutManager(new LinearLayoutManager(app));

		final MenuItemsAdapter adapter = new MenuItemsAdapter(app, getAdapterItems());
		final ItemTouchHelper touchHelper = new ItemTouchHelper(new ReorderItemTouchHelperCallback(adapter));
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
					adapter.notifyDataSetChanged();
				}
			}

			@Override
			public void onButtonClicked(int position) {
				AdapterItem adapterItem = adapter.getItem(position);
				if (adapterItem.getValue() instanceof MenuItemBase) {
					MenuItemBase menuItemBase = (MenuItemBase) adapterItem.getValue();
					menuItemBase.toggleHidden();
					if (menuItemBase.isHidden()) {
						hiddenMenuItems.add(menuItemBase.getId());
					} else {
						hiddenMenuItems.remove(menuItemBase.getId());
					}
					adapter.updateItems(getAdapterItems());
				}
			}

			@Override
			public void onItemMoved(String id, int position) {
				menuItemsOrder.put(id, position);
			}
		};
		adapter.setListener(listener);
		recyclerView.setAdapter(adapter);

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
				MapActivity mapActivity = (MapActivity) getActivity();
				if (mapActivity != null) {
					OsmandApplication app = mapActivity.getMyApplication();
					app.getSettings().HIDDEN_DRAWER_ITEMS.setStringsList(hiddenMenuItems);


					List<MenuItemBase> drawerItems = menuItemsManager.getDrawerItemsDefault();

					for (MenuItemBase menuItemBase : drawerItems) {
						Integer order = menuItemsOrder.get(menuItemBase.getId());
						if (order == null) {
							order = menuItemBase.getOrder();
						}
						menuItemBase.setOrder(order);

					}

					Collections.sort(drawerItems, new Comparator<MenuItemBase>() {
						@Override
						public int compare(MenuItemBase item1, MenuItemBase item2) {
							int order1 = item1.getOrder();
							int order2 = item2.getOrder();
							return (order1 < order2) ? -1 : ((order1 == order2) ? 0 : 1);
						}
					});

					List<String> ids = new ArrayList<>();
					for (MenuItemBase menuItemBase : drawerItems) {
						ids.add(menuItemBase.getId());
					}
					app.getSettings().DRAWER_ITEMS_ORDER.setStringsList(ids);

					dismissFragment();
				}
			}
		});

		if (Build.VERSION.SDK_INT >= 21) {
			AndroidUtils.addStatusBarPadding21v(app, root);
		}
		return root;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
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

	public void setType(@NonNull ScreenType type) {
		this.type = type;
	}

	private List<AdapterItem> getAdapterItems() {
		List<AdapterItem> items = new ArrayList<>();
		items.add(new AdapterItem(DESCRIPTION, type));
		switch (type) {
			case DRAWER:
				items.addAll(getDrawerAdapterItems());
				break;
			case CONFIGURE_MAP:
				items.addAll(getConfigureMapAdapterItems());
				break;
			case CONTEXT_MENU_ACTIONS:
				items.addAll(getContextMenuActionsAdapterItems());
				break;
		}
		items.add(new AdapterItem(DIVIDER, 1));
		items.add(new AdapterItem(BUTTON, new ButtonItem(
				R.string.reset_to_default,
				R.drawable.ic_action_reset_to_default_dark,
				new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						switch (type) {
							case DRAWER:
								app.getSettings().DRAWER_ITEMS_ORDER.setStringsList(null);
								app.getSettings().HIDDEN_DRAWER_ITEMS.setStringsList(null);

								break;
							case CONFIGURE_MAP:
								break;
							case CONTEXT_MENU_ACTIONS:
								break;
						}
					}
				})));
		items.add(new AdapterItem(BUTTON, new ButtonItem(
				R.string.copy_from_other_profile,
				R.drawable.ic_action_copy,
				new View.OnClickListener() {
					@Override
					public void onClick(View view) {

					}
				})));
		return items;
	}

	public List<MenuItemBase> getDrawerItems(boolean hidden) {

		List<MenuItemBase> items = new ArrayList<>();
		List<MenuItemBase> drawerItems = menuItemsManager.getDrawerItemsDefault();

		for (MenuItemBase menuItemBase : drawerItems) {
			Integer order = menuItemsOrder.get(menuItemBase.getId());
			if (order == null) {
				order = menuItemBase.getOrder();
			}
			menuItemBase.setOrder(order);
//			menuItemBase.setHidden(hiddenMenuItems.contains(menuItemBase.getId()));

		}

		Collections.sort(drawerItems, new Comparator<MenuItemBase>() {
			@Override
			public int compare(MenuItemBase item1, MenuItemBase item2) {
				int order1 = item1.getOrder();
				int order2 = item2.getOrder();
				return (order1 < order2) ? -1 : ((order1 == order2) ? 0 : 1);
			}
		});


		for (MenuItemBase drawerItem : drawerItems) {
			if (hidden && hiddenMenuItems.contains(drawerItem.getId())) {
				drawerItem.setHidden(true);
				items.add(drawerItem);
			} else if (!hidden && !hiddenMenuItems.contains(drawerItem.getId())) {
				drawerItem.setHidden(false);
				items.add(drawerItem);
			}
		}
		return items;
	}

	private List<AdapterItem> getDrawerAdapterItems() {
		List<AdapterItem> active = convertToAdapterItems(getDrawerItems(false));
		List<AdapterItem> hidden = convertToAdapterItems(getDrawerItems(true));
		List<AdapterItem> items = new ArrayList<>(active);
		if (!hidden.isEmpty()) {
			items.add(new AdapterItem(HEADER, new HeaderItem(
					R.string.shared_string_hidden,
					R.string.hidden_items_descr,
					false)
			));
			items.addAll(hidden);
		}
		return items;
	}

	private List<AdapterItem> convertToAdapterItems(List<MenuItemBase> itemBaseList) {
		List<AdapterItem> items = new ArrayList<>();
		for (MenuItemBase menuItem : itemBaseList) {
			items.add(new AdapterItem(MENU_ITEM, menuItem));
		}
		return items;
	}

	private List<AdapterItem> getConfigureMapAdapterItems() {
		List<AdapterItem> items = new ArrayList<>();
		items.add(new AdapterItem(HEADER, new HeaderItem(
				R.string.main_actions,
				R.string.main_actions_descr,
				false)
		));

		items.add(new AdapterItem(HEADER, new HeaderItem(
				R.string.additional_actions,
				R.string.additional_actions_descr,
				false)
		));

		items.add(new AdapterItem(HEADER, new HeaderItem(
				R.string.shared_string_hidden,
				R.string.hidden_items_descr,
				false)
		));

		return items;
	}

	private List<AdapterItem> getContextMenuActionsAdapterItems() {
		List<AdapterItem> items = new ArrayList<>();
		items.add(new AdapterItem(HEADER, new HeaderItem(
				R.string.shared_string_show,
				R.string.move_inside_category,
				false)
		));

		items.add(new AdapterItem(HEADER, new HeaderItem(
				R.string.map_widget_map_rendering,
				R.string.move_inside_category,
				false)
		));

		items.add(new AdapterItem(HEADER, new HeaderItem(
				R.string.shared_string_hidden,
				R.string.reset_items_descr,
				false)
		));

		return items;
	}

//	private List<MenuItemBase> getDrawerItems() {
//		List<MenuItemBase> items = new ArrayList<>();
//
//		return items;
//	}

//	private List<MenuItemBase> getConfigureMapItems() {
//		List<MenuItemBase> items = new ArrayList<>();
//
//		return items;
//	}

//	private List<MenuItemBase> getContextMenuActionsItems() {
//		List<MenuItemBase> items = new ArrayList<>();
//
//		return items;
//	}
}
