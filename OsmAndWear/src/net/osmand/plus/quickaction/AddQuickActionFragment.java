package net.osmand.plus.quickaction;

import static net.osmand.plus.quickaction.AddQuickActionsAdapter.DEFAULT_MODE;

import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.base.dialog.interfaces.dialog.IAskDismissDialog;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.quickaction.AddQuickActionsAdapter.ItemClickListener;
import net.osmand.plus.quickaction.controller.AddQuickActionController;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.tools.SimpleTextWatcher;
import net.osmand.util.Algorithms;

public class AddQuickActionFragment extends BaseOsmAndFragment implements ItemClickListener, IAskDismissDialog {

	public static final String TAG = AddQuickActionFragment.class.getSimpleName();

	public static final String QUICK_ACTION_BUTTON_KEY = "quick_action_button_key";
	public static final String QUICK_ACTION_SEARCH_KEY = "quick_action_search_key";
	public static final String QUICK_ACTION_SEARCH_MODE_KEY = "quick_action_search_mode_key";

	private AddQuickActionsAdapter adapter;
	private ImageButton clearSearchQuery;
	private EditText searchEditText;
	private ImageView backButton;
	private TextView title;
	private ImageView searchButton;

	private AddQuickActionController controller;
	private boolean searchMode = false;
	private OnBackPressedCallback backPressedCallback;

	public boolean getContentStatusBarNightMode() {
		return nightMode;
	}

	@Override
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return ColorUtilities.getListBgColorId(nightMode);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		controller = AddQuickActionController.getExistedInstance(app);
		if (controller == null) {
			dismiss();
		} else {
			controller.registerDialog(TAG, this);
			if (savedInstanceState != null) {
				searchMode = savedInstanceState.getBoolean(QUICK_ACTION_SEARCH_MODE_KEY, false);
			}
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = themedInflater.inflate(R.layout.fragment_add_quick_action, container, false);
		if (Build.VERSION.SDK_INT < 30) {
			AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);
		}
		setupSearchBar(view);
		setupToolbar(view);
		setupContent(view, savedInstanceState);
		setupOnBackPressedCallback();

		return view;
	}

	private void setupOnBackPressedCallback() {
		backPressedCallback = new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				if (searchMode) {
					setSearchMode(false);
				} else {
					this.setEnabled(false);
					FragmentActivity activity = getActivity();
					if (activity != null) {
						activity.onBackPressed();
					}
				}
			}
		};
		requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), backPressedCallback);
	}

	private void setupToolbar(@NonNull View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		title = toolbar.findViewById(R.id.toolbar_title);
		title.setText(R.string.dialog_add_action_title);

		backButton = toolbar.findViewById(R.id.back_button);
		backButton.setOnClickListener(v -> backPressedCallback.handleOnBackPressed());

		searchButton = toolbar.findViewById(R.id.search_button);
		searchButton.setOnClickListener(v -> setSearchMode(true));
		searchButton.setImageDrawable(getContentIcon(R.drawable.ic_action_search_dark));
		updateToolbar();
	}

	private void setupSearchBar(@NonNull View view) {
		View searchContainer = view.findViewById(R.id.search_container);
		clearSearchQuery = searchContainer.findViewById(R.id.clearButton);
		clearSearchQuery.setVisibility(View.GONE);
		clearSearchQuery.setImageDrawable(getContentIcon(R.drawable.ic_action_cancel));
		searchEditText = searchContainer.findViewById(R.id.searchEditText);
		searchEditText.setHint(null);
		searchEditText.addTextChangedListener(new SimpleTextWatcher() {
			@Override
			public void afterTextChanged(Editable query) {
				adapter.filter(query.toString());
				AndroidUiHelper.updateVisibility(clearSearchQuery, query.length() > 0);
			}
		});
		clearSearchQuery.setOnClickListener((v) -> resetSearchQuery());
	}

	private void resetSearchQuery() {
		adapter.filter(null);
		searchEditText.setText(null);
	}

	private void setSearchMode(boolean searchMode) {
		this.searchMode = searchMode;
		if (!searchMode) {
			resetSearchQuery();
		} else {
			backPressedCallback.setEnabled(true);
		}
		updateToolbar();
		updateAdapter();
	}

	private void updateToolbar() {
		backButton.setImageDrawable(getContentIcon(searchMode ? AndroidUtils.getNavigationIconResId(app) : R.drawable.ic_action_close));
		backButton.setContentDescription(app.getString(searchMode ? R.string.access_shared_string_navigate_up : R.string.shared_string_close));
		AndroidUiHelper.setVisibility(searchMode ? View.GONE : View.VISIBLE, searchButton, title);
		AndroidUiHelper.setVisibility(searchMode ? View.VISIBLE : View.GONE, searchEditText);
		if (searchMode) {
			searchEditText.requestFocus();
			AndroidUtils.showSoftKeyboard(requireActivity(), searchEditText);
		} else {
			AndroidUtils.hideSoftKeyboard(requireActivity(), searchEditText);
			AndroidUiHelper.updateVisibility(clearSearchQuery, false);
		}
	}

	private void setupContent(@NonNull View view, @Nullable Bundle savedInstanceState) {
		adapter = new AddQuickActionsAdapter(app, requireActivity(), this, nightMode);
		adapter.setAdapterMode(DEFAULT_MODE);
		adapter.setMap(controller.getAdapterItems());
		RecyclerView recyclerView = view.findViewById(R.id.content_list);
		recyclerView.setLayoutManager(new LinearLayoutManager(app));
		recyclerView.setAdapter(adapter);
		updateAdapter();

		String searchQuery = savedInstanceState != null ? savedInstanceState.getString(QUICK_ACTION_SEARCH_KEY) : null;
		if (searchQuery != null) {
			adapter.filter(searchQuery);
		}
	}

	private void updateAdapter() {
		adapter.setSearchMode(searchMode);
	}

	private void dismiss() {
		FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
		if (!fragmentManager.isStateSaved()) {
			fragmentManager.popBackStackImmediate(TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		FragmentActivity activity = getActivity();
		if (activity != null && !activity.isChangingConfigurations()) {
			controller.unregisterDialog(TAG);
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		String searchQuery = adapter.getSearchQuery();
		if (!Algorithms.isEmpty(searchQuery)) {
			outState.putString(QUICK_ACTION_SEARCH_KEY, adapter.getSearchQuery());
		}
		outState.putBoolean(QUICK_ACTION_SEARCH_MODE_KEY, searchMode);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onItemClick(@NonNull QuickActionType quickActionType) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			FragmentManager manager = activity.getSupportFragmentManager();
			if (quickActionType.getId() != 0) {
				CreateEditActionDialog.showInstance(manager, quickActionType.getId());
			} else {
				AddCategoryQuickActionFragment.showInstance(manager, quickActionType.getCategory());
			}
		}
	}

	@Override
	public void onAskDismissDialog(@NonNull String processId) {
		dismiss();
	}

	@Override
	public void onResume() {
		super.onResume();
		FragmentActivity activity = getActivity();
		if (activity instanceof MapActivity) {
			((MapActivity) activity).disableDrawer();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		FragmentActivity activity = getActivity();
		if (activity instanceof MapActivity) {
			((MapActivity) activity).enableDrawer();
		}
	}

	public static void showInstance(@NonNull FragmentManager manager) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			AddQuickActionFragment fragment = new AddQuickActionFragment();
			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}

