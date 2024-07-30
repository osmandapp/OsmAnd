package net.osmand.plus.mapcontextmenu.editors;

import static net.osmand.data.FavouritePoint.DEFAULT_BACKGROUND_TYPE;
import static net.osmand.data.FavouritePoint.DEFAULT_UI_ICON_ID;
import static net.osmand.shared.gpx.GpxUtilities.DEFAULT_ICON_NAME;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
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
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.card.base.multistate.MultiStateCard;
import net.osmand.plus.card.color.palette.main.ColorsPaletteCard;
import net.osmand.plus.card.color.palette.main.ColorsPaletteController;
import net.osmand.plus.card.color.palette.main.OnColorsPaletteListener;
import net.osmand.plus.card.color.palette.main.data.PaletteColor;
import net.osmand.plus.card.icon.OnIconsPaletteListener;
import net.osmand.plus.mapcontextmenu.editors.controller.EditorColorController;
import net.osmand.plus.mapcontextmenu.editors.icon.EditorIconController;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.plus.widgets.tools.SimpleTextWatcher;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.measurementtool.ExitBottomSheetDialogFragment;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.routepreparationmenu.cards.BaseCard.CardListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.util.Algorithms;

import java.util.List;

public abstract class EditorFragment extends BaseOsmAndFragment
		implements CardListener, OnColorsPaletteListener, OnIconsPaletteListener<String> {

	protected ShapesCard shapesCard;

	protected View view;
	protected EditText nameEdit;
	protected TextInputLayout nameCaption;
	private OnGlobalLayoutListener onGlobalLayoutListener;

	private int color;
	private String iconName = DEFAULT_ICON_NAME;
	private BackgroundType backgroundType = DEFAULT_BACKGROUND_TYPE;

	private int scrollViewY;
	private int layoutHeightPrevious;

	protected boolean cancelled;

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
	protected boolean isUsedOnMap() {
		return true;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
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
		updateNightMode();
		view = themedInflater.inflate(getLayoutId(), container, false);
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);

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

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		requireMapActivity().disableDrawer();
		view.getViewTreeObserver().addOnGlobalLayoutListener(getOnGlobalLayoutListener());
	}

	@Override
	public void onPause() {
		super.onPause();
		requireMapActivity().enableDrawer();
		view.getViewTreeObserver().removeOnGlobalLayoutListener(getOnGlobalLayoutListener());
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		FragmentActivity activity = getActivity();
		if (activity != null && !activity.isChangingConfigurations()) {
			EditorColorController.onDestroy(app);
			EditorIconController.onDestroy(app);
		}
	}

	private void setupToolbar() {
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		toolbar.setTitle(getToolbarTitle());

		int navigationIconColorId = nightMode
				? R.color.active_buttons_and_links_text_dark
				: R.color.icon_color_default_light;
		Drawable navigationIcon = getIcon(getToolbarNavigationIconId(), navigationIconColorId);
		toolbar.setNavigationIcon(navigationIcon);
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(v -> showExitDialog());
	}

	private void setupScrollListener() {
		ScrollView scrollView = view.findViewById(R.id.editor_scroll_view);
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
		DialogButton saveButton = view.findViewById(R.id.right_bottom_button);
		AndroidUiHelper.updateVisibility(saveButton, true);
		saveButton.setOnClickListener(v -> savePressed());
		saveButton.setButtonType(DialogButtonType.PRIMARY);
		saveButton.setTitleId(R.string.shared_string_save);
		AndroidUtils.setBackgroundColor(app, view.findViewById(R.id.buttons_container), ColorUtilities.getListBgColorId(nightMode));
	}

	protected void setupNameChangeListener() {
		View saveButton = view.findViewById(R.id.right_bottom_button);
		nameEdit.addTextChangedListener(new SimpleTextWatcher() {
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
		if (onGlobalLayoutListener == null) {
			onGlobalLayoutListener = () -> {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					int layoutHeight = AndroidUtils.resizeViewForKeyboard(activity, view, layoutHeightPrevious);
					if (layoutHeight != layoutHeightPrevious) {
						layoutHeightPrevious = layoutHeight;
					}
				}
			};
		}
		return onGlobalLayoutListener;
	}

	private void createIconSelector() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			EditorIconController iconController = getIconController();
			ViewGroup iconsCardContainer = view.findViewById(R.id.icons_card_container);
			iconsCardContainer.addView(new MultiStateCard(mapActivity, iconController.getCardController()) {
				@Override
				public int getCardLayoutId() {
					return R.layout.card_select_editor_icon;
				}
			}.build());
		}
	}

	private void createColorSelector() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			ColorsPaletteCard colorsPaletteCard = new ColorsPaletteCard(mapActivity, getColorController());
			ViewGroup colorsCardContainer = view.findViewById(R.id.colors_card_container);
			colorsCardContainer.addView(colorsPaletteCard.build(view.getContext()));
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
		if (card instanceof ShapesCard) {
			setBackgroundType(shapesCard.getSelectedShape());
			updateSelectedShapeText();
		}
	}

	@Override
	public void onColorSelectedFromPalette(@NonNull PaletteColor paletteColor) {
		setColor(paletteColor.getColor());
		updateContent();
	}

	@Override
	public void onIconSelectedFromPalette(@NonNull String icon) {
		setIconName(icon);
		updateContent();
	}

	@NonNull
	private ColorsPaletteController getColorController() {
		return EditorColorController.getInstance(app, this, getColor());
	}

	@NonNull
	private EditorIconController getIconController() {
		return EditorIconController.getInstance(app, this, iconName);
	}

	protected void updateContent() {
		updateSelectedColorText();
		getIconController().updateAccentColor(color);
		shapesCard.updateSelectedShape(color, backgroundType);
	}

	protected void updateSelectedShapeText() {
		((TextView) view.findViewById(R.id.shape_name)).setText(backgroundType.getNameId());
	}

	protected void updateSelectedColorText() {
		ColorsPaletteController controller = getColorController();
		((TextView) view.findViewById(R.id.color_name)).setText(controller.getColorName(color));
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
		getIconController().addIconToLastUsed(iconName);
	}

	@Override
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return ColorUtilities.getListBgColorId(nightMode);
	}

	public boolean getContentStatusBarNightMode() {
		return nightMode;
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
		getColorController().refreshLastUsedTime();
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