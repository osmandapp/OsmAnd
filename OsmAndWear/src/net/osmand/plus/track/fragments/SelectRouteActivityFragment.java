package net.osmand.plus.track.fragments;

import static net.osmand.plus.base.dialog.data.DialogExtra.TITLE;

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

import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.fragments.customizable.CustomizableSingleSelectionDialogFragment;
import net.osmand.plus.track.fragments.controller.SelectRouteActivityController;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.tools.SimpleTextWatcher;

public class SelectRouteActivityFragment extends CustomizableSingleSelectionDialogFragment {

	private SelectRouteActivityController controller;
	private View searchContainer;
	private TextView toolbarTitle;
	private ImageButton backButton;
	private ImageButton searchButton;
	protected View clearSearchQueryButton;
	protected EditText searchEditText;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		controller = SelectRouteActivityController.getExistedInstance(app);
		if (controller == null) {
			dismiss();
		}
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Dialog dialog = super.onCreateDialog(savedInstanceState);
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
		View view = super.onCreateView(inflater, container, savedInstanceState);
		setupSearch(view);
		onScreenModeChanged();
		return view;
	}

	@Override
	protected void setupToolbar(@NonNull View view) {
		View appbar = view.findViewById(R.id.appbar);
		ViewCompat.setElevation(appbar, 5.0f);
		appbar.setBackgroundColor(ColorUtilities.getAppBarColor(app, nightMode));
		setStatusBarBackgroundColor(ColorUtilities.getStatusBarColor(app, nightMode));

		searchButton = view.findViewById(R.id.action_button);
		searchButton.setOnClickListener(v -> {
			controller.enterSearchMode();
		});

		backButton = view.findViewById(R.id.close_button);
		backButton.setVisibility(View.VISIBLE);
		backButton.setOnClickListener((v) -> onBackPressed());

		Toolbar toolbar = view.findViewById(R.id.toolbar);
		toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
		toolbarTitle.setText((String) displayData.getExtra(TITLE));
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
				controller.searchActivities(query.toString());
				AndroidUiHelper.updateVisibility(clearSearchQueryButton, query.length() > 0);
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
		});
	}

	public void onScreenModeChanged() {
		boolean inSearchMode = controller.isInSearchMode();
		AndroidUiHelper.updateVisibility(searchContainer, inSearchMode);
		AndroidUiHelper.updateVisibility(searchButton, !inSearchMode);
		AndroidUiHelper.updateVisibility(toolbarTitle, !inSearchMode);
		backButton.setImageResource(getNavigationIconId());
		if (inSearchMode) {
			searchEditText.requestFocus();
		}
		View view = getView();
		if (view != null) {
			updateContent(view);
		}
	}

	private void setStatusBarBackgroundColor(@ColorInt int color) {
		Window window = requireDialog().getWindow();
		if (window != null) {
			AndroidUiHelper.setStatusBarContentColor(window.getDecorView(), true);
			window.setStatusBarColor(color);
		}
	}

	@DrawableRes
	private int getNavigationIconId() {
		return controller.isInSearchMode() ? AndroidUtils.getNavigationIconResId(app) : R.drawable.ic_action_close;
	}

	@Override
	protected int getLayoutId() {
		return R.layout.fragment_select_route_activity;
	}

	public static boolean showInstance(@NonNull FragmentManager fragmentManager,
	                                   @NonNull String processId) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			SelectRouteActivityFragment fragment = new SelectRouteActivityFragment();
			fragment.setProcessId(processId);
			fragment.show(fragmentManager, TAG);
			return true;
		}
		return false;
	}
}
