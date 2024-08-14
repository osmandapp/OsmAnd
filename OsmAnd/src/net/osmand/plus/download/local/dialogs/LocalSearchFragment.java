package net.osmand.plus.download.local.dialogs;

import static net.osmand.plus.download.local.LocalItemType.MAP_DATA;

import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.Collator;
import net.osmand.OsmAndCollator;
import net.osmand.plus.R;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.local.BaseLocalItem;
import net.osmand.plus.download.local.CategoryType;
import net.osmand.plus.download.local.LocalCategory;
import net.osmand.plus.download.local.LocalGroup;
import net.osmand.plus.download.local.LocalItem;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.download.local.LocalItemUtils;
import net.osmand.plus.download.local.dialogs.LocalItemsAdapter.LocalItemListener;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.enums.LocalSortMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.tools.SimpleTextWatcher;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class LocalSearchFragment extends LocalBaseFragment implements LocalItemListener {

	public static final String TAG = LocalSearchFragment.class.getSimpleName();

	private static final String ITEM_TYPE_KEY = "item_type_key";
	private static final String SEARCH_TEXT_KEY = "search_text_key";

	private LocalItemType type;
	private LocalSearchAdapter adapter;

	private String searchText;
	private EditText searchEditText;
	private ProgressBar progressBar;
	private ImageButton clearButton;

	@Override
	@ColorRes
	public int getStatusBarColorId() {
		return ColorUtilities.getStatusBarColorId(nightMode);
	}

	@Nullable
	@Override
	public Map<CategoryType, LocalCategory> getCategories() {
		Fragment fragment = getTargetFragment();
		if (fragment instanceof LocalBaseFragment) {
			return ((LocalBaseFragment) fragment).getCategories();
		}
		return null;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null) {
			searchText = savedInstanceState.getString(SEARCH_TEXT_KEY);
		}
		Bundle args = getArguments();
		if (args != null) {
			type = AndroidUtils.getSerializable(args, ITEM_TYPE_KEY, LocalItemType.class);
			if (searchText == null) {
				searchText = args.getString(SEARCH_TEXT_KEY);
			}
		}
		if (searchText == null) {
			searchText = "";
		}
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		updateNightMode();
		View view = themedInflater.inflate(R.layout.local_search_fragment, container, false);

		setupToolbar(view);
		setupSearchView(view);
		setupRecyclerView(view);
		updateAdapter();

		return view;
	}

	private void setupToolbar(@NonNull View view) {
		int color = ColorUtilities.getActiveButtonsAndLinksTextColorId(nightMode);
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		toolbar.setNavigationIcon(getIcon(AndroidUtils.getNavigationIconResId(app), color));
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(v -> dismiss());
	}

	private void setupRecyclerView(@NonNull View view) {
		adapter = new LocalSearchAdapter(app, this, nightMode);

		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
		recyclerView.setAdapter(adapter);
	}

	private void setupSearchView(@NonNull View view) {
		int activeTextColor = ColorUtilities.getActiveButtonsAndLinksTextColor(app, nightMode);
		searchEditText = view.findViewById(R.id.searchEditText);
		searchEditText.setHint(R.string.poi_filter_by_name);
		searchEditText.setTextColor(activeTextColor);

		int hintColorId = nightMode ? R.color.searchbar_tab_inactive_dark : R.color.inactive_item_orange;
		searchEditText.setHintTextColor(ContextCompat.getColor(app, hintColorId));

		progressBar = view.findViewById(R.id.searchProgressBar);
		clearButton = view.findViewById(R.id.clearButton);
		clearButton.setColorFilter(activeTextColor);
		clearButton.setVisibility(View.GONE);

		searchEditText.addTextChangedListener(new SimpleTextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				updateSearchText(s.toString());
			}
		});
		clearButton.setOnClickListener(v -> {
			if (searchEditText.getText().length() == 0) {
				dismiss();
			} else {
				searchEditText.setText("");
			}
		});
		searchEditText.requestFocus();
	}

	public void updateSearchText(String searchText) {
		this.searchText = searchText;
		LocalSearchFilter filter = (LocalSearchFilter) adapter.getFilter();
		filter.filter(searchText);
	}

	public void updateContent() {
		updateAdapter();
	}

	private void updateAdapter() {
		adapter.setItems(getSortedItems());
	}

	@NonNull
	private List<BaseLocalItem> getSortedItems() {
		List<BaseLocalItem> items = new ArrayList<>();

		Map<CategoryType, LocalCategory> categories = getCategories();
		if (!Algorithms.isEmpty(categories)) {
			if (type != null) {
				LocalCategory category = categories.get(type.getCategoryType());
				LocalGroup group = category != null ? category.getGroups().get(type) : null;
				if (group != null) {
					items.addAll(group.getItems());
				}
			} else {
				for (LocalCategory category : categories.values()) {
					for (LocalGroup group : category.getGroups().values()) {
						items.addAll(group.getItems());
					}
				}
			}
		}
		sortItems(items);
		return items;
	}

	private void sortItems(@NonNull List<BaseLocalItem> items) {
		if (type == MAP_DATA) {
			LocalSortMode sortMode = LocalItemUtils.getSortModePref(app, type).get();
			Collections.sort(items, new LocalItemsComparator(app, sortMode));
		} else {
			Collator collator = OsmAndCollator.primaryCollator();
			Collections.sort(items, (o1, o2) -> collator.compare(o1.getName(app).toString(), o2.getName(app).toString()));
		}
	}

	public void showProgressBar() {
		updateClearButtonVisibility(false);
		progressBar.setVisibility(View.VISIBLE);
	}

	public void hideProgressBar() {
		updateClearButtonVisibility(true);
		progressBar.setVisibility(View.GONE);
	}

	private void updateClearButtonVisibility(boolean show) {
		if (show) {
			clearButton.setVisibility(searchEditText.length() > 0 ? View.VISIBLE : View.GONE);
		} else {
			clearButton.setVisibility(View.GONE);
		}
	}

	private void dismiss() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			activity.onBackPressed();
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		if (!Algorithms.isEmpty(searchText)) {
			searchEditText.setText(searchText);
		}
		searchEditText.requestFocus();
		AndroidUtils.showSoftKeyboard(requireActivity(), searchEditText);
		AndroidUiHelper.updateActionBarVisibility(getDownloadActivity(), false);
	}

	@Override
	public void onPause() {
		super.onPause();
		AndroidUiHelper.updateActionBarVisibility(getDownloadActivity(), true);
	}

	@Override
	public boolean itemUpdateAvailable(@NonNull LocalItem item) {
		return getItemsToUpdate().containsKey(item.getFile().getName());
	}

	@Override
	public void onItemSelected(@NonNull BaseLocalItem item) {
		FragmentManager manager = getFragmentManager();
		if (manager != null) {
			LocalItemFragment.showInstance(manager, item, this);
		}
	}

	@Override
	public void onItemOptionsSelected(@NonNull BaseLocalItem item, @NonNull View view) {
		DownloadActivity activity = getDownloadActivity();
		if (activity != null) {
			ItemMenuProvider menuProvider = new ItemMenuProvider(activity, this);
			menuProvider.setItem(item);
			menuProvider.setColorId(ColorUtilities.getDefaultIconColorId(nightMode));
			menuProvider.showMenu(view);
		}
	}

	public static void showInstance(@NonNull FragmentManager manager, @Nullable LocalItemType type,
	                                @Nullable String searchText, @Nullable Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			Bundle args = new Bundle();
			args.putString(SEARCH_TEXT_KEY, searchText);
			args.putSerializable(ITEM_TYPE_KEY, type);

			LocalSearchFragment fragment = new LocalSearchFragment();
			fragment.setArguments(args);
			fragment.setTargetFragment(target, 0);

			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}
