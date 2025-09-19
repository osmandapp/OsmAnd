package net.osmand.plus.mapcontextmenu.editors;

import static net.osmand.data.FavouritePoint.DEFAULT_BACKGROUND_TYPE;
import static net.osmand.data.FavouritePoint.DEFAULT_UI_ICON_ID;
import static net.osmand.plus.configmap.tracks.appearance.favorite.FavoriteAppearanceController.PROCESS_ID;
import static net.osmand.shared.gpx.GpxUtilities.DEFAULT_ICON_NAME;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.data.BackgroundType;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseFullScreenDialogFragment;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.card.base.multistate.IMultiStateCardController;
import net.osmand.plus.card.base.multistate.MultiStateCard;
import net.osmand.plus.configmap.tracks.appearance.favorite.FavoriteAppearanceController;
import net.osmand.plus.configmap.tracks.appearance.favorite.FavoriteAppearanceController.DefaultFavoriteListener;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.myplaces.favorites.FavouritesHelper;
import net.osmand.plus.myplaces.favorites.SaveOption;
import net.osmand.plus.myplaces.favorites.dialogs.FavoritesTreeFragment;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.shared.gpx.GpxUtilities.PointsGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FavoriteAppearanceFragment extends BaseFullScreenDialogFragment {

	public static final String TAG = FavoriteAppearanceFragment.class.getName();

	private FavouritesHelper favouritesHelper;
	private DialogManager dialogManager;
	private FavoriteAppearanceController controller;

	protected View view;
	protected PointsGroup pointsGroup;
	protected String groupName;
	private FavoriteGroup favoriteGroup;

	private int color;
	private String iconName = DEFAULT_ICON_NAME;
	private BackgroundType backgroundType = DEFAULT_BACKGROUND_TYPE;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		favouritesHelper = app.getFavoritesHelper();
		dialogManager = app.getDialogManager();

		groupName = pointsGroup.getName();
		setColor(pointsGroup.getColor());
		setIconName(pointsGroup.getIconName());
		setBackgroundType(BackgroundType.getByTypeName(pointsGroup.getBackgroundType(), DEFAULT_BACKGROUND_TYPE));

		favoriteGroup = favouritesHelper.getGroup(pointsGroup.getName());
		if (favoriteGroup == null) {
			dismiss();
		}

		registerFavoriteAppearanceController();
	}

	@Override
	protected int getThemeId() {
		return nightMode ? R.style.OsmandDarkTheme_DarkActionbar : R.style.OsmandLightTheme_DarkActionbar_LightStatusBar;
	}

	@ColorRes
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return ColorUtilities.getStatusBarColorId(nightMode);
	}

	@NonNull
	@Override
	public Dialog createDialog(@Nullable Bundle savedInstanceState) {
		return new Dialog(requireActivity(), getThemeId()) {
			@Override
			public void onBackPressed() {
				dismiss();
			}
		};
	}

	private void registerFavoriteAppearanceController() {
		dialogManager.register(PROCESS_ID, new FavoriteAppearanceController(app, favoriteGroup, new DefaultFavoriteListener() {
			@Override
			@NonNull
			public String getOriginalIconKey() {
				return iconName;
			}

			@Override
			public int getOriginalColor() {
				return color;
			}

			@Override
			@NonNull
			public BackgroundType getOriginalShape() {
				return backgroundType;
			}
		}));

		controller = (FavoriteAppearanceController) dialogManager.findController(PROCESS_ID);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		view = inflate(R.layout.favorite_default_appearance_fragment, container, false);
		setupToolbar();
		setupButtons();
		setupCards();
		return view;
	}

	@Nullable
	@Override
	public List<Integer> getCollapsingAppBarLayoutId() {
		List<Integer> ids = new ArrayList<>();
		ids.add(R.id.appbar);
		return ids;
	}

	private void setupCards(){
		FragmentActivity activity = requireActivity();
		ViewGroup cardContainer = view.findViewById(R.id.cards_container);
		setupCard(activity, cardContainer, controller.getColorCardController());
		setupCard(activity, cardContainer, controller.getIconController().getCardController());
		setupCard(activity, cardContainer, controller.getShapesController());
	}

	private void setupCard(@NonNull FragmentActivity activity, @NonNull ViewGroup cardContainer, @NonNull IMultiStateCardController controller){
		inflate(R.layout.list_item_divider, cardContainer, true);
		MultiStateCard card = new MultiStateCard(activity, controller, false);
		cardContainer.addView(card.build());
	}

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

	public void setIconName(@Nullable String iconName) {
		this.iconName = iconName != null ? iconName : DEFAULT_ICON_NAME;
	}

	@DrawableRes
	public int getIconId() {
		return AndroidUtils.getDrawableId(app, iconName, DEFAULT_UI_ICON_ID);
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

	private void setupToolbar() {
		View appbar = view.findViewById(R.id.appbar);
		ViewCompat.setElevation(appbar, 5.0f);

		int colorId = ColorUtilities.getActiveButtonsAndLinksTextColorId(nightMode);
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		toolbar.setNavigationIcon(getIcon(AndroidUtils.getNavigationIconResId(app), colorId));
		toolbar.setNavigationContentDescription(R.string.shared_string_close);
		toolbar.setNavigationOnClickListener(v -> dismiss());
	}

	protected void setupButtons() {
		DialogButton saveButton = view.findViewById(R.id.right_bottom_button);
		AndroidUiHelper.updateVisibility(saveButton, true);
		saveButton.setOnClickListener(v -> savePressed());
		saveButton.setButtonType(DialogButtonType.PRIMARY);
		saveButton.setTitleId(R.string.shared_string_save);
		AndroidUtils.setBackgroundColor(app, view.findViewById(R.id.bottom_buttons_container), ColorUtilities.getListBgColorId(nightMode));

		DialogButton dismissButton = view.findViewById(R.id.dismiss_button);
		AndroidUiHelper.updateVisibility(dismissButton, false);
	}

	protected void savePressed() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			FragmentManager manager = activity.getSupportFragmentManager();
			DefaultFavoriteAppearanceSaveBottomSheet.showInstance(manager, this, pointsGroup.getPoints().size());
		}
	}

	@Override
	public void dismiss() {
		boolean hasChanges = false;
		if (favoriteGroup != null) {
			if (controller.getColor() != null && controller.getColor() != color) {
				hasChanges = true;
			}
			if (controller.getIcon() != null && !controller.getIcon().equals(iconName)) {
				hasChanges = true;
			}
			if (controller.getShape() != null && controller.getShape() != backgroundType) {
				hasChanges = true;
			}
		}

		if (hasChanges) {
			Context themedContext = UiUtilities.getThemedContext(requireActivity(), nightMode);
			AlertDialog.Builder dismissDialog = new AlertDialog.Builder(themedContext);
			dismissDialog.setTitle(getString(R.string.exit_without_saving));
			dismissDialog.setMessage(getString(R.string.dismiss_changes_descr));
			dismissDialog.setNegativeButton(R.string.shared_string_cancel, null);
			dismissDialog.setPositiveButton(R.string.shared_string_exit, (dialog, which) -> super.dismiss());
			dismissDialog.show();
		} else {
			super.dismiss();
		}
	}

	public void editPointsGroup(@NonNull SaveOption saveOption) {
		boolean shouldSave = false;
		if (favoriteGroup != null) {
			if (controller.getColor() != null) {
				favouritesHelper.updateGroupColor(favoriteGroup, controller.getColor(), saveOption, false);
				shouldSave = true;
				controller.getColorCardController().getColorsPaletteController().refreshLastUsedTime();
			}
			if (controller.getIcon() != null) {
				favouritesHelper.updateGroupIconName(favoriteGroup, controller.getIcon(), saveOption, false);
				shouldSave = true;
				controller.getIconController().addIconToLastUsed(controller.getIcon());
			}
			if (controller.getShape() != null) {
				favouritesHelper.updateGroupBackgroundType(favoriteGroup, controller.getShape(), saveOption, false);
				shouldSave = true;
			}

			if (shouldSave) {
				favouritesHelper.saveSelectedGroupsIntoFile(Collections.singletonList(favoriteGroup), true);
			}
		}

		Fragment targetFragment = getTargetFragment();
		if (targetFragment instanceof FavoritesTreeFragment treeFragment) {
			treeFragment.reloadData();
		}
		super.dismiss();
	}

	@Override
	public void onDestroy() {
		FragmentActivity activity = getActivity();
		if (activity != null && !activity.isChangingConfigurations()) {
			dialogManager.unregister(FavoriteAppearanceController.PROCESS_ID);
		}

		super.onDestroy();
	}

	public static void showInstance(@NonNull FragmentManager manager,
	                                @NonNull PointsGroup pointsGroup,
	                                @NonNull Fragment treeFragment) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			FavoriteAppearanceFragment fragment = new FavoriteAppearanceFragment();
			fragment.pointsGroup = pointsGroup;
			fragment.setTargetFragment(treeFragment, 0);
			fragment.show(manager, TAG);
		}
	}
}
