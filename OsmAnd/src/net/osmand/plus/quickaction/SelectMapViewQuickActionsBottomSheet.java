package net.osmand.plus.quickaction;


import static net.osmand.plus.quickaction.AddQuickActionFragment.QUICK_ACTION_BUTTON_KEY;
import static net.osmand.plus.quickaction.SwitchableAction.KEY_ID;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.core.widget.CompoundButtonCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.srtm.TerrainColorSchemeAction;
import net.osmand.plus.quickaction.actions.ChangeMapOrientationAction;
import net.osmand.plus.quickaction.actions.MapStyleAction;
import net.osmand.plus.quickaction.actions.SwitchProfileAction;
import net.osmand.plus.quickaction.controller.AddQuickActionController;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.enums.CompassMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.mapwidgets.configure.buttons.QuickActionButtonState;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;

import java.util.List;

public class SelectMapViewQuickActionsBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = SelectMapViewQuickActionsBottomSheet.class.getSimpleName();

	private static final String SELECTED_ITEM_KEY = "selected_item";

	private LinearLayout itemsContainer;
	private View.OnClickListener onClickListener;
	private ColorStateList colorStateList;

	private String selectedItem;
	private QuickAction action;
	private QuickActionButtonState buttonState;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Bundle args = getArguments();
		if (args == null) return;

		MapButtonsHelper mapButtonsHelper = app.getMapButtonsHelper();

		String key = args.getString(QUICK_ACTION_BUTTON_KEY);
		if (key != null) {
			buttonState = mapButtonsHelper.getActionButtonStateById(key);
		}
		long actionId = args.getLong(KEY_ID);
		action = MapButtonsHelper.produceAction(buttonState.getQuickAction(actionId));

		if (savedInstanceState != null) {
			selectedItem = savedInstanceState.getString(SELECTED_ITEM_KEY);
		} else {
			selectedItem = ((SwitchableAction<?>) action).getSelectedItem(app);
		}
		colorStateList = AndroidUtils.createCheckedColorStateList(app, R.color.icon_color_default_light, getActiveColorId());

		items.add(new TitleItem(action.getName(app)));

		NestedScrollView nestedScrollView = new NestedScrollView(app);
		itemsContainer = new LinearLayout(app);
		itemsContainer.setLayoutParams((new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)));
		itemsContainer.setOrientation(LinearLayout.VERTICAL);
		int padding = getDimensionPixelSize(R.dimen.bottom_sheet_content_padding_small);
		itemsContainer.setPadding(0, padding, 0, padding);

		int itemsSize = 0;
		if (action instanceof SwitchableAction switchableAction) {
			itemsSize = switchableAction.loadListFromParams().size();
		}
		for (int i = 0; i < itemsSize; i++) {
			inflate(getLayoutId(), itemsContainer, true);
		}

		nestedScrollView.addView(itemsContainer);
		items.add(new BaseBottomSheetItem.Builder().setCustomView(nestedScrollView).create());

		populateItemsList();
	}

	@LayoutRes
	private int getLayoutId() {
		if (action instanceof ChangeMapOrientationAction) {
			return R.layout.bottom_sheet_item_with_descr_and_radio_btn;
		} else {
			return R.layout.bottom_sheet_item_with_radio_btn;
		}
	}
	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(SELECTED_ITEM_KEY, selectedItem);
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_close;
	}

	@Override
	protected void onRightBottomButtonClick() {
		dismiss();
	}

	@Override
	protected DialogButtonType getRightBottomButtonType() {
		if (action instanceof ChangeMapOrientationAction) {
			return DialogButtonType.SECONDARY;
		} else {
			return DialogButtonType.PRIMARY;
		}
	}

	@Override
	protected int getDismissButtonTextId() {
		if (action instanceof ChangeMapOrientationAction) {
			return DEFAULT_VALUE;
		} else {
			return R.string.quick_action_edit_actions;
		}
	}

	@Override
	protected void onDismissButtonClickAction() {
		FragmentManager manager = getFragmentManager();
		if (manager != null) {
			AddQuickActionController.showCreateEditActionDialog(app, manager, buttonState, action);
		}
	}

	@Override
	protected boolean useScrollableItemsContainer() {
		return false;
	}

	private void populateItemsList() {
		int counter = 0;
		if (action instanceof MapStyleAction mapStyleAction) {
			List<String> stylesList = mapStyleAction.getFilteredStyles();
			for (String entry : stylesList) {
				boolean selected = entry.equals(selectedItem);
				createItemRow(selected, counter, getContentIcon(action.getIconRes()),
						mapStyleAction.getTranslatedItemName(app, entry), entry);
				counter++;
			}
		} else if (action instanceof TerrainColorSchemeAction terrainColorSchemeAction) {
			List<String> terrainModes = terrainColorSchemeAction.getFilteredStyles();
			for (String entry : terrainModes) {
				boolean selected = entry.equals(selectedItem);
				createItemRow(selected, counter, getContentIcon(action.getIconRes()),
						terrainColorSchemeAction.getTranslatedItemName(app, entry), entry);
				counter++;
			}
		} else if (action instanceof SwitchProfileAction switchProfileAction) {
			List<String> profilesKeys = switchProfileAction.loadListFromParams();
			for (String key : profilesKeys) {
				ApplicationMode appMode = ApplicationMode.valueOfStringKey(key, null);
				if (appMode != null) {
					boolean selected = key.equals(selectedItem);
					int iconId = appMode.getIconRes();
					int color = appMode.getProfileColor(nightMode);
					Drawable icon = getPaintedIcon(iconId, color);
					String translatedName = appMode.toHumanString();
					createItemRow(selected, counter, icon, translatedName, key);
					counter++;
				}
			}
		} else if (action instanceof ChangeMapOrientationAction mapOrientationAction) {
			List<String> compassModes = mapOrientationAction.loadListFromParams();
			for (String key : compassModes) {
				CompassMode compassMode = CompassMode.valueOf(key);
				boolean selected = key.equals(selectedItem);
				int iconId = compassMode.getIconId(nightMode);
				Drawable icon = getIcon(iconId);
				String translatedName = compassMode.getTitle(app);
				createItemRow(selected, counter, icon, translatedName, key);
				counter++;
			}
		} else if (action instanceof SwitchableAction switchableAction) {
			List<Pair<String, String>> sources = (List<Pair<String, String>>) switchableAction.loadListFromParams();
			for (Pair<String, String> entry : sources) {
				String tag = entry.first;
				boolean selected = tag.equals(selectedItem);
				createItemRow(selected, counter, getContentIcon(action.getIconRes()), entry.second, tag);
				counter++;
			}
		}
	}

	private void createItemRow(boolean selected, int counter, Drawable icon, String text, String tag) {
		View view = itemsContainer.getChildAt(counter);
		view.setTag(tag);
		view.setOnClickListener(getOnClickListener());

		TextView titleTv = view.findViewById(R.id.title);
		titleTv.setText(text);
		titleTv.setTextColor(getStyleTitleColor(selected));

		RadioButton rb = view.findViewById(R.id.compound_button);
		rb.setChecked(selected);
		CompoundButtonCompat.setButtonTintList(rb, colorStateList);
		ImageView imageView = view.findViewById(R.id.icon);
		imageView.setImageDrawable(icon);

		TextView descriptionTv = view.findViewById(R.id.description);
		AndroidUiHelper.updateVisibility(descriptionTv, false);
	}

	@ColorInt
	private int getStyleTitleColor(boolean selected) {
		int colorId = selected ? getActiveColorId() : ColorUtilities.getPrimaryTextColorId(nightMode);
		return getColor(colorId);
	}

	@NonNull
	private View.OnClickListener getOnClickListener() {
		if (onClickListener == null) {
			onClickListener = v -> callMapActivity(mapActivity -> {
				selectedItem = (String) v.getTag();
				if (action instanceof SwitchableAction) {
					((SwitchableAction) action).executeWithParams(mapActivity, selectedItem);
				}
				dismiss();
			});
		}
		return onClickListener;
	}

	public static void showInstance(@NonNull FragmentManager manager,
	                                @NonNull QuickActionButtonState buttonState, long actionId) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			Bundle args = new Bundle();
			args.putLong(KEY_ID, actionId);
			args.putString(QUICK_ACTION_BUTTON_KEY, buttonState.getId());

			SelectMapViewQuickActionsBottomSheet fragment = new SelectMapViewQuickActionsBottomSheet();
			fragment.setArguments(args);
			fragment.show(manager, TAG);
		}
	}
}