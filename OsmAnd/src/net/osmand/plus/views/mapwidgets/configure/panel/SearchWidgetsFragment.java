package net.osmand.plus.views.mapwidgets.configure.panel;

import static androidx.recyclerview.widget.DiffUtil.*;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.AVAILABLE_MODE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.DEFAULT_MODE;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.chooseplan.ChoosePlanFragment;
import net.osmand.plus.chooseplan.OsmAndFeature;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.WidgetGroup;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.configure.WidgetIconsHelper;
import net.osmand.plus.views.mapwidgets.configure.dialogs.AddWidgetFragment;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

public class SearchWidgetsFragment extends BaseOsmAndFragment implements SearchWidgetListener {

	public static final String TAG = SearchWidgetsFragment.class.getSimpleName();

	private ApplicationMode selectedAppMode;
	private MapWidgetRegistry widgetRegistry;
	private WidgetIconsHelper iconsHelper;
	private WidgetsPanel selectedPanel;

	private List<WidgetType> allWidgetTypes = new ArrayList<>();
	private List<MapWidgetInfo> externalWidgets = new ArrayList<>();
	private final Map<WidgetGroup, List<WidgetType>> allGroupedWidgets = new HashMap<>();
	private SearchWidgetsAdapter adapter;

	private ImageButton actionButton;
	private ImageView backButton;
	private TextView title;
	private EditText editText;
	private Toolbar appBarLayout;

	private boolean searchMode;
	private String searchQuery = "";

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = (OsmandApplication) requireContext().getApplicationContext();
		widgetRegistry = app.getOsmandMap().getMapLayers().getMapWidgetRegistry();
		settings = app.getSettings();
		nightMode = !settings.isLightContent();
		selectedAppMode = settings.getApplicationMode();
		iconsHelper = new WidgetIconsHelper(app, selectedAppMode.getProfileColor(nightMode), nightMode);
	}

	@Override
	public int getStatusBarColorId() {
		if (searchMode) {
			AndroidUiHelper.setStatusBarContentColor(getView(), true);
			return ColorUtilities.getStatusBarColorId(nightMode);
		} else {
			AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
			return ColorUtilities.getStatusBarSecondaryColorId(nightMode);
		}
	}

	@Override
	public boolean getContentStatusBarNightMode() {
		if (searchMode) {
			return true;
		} else {
			return nightMode;
		}
	}

	public int getToolbarColorId() {
		return searchMode
				? ColorUtilities.getAppBarColor(app, nightMode)
				: ColorUtilities.getListBgColor(app, nightMode);
	}

	private void updateToolbarColor() {
		updateStatusBar();

		appBarLayout.setBackground(null);
		appBarLayout.setBackgroundColor(getToolbarColorId());
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = themedInflater.inflate(R.layout.fragment_search_widgets, container, false);
		AndroidUtils.addStatusBarPadding21v(requireMapActivity(), view);
		actionButton = view.findViewById(R.id.clearButton);
		backButton = view.findViewById(R.id.back_button);
		title = view.findViewById(R.id.toolbar_title);
		editText = view.findViewById(R.id.searchEditText);
		appBarLayout = view.findViewById(R.id.toolbar);

		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		adapter = new SearchWidgetsAdapter(app, this, new ArrayList<>(), iconsHelper, nightMode);
		recyclerView.setAdapter(adapter);

		setupButtonListeners();
		setupToolbar();

		toggleSearchMode(false);

		return view;
	}

	private void setupToolbar() {
		editText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {

			}

			@Override
			public void afterTextChanged(Editable s) {
				searchQuery = s.toString().toLowerCase();
				updateSearchResults(searchQuery);
			}
		});

		title.setVisibility(View.VISIBLE);
		title.setTextColor(ColorUtilities.getPrimaryTextColor(app, nightMode));
		editText.setVisibility(View.INVISIBLE);
		editText.setTextColor(ColorUtilities.getActiveButtonsAndLinksTextColor(app, nightMode));
		editText.setHintTextColor(ColorUtilities.getActiveButtonsAndLinksTextColor(app, nightMode));
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				closeFragment(false);
			}
		});
	}

	private void closeFragment(boolean forceClose) {
		if (!searchMode || forceClose) {
			requireActivity().getSupportFragmentManager().popBackStack();
		} else {
			toggleSearchMode(false);
		}
	}

	private void setupButtonListeners() {
		backButton.setOnClickListener(v -> {
			AndroidUtils.hideSoftKeyboard(requireActivity(), editText);
			closeFragment(false);
		});

		actionButton.setOnClickListener(v -> {
			if (searchMode) {
				editText.setText("");
				updateSearchResults("");
			} else {
				toggleSearchMode(true);
			}
		});
	}

	private void toggleSearchMode(boolean searchMode) {
		this.searchMode = searchMode;
		if (searchMode) {
			updateToolbarColor();
			swapViews(title, editText);

			Drawable actionIcon = app.getUIUtilities().getPaintedIcon(R.drawable.ic_action_close, getIconColor());
			actionButton.setImageDrawable(actionIcon);
			Drawable backIcon = app.getUIUtilities().getPaintedIcon(R.drawable.ic_arrow_back, getIconColor());
			backButton.setImageDrawable(backIcon);

			loadAllWidgetsForSearch();

			editText.requestFocus();
			AndroidUtils.showSoftKeyboard(requireActivity(), editText);
		} else {
			editText.clearFocus();

			updateToolbarColor();
			swapViews(editText, title);

			Drawable actionIcon = app.getUIUtilities().getPaintedIcon(R.drawable.ic_action_search_dark, getIconColor());
			actionButton.setImageDrawable(actionIcon);
			Drawable backIcon = app.getUIUtilities().getPaintedIcon(R.drawable.ic_action_close, getIconColor());
			backButton.setImageDrawable(backIcon);

			loadWidgets();
		}
	}

	private void swapViews(@NonNull View fromView, @NonNull View toView) {
		fromView.setVisibility(View.INVISIBLE);
		toView.setVisibility(View.VISIBLE);
	}

	@ColorInt
	private int getIconColor() {
		return searchMode
				? ColorUtilities.getActiveButtonsAndLinksTextColor(app, nightMode)
				: ColorUtilities.getDefaultIconColor(app, nightMode);
	}

	@NonNull
	protected MapActivity requireMapActivity() {
		return ((MapActivity) requireActivity());
	}

	@NonNull
	private List<WidgetType> listDefaultWidgets(@NonNull Set<MapWidgetInfo> widgets) {
		Map<Integer, WidgetType> defaultWidgets = new TreeMap<>();
		for (MapWidgetInfo widgetInfo : widgets) {
			WidgetType widgetType = widgetInfo.getWidgetType();
			defaultWidgets.put(widgetType.ordinal(), widgetType);
		}
		return new ArrayList<>(defaultWidgets.values());
	}

	private void loadWidgets() {
		int filter = AVAILABLE_MODE | DEFAULT_MODE;

		Set<MapWidgetInfo> availableWidgets = widgetRegistry.getWidgetsForPanel(
				requireMapActivity(), selectedAppMode, filter, Collections.singletonList(selectedPanel));
		boolean hasAvailableWidgets = !Algorithms.isEmpty(availableWidgets);

		if (hasAvailableWidgets) {
			allWidgetTypes = listDefaultWidgets(availableWidgets);
			externalWidgets = getExternalWidgets(availableWidgets);
			allGroupedWidgets.clear();

			List<Object> sortedItems = new ArrayList<>();
			Map<WidgetGroup, List<WidgetType>> groupedWidgets = new HashMap<>();

			for (WidgetType widgetType : allWidgetTypes) {
				if (widgetType.getGroup() != null) {
					groupedWidgets.computeIfAbsent(widgetType.getGroup(), k -> new ArrayList<>()).add(widgetType);
				} else {
					sortedItems.add(widgetType);
				}
			}

			allGroupedWidgets.putAll(groupedWidgets);

			for (Map.Entry<WidgetGroup, List<WidgetType>> entry : groupedWidgets.entrySet()) {
				sortedItems.add(new GroupItem(entry.getKey(), entry.getValue().size()));
			}

			sortedItems.addAll(externalWidgets);
			sortWidgetsItems(app, sortedItems);
			updateWidgetItems(sortedItems);
		}
	}

	@Nullable
	public static String getListItemName(@NonNull OsmandApplication app, @NonNull Object item) {
		if (item instanceof WidgetType widgetType) {
			return app.getString(widgetType.titleId);
		} else if (item instanceof MapWidgetInfo widgetInfo) {
			return widgetInfo.getTitle(app);
		} else if (item instanceof GroupItem groupItem) {
			return app.getString(groupItem.group().titleId);
		}
		return null;
	}

	public static void sortWidgetsItems(@NonNull OsmandApplication app, @NonNull List<?> widgets) {
		widgets.sort((o1, o2) -> {
			String firstName = getListItemName(app, o1);
			String secondName = getListItemName(app, o2);
			if (firstName != null && secondName != null) {
				return firstName.compareTo(secondName);
			}
			return 0;
		});
	}

	@NonNull
	private List<MapWidgetInfo> getExternalWidgets(Set<MapWidgetInfo> availableWidgets) {
		List<MapWidgetInfo> externalWidgets = new ArrayList<>();
		for (MapWidgetInfo widgetInfo : availableWidgets) {
			if (widgetInfo.isExternal()) {
				externalWidgets.add(widgetInfo);
			}
		}
		return externalWidgets;
	}

	private void loadAllWidgetsForSearch() {
		List<Object> searchItems = new ArrayList<>();

		searchItems.addAll(allWidgetTypes);
		searchItems.addAll(externalWidgets);

		for (Map.Entry<WidgetGroup, List<WidgetType>> entry : allGroupedWidgets.entrySet()) {
			searchItems.add(new GroupItem(entry.getKey(), entry.getValue().size()));
		}

		sortWidgetsItems(app, searchItems);

		updateWidgetItems(searchItems);
	}

	private void updateWidgetItems(@NonNull List<Object> newItems) {
		SearchWidgetsDiffCallback diffCallback = new SearchWidgetsDiffCallback(adapter.getItems(), newItems);
		DiffResult diffRes = calculateDiff(diffCallback);
		adapter.setItems(newItems);
		diffRes.dispatchUpdatesTo(adapter);
	}

	private void updateSearchResults(@NonNull String query) {
		if (!searchMode) {
			return;
		}

		List<Object> searchResults = new ArrayList<>();

		if (query.isEmpty()) {
			loadAllWidgetsForSearch();
			return;
		}

		for (WidgetType widgetType : allWidgetTypes) {
			String widgetTitle = getString(widgetType.titleId).toLowerCase();
			if (widgetTitle.contains(query.toLowerCase())) {
				searchResults.add(widgetType);
			}
		}

		for (MapWidgetInfo widgetInfo : externalWidgets) {
			String widgetTitle = widgetInfo.key.toLowerCase();
			if (widgetTitle.contains(query.toLowerCase())) {
				searchResults.add(widgetInfo);
			}
		}

		for (Map.Entry<WidgetGroup, List<WidgetType>> entry : allGroupedWidgets.entrySet()) {
			WidgetGroup group = entry.getKey();
			String groupTitle = getString(group.titleId).toLowerCase();

			if (groupTitle.contains(query.toLowerCase())) {
				searchResults.add(new GroupItem(group, entry.getValue().size()));
			}
		}

		sortWidgetsItems(app, searchResults);

		updateWidgetItems(searchResults);
	}

	@Override
	public void widgetSelected(@NonNull WidgetType widgetType) {
		Fragment target = getTargetFragment();
		if (widgetType.isPurchased(app)) {
			closeFragment(true);

			if (target instanceof AddWidgetFragment.AddWidgetListener) {
				((AddWidgetFragment.AddWidgetListener) target).onWidgetSelectedToAdd(widgetType.id, selectedPanel, true);
			}
		} else {
			OsmAndFeature feature = widgetType.isOBDWidget()
					? OsmAndFeature.VEHICLE_METRICS
					: OsmAndFeature.ADVANCED_WIDGETS;
			ChoosePlanFragment.showInstance(requireMapActivity(), feature);
		}
	}

	@Override
	public void externalWidgetSelected(@NonNull MapWidgetInfo widgetInfo) {
		closeFragment(true);

		FragmentActivity activity = getActivity();
		Fragment target = getTargetFragment();

		String externalProviderPackage = widgetInfo.getExternalProviderPackage();
		if (activity != null && !Algorithms.isEmpty(externalProviderPackage) && target != null) {
			FragmentManager fragmentManager = activity.getSupportFragmentManager();
			AddWidgetFragment.showExternalWidgetDialog(fragmentManager, target,
					selectedAppMode, selectedPanel, widgetInfo.key, externalProviderPackage, null);
		}
	}

	@Override
	public void groupSelected(@NonNull WidgetGroup group) {
		closeFragment(true);

		FragmentActivity activity = getActivity();
		Fragment target = getTargetFragment();
		if (activity != null && target != null) {
			FragmentManager fragmentManager = activity.getSupportFragmentManager();
			AddWidgetFragment.showGroupDialog(fragmentManager, target,
					selectedAppMode, selectedPanel, group, null);
		}
	}

	private static class SearchWidgetsDiffCallback extends Callback {

		private final List<Object> oldItems;
		private final List<Object> newItems;

		SearchWidgetsDiffCallback(@NonNull List<Object> oldItems, @NonNull List<Object> newItems) {
			this.oldItems = oldItems;
			this.newItems = newItems;
		}

		@Override
		public int getOldListSize() {
			return oldItems.size();
		}

		@Override
		public int getNewListSize() {
			return newItems.size();
		}

		@Override
		public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
			Object oldItem = oldItems.get(oldItemPosition);
			Object newItem = newItems.get(newItemPosition);

			if (oldItem instanceof WidgetType && newItem instanceof WidgetType) {
				return ((WidgetType) oldItem).ordinal() == ((WidgetType) newItem).ordinal();
			} else if (oldItem instanceof GroupItem && newItem instanceof GroupItem) {
				return ((GroupItem) oldItem).group() == ((GroupItem) newItem).group();
			} else if (oldItem instanceof MapWidgetInfo && newItem instanceof MapWidgetInfo) {
				return Objects.equals(((MapWidgetInfo) oldItem).key, ((MapWidgetInfo) newItem).key);
			}
			return false;
		}

		@Override
		public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
			return areItemsTheSame(oldItemPosition, newItemPosition);
		}
	}

	public record GroupItem(WidgetGroup group, int count) {

	}

	public static void showInstance(@NonNull FragmentActivity activity, @NonNull WidgetsPanel selectedPanel, @NonNull Fragment target) {
		FragmentManager manager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			SearchWidgetsFragment fragment = new SearchWidgetsFragment();
			fragment.selectedPanel = selectedPanel;
			fragment.setTargetFragment(target, 0);
			manager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commit();
		}
	}
}

