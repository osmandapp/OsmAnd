package net.osmand.plus.mapcontextmenu.editors;

import static net.osmand.data.FavouritePoint.DEFAULT_BACKGROUND_TYPE;
import static net.osmand.data.FavouritePoint.DEFAULT_UI_ICON_ID;
import static net.osmand.plus.configmap.tracks.appearance.favorite.FavoriteAppearanceController.PROCESS_ID;
import static net.osmand.shared.gpx.GpxUtilities.DEFAULT_ICON_NAME;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.data.BackgroundType;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.card.base.multistate.MultiStateCard;
import net.osmand.plus.configmap.tracks.appearance.favorite.FavoriteAppearanceController;
import net.osmand.plus.configmap.tracks.appearance.favorite.FavoriteAppearanceController.DefaultFavoriteListener;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.editors.DefaultFavoriteAppearanceSaveBottomSheet.SaveOption;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.myplaces.favorites.FavouritesHelper;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.shared.gpx.GpxUtilities.PointsGroup;
import net.osmand.util.Algorithms;

public class FavoriteAppearanceFragment extends BaseOsmAndFragment {

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

	private boolean launchPrevIntent;

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
		view = themedInflater.inflate(R.layout.favorite_default_appearance_fragment, container, false);
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);

		setupToolbar();
		setupButtons();

		FragmentActivity activity = requireActivity();
		ViewGroup cardContainer = view.findViewById(R.id.cards_container);
		inflate(R.layout.list_item_divider, cardContainer, true);

		MultiStateCard colorsCard = new MultiStateCard(activity, controller.getColorCardController(), false);
		cardContainer.addView(colorsCard.build());
		inflate(R.layout.list_item_divider, cardContainer, true);

		MultiStateCard iconsCard = new MultiStateCard(requireMapActivity(), controller.getIconController().getCardController());
		cardContainer.addView(iconsCard.build());
		inflate(R.layout.list_item_divider, cardContainer, true);

		MultiStateCard shapeCard = new MultiStateCard(requireMapActivity(), controller.getShapesController());
		cardContainer.addView(shapeCard.build());

		return view;
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

	@Override
	public int getStatusBarColorId() {
		return ColorUtilities.getStatusBarColorId(nightMode);
	}

	@Nullable
	protected PointEditor getEditor() {
		return requireMapActivity().getContextMenu().getFavoritePointEditor();
	}

	public void dismiss() {
		hideKeyboard();
		FragmentActivity activity = getActivity();
		if (activity != null) {
			activity.getSupportFragmentManager().popBackStack();
		}
	}

	protected void hideKeyboard() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			AndroidUtils.hideSoftKeyboard(activity, activity.getCurrentFocus());
		}
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
		DialogButton saveButton = view.findViewById(R.id.apply_button);
		AndroidUiHelper.updateVisibility(saveButton, true);
		saveButton.setOnClickListener(v -> savePressed());
		saveButton.setButtonType(DialogButtonType.PRIMARY);
		saveButton.setTitleId(R.string.shared_string_save);
		AndroidUtils.setBackgroundColor(app, view.findViewById(R.id.buttons_container), ColorUtilities.getListBgColorId(nightMode));
	}

	protected void savePressed() {
		PointEditor editor = getEditor();
		FragmentActivity activity = getActivity();
		if (editor != null && activity != null) {
			String tag = editor.getFragmentTag();
			FragmentManager manager = activity.getSupportFragmentManager();
			DefaultFavoriteAppearanceSaveBottomSheet.showInstance(manager, this, tag, pointsGroup.getPoints().size());
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
				favouritesHelper.saveCurrentPointsIntoFile(true);
			}
		}
		dismiss();
	}

	@Override
	public void onDestroy() {
		MapActivity mapActivity = getMapActivity();
		if (launchPrevIntent && mapActivity != null && !mapActivity.isChangingConfigurations()) {
			mapActivity.launchPrevActivityIntent();
		}

		FragmentActivity activity = getActivity();
		if (activity != null && !activity.isChangingConfigurations()) {
			dialogManager.unregister(FavoriteAppearanceController.PROCESS_ID);
		}

		super.onDestroy();
	}

	@Nullable
	protected MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}

	@NonNull
	protected MapActivity requireMapActivity() {
		return (MapActivity) requireActivity();
	}

	public static void showInstance(@NonNull FragmentManager manager,
	                                @NonNull PointsGroup pointsGroup,
	                                boolean launchPrevIntent) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			FavoriteAppearanceFragment fragment = new FavoriteAppearanceFragment();
			fragment.pointsGroup = pointsGroup;
			fragment.launchPrevIntent = launchPrevIntent;
			fragment.setRetainInstance(true);
			manager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}
