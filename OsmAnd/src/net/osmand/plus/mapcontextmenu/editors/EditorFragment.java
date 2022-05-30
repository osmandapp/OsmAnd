package net.osmand.plus.mapcontextmenu.editors;

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
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.textfield.TextInputLayout;

import net.osmand.data.BackgroundType;
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

import static net.osmand.GPXUtilities.DEFAULT_ICON_NAME;
import static net.osmand.data.FavouritePoint.DEFAULT_BACKGROUND_TYPE;
import static net.osmand.data.FavouritePoint.DEFAULT_UI_ICON_ID;

public abstract class EditorFragment extends BaseOsmAndFragment implements ColorPickerListener, CardListener {

	protected OsmandApplication app;

	protected IconsCard iconsCard;
	protected ColorsCard colorsCard;
	protected ShapesCard shapesCard;

	protected View view;
	protected EditText nameEdit;
	protected TextInputLayout nameCaption;

	private int color;
	private String iconName = DEFAULT_ICON_NAME;
	private BackgroundType backgroundType = DEFAULT_BACKGROUND_TYPE;

	private int scrollViewY;
	private int layoutHeightPrevious = 0;

	protected boolean cancelled;
	protected boolean nightMode;

	@ColorInt
	public int getColor() {
		return color;
	}

	public void setColor(@ColorInt int color) {
		this.color = color;
	}

	public String getIconName() {
		return iconName;
	}

	public void setIconName(@NonNull String iconName) {
		this.iconName = iconName;
	}

	@DrawableRes
	public int getIconId() {
		int iconId = RenderingIcons.getBigIconResourceId(iconName);
		return iconId != 0 ? iconId : DEFAULT_UI_ICON_ID;
	}

	public void setIcon(@DrawableRes int iconId) {
		String name = RenderingIcons.getBigIconName(iconId);
		iconName = name != null ? name : DEFAULT_ICON_NAME;
	}

	@NonNull
	public BackgroundType getBackgroundType() {
		return backgroundType;
	}

	public void setBackgroundType(@NonNull String typeName) {
		setBackgroundType(BackgroundType.getByTypeName(typeName, DEFAULT_BACKGROUND_TYPE));
	}

	public void setBackgroundType(@NonNull BackgroundType backgroundType) {
		this.backgroundType = backgroundType;
	}

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

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		Context context = requireContext();
		view = UiUtilities.getInflater(context, nightMode).inflate(getLayoutId(), container, false);
		AndroidUtils.addStatusBarPadding21v(context, view);

		setupToolbar();
		setupScrollListener();
		setupButtons();

		nameCaption = view.findViewById(R.id.name_caption);
		nameEdit = view.findViewById(R.id.name_edit);
		nameEdit.setText(getNameInitValue());
		setupNameChangeListener();
		if (app.accessibilityEnabled()) {
			nameCaption.setFocusable(true);
			nameEdit.setHint(R.string.access_hint_enter_name);
		}

		createIconSelector();
		createColorSelector();
		createShapeSelector();
		updateContent();

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
		AndroidUtils.setBackgroundColor(app, view.findViewById(R.id.buttons_container), ColorUtilities.getListBgColorId(nightMode));
	}

	protected void setupNameChangeListener() {
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
				checkEnteredName(s.toString(), saveButton);
			}
		});
		checkEnteredName(nameEdit.getText().toString(), saveButton);
	}

	protected void checkEnteredName(@NonNull String name, @NonNull View saveButton) {
		if (name.trim().isEmpty()) {
			nameCaption.setError(app.getString(R.string.please_provide_point_name_error));
			saveButton.setEnabled(false);
		} else {
			nameCaption.setError(null);
			saveButton.setEnabled(true);
		}
	}

	private OnGlobalLayoutListener getOnGlobalLayoutListener() {
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
			iconsCard = new IconsCard(mapActivity, getIconId(), getPreselectedIconName(), getColor());
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
			int customColor = getColor();
			if (!ColorDialogs.isPaletteColor(customColor)) {
				colors.add(customColor);
			}
			colorsCard = new ColorsCard(mapActivity, null, this, getColor(),
					colors, app.getSettings().CUSTOM_TRACK_COLORS, true);
			colorsCard.setListener(this);
			ViewGroup colorsCardContainer = view.findViewById(R.id.colors_card_container);
			colorsCardContainer.addView(colorsCard.build(view.getContext()));
		}
	}

	private void createShapeSelector() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			shapesCard = new ShapesCard(mapActivity, getBackgroundType(), getColor());
			shapesCard.setListener(this);
			ViewGroup shapesCardContainer = view.findViewById(R.id.shapes_card_container);
			shapesCardContainer.addView(shapesCard.build(mapActivity));
			updateSelectedShapeText();
		}
	}

	@Override
	public void onCardPressed(@NonNull BaseCard card) {
		if (card instanceof IconsCard) {
			setIcon(iconsCard.getSelectedIconId());
		} else if (card instanceof ColorsCard) {
			setColor(colorsCard.getSelectedColor());
			updateContent();
		} else if (card instanceof ShapesCard) {
			setBackgroundType(shapesCard.getSelectedShape());
			updateSelectedShapeText();
		}
	}

	@Override
	public void onColorSelected(Integer prevColor, int newColor) {
		colorsCard.onColorSelected(prevColor, newColor);
		setColor(colorsCard.getSelectedColor());
		updateContent();
	}

	protected void updateContent() {
		updateSelectedColorText();

		colorsCard.setSelectedColor(color);
		iconsCard.updateSelectedIcon(color, iconName);
		shapesCard.updateSelectedShape(color, backgroundType);
	}

	protected void updateSelectedShapeText() {
		((TextView) view.findViewById(R.id.shape_name)).setText(backgroundType.getNameId());
	}

	protected void updateSelectedColorText() {
		((TextView) view.findViewById(R.id.color_name)).setText(ColorDialogs.getColorName(color));
	}

	@Override
	public void onCardLayoutNeeded(@NonNull BaseCard card) {
	}

	@Override
	public void onCardButtonPressed(@NonNull BaseCard card, int buttonIndex) {
	}

	@DrawableRes
	protected int getDefaultIconId() {
		String iconName = getDefaultIconName();
		int iconId = RenderingIcons.getBigIconResourceId(iconName);
		return iconId == 0 ? DEFAULT_UI_ICON_ID : iconId;
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
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
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

	protected void savePressed() {
		save(true);
	}

	public void dismiss() {
		hideKeyboard();
		FragmentActivity activity = getActivity();
		if (activity != null) {
			activity.getSupportFragmentManager().popBackStack();
		}
	}

	@DrawableRes
	protected int getToolbarNavigationIconId() {
		return AndroidUtils.getNavigationIconResId(app);
	}

	@Nullable
	protected abstract PointEditor getEditor();

	@LayoutRes
	protected abstract int getLayoutId();

	@NonNull
	protected abstract String getToolbarTitle();

	@Nullable
	public abstract String getNameInitValue();

	@ColorInt
	protected abstract int getDefaultColor();

	protected abstract boolean wasSaved();

	protected abstract void save(boolean needDismiss);

	@Nullable
	protected String getPreselectedIconName() {
		PointEditor editor = getEditor();
		return editor != null ? editor.getPreselectedIconName() : null;
	}

	@NonNull
	protected String getNameTextValue() {
		return nameEdit.getText().toString().trim();
	}

	protected Drawable getPaintedIcon(@DrawableRes int iconId, @ColorInt int color) {
		return getPaintedContentIcon(iconId, color);
	}

	public void showExitDialog() {
		hideKeyboard();
		if (wasSaved()) {
			exitEditing();
		} else {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				FragmentManager manager = activity.getSupportFragmentManager();
				String message = getString(R.string.exit_without_saving_warning);
				ExitBottomSheetDialogFragment.showInstance(manager, this, message);
			}
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == ExitBottomSheetDialogFragment.REQUEST_CODE) {
			if (resultCode == ExitBottomSheetDialogFragment.EXIT_RESULT_CODE) {
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

	@NonNull
	protected MapActivity requireMapActivity() {
		return (MapActivity) requireActivity();
	}
}