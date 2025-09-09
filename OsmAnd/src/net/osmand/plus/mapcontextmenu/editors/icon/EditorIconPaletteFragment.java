package net.osmand.plus.mapcontextmenu.editors.icon;

import static net.osmand.plus.card.icon.IIconsPaletteController.ALL_ICONS_PROCESS_ID;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseFullScreenDialogFragment;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.editors.icon.data.IconsCategory;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.chips.ChipItem;
import net.osmand.plus.widgets.chips.HorizontalChipsView;
import net.osmand.plus.widgets.tools.SimpleTextWatcher;

import java.util.List;

public class EditorIconPaletteFragment extends BaseFullScreenDialogFragment implements IEditorIconPaletteScreen {

	public static final String TAG = EditorIconPaletteFragment.class.getSimpleName();

	private EditorIconScreenController controller;
	private HorizontalChipsView categorySelector;
	private View searchContainer;
	private TextView toolbarTitle;
	private ImageButton backButton;
	private ImageButton searchButton;
	protected View clearSearchQueryButton;
	protected EditText searchEditText;
	private View progressBar;
	private RecyclerView recyclerView;
	private EditorIconScreenAdapter adapter;

	@Override
	protected int getThemeId() {
		return nightMode ? R.style.OsmandDarkTheme_DarkActionbar : R.style.OsmandLightTheme_DarkActionbar;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		DialogManager dialogManager = app.getDialogManager();
		controller = (EditorIconScreenController) dialogManager.findController(ALL_ICONS_PROCESS_ID);
		if (controller != null) {
			controller.bindScreen(this);
		} else {
			dismiss();
		}
	}

	@NonNull
	@Override
	public Dialog createDialog(@Nullable Bundle savedInstanceState) {
		Dialog dialog = super.createDialog(savedInstanceState);
		dialog.setOnKeyListener((d, keyCode, event) -> {
			if (KeyEvent.KEYCODE_BACK == keyCode && KeyEvent.ACTION_UP == event.getAction()) {
				onBackPressed();
				return true;
			}
			return false;
		});
		return dialog;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = inflate(R.layout.fragment_icon_categories, container, false);
		progressBar = view.findViewById(R.id.progress_bar);
		setupToolbar(view);
		setupSearch(view);
		setupCategorySelector(view);
		setupContentContainer(view);
		onScreenModeChanged();
		updateSelectedCategory();
		return view;
	}

	private void setupToolbar(@NonNull View view) {
		View appbar = view.findViewById(R.id.appbar);
		ViewCompat.setElevation(appbar, 5.0f);
		appbar.setBackgroundColor(ColorUtilities.getAppBarColor(app, nightMode));
		setStatusBarBackgroundColor(ColorUtilities.getStatusBarColor(app, nightMode));

		searchButton = view.findViewById(R.id.action_button);
		searchButton.setOnClickListener(v -> {
			controller.enterSearchMode();
		});

		backButton = view.findViewById(R.id.back_button);
		backButton.setVisibility(View.VISIBLE);
		backButton.setOnClickListener((v) -> onBackPressed());

		Toolbar toolbar = view.findViewById(R.id.toolbar);
		toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
		toolbarTitle.setText(controller.getToolbarTitle());
	}

	private void setStatusBarBackgroundColor(@ColorInt int color) {
		Window window = requireDialog().getWindow();
		if (window != null) {
			AndroidUiHelper.setStatusBarContentColor(window.getDecorView(), true);
			AndroidUiHelper.setStatusBarColor(window, color);
		}
	}

	private void onBackPressed() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			AndroidUtils.hideSoftKeyboard(activity, searchEditText);
		}
		if (controller.isInSearchMode()) {
			searchEditText.setText("");
			controller.exitSearchMode();
		} else {
			dismiss();
		}
	}

	private void setupSearch(@NonNull View view) {
		searchContainer = view.findViewById(R.id.search_container);
		clearSearchQueryButton = searchContainer.findViewById(R.id.clearButton);
		clearSearchQueryButton.setVisibility(View.GONE);
		searchEditText = searchContainer.findViewById(R.id.searchEditText);
		searchEditText.setHint(R.string.poi_filter_by_name);
		searchEditText.setTextColor(ColorUtilities.getColor(app, R.color.card_and_list_background_light));
		searchEditText.setHintTextColor(ColorUtilities.getColor(app, R.color.white_50_transparent));
		searchEditText.addTextChangedListener(new SimpleTextWatcher() {
			@Override
			public void afterTextChanged(Editable query) {
				updateProgressBarVisibility(true);
				controller.searchIcons(query.toString());
				AndroidUiHelper.updateVisibility(clearSearchQueryButton, query.length() > 0);
				adapter.notifyItemChanged(0);
			}
		});
		searchEditText.setOnFocusChangeListener((v, hasFocus) -> {
			if (hasFocus) {
				searchEditText.setSelection(searchEditText.getText().length());
				AndroidUtils.showSoftKeyboard(requireActivity(), v);
			} else {
				AndroidUtils.hideSoftKeyboard(requireActivity(), v);
			}
		});
		clearSearchQueryButton.setOnClickListener((v) -> {
			controller.clearSearchQuery();
			searchEditText.setText(null);
			updateProgressBarVisibility(false);
		});
	}

	private void setupCategorySelector(@NonNull View view) {
		List<ChipItem> items = controller.collectCategoriesChipItems(nightMode);
		categorySelector = view.findViewById(R.id.icons_categories_selector);
		categorySelector.setItems(items);

		IconsCategory selectedCategory = controller.getSelectedCategory();
		ChipItem selectedItem = selectedCategory != null ? categorySelector.getChipById(selectedCategory.getKey()) : null;
		if (selectedItem != null) {
			categorySelector.setSelected(selectedItem);
			categorySelector.notifyDataSetChanged();
			categorySelector.scrollTo(selectedItem);
		} else {
			categorySelector.notifyDataSetChanged();
		}

		categorySelector.setOnSelectChipListener(chip -> controller.onChipClick(chip));
	}

	private void setupContentContainer(@NonNull View view) {
		OsmandSettings settings = app.getSettings();
		ApplicationMode appMode = settings.getApplicationMode();
		FragmentActivity activity = requireActivity();
		adapter = new EditorIconScreenAdapter(activity, appMode, controller, isUsedOnMap());
		recyclerView = view.findViewById(R.id.icon_categories);
		recyclerView.setLayoutManager(new LinearLayoutManager(activity));
		recyclerView.setAdapter(adapter);
	}

	@Override
	public void onScreenModeChanged() {
		boolean inSearchMode = controller.isInSearchMode();
		AndroidUiHelper.updateVisibility(categorySelector, !inSearchMode);
		AndroidUiHelper.updateVisibility(searchContainer, inSearchMode);
		AndroidUiHelper.updateVisibility(searchButton, !inSearchMode);
		AndroidUiHelper.updateVisibility(toolbarTitle, !inSearchMode);
		backButton.setImageResource(getNavigationIconId());
		if (inSearchMode) {
			searchEditText.requestFocus();
		}
		updateScreenContent();
	}

	@Override
	public void updateScreenContent() {
		updateProgressBarVisibility(false);
		adapter.setScreenData(controller.populateScreenItems());
	}

	@Override
	public void updateSelectedCategory() {
		IconsCategory category = controller.getSelectedCategory();
		ChipItem selected = categorySelector.findChipByTag(category);
		if (selected != null) {
			categorySelector.setSelected(selected);
			categorySelector.notifyDataSetChanged();
			categorySelector.smoothScrollTo(selected);
		}
		updateProgressBarVisibility(true);
		recyclerView.setVisibility(View.INVISIBLE);
		int position = adapter.positionOfValue(category);
		if (position >= 0) {
			recyclerView.scrollToPosition(position);
			app.runInUIThread(() -> {
				ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(position);
				View view = viewHolder != null ? viewHolder.itemView : null;
				if (view != null) {
					int targetItemY = AndroidUtils.getViewOnScreenY(categorySelector) + categorySelector.getHeight();
					int currentItemY = AndroidUtils.getViewOnScreenY(view);
					int correction = currentItemY - targetItemY;
					recyclerView.scrollBy(0, correction);
				}
				updateProgressBarVisibility(false);
				recyclerView.setVisibility(View.VISIBLE);
			}, 500);
		}
	}

	private void updateProgressBarVisibility(boolean visible) {
		AndroidUiHelper.updateVisibility(progressBar, visible);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		FragmentActivity activity = getActivity();
		if (activity != null && !activity.isChangingConfigurations()) {
			// Automatically unregister controller when close the dialog
			// to avoid any possible memory leaks
			controller.onDestroyScreen();
			DialogManager manager = app.getDialogManager();
			manager.unregister(ALL_ICONS_PROCESS_ID);
		}
	}

	@DrawableRes
	private int getNavigationIconId() {
		return controller.isInSearchMode() ? AndroidUtils.getNavigationIconResId(app) : R.drawable.ic_action_close;
	}

	protected int getStatusBarColorId() {
		return ColorUtilities.getStatusBarColorId(nightMode);
	}

	public static void showInstance(@NonNull FragmentActivity activity,
	                                @NonNull EditorIconScreenController controller) {
		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			OsmandApplication app = (OsmandApplication) activity.getApplicationContext();
			DialogManager dialogManager = app.getDialogManager();
			dialogManager.register(ALL_ICONS_PROCESS_ID, controller);
			new EditorIconPaletteFragment().show(fragmentManager, TAG);
		}
	}
}
