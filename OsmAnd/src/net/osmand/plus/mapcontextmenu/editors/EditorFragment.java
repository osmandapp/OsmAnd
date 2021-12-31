package net.osmand.plus.mapcontextmenu.editors;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputLayout;

import net.osmand.data.FavouritePoint.BackgroundType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.ColorDialogs;
import net.osmand.plus.measurementtool.ExitBottomSheetDialogFragment;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.routepreparationmenu.cards.BaseCard.CardListener;
import net.osmand.plus.track.cards.ColorsCard;
import net.osmand.plus.track.fragments.CustomColorBottomSheet.ColorPickerListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentActivity;

import static net.osmand.GPXUtilities.DEFAULT_ICON_NAME;
import static net.osmand.data.FavouritePoint.DEFAULT_BACKGROUND_TYPE;
import static net.osmand.data.FavouritePoint.DEFAULT_UI_ICON_ID;

public abstract class EditorFragment extends BaseOsmAndFragment implements ColorPickerListener, CardListener {

	protected OsmandApplication app;

	protected boolean cancelled;
	protected boolean nightMode;

	@ColorInt
	protected int selectedColor;
	protected BackgroundType selectedShape = DEFAULT_BACKGROUND_TYPE;

	protected View view;
	protected EditText nameEdit;

	private int scrollViewY;
	private int layoutHeightPrevious = 0;

	protected IconsCard iconsCard;
	protected ColorsCard colorsCard;
	protected ShapesCard shapesCard;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		nightMode = app.getDaynightHelper().isNightModeForMapControls();

		requireMyActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			public void handleOnBackPressed() {
				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null) {
					showExitDialog();
				}
			}
		});
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {

		Context context = requireContext();
		view = UiUtilities.getInflater(context, nightMode)
				.inflate(getLayoutId(), container, false);
		AndroidUtils.addStatusBarPadding21v(context, view);

		selectedColor = getPointColor();
		selectedShape = getBackgroundType();

		setupToolbar();
		setupScrollListener();
		setupButtons();

		nameEdit = view.findViewById(R.id.name_edit);
		nameEdit.setText(getNameInitValue());
		setupNameChangeListener();
		if (app.accessibilityEnabled()) {
			view.findViewById(R.id.name_caption).setFocusable(true);
			nameEdit.setHint(R.string.access_hint_enter_name);
		}

		createIconSelector();
		createColorSelector();
		createShapeSelector();
		updateColorSelector(selectedColor);

		view.getViewTreeObserver().addOnGlobalLayoutListener(getOnGlobalLayoutListener());
		return view;
	}

	private void setupToolbar() {
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		toolbar.setTitle(getToolbarTitle());

		int navigationIconColorId = nightMode
				? R.color.active_buttons_and_links_text_dark
				: R.color.description_font_and_bottom_sheet_icons;
		Drawable navigationIcon = getIcon(getToolbarNavigationIconId(), navigationIconColorId);
		toolbar.setNavigationIcon(navigationIcon);
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(v -> showExitDialog());
	}

	private void setupScrollListener() {
		final ScrollView scrollView = view.findViewById(R.id.editor_scroll_view);
		scrollViewY = scrollView.getScrollY();
		scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
			if (scrollViewY != scrollView.getScrollY()) {
				scrollViewY = scrollView.getScrollY();
				onMainScrollChanged();
			}
		});
	}

	protected void onMainScrollChanged() {
		hideKeyboard();
		nameEdit.clearFocus();
	}

	protected void setupButtons() {
		View saveButton = view.findViewById(R.id.right_bottom_button);
		AndroidUiHelper.updateVisibility(saveButton, true);
		saveButton.setOnClickListener(v -> savePressed());
		UiUtilities.setupDialogButton(nightMode, saveButton, UiUtilities.DialogButtonType.PRIMARY, R.string.shared_string_save);
	}

	protected void setupNameChangeListener() {
		TextInputLayout nameCaption = view.findViewById(R.id.name_caption);
		View saveButton = view.findViewById(R.id.right_bottom_button);
		nameEdit.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				checkEmptyName(s, nameCaption, saveButton);
			}
		});
		checkEmptyName(nameEdit.getText(), nameCaption, saveButton);
	}

	private void checkEmptyName(Editable name, TextInputLayout nameCaption, View saveButton) {
		if (name.toString().trim().isEmpty()) {
			nameCaption.setError(app.getString(R.string.please_provide_point_name_error));
			saveButton.setEnabled(false);
		} else {
			nameCaption.setError(null);
			saveButton.setEnabled(true);
		}
	}

	private ViewTreeObserver.OnGlobalLayoutListener getOnGlobalLayoutListener() {
		return () -> {
			Rect visibleDisplayFrame = new Rect();
			view.getWindowVisibleDisplayFrame(visibleDisplayFrame);
			int layoutHeight = visibleDisplayFrame.bottom;
			if (layoutHeight != layoutHeightPrevious) {
				FrameLayout.LayoutParams rootViewLayout = (FrameLayout.LayoutParams) view.getLayoutParams();
				rootViewLayout.height = layoutHeight;
				view.requestLayout();
				layoutHeightPrevious = layoutHeight;
			}
		};
	}

	private void createIconSelector() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			iconsCard = new IconsCard(mapActivity, getIconId(), getPreselectedIconName(), selectedColor);
			iconsCard.setListener(this);
			ViewGroup shapesCardContainer = view.findViewById(R.id.icons_card_container);
			shapesCardContainer.addView(iconsCard.build(mapActivity));
		}
	}

	private void createColorSelector() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			List<Integer> colors = new ArrayList<>();
			for (int color : ColorDialogs.pallette) {
				colors.add(color);
			}
			int customColor = getPointColor();
			if (!ColorDialogs.isPaletteColor(customColor)) {
				colors.add(customColor);
			}
			colorsCard = new ColorsCard(mapActivity, null, this, selectedColor,
					colors, app.getSettings().CUSTOM_TRACK_COLORS, true);
			colorsCard.setListener(this);
			LinearLayout colorsCardContainer = view.findViewById(R.id.colors_card_container);
			colorsCardContainer.addView(colorsCard.build(view.getContext()));
		}
	}

	private void createShapeSelector() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			shapesCard = new ShapesCard(mapActivity, selectedShape, selectedColor);
			shapesCard.setListener(this);
			ViewGroup shapesCardContainer = view.findViewById(R.id.shapes_card_container);
			shapesCardContainer.addView(shapesCard.build(mapActivity));
		}
	}

	@Override
	public void onColorSelected(Integer prevColor, int newColor) {
		colorsCard.onColorSelected(prevColor, newColor);
		updateColorSelector(colorsCard.getSelectedColor());
	}

	protected void updateColorSelector(int color) {
		selectedColor = color;
		((TextView) view.findViewById(R.id.color_name)).setText(ColorDialogs.getColorName(color));
		setColor(color);
		iconsCard.updateSelectedColor(color);
		shapesCard.updateSelectedColor(color);
	}

	@Override
	public void onCardLayoutNeeded(@NonNull BaseCard card) {
	}

	@Override
	public void onCardButtonPressed(@NonNull BaseCard card, int buttonIndex) {
	}

	@Nullable
	protected String getNameFromIconId(@DrawableRes int iconId) {
		return RenderingIcons.getBigIconName(iconId);
	}

	@DrawableRes
	protected int getIconIdByName(@Nullable String iconName) {
		return RenderingIcons.getBigIconResourceId(iconName);
	}

	@DrawableRes
	protected int getDefaultIconId() {
		try {
			String iconName = getDefaultIconName();
			return getIconIdByName(iconName);
		} catch (Exception e) {
			return DEFAULT_UI_ICON_ID;
		}
	}

	@NonNull
	protected String getDefaultIconName() {
		String preselectedIconName = getPreselectedIconName();
		List<String> lastUsedIcons = app.getSettings().LAST_USED_FAV_ICONS.getStringsList();
		if (!Algorithms.isEmpty(preselectedIconName)) {
			return preselectedIconName;
		} else if (!Algorithms.isEmpty(lastUsedIcons)) {
			return lastUsedIcons.get(0);
		}
		return DEFAULT_ICON_NAME;
	}

	protected void addLastUsedIcon(@DrawableRes int iconId) {
		String iconName = RenderingIcons.getBigIconName(iconId);
		if (!Algorithms.isEmpty(iconName)) {
			addLastUsedIcon(iconName);
		}
	}

	protected void addLastUsedIcon(@NonNull String iconName) {
		iconsCard.addLastUsedIcon(iconName);
	}

	@Override
	public int getStatusBarColorId() {
		View view = getView();
		if (view != null && !nightMode) {
			view.setSystemUiVisibility(view.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
		}
		return ColorUtilities.getListBgColorId(nightMode);
	}

	protected void showKeyboard() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
			if (inputMethodManager != null) {
				inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
			}
		}
	}

	protected void hideKeyboard() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			AndroidUtils.hideSoftKeyboard(activity, activity.getCurrentFocus());
		}
	}

	private void savePressed() {
		save(true);
	}

	public void dismiss() {
		hideKeyboard();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getSupportFragmentManager().popBackStack();
		}
	}

	@LayoutRes
	protected abstract int getLayoutId();

	protected abstract boolean wasSaved();

	protected abstract void save(boolean needDismiss);

	@Nullable
	protected abstract PointEditor getEditor();

	@NonNull
	protected abstract String getToolbarTitle();

	@DrawableRes
	protected abstract int getToolbarNavigationIconId();

	@Nullable
	public abstract String getNameInitValue();

	@ColorInt
	public abstract int getPointColor();

	public abstract int getDefaultColor();

	public abstract void setColor(@ColorInt int color);

	public abstract int getIconId();

	@Nullable
	public abstract String getPreselectedIconName();

	public abstract void setIcon(int iconId);

	public abstract BackgroundType getBackgroundType();

	public abstract void setBackgroundType(BackgroundType backgroundType);

	@NonNull
	protected String getNameTextValue() {
		EditText nameEdit = view.findViewById(R.id.name_edit);
		return nameEdit.getText().toString().trim();
	}

	protected Drawable getPaintedIcon(int iconId, int color) {
		return getPaintedContentIcon(iconId, color);
	}

	public void showExitDialog() {
		hideKeyboard();
		if (!wasSaved()) {
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null){
				ExitBottomSheetDialogFragment.showInstance(mapActivity.getSupportFragmentManager(), this, getString(R.string.exit_without_saving_warning));
			}
		} else {
			exitEditing();
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == ExitBottomSheetDialogFragment.REQUEST_CODE){
			if (resultCode == ExitBottomSheetDialogFragment.EXIT_RESULT_CODE){
				exitEditing();
			} else if (resultCode == ExitBottomSheetDialogFragment.SAVE_RESULT_CODE) {
				savePressed();
			}
		}
	}

	public void exitEditing() {
		cancelled = true;
		dismiss();
	}

	@Nullable
	protected MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}
}