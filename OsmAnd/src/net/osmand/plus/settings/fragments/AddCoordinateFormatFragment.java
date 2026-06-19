package net.osmand.plus.settings.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.search.SearchBar;
import com.google.android.material.search.SearchView;

import net.osmand.plus.R;
import net.osmand.plus.base.BaseFullScreenDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.coordinates.BuiltInCoordinateFormat;
import net.osmand.plus.settings.coordinates.CoordinateFormat;
import net.osmand.plus.settings.coordinates.CoordinateFormatHelper;
import net.osmand.plus.settings.coordinates.CoordinateFormatIds;
import net.osmand.plus.settings.coordinates.CoordinateFormatSettingsStorage;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.InsetTarget;
import net.osmand.plus.utils.InsetTargetsCollection;
import net.osmand.plus.widgets.tools.SimpleTextWatcher;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class AddCoordinateFormatFragment extends BaseFullScreenDialogFragment {

	public static final String TAG = AddCoordinateFormatFragment.class.getSimpleName();

	public static final String REQUEST_ADD_TO_EDIT = "coordinate_format_add_to_edit";
	public static final String ADD_SCREEN_BACK_STACK_TAG = TAG;

	private static final String ARG_FOCUS_SEARCH = "focus_search";
	private static final String ARG_ADD_TO_EDIT_DRAFT = "add_to_edit_draft";
	private static final String ARG_EXCLUDED_IDS = "excluded_ids";
	private static final String ARG_CLOSE_RESULT_KEY = "close_result_key";
	private static final String STATE_SEARCH_QUERY = "search_query";
	private static final int SOFT_INPUT_MODE_NOT_SET = Integer.MIN_VALUE;

	private CoordinateFormatSettingsStorage formatPreferences;
	private CoordinateFormatHelper coordinateFormatHelper;

	private final List<String> excludedIds = new ArrayList<>();
	private final List<CoordinateFormat> searchItems = new ArrayList<>();

	private LinearLayout contentContainer;
	private SearchBar searchBar;
	private SearchView searchInputView;
	private RecyclerView searchResults;
	private SearchResultsAdapter searchResultsAdapter;

	private String searchQuery = "";
	private String closeResultKey;
	private boolean addToEditDraft;
	private boolean focusSearch;
	private int previousSoftInputMode = SOFT_INPUT_MODE_NOT_SET;

	@Override
	@StyleRes
	protected int getDialogThemeId() {
		return nightMode ? R.style.OsmandMaterialDarkTheme : R.style.OsmandMaterialLightTheme;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		formatPreferences = settings.getCoordinateFormatSettingsStorage();
		coordinateFormatHelper = app.getCoordinateFormatHelper();

		Bundle args = getArguments();
		addToEditDraft = args != null && args.getBoolean(ARG_ADD_TO_EDIT_DRAFT);
		focusSearch = args != null && args.getBoolean(ARG_FOCUS_SEARCH);
		closeResultKey = args != null ? args.getString(ARG_CLOSE_RESULT_KEY) : null;
		ArrayList<String> argExcludedIds = args != null ? args.getStringArrayList(ARG_EXCLUDED_IDS) : null;
		if (addToEditDraft && argExcludedIds != null) {
			excludedIds.addAll(argExcludedIds);
		} else {
			excludedIds.addAll(formatPreferences.getPreferredIds(appMode));
		}
		if (savedInstanceState != null) {
			searchQuery = savedInstanceState.getString(STATE_SEARCH_QUERY, "");
			ArrayList<String> savedExcludedIds = savedInstanceState.getStringArrayList(ARG_EXCLUDED_IDS);
			if (savedExcludedIds != null) {
				excludedIds.clear();
				excludedIds.addAll(savedExcludedIds);
			}
		}

		requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				onBackPressed();
			}
		});
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = inflate(R.layout.coordinate_format_add_fragment, container, false);
		contentContainer = view.findViewById(R.id.content_container);
		setupToolbar(view);
		setupSearch(view);
		renderAddContent();
		return view;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		if (focusSearch) {
			focusSearch = false;
			searchBar.post(() -> {
				if (getView() != null && !searchInputView.isShowing()) {
					searchInputView.show();
				}
			});
		}
	}

	@Override
	public void onStart() {
		super.onStart();
	}

	@Override
	public void onResume() {
		super.onResume();
		updateStatusBarAppearance(getContentView());
		if (!addToEditDraft) {
			excludedIds.clear();
			excludedIds.addAll(formatPreferences.getPreferredIds(appMode));
			renderAddContent();
		}
	}

	@Override
	public void onDestroyView() {
		coordinateFormatHelper.cancelSearch();
		restoreSearchSoftInputMode();
		super.onDestroyView();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		outState.putString(STATE_SEARCH_QUERY, searchQuery);
		outState.putStringArrayList(ARG_EXCLUDED_IDS, new ArrayList<>(excludedIds));
		super.onSaveInstanceState(outState);
	}

	@Override
	public InsetTargetsCollection getInsetTargets() {
		InsetTargetsCollection collection = super.getInsetTargets();
		collection.removeType(InsetTarget.Type.ROOT_INSET);
		collection.add(InsetTarget.createScrollable(R.id.search_results));
		collection.add(InsetTarget.createScrollable(R.id.scroll_view));
		collection.replace(InsetTarget.createCollapsingAppBar(R.id.search_app_bar));
		return collection;
	}

	@Override
	@ColorRes
	public int getStatusBarColorId() {
		return nightMode ? R.color.activity_background_color_dark : R.color.activity_background_color_light;
	}

	private void updateStatusBarAppearance(@Nullable View contentView) {
		updateNightMode();
		Dialog dialog = getDialog();
		Window window = dialog != null ? dialog.getWindow() : null;
		if (window != null) {
			AndroidUiHelper.setStatusBarColor(window, getColor(getStatusBarColorId()));
			AndroidUiHelper.setStatusBarContentColor(window.getDecorView(), nightMode);
		} else {
			Activity activity = getActivity();
			if (activity != null) {
				AndroidUiHelper.setStatusBarColor(activity, getColor(getStatusBarColorId()));
			}
			AndroidUiHelper.setStatusBarContentColor(contentView, nightMode);
		}
	}

	@Nullable
	private View getContentView() {
		return searchInputView != null && searchInputView.isShowing() ? searchInputView : getView();
	}

	private void setupToolbar(@NonNull View view) {
		TextView title = view.findViewById(R.id.toolbar_title);
		title.setText(R.string.coordinate_format_add_title);
		View closeButton = view.findViewById(R.id.close_button);
		closeButton.setOnClickListener(v -> onBackPressed());
	}

	private void setupSearch(@NonNull View view) {
		searchBar = view.findViewById(R.id.search_anchor_bar);
		searchInputView = view.findViewById(R.id.search_input_view);
		searchResults = view.findViewById(R.id.search_results);
		searchResultsAdapter = new SearchResultsAdapter();
		searchResults.setLayoutManager(new LinearLayoutManager(requireContext()));
		searchResults.setAdapter(searchResultsAdapter);
		searchResults.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
				if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
					AndroidUtils.hideSoftKeyboard(requireActivity(), searchInputView.getEditText());
				}
			}
		});

		searchBar.setOnClickListener(v -> searchInputView.show());

		searchInputView.setVisible(false);
		searchInputView.setupWithSearchBar(searchBar);
		searchInputView.setMenuItemsAnimated(false);
		searchInputView.setAutoShowKeyboard(true);
		searchInputView.updateSoftInputMode();

		View divider = searchInputView.findViewById(com.google.android.material.R.id.open_search_view_divider);
		if (divider != null) {
			divider.setVisibility(View.GONE);
		}
		searchInputView.getToolbar().setElevation(0);
		searchInputView.getToolbar().setTranslationZ(0);
		searchInputView.getToolbar().getMenu().clear();
		if (searchInputView.getToolbar().getNavigationIcon() != null) {
			searchInputView.getToolbar().getNavigationIcon().mutate().setTint(
					ColorUtilities.getDefaultIconColor(requireContext(), nightMode));
		}
		searchInputView.addTransitionListener((searchView, previousState, newState) -> {
			if (newState == SearchView.TransitionState.SHOWN) {
				updateStatusBarAppearance(searchView);
				applySearchSoftInputMode();
				searchInputView.getEditText().setSelection(searchInputView.getEditText().length());
			} else if (newState == SearchView.TransitionState.HIDDEN) {
				updateStatusBarAppearance(getView());
				restoreSearchSoftInputMode();
			}
		});

		searchInputView.getEditText().setText(searchQuery);
		searchInputView.getEditText().setSelection(searchInputView.getEditText().length());
		searchInputView.getEditText().addTextChangedListener(new SimpleTextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				searchQuery = s.toString();
				renderSearchResults();
			}
		});
		renderSearchResults();
	}

	private void renderAddContent() {
		if (contentContainer == null) {
			return;
		}
		contentContainer.removeAllViews();
		contentContainer.addView(createInfoCard());
		View generalCard = createGeneralCard();
		if (generalCard != null) {
			contentContainer.addView(generalCard);
		}
	}

	private View createInfoCard() {
		return inflate(R.layout.coordinate_format_add_info_card, contentContainer, false);
	}

	@Nullable
	private View createGeneralCard() {
		List<CoordinateFormat> formats = new ArrayList<>();
		for (CoordinateFormat format : BuiltInCoordinateFormat.getAll(app)) {
			if (!excludedIds.contains(format.getId())) {
				formats.add(format);
			}
		}
		if (formats.isEmpty()) {
			return null;
		}

		MaterialCardView card = createCard(0, 0, 0, 0);
		LinearLayout list = createVerticalContainer();
		card.addView(list);

		TextView header = createTitleText(getString(R.string.group_general));
		header.setPadding(dp(16), dp(18), dp(16), dp(12));
		list.addView(header);

		for (int i = 0; i < formats.size(); i++) {
			CoordinateFormat format = formats.get(i);
			list.addView(createFormatRow(format, true, false, i < formats.size() - 1, v -> onFormatChosen(format.getId())));
		}
		return card;
	}

	private void renderSearchResults() {
		if (searchResultsAdapter == null) {
			return;
		}
		String query = searchQuery;
		coordinateFormatHelper.searchFormats(query, results -> {
			if (searchResultsAdapter == null || !query.equals(searchQuery)) {
				return;
			}
			searchItems.clear();
			for (CoordinateFormat format : results) {
				if (!excludedIds.contains(format.getId())) {
					searchItems.add(format);
				}
			}
			searchResultsAdapter.notifyDataSetChanged();
		});
	}

	private void onFormatChosen(@NonNull String id) {
		if (addToEditDraft) {
			Bundle result = new Bundle();
			result.putString(REQUEST_ADD_TO_EDIT, id);
			getParentFragmentManager().setFragmentResult(REQUEST_ADD_TO_EDIT, result);
			closeAddScreen();
			return;
		}
		boolean added = formatPreferences.addPreferredId(appMode, id);
		if (added) {
			String normalizedId = CoordinateFormatIds.normalize(id);
			excludedIds.add(normalizedId != null ? normalizedId : id);
			formatPreferences.addRecentId(id);
			renderAddContent();
			renderSearchResults();
		}
	}

	private void onBackPressed() {
		if (searchInputView != null && searchInputView.isShowing()) {
			searchInputView.hide();
		} else {
			closeAddScreen();
		}
	}

	private void closeAddScreen() {
		if (getShowsDialog()) {
			dismissAllowingStateLoss();
		} else {
			FragmentManager fragmentManager = getParentFragmentManager();
			if (!fragmentManager.isStateSaved()) {
				fragmentManager.popBackStack();
			}
		}
	}

	@Override
	public void onDismiss(@NonNull DialogInterface dialog) {
		restoreSearchSoftInputMode();
		if (!Algorithms.isEmpty(closeResultKey)) {
			getParentFragmentManager().setFragmentResult(closeResultKey, new Bundle());
		}
		super.onDismiss(dialog);
	}

	private void applySearchSoftInputMode() {
		Activity activity = getActivity();
		Window window = activity != null ? activity.getWindow() : null;
		if (window == null) {
			return;
		}
		if (previousSoftInputMode == SOFT_INPUT_MODE_NOT_SET) {
			previousSoftInputMode = window.getAttributes().softInputMode;
		}
		window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
		if (searchInputView != null) {
			searchInputView.updateSoftInputMode();
		}
	}

	private void restoreSearchSoftInputMode() {
		Activity activity = getActivity();
		Window window = activity != null ? activity.getWindow() : null;
		if (window == null || previousSoftInputMode == SOFT_INPUT_MODE_NOT_SET) {
			return;
		}
		window.setSoftInputMode(previousSoftInputMode);
		previousSoftInputMode = SOFT_INPUT_MODE_NOT_SET;
	}

	private int dp(float value) {
		return AndroidUtils.dpToPx(app, value);
	}

	@NonNull
	private String getFormatSummary(@NonNull CoordinateFormat format) {
		return coordinateFormatHelper.getFormatSummary(format);
	}

	@NonNull
	private MaterialCardView createCard(int marginStart, int marginTop, int marginEnd, int marginBottom) {
		Context themedContext = getThemedContext();
		MaterialCardView card = new MaterialCardView(themedContext);
		card.setCardElevation(0);
		card.setRadius(dp(12));
		card.setStrokeWidth(0);
		card.setUseCompatPadding(false);
		card.setCardBackgroundColor(AndroidUtils.getColorFromAttr(themedContext, R.attr.list_background_color));
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		params.setMargins(dp(marginStart), dp(marginTop), dp(marginEnd), dp(marginBottom));
		card.setLayoutParams(params);
		return card;
	}

	@NonNull
	private LinearLayout createVerticalContainer() {
		LinearLayout layout = new LinearLayout(getThemedContext());
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.setLayoutParams(new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		return layout;
	}

	@NonNull
	private TextView createTitleText(@NonNull String text) {
		Context themedContext = getThemedContext();
		TextView textView = new TextView(themedContext);
		textView.setText(text);
		textView.setTextColor(AndroidUtils.getColorFromAttr(themedContext, android.R.attr.textColorPrimary));
		textView.setTextSize(16);
		textView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
		return textView;
	}

	@NonNull
	private View createFormatRow(@NonNull CoordinateFormat format, boolean addRow, boolean primary,
	                             boolean showDivider, @NonNull View.OnClickListener clickListener) {
		View row = inflate(R.layout.coordinate_format_settings_item, null, false);
		bindFormatRow(row, format, addRow, primary, showDivider, clickListener);
		return row;
	}

	private void bindFormatRow(@NonNull View row, @NonNull CoordinateFormat format, boolean addRow, boolean primary,
	                           boolean showDivider, @NonNull View.OnClickListener clickListener) {
		TextView title = row.findViewById(android.R.id.title);
		TextView summary = row.findViewById(android.R.id.summary);
		View divider = row.findViewById(R.id.divider);
		View iconContainer = row.findViewById(R.id.iconContainer);
		ImageView icon = row.findViewById(R.id.icon);
		View selectable = row.findViewById(R.id.selectable_list_item);

		title.setText(format.getTitle());
		String description = getFormatSummary(format);
		if (primary) {
			description = description + " - " + getString(R.string.coordinate_format_primary);
		}
		summary.setText(description);
		divider.setVisibility(showDivider ? View.VISIBLE : View.GONE);
		if (addRow) {
			iconContainer.setVisibility(View.VISIBLE);
			iconContainer.setBackground(null);
			icon.setImageDrawable(getIcon(R.drawable.ic_action_add, R.color.color_osm_edit_create));
			setDividerTextStartMargin(divider);
		} else {
			iconContainer.setVisibility(View.GONE);
			setDividerDefaultMargin(divider);
		}
		selectable.setOnClickListener(clickListener);
	}

	private void setDividerTextStartMargin(@NonNull View divider) {
		setDividerStartMargin(divider, dp(72));
	}

	private void setDividerDefaultMargin(@NonNull View divider) {
		setDividerStartMargin(divider, dp(16));
	}

	private void setDividerStartMargin(@NonNull View divider, int marginStart) {
		ViewGroup.LayoutParams params = divider.getLayoutParams();
		if (params instanceof ViewGroup.MarginLayoutParams marginParams) {
			marginParams.setMarginStart(marginStart);
			divider.setLayoutParams(marginParams);
		}
	}

	private static void applyArguments(@NonNull AddCoordinateFormatFragment fragment,
	                                   @NonNull ApplicationMode appMode,
	                                   boolean addToEditDraft,
	                                   boolean focusSearch,
	                                   @Nullable List<String> editableIds,
	                                   @Nullable String closeResultKey) {
		Bundle args = new Bundle();
		args.putString(APP_MODE_KEY, appMode.getStringKey());
		args.putBoolean(ARG_ADD_TO_EDIT_DRAFT, addToEditDraft);
		args.putBoolean(ARG_FOCUS_SEARCH, focusSearch);
		args.putString(ARG_CLOSE_RESULT_KEY, closeResultKey);
		if (addToEditDraft && editableIds != null) {
			args.putStringArrayList(ARG_EXCLUDED_IDS, new ArrayList<>(editableIds));
		}
		fragment.setArguments(args);
	}

	public static void showDialog(@NonNull FragmentManager fragmentManager,
	                              @NonNull ApplicationMode appMode,
	                              boolean focusSearch,
	                              @Nullable String closeResultKey) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			AddCoordinateFormatFragment fragment = new AddCoordinateFormatFragment();
			applyArguments(fragment, appMode, false, focusSearch, null, closeResultKey);
			fragment.show(fragmentManager, TAG);
		}
	}

	public static void show(@NonNull FragmentActivity activity, @NonNull ApplicationMode appMode,
	                        boolean addToEditDraft, boolean focusSearch, @Nullable List<String> editableIds) {
		AddCoordinateFormatFragment fragment = new AddCoordinateFormatFragment();
		fragment.setShowsDialog(false);
		applyArguments(fragment, appMode, addToEditDraft, focusSearch, editableIds, null);
		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		if (!fragmentManager.isStateSaved()) {
			fragmentManager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(ADD_SCREEN_BACK_STACK_TAG)
					.commitAllowingStateLoss();
		}
	}

	private class SearchResultsAdapter extends RecyclerView.Adapter<SearchResultsAdapter.FormatViewHolder> {

		@NonNull
		@Override
		public FormatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			View view = inflate(R.layout.coordinate_format_settings_item, parent, false);
			return new FormatViewHolder(view);
		}

		@Override
		public void onBindViewHolder(@NonNull FormatViewHolder holder, int position) {
			CoordinateFormat format = searchItems.get(position);
			bindFormatRow(holder.itemView, format, true, false, position < getItemCount() - 1,
					v -> onFormatChosen(format.getId()));
		}

		@Override
		public int getItemCount() {
			return searchItems.size();
		}

		private class FormatViewHolder extends RecyclerView.ViewHolder {
			FormatViewHolder(@NonNull View itemView) {
				super(itemView);
			}
		}
	}
}
