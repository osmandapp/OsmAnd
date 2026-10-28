package net.osmand.plus.poi;

import static net.osmand.plus.poi.PoiUIFilter.USER_PREFIX;
import static net.osmand.plus.poi.RearrangePoiFiltersFragment.ItemType.DESCRIPTION;
import static net.osmand.plus.poi.RearrangePoiFiltersFragment.ItemType.POI;
import static net.osmand.plus.poi.RearrangePoiFiltersFragment.ItemType.SPACE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.profiles.SelectAppModesBottomSheetDialogFragment;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.search.listitems.QuickSearchListItem;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback;
import net.osmand.plus.widgets.dialogbutton.DialogButton;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class RearrangePoiFiltersFragment extends DialogFragment implements SelectAppModesBottomSheetDialogFragment.AppModeChangedListener {

	public static final String TAG = "RearrangePoiFiltersFragment";

	private static final Log LOG = PlatformUtil.getLog(RearrangePoiFiltersFragment.class);

	private boolean usedOnMap;
	private OnApplyPoiFiltersState resultCallback;

	private final List<ListItem> items = new ArrayList<>();
	private EditPoiFiltersAdapter adapter;
	private boolean orderModified;
	private boolean activationModified;
	private boolean wasReset;
	private boolean isChanged;
	private boolean filterDeleted;
	private ApplicationMode appMode;
	private LinearLayout buttonsContainer;

	private final HashMap<String, Integer> poiFiltersOrders = new HashMap<>();
	private final List<String> availableFiltersKeys = new ArrayList<>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		boolean nightMode = isNightMode(requireMyApplication(), usedOnMap);
		int themeId = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		setStyle(STYLE_NO_FRAME, themeId);
	}

	@Override
	public void onPause() {
		super.onPause();
		if (requireActivity().isChangingConfigurations()) {
			dismiss();
		}
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		updateProfileButton();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		OsmandApplication app = requireMyApplication();

		boolean nightMode = isNightMode(app, usedOnMap);

		View mainView = UiUtilities.getInflater(app, nightMode).inflate(R.layout.edit_arrangement_list_fragment, container, false);
		createToolbar(mainView, nightMode);

		RecyclerView recyclerView = mainView.findViewById(R.id.profiles_list);
		recyclerView.setLayoutManager(new LinearLayoutManager(app));

		adapter = new EditPoiFiltersAdapter(app, nightMode);
		initFiltersOrders(app, false);

		ItemTouchHelper touchHelper = new ItemTouchHelper(new ReorderItemTouchHelperCallback(adapter));
		touchHelper.attachToRecyclerView(recyclerView);

		orderModified = app.getSettings().POI_FILTERS_ORDER.get() != null;
		activationModified = app.getSettings().INACTIVE_POI_FILTERS.get() != null;

		adapter.setListener(new PoiAdapterListener() {

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
			public void onButtonClicked(int pos) {
				ListItem item = items.get(pos);
				if (item.value instanceof PoiUIFilterDataObject) {
					isChanged = true;
					activationModified = true;
					PoiUIFilterDataObject poiInfo = (PoiUIFilterDataObject) item.value;
					poiInfo.toggleActive();
					if (!poiInfo.isActive) {
						availableFiltersKeys.add(poiInfo.filterId);
					} else {
						availableFiltersKeys.remove(poiInfo.filterId);
					}
					updateItems();
				}
			}

			@Override
			public void onDeleteClicked(int position) {
				ListItem item = items.get(position);
				if (item.value instanceof PoiUIFilterDataObject) {
					PoiUIFilterDataObject poiInfo = (PoiUIFilterDataObject) item.value;
					PoiUIFilter filter = app.getPoiFilters().getFilterById(poiInfo.filterId);
					if (filter != null && app.getPoiFilters().removePoiFilter(filter)) {
						filter.setDeleted(true);
						filterDeleted = true;
					}
					items.remove(item);
					adapter.notifyDataSetChanged();
					Snackbar snackbar = Snackbar.make(requireView(),
									getString(R.string.item_deleted, poiInfo.name), Snackbar.LENGTH_LONG)
							.setAction(R.string.shared_string_undo, new View.OnClickListener() {
								@Override
								public void onClick(View view) {
									items.add(position, item);
									adapter.notifyDataSetChanged();
									if (filter != null) {
										filter.setDeleted(false);
										app.getPoiFilters().createPoiFilter(filter, false);
									}
								}
							});
					ViewCompat.setElevation(snackbar.getView(), 0f);
					snackbar.setAnchorView(buttonsContainer);
					snackbar.setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_FADE);
					UiUtilities.setupSnackbar(snackbar, nightMode);
					snackbar.show();
				}
			}
		});
		recyclerView.setAdapter(adapter);

		DialogButton cancelButton = mainView.findViewById(R.id.dismiss_button);
		cancelButton.setButtonType(DialogButtonType.SECONDARY);
		cancelButton.setTitleId(R.string.shared_string_cancel);
		cancelButton.setOnClickListener(v -> dismiss());

		mainView.findViewById(R.id.buttons_divider).setVisibility(View.VISIBLE);

		DialogButton applyButton = mainView.findViewById(R.id.right_bottom_button);
		applyButton.setButtonType(DialogButtonType.PRIMARY);
		applyButton.setTitleId(R.string.shared_string_apply);
		applyButton.setVisibility(View.VISIBLE);
		applyButton.setOnClickListener(v -> {
			ApplicationMode selectedAppMode = getSelectedAppMode();
			if (isChanged) {
				if (activationModified) {
					app.getPoiFilters().saveInactiveFilters(selectedAppMode, availableFiltersKeys);
				} else if (wasReset) {
					app.getPoiFilters().saveInactiveFilters(selectedAppMode, null);
				}
				if (orderModified) {
					List<PoiUIFilter> dataToSave = new ArrayList<>();
					for (PoiUIFilter filter : getSortedPoiUiFilters(selectedAppMode, app)) {
						String filterId = filter.getFilterId();
						Integer order = poiFiltersOrders.get(filterId);
						if (order == null) {
							order = filter.getOrder();
						}
						boolean isActive = !availableFiltersKeys.contains(filterId);
						filter.setActive(isActive);
						filter.setOrder(order);
						if (isActive) {
							dataToSave.add(filter);
						}
					}
					Collections.sort(dataToSave);
					List<String> filterIds = new ArrayList<>();
					for (PoiUIFilter filter : dataToSave) {
						filterIds.add(filter.getFilterId());
					}
					app.getPoiFilters().saveFiltersOrder(selectedAppMode, filterIds);
				} else if (wasReset) {
					app.getPoiFilters().saveFiltersOrder(selectedAppMode, null);
				}
			}
			if (resultCallback != null) {
				resultCallback.onApplyPoiFiltersState(selectedAppMode, isChanged);
			}
			dismiss();
		});
		buttonsContainer = mainView.findViewById(R.id.buttons_container);

		return mainView;
	}

	@Override
	public void onDismiss(@NonNull DialogInterface dialog) {
		if (filterDeleted && resultCallback != null) {
			requireMyApplication().getPoiFilters().saveInactiveFilters(getSelectedAppMode(), availableFiltersKeys);
			resultCallback.onCustomFiltersDeleted();
		}
		super.onDismiss(dialog);
	}

	private void createToolbar(View mainView, boolean nightMode) {
		AppBarLayout appbar = mainView.findViewById(R.id.appbar);
		View toolbar = UiUtilities.getInflater(getContext(), nightMode).inflate(R.layout.profile_preference_toolbar_with_icon, appbar, false);

		ImageButton closeButton = toolbar.findViewById(R.id.close_button);
		closeButton.setImageResource(R.drawable.ic_action_remove_dark);
		closeButton.setOnClickListener(v -> dismiss());

		TextView toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
		toolbarTitle.setText(R.string.rearrange_categories);

		View switchProfile = toolbar.findViewById(R.id.profile_button);
		if (switchProfile != null) {
			switchProfile.setContentDescription(getString(R.string.switch_profile));
			switchProfile.setOnClickListener(v -> {
				FragmentManager fragmentManager = getFragmentManager();
				if (fragmentManager != null) {
					SelectAppModesBottomSheetDialogFragment.showInstance(fragmentManager,
							RearrangePoiFiltersFragment.this, false, getSelectedAppMode(), false);
				}
			});
		}
		appbar.addView(toolbar);
	}

	protected void updateProfileButton() {
		View view = getView();
		if (view == null) {
			return;
		}

		OsmandApplication app = requireMyApplication();
		UiUtilities uiUtilities = app.getUIUtilities();
		ApplicationMode selectedAppMode = getSelectedAppMode();
		boolean nightMode = isNightMode(app, usedOnMap);

		ImageView profileIcon = view.findViewById(R.id.profile_icon);
		if (profileIcon != null) {
			int iconRes = selectedAppMode.getIconRes();
			profileIcon.setImageDrawable(uiUtilities.getPaintedIcon(iconRes, selectedAppMode.getProfileColor(nightMode)));
		}

		View profileButton = view.findViewById(R.id.profile_button);
		if (profileButton != null) {
			int iconColor = getSelectedAppMode().getProfileColor(nightMode);
			int bgColor = ContextCompat.getColor(app, nightMode ?
					R.color.divider_color_dark : R.color.active_buttons_and_links_text_light);
			int selectedColor = ColorUtilities.getColorWithAlpha(iconColor, 0.3f);

			if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
				int bgResId = R.drawable.circle_background_light;
				int selectableResId = R.drawable.ripple_circle;
				Drawable bgDrawable = uiUtilities.getPaintedIcon(bgResId, bgColor);
				Drawable selectable = uiUtilities.getPaintedIcon(selectableResId, selectedColor);
				Drawable[] layers = {bgDrawable, selectable};
				AndroidUtils.setBackground(profileButton, new LayerDrawable(layers));
			} else {
				int bgResId = R.drawable.circle_background_light;
				Drawable bgDrawable = uiUtilities.getPaintedIcon(bgResId, bgColor);
				AndroidUtils.setBackground(profileButton, bgDrawable);
			}
		}
	}

	private void initFiltersOrders(OsmandApplication app, boolean arrangementByDefault) {
		poiFiltersOrders.clear();
		availableFiltersKeys.clear();
		ApplicationMode selectedAppMode = getSelectedAppMode();
		List<PoiUIFilter> filters = getSortedPoiUiFilters(selectedAppMode, app);
		if (arrangementByDefault) {
			Collections.sort(filters, (o1, o2) -> {
				if (o1.filterId.equals(o2.filterId)) {
					String filterByName1 = o1.filterByName == null ? "" : o1.filterByName;
					String filterByName2 = o2.filterByName == null ? "" : o2.filterByName;
					return filterByName1.compareToIgnoreCase(filterByName2);
				} else {
					return o1.name.compareToIgnoreCase(o2.name);
				}
			});
			for (int i = 0; i < filters.size(); i++) {
				PoiUIFilter filter = filters.get(i);
				poiFiltersOrders.put(filter.getFilterId(), i);
			}
		} else {
			for (int i = 0; i < filters.size(); i++) {
				PoiUIFilter filter = filters.get(i);
				poiFiltersOrders.put(filter.getFilterId(), i);
				if (!filter.isActive) {
					availableFiltersKeys.add(filter.getFilterId());
				}
			}
		}
		updateItems();
	}

	private void updateItems() {
		OsmandApplication app = requireMyApplication();
		List<ListItem> active = getPoiFilters(true);
		List<ListItem> available = getPoiFilters(false);
		items.clear();
		items.add(new ListItem(DESCRIPTION, app.getString(R.string.create_custom_categories_list_promo)));
		items.add(new ListItem(ItemType.SPACE, app.getResources().getDimension(R.dimen.content_padding)));
		items.addAll(active);
		items.add(new ListItem(ItemType.DIVIDER, 0));
		if (availableFiltersKeys != null && availableFiltersKeys.size() > 0) {
			items.add(new ListItem(ItemType.HEADER, app.getString(R.string.shared_string_available)));
			items.addAll(available);
			items.add(new ListItem(ItemType.DIVIDER, 1));
		}
		/*items.add(new ListItem(ItemType.BUTTON, new ControlButton(app.getString(R.string.add_custom_category),
				R.drawable.ic_action_plus, new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				QuickSearchCustomPoiFragment.showDialog(RearrangePoiFiltersFragment.this, app.getPoiFilters().getCustomPOIFilter().getFilterId());
			}
		})));*/
		items.add(new ListItem(ItemType.BUTTON, new ControlButton(app.getString(R.string.reset_to_default),
				R.drawable.ic_action_reset_to_default_dark, new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				isChanged = true;
				wasReset = true;
				activationModified = false;
				orderModified = false;
				initFiltersOrders(app, true);
			}
		})));
		items.add(new ListItem(DESCRIPTION,
//				app.getString(R.string.add_new_custom_category_button_promo) + '\n' + 
				app.getString(R.string.reset_to_default_category_button_promo)));

		adapter.setItems(items);
	}

	public static void showInstance(@NonNull ApplicationMode appMode, @NonNull DialogFragment parentFragment,
	                                boolean usedOnMap, OnApplyPoiFiltersState callback) {
		try {
			RearrangePoiFiltersFragment fragment = new RearrangePoiFiltersFragment();
			fragment.setUsedOnMap(usedOnMap);
			fragment.setResultCallback(callback);
			fragment.setSelectedAppMode(appMode);
			fragment.show(parentFragment.getChildFragmentManager(), TAG);
		} catch (RuntimeException e) {
			LOG.error("showInstance", e);
		}
	}

	public ApplicationMode getSelectedAppMode() {
		if (appMode == null) {
			appMode = requireMyApplication().getSettings().getApplicationMode();
		}
		return appMode;
	}

	public void setSelectedAppMode(ApplicationMode appMode) {
		this.appMode = appMode;
	}

	public List<ListItem> getPoiFilters(boolean isActive) {
		OsmandApplication app = requireMyApplication();
		ApplicationMode selectedAppMode = getSelectedAppMode();
		List<ListItem> result = new ArrayList<>();
		for (PoiUIFilter f : getSortedPoiUiFilters(selectedAppMode, app)) {
			addFilterToList(result, f, isActive);
		}
		Collections.sort(result, (o1, o2) -> {
			int order1 = ((PoiUIFilterDataObject) o1.value).order;
			int order2 = ((PoiUIFilterDataObject) o2.value).order;
			return (order1 < order2) ? -1 : ((order1 == order2) ? 0 : 1);
		});
		return result;
	}

	private void addFilterToList(List<ListItem> list, PoiUIFilter f, boolean isActive) {
		String filterId = f.getFilterId();
		if (!isActive && availableFiltersKeys.contains(filterId) || isActive && !availableFiltersKeys.contains(filterId)) {
			Integer order = poiFiltersOrders.get(filterId);
			if (order == null) {
				order = f.getOrder();
			}
			PoiUIFilterDataObject poiInfo = new PoiUIFilterDataObject();
			poiInfo.filterId = filterId;
			poiInfo.name = f.getName();
			poiInfo.order = order;
			String iconRes = f.getIconId();
			if (iconRes != null && RenderingIcons.containsBigIcon(iconRes)) {
				poiInfo.iconRes = RenderingIcons.getBigIconResourceId(iconRes);
			} else {
				poiInfo.iconRes = R.drawable.mx_special_custom_category;
			}
			poiInfo.isActive = !availableFiltersKeys.contains(filterId);
			list.add(new ListItem(POI, poiInfo));
		}
	}

	private static List<PoiUIFilter> getSortedPoiUiFilters(@NonNull ApplicationMode appMode,
	                                                       @NonNull OsmandApplication app) {
		List<PoiUIFilter> filters = app.getPoiFilters().getSortedPoiFilters(appMode, false);
		//remove custom filter
		for (int i = filters.size() - 1; i >= 0; i--) {
			PoiUIFilter filter = filters.get(i);
			if (filter.isCustomPoiFilter()) {
				filters.remove(filter);
				break;
			}
		}
		return filters;
	}

	public void setUsedOnMap(boolean usedOnMap) {
		this.usedOnMap = usedOnMap;
	}

	public void setResultCallback(OnApplyPoiFiltersState resultCallback) {
		this.resultCallback = resultCallback;
	}

	@NonNull
	protected OsmandApplication requireMyApplication() {
		FragmentActivity activity = requireActivity();
		return (OsmandApplication) activity.getApplication();
	}

	public static boolean isNightMode(@NonNull OsmandApplication app, boolean usedOnMap) {
		return app.getDaynightHelper().isNightMode(usedOnMap);
	}

	@Override
	public void onAppModeChanged(ApplicationMode appMode) {
		this.appMode = appMode;
		updateProfileButton();
		initFiltersOrders(requireMyApplication(), false);
	}

	public class PoiUIFilterDataObject {
		String filterId;
		String name;
		int iconRes;
		int order;
		boolean isActive;

		public void toggleActive() {
			isActive = !isActive;
		}
	}

	protected class ControlButton {
		private final String title;
		private final int iconRes;
		private final View.OnClickListener listener;

		public ControlButton(String title, int iconRes, View.OnClickListener listener) {
			this.title = title;
			this.iconRes = iconRes;
			this.listener = listener;
		}
	}

	protected enum ItemType {
		DESCRIPTION,
		POI,
		HEADER,
		DIVIDER,
		SPACE,
		BUTTON
	}

	private class ListItem {
		ItemType type;
		Object value;

		public ListItem(ItemType type, Object value) {
			this.type = type;
			this.value = value;
		}
	}

	private class EditPoiFiltersAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
			implements ReorderItemTouchHelperCallback.OnItemMoveCallback {

		private final OsmandApplication app;
		private final UiUtilities uiUtilities;
		private final PoiFiltersHelper poiHelper;

		private List<ListItem> items = new ArrayList<>();
		private final boolean nightMode;
		private PoiAdapterListener listener;

		public EditPoiFiltersAdapter(OsmandApplication app, boolean nightMode) {
			setHasStableIds(true);
			this.app = app;
			this.nightMode = nightMode;
			uiUtilities = app.getUIUtilities();
			poiHelper = app.getPoiFilters();
		}

		@NonNull
		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewTypeId) {
			Context ctx = parent.getContext();
			LayoutInflater inflater = UiUtilities.getInflater(ctx, nightMode);
			ItemType type = viewTypeId < ItemType.values().length ? ItemType.values()[viewTypeId] : SPACE;
			View itemView;
			switch (type) {
				case POI:
					itemView = inflater.inflate(R.layout.order_poi_list_item, parent, false);
					return new PoiViewHolder(itemView);
				case SPACE:
					itemView = new View(ctx);
					return new SpaceViewHolder(itemView);
				case BUTTON:
					itemView = inflater.inflate(R.layout.preference_button, parent, false);
					return new ButtonViewHolder(itemView);
				case HEADER:
					itemView = inflater.inflate(R.layout.preference_category_with_descr, parent, false);
					return new HeaderViewHolder(itemView);
				case DIVIDER:
					itemView = inflater.inflate(R.layout.divider, parent, false);
					return new DividerViewHolder(itemView);
				case DESCRIPTION:
					itemView = inflater.inflate(R.layout.bottom_sheet_item_description_long, parent, false);
					return new DescriptionViewHolder(itemView);
				default:
					throw new IllegalArgumentException("Unsupported view type");
			}
		}

		@SuppressLint("ClickableViewAccessibility")
		@Override
		public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
			ListItem item = items.get(position);
			boolean nightMode = isNightMode(app, usedOnMap);
			int activeColorResId = ColorUtilities.getActiveColorId(nightMode);
			if (holder instanceof PoiViewHolder) {
				PoiViewHolder h = (PoiViewHolder) holder;
				PoiUIFilterDataObject poiInfo = (PoiUIFilterDataObject) item.value;
				int osmandOrangeColorResId = nightMode ? R.color.osmand_orange_dark : R.color.osmand_orange;
				boolean isActive = poiInfo.isActive;
				h.title.setText(poiInfo.name);
				int padding = (int) getResources().getDimension(R.dimen.content_padding);
				int paddingSmall = (int) getResources().getDimension(R.dimen.content_padding_small);
				h.title.setPadding(isActive ? 0 : padding, paddingSmall, paddingSmall, padding);
				boolean userFilter = poiInfo.filterId.startsWith(USER_PREFIX);
				int iconRes = QuickSearchListItem.getCustomFilterIconRes(poiHelper.getFilterById(poiInfo.filterId));
				h.icon.setImageDrawable(uiUtilities.getIcon(userFilter ? iconRes : poiInfo.iconRes, osmandOrangeColorResId));
				h.moveIcon.setVisibility(isActive ? View.VISIBLE : View.GONE);
				h.actionIcon.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						int pos = holder.getAdapterPosition();
						if (listener != null && pos != RecyclerView.NO_POSITION) {
							listener.onButtonClicked(pos);
						}
					}
				});
				if (isActive) {
					h.actionIcon.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_action_remove, R.color.color_osm_edit_delete));
					h.moveIcon.setOnTouchListener((view, event) -> {
						if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
							listener.onDragStarted(holder);
						}
						return false;
					});
				} else {
					h.actionIcon.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_action_add, R.color.color_osm_edit_create));
				}
				h.actionDelete.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_action_delete_item, R.color.color_osm_edit_delete));
				h.actionDelete.setVisibility(userFilter ? View.VISIBLE : View.GONE);
				h.actionDelete.setOnClickListener(view -> {
					int pos = holder.getAdapterPosition();
					if (listener != null && pos != RecyclerView.NO_POSITION) {
						listener.onDeleteClicked(pos);
					}
				});
			} else if (holder instanceof SpaceViewHolder) {
				float space = (float) item.value;
				((SpaceViewHolder) holder).setSpace((int) space);
			} else if (holder instanceof ButtonViewHolder) {
				ControlButton buttonInfo = (ControlButton) item.value;
				ButtonViewHolder h = (ButtonViewHolder) holder;
				h.buttonView.setOnClickListener(buttonInfo.listener);
				h.icon.setImageDrawable(uiUtilities.getIcon(buttonInfo.iconRes, activeColorResId));
				h.title.setText(buttonInfo.title);
				Drawable drawable = UiUtilities.getColoredSelectableDrawable(app, ContextCompat.getColor(app, activeColorResId), 0.3f);
				AndroidUtils.setBackground(h.buttonView, drawable);
			} else if (holder instanceof HeaderViewHolder) {
				String header = (String) item.value;
				((HeaderViewHolder) holder).tvTitle.setText(header);
			} else if (holder instanceof DescriptionViewHolder) {
				String description = (String) item.value;
				((DescriptionViewHolder) holder).tvDescription.setText(description);
			}
		}

		public void setListener(PoiAdapterListener listener) {
			this.listener = listener;
		}

		public void setItems(List<ListItem> items) {
			this.items = items;
			notifyDataSetChanged();
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
		public boolean onItemMove(int from, int to) {
			Object itemFrom = items.get(from).value;
			Object itemTo = items.get(to).value;
			if (itemFrom instanceof PoiUIFilterDataObject && itemTo instanceof PoiUIFilterDataObject) {
				isChanged = true;
				orderModified = true;
				PoiUIFilterDataObject poiFrom = (PoiUIFilterDataObject) itemFrom;
				PoiUIFilterDataObject poiTo = (PoiUIFilterDataObject) itemTo;

				int orderFrom = poiFrom.order;
				int orderTo = poiTo.order;

				poiFrom.order = orderTo;
				poiTo.order = orderFrom;

				poiFiltersOrders.put(poiFrom.filterId, orderTo);
				poiFiltersOrders.put(poiTo.filterId, orderFrom);

				Collections.swap(items, from, to);
				notifyItemMoved(from, to);
				return true;
			}
			return false;
		}

		@Override
		public long getItemId(int position) {
			ListItem item = items.get(position);
			if (item.value instanceof PoiUIFilterDataObject) {
				return ((PoiUIFilterDataObject) item.value).filterId.hashCode();
			} else if (item.value instanceof ControlButton) {
				return ((ControlButton) item.value).title.hashCode();
			} else if (item.value != null) {
				return item.value.hashCode();
			}
			return item.hashCode();
		}

		@Override
		public void onItemDismiss(@NonNull ViewHolder holder) {
			listener.onDragOrSwipeEnded(holder);
		}

		private class DividerViewHolder extends RecyclerView.ViewHolder implements ReorderItemTouchHelperCallback.UnmovableItem {

			public DividerViewHolder(View itemView) {
				super(itemView);
			}

			@Override
			public boolean isMovingDisabled() {
				return true;
			}
		}

		private class HeaderViewHolder extends RecyclerView.ViewHolder implements ReorderItemTouchHelperCallback.UnmovableItem {
			private final TextView tvTitle;
			private final TextView tvDescription;

			public HeaderViewHolder(View itemView) {
				super(itemView);
				tvTitle = itemView.findViewById(android.R.id.title);
				tvDescription = itemView.findViewById(android.R.id.summary);
				tvDescription.setVisibility(View.GONE);
			}

			@Override
			public boolean isMovingDisabled() {
				return true;
			}
		}

		private class ButtonViewHolder extends RecyclerView.ViewHolder implements ReorderItemTouchHelperCallback.UnmovableItem {

			private final View buttonView;
			private final ImageView icon;
			private final TextView title;

			public ButtonViewHolder(View itemView) {
				super(itemView);
				buttonView = itemView;
				icon = itemView.findViewById(android.R.id.icon);
				title = itemView.findViewById(android.R.id.title);
			}

			@Override
			public boolean isMovingDisabled() {
				return true;
			}
		}

		private class SpaceViewHolder extends RecyclerView.ViewHolder implements ReorderItemTouchHelperCallback.UnmovableItem {

			View space;

			public SpaceViewHolder(View itemView) {
				super(itemView);
				space = itemView;
			}

			public void setSpace(int hSpace) {
				ViewGroup.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, hSpace);
				space.setLayoutParams(lp);
			}

			@Override
			public boolean isMovingDisabled() {
				return true;
			}
		}

		private class PoiViewHolder extends RecyclerView.ViewHolder implements ReorderItemTouchHelperCallback.UnmovableItem {

			private final TextView title;
			private final ImageView icon;
			private final ImageView actionIcon;
			private final ImageView actionDelete;
			private final ImageView moveIcon;

			public PoiViewHolder(View itemView) {
				super(itemView);
				title = itemView.findViewById(R.id.title);
				actionIcon = itemView.findViewById(R.id.action_icon);
				actionDelete = itemView.findViewById(R.id.action_delete);
				icon = itemView.findViewById(R.id.icon);
				moveIcon = itemView.findViewById(R.id.move_icon);
			}

			@Override
			public boolean isMovingDisabled() {
				int position = getAdapterPosition();
				if (position != RecyclerView.NO_POSITION) {
					ListItem item = items.get(position);
					if (item.value instanceof PoiUIFilterDataObject) {
						PoiUIFilterDataObject pdo = (PoiUIFilterDataObject) item.value;
						return !pdo.isActive;
					}
				}
				return false;
			}
		}

		private class DescriptionViewHolder extends RecyclerView.ViewHolder implements ReorderItemTouchHelperCallback.UnmovableItem {

			private final TextView tvDescription;

			public DescriptionViewHolder(View itemView) {
				super(itemView);
				tvDescription = itemView.findViewById(R.id.description);
			}

			@Override
			public boolean isMovingDisabled() {
				return true;
			}
		}
	}

	public interface PoiAdapterListener {

		void onDragStarted(RecyclerView.ViewHolder holder);

		void onDragOrSwipeEnded(RecyclerView.ViewHolder holder);

		void onButtonClicked(int position);

		void onDeleteClicked(int position);
	}

	public interface OnApplyPoiFiltersState {
		void onApplyPoiFiltersState(ApplicationMode mode, boolean stateChanged);

		void onCustomFiltersDeleted();
	}
}
