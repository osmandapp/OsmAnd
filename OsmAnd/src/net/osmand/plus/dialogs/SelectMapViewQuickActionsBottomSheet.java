package net.osmand.plus.dialogs;


import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.core.widget.CompoundButtonCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.quickaction.CreateEditActionDialog;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionRegistry;
import net.osmand.plus.quickaction.SwitchableAction;
import net.osmand.plus.quickaction.actions.MapStyleAction;
import net.osmand.plus.quickaction.actions.SwitchProfileAction;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;

import java.util.List;

public class SelectMapViewQuickActionsBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = SelectMapViewQuickActionsBottomSheet.class.getSimpleName();

	private static final String SELECTED_ITEM_KEY = "selected_item";

	private LinearLayout itemsContainer;
	private View.OnClickListener onClickListener;
	private ColorStateList colorStateList;

	private String selectedItem;
	private QuickAction action;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Bundle args = getArguments();
		MapActivity mapActivity = getMapActivity();
		if (args == null || mapActivity == null) {
			return;
		}
		long id = args.getLong(SwitchableAction.KEY_ID);
		OsmandApplication app = mapActivity.getMyApplication();
		QuickActionRegistry quickActionRegistry = app.getQuickActionRegistry();
		action = QuickActionRegistry.produceAction(quickActionRegistry.getQuickAction(id));

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
		int padding = getResources().getDimensionPixelSize(R.dimen.bottom_sheet_content_padding_small);
		itemsContainer.setPadding(0, padding, 0, padding);

		int itemsSize = 0;
		if (action instanceof SwitchableAction) {
			SwitchableAction switchableAction = (SwitchableAction) action;
			List sources = switchableAction.loadListFromParams();
			itemsSize = sources.size();
		}
		for (int i = 0; i < itemsSize; i++) {
			LayoutInflater.from(new ContextThemeWrapper(app, themeRes))
					.inflate(R.layout.bottom_sheet_item_with_radio_btn, itemsContainer, true);
		}

		nestedScrollView.addView(itemsContainer);
		items.add(new BaseBottomSheetItem.Builder().setCustomView(nestedScrollView).create());

		populateItemsList();
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
	protected int getDismissButtonTextId() {
		return R.string.quick_action_edit_actions;
	}

	@Override
	protected void onDismissButtonClickAction() {
		FragmentManager manager = getFragmentManager();
		if (manager != null) {
			CreateEditActionDialog.showInstance(manager, action);
		}
	}

	@Override
	protected boolean useScrollableItemsContainer() {
		return false;
	}

	@Nullable
	private MapActivity getMapActivity() {
		Activity activity = getActivity();
		if (activity instanceof MapActivity) {
			return (MapActivity) activity;
		}
		return null;
	}

	private void populateItemsList() {
		Context context = getContext();
		if (context == null) {
			return;
		}
		int counter = 0;
		if (action instanceof MapStyleAction) {
			MapStyleAction mapStyleAction = (MapStyleAction) action;
			List<String> stylesList = mapStyleAction.getFilteredStyles();
			for (String entry : stylesList) {
				boolean selected = entry.equals(selectedItem);
				createItemRow(selected, counter, getContentIcon(action.getIconRes()),
						mapStyleAction.getTranslatedItemName(context, entry), entry);
				counter++;
			}
		} else if (action instanceof SwitchProfileAction) {
			SwitchProfileAction switchProfileAction = (SwitchProfileAction) action;
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
		} else if (action instanceof SwitchableAction) {
			SwitchableAction switchableAction = (SwitchableAction) action;
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
	}

	@ColorInt
	private int getStyleTitleColor(boolean selected) {
		int colorId = selected ? getActiveColorId() : ColorUtilities.getPrimaryTextColorId(nightMode);
		return getResolvedColor(colorId);
	}

	@NonNull
	private View.OnClickListener getOnClickListener() {
		if (onClickListener == null) {
			onClickListener = v -> {
				MapActivity mapActivity = getMapActivity();
				if (mapActivity == null) {
					return;
				}
				selectedItem = (String) v.getTag();
				if (action instanceof SwitchableAction) {
					((SwitchableAction) action).executeWithParams(mapActivity, selectedItem);
				}
				dismiss();
			};
		}
		return onClickListener;
	}
}