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
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseFullScreenFragment;
import net.osmand.plus.chooseplan.ChoosePlanFragment;
import net.osmand.plus.chooseplan.OsmAndFeature;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.enums.ThemeUsageContext;
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

public class SearchWidgetsFragment extends BaseFullScreenFragment implements SearchWidgetListener {

	public static final String TAG = SearchWidgetsFragment.class.getSimpleName();

	public static final String KEY_SELECTED_PANEL = "key_selected_panel";
	public static final String KEY_SEARCH_MODE = "key_search_mode";
	public static final int PAYLOAD_SEPARATOR_UPDATE = 1;

	private ApplicationMode selectedAppMode;
	private MapWidgetRegistry widgetRegistry;
	private WidgetIconsHelper iconsHelper;
	private WidgetsPanel selectedPanel;

	private final List<Object> widgetItems = new ArrayList<>();
	private final List<Object> allWidgetItems = new ArrayList<>();
	private SearchWidgetsAdapter adapter;

	private ImageButton actionButton;
	private ImageView backButton;
	private TextView title;
	private EditText editText;
	private FragmentLifecycleCallbacks lifecycleCallbacks;
	private boolean searchMode = false;
	private String searchQuery = "";
	private OnBackPressedCallback onBackPressedCallback;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = (OsmandApplication) requireContext().getApplicationContext();
		widgetRegistry = app.getOsmandMap().getMapLayers().getMapWidgetRegistry();
		settings = app.getSettings();
		nightMode = app.getDaynightHelper().isNightMode(ThemeUsageContext.APP);
		selectedAppMode = settings.getApplicationMode();
		iconsHelper = new WidgetIconsHelper(app, selectedAppMode.getProfileColor(nightMode), nightMode);

		if (savedInstanceState != null) {
			selectedPanel = WidgetsPanel.valueOf(savedInstanceState.getString(KEY_SELECTED_PANEL));
			searchMode = savedInstanceState.getBoolean(KEY_SEARCH_MODE);
		}

		onBackPressedCallback = new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				closeFragment();
			}
		};
	}

	@Override
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return ColorUtilities.getListBgColorId(nightMode);
	}

	@Override
	public boolean getContentStatusBarNightMode() {
		return nightMode;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = inflate(R.layout.fragment_search_widgets, container, false);
		AndroidUtils.addStatusBarPadding21v(requireMapActivity(), view);
		actionButton = view.findViewById(R.id.clearButton);
		backButton = view.findViewById(R.id.back_button);
		title = view.findViewById(R.id.toolbar_title);
		editText = view.findViewById(R.id.searchEditText);

		loadWidgets();

		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		adapter = new SearchWidgetsAdapter(app, selectedAppMode, this, new ArrayList<>(), iconsHelper, nightMode);
		recyclerView.setAdapter(adapter);

		setupButtonListeners();
		setupToolbar();

		lifecycleCallbacks = new FragmentManager.FragmentLifecycleCallbacks() {
			@Override
			public void onFragmentDestroyed(@NonNull FragmentManager fm, @NonNull Fragment f) {
				super.onFragmentDestroyed(fm, f);
				if (isAdded()) {
					Fragment currentFragment = fm.findFragmentById(R.id.fragmentContainer);
					if (currentFragment instanceof SearchWidgetsFragment) {
						onBackPressedCallback.setEnabled(true);
					}
				}
			}

			@Override
			public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
				super.onFragmentResumed(fm, f);
				onBackPressedCallback.setEnabled(false);
				if (editText.hasFocus()) {
					AndroidUtils.hideSoftKeyboard(requireMapActivity(), editText);
				}
			}
		};
		getParentFragmentManager().registerFragmentLifecycleCallbacks(lifecycleCallbacks, false);

		toggleSearchMode(searchMode);

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
				searchQuery = s.toString().toLowerCase().trim();
				updateSearchResults(searchQuery);
			}
		});

		title.setVisibility(View.VISIBLE);
		title.setTextColor(ColorUtilities.getPrimaryTextColor(app, nightMode));
		editText.setVisibility(View.INVISIBLE);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), onBackPressedCallback);
	}

	private void closeFragment() {
		if (!searchMode) {
			requireActivity().getSupportFragmentManager().popBackStack();
		} else {
			toggleSearchMode(false);
		}
	}

	private void setupButtonListeners() {
		backButton.setOnClickListener(v -> {
			AndroidUtils.hideSoftKeyboard(requireActivity(), editText);
			closeFragment();
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
			swapViews(title, editText);

			Drawable actionIcon = app.getUIUtilities().getPaintedIcon(R.drawable.ic_action_close, getIconColor());
			actionButton.setImageDrawable(actionIcon);
			Drawable backIcon = app.getUIUtilities().getPaintedIcon(R.drawable.ic_arrow_back, getIconColor());
			backButton.setImageDrawable(backIcon);

			setWidgetList(true);

			editText.requestFocus();
			AndroidUtils.showSoftKeyboard(requireActivity(), editText);
		} else {
			editText.clearFocus();
			editText.setText("");

			swapViews(editText, title);

			Drawable actionIcon = app.getUIUtilities().getPaintedIcon(R.drawable.ic_action_search_dark, getIconColor());
			actionButton.setImageDrawable(actionIcon);
			Drawable backIcon = app.getUIUtilities().getPaintedIcon(R.drawable.ic_action_close, getIconColor());
			backButton.setImageDrawable(backIcon);

			setWidgetList(false);
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
			List<WidgetType> allWidgetTypes;
			List<MapWidgetInfo> externalWidgets;
			Map<WidgetGroup, List<WidgetType>> groupedWidgets = new HashMap<>();

			allWidgetTypes = listDefaultWidgets(availableWidgets);
			externalWidgets = getExternalWidgets(availableWidgets);


			for (WidgetType widgetType : allWidgetTypes) {
				if (widgetType.getGroup(selectedPanel) != null) {
					groupedWidgets.computeIfAbsent(widgetType.getGroup(selectedPanel), k -> new ArrayList<>()).add(widgetType);
				}
			}
			widgetItems.clear();
			widgetItems.addAll(externalWidgets);
			allWidgetItems.clear();
			allWidgetItems.addAll(allWidgetTypes);
			allWidgetItems.addAll(externalWidgets);

			for (Map.Entry<WidgetGroup, List<WidgetType>> entry : groupedWidgets.entrySet()) {
				GroupItem groupItem = new GroupItem(entry.getKey(), entry.getValue().size());
				allWidgetItems.add(groupItem);
				widgetItems.add(groupItem);
			}
			sortWidgetsItems(app, allWidgetItems);

			for (WidgetType widgetType : allWidgetTypes) {
				if (widgetType.getGroup(selectedPanel) == null) {
					widgetItems.add(widgetType);
				}
			}
			sortWidgetsItems(app, widgetItems);
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

	private void setWidgetList(boolean allWidgets) {
		if (allWidgets) {
			updateWidgetItems(allWidgetItems);
		} else {
			updateWidgetItems(widgetItems);
		}
	}

	private void updateWidgetItems(@NonNull List<Object> items) {
		List<Object> newItems = new ArrayList<>(items);
		SearchWidgetsDiffCallback diffCallback = new SearchWidgetsDiffCallback(adapter.getItems(), newItems);
		DiffResult diffRes = calculateDiff(diffCallback);
		adapter.setItems(newItems);
		diffRes.dispatchUpdatesTo(adapter);
	}

	private void updateSearchResults(@NonNull String query) {
		if (!searchMode) {
			return;
		}

		if (query.isEmpty()) {
			setWidgetList(true);
			return;
		}

		List<Object> searchResults = new ArrayList<>();

		for (Object object : allWidgetItems) {
			if (object instanceof WidgetType widgetType) {
				String widgetTitle = getString(widgetType.titleId).toLowerCase();
				if (widgetTitle.contains(query.toLowerCase())) {
					searchResults.add(widgetType);
				}
			} else if (object instanceof MapWidgetInfo widgetInfo) {
				String widgetTitle = widgetInfo.key.toLowerCase();
				if (widgetTitle.contains(query.toLowerCase())) {
					searchResults.add(widgetInfo);
				}
			} else if (object instanceof GroupItem groupItem) {
				WidgetGroup group = groupItem.group;
				String groupTitle = getString(group.titleId).toLowerCase();

				if (groupTitle.contains(query.toLowerCase())) {
					searchResults.add(groupItem);
				}
			}
		}

		updateWidgetItems(searchResults);
	}

	@Override
	public void widgetSelected(@NonNull WidgetType widgetType) {

		Fragment target = getTargetFragment();
		if (widgetType.isPurchased(app)) {
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
	public void onDestroy() {
		super.onDestroy();
		getParentFragmentManager().unregisterFragmentLifecycleCallbacks(lifecycleCallbacks);
	}

	@Override
	public void externalWidgetSelected(@NonNull MapWidgetInfo widgetInfo) {

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

		FragmentActivity activity = getActivity();
		Fragment target = getTargetFragment();
		if (activity != null && target != null) {
			FragmentManager fragmentManager = activity.getSupportFragmentManager();
			AddWidgetFragment.showGroupDialog(fragmentManager, target,
					selectedAppMode, selectedPanel, group, null);
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(KEY_SELECTED_PANEL, selectedPanel.name());
		outState.putBoolean(KEY_SEARCH_MODE, searchMode);
	}

	private static class SearchWidgetsDiffCallback extends Callback {

		private final List<Object> oldItems;
		private final List<Object> newItems;

		SearchWidgetsDiffCallback(@NonNull List<Object> oldItems, @NonNull List<Object> newItems) {
			this.oldItems = new ArrayList<>(oldItems);
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
			Object oldItem = oldItems.get(oldItemPosition);
			Object newItem = newItems.get(newItemPosition);

			boolean isSameContent = oldItem.equals(newItem);

			boolean wasLastOld = oldItemPosition == oldItems.size() - 1;
			boolean isLastNew = newItemPosition == newItems.size() - 1;

			return isSameContent && wasLastOld == isLastNew;
		}

		@Nullable
		@Override
		public Object getChangePayload(int oldItemPosition, int newItemPosition) {
			boolean wasLastOld = oldItemPosition == oldItems.size() - 1;
			boolean isLastNew = newItemPosition == newItems.size() - 1;

			if (wasLastOld != isLastNew) {
				return PAYLOAD_SEPARATOR_UPDATE;
			}

			return super.getChangePayload(oldItemPosition, newItemPosition);
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
					.commitAllowingStateLoss();
		}
	}
}

