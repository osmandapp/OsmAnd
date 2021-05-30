package net.osmand.plus.profiles;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.ProgressWithTitleItem;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.profiles.dto.ProfileDataObject;
import net.osmand.plus.profiles.dto.ProfilesGroup;
import net.osmand.plus.settings.bottomsheets.BasePreferenceBottomSheet;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton.TextRadioItem;
import net.osmand.util.Algorithms;

public abstract class SelectProfileBottomSheet extends BasePreferenceBottomSheet {

	public static final String TAG = SelectProfileBottomSheet.class.getSimpleName();

	public final static String SELECTED_KEY = "selected_base";

	public final static String PROFILE_KEY_ARG = "profile_key_arg";
	public final static String USE_LAST_PROFILE_ARG = "use_last_profile_arg";
	public final static String PROFILES_LIST_UPDATED_ARG = "is_profiles_list_updated";

	protected OsmandApplication app;
	protected String selectedItemKey;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requiredMyApplication();
		Bundle args = getArguments();
		if (args != null) {
			selectedItemKey = args.getString(SELECTED_KEY, null);
			refreshProfiles();
		}
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		view.findViewById(R.id.dismiss_button).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onDismissButtonClickAction();
				dismiss();
			}
		});
	}

	protected void addProfileItem(final ProfileDataObject profile) {
		LayoutInflater inflater = UiUtilities.getInflater(getContext(), nightMode);
		View itemView = inflater.inflate(getItemLayoutId(profile), null);

		TextView tvTitle = itemView.findViewById(R.id.title);
		tvTitle.setText(profile.getName());

		TextView tvDescription = itemView.findViewById(R.id.description);
		tvDescription.setText(profile.getDescription());

		ImageView ivIcon = itemView.findViewById(R.id.icon);
		Drawable drawableIcon = getPaintedIcon(profile.getIconRes(), getIconColor(profile));
		ivIcon.setImageDrawable(drawableIcon);

		CompoundButton compoundButton = itemView.findViewById(R.id.compound_button);
		compoundButton.setChecked(isSelected(profile));
		UiUtilities.setupCompoundButton(compoundButton, nightMode, UiUtilities.CompoundButtonType.GLOBAL);

		BaseBottomSheetItem.Builder builder = new BaseBottomSheetItem.Builder().setCustomView(itemView);
		builder.setOnClickListener(getItemClickListener(profile));
		items.add(builder.create());
	}

	protected void addToggleButton(TextRadioItem selectedItem, TextRadioItem ... radioItems) {
		int padding = getDimen(R.dimen.content_padding_small);
		Context themedCtx = UiUtilities.getThemedContext(app, nightMode);
		LayoutInflater inflater = UiUtilities.getInflater(themedCtx, nightMode);
		LinearLayout container = (LinearLayout) inflater.inflate(R.layout.custom_radio_buttons, null);
		LinearLayout.MarginLayoutParams params = new LinearLayout.MarginLayoutParams(
				LinearLayout.MarginLayoutParams.MATCH_PARENT, LinearLayout.MarginLayoutParams.WRAP_CONTENT);
		AndroidUtils.setMargins(params, padding, padding, padding, 0);
		container.setLayoutParams(params);

		TextToggleButton radioGroup = new TextToggleButton(app, container, nightMode);
		radioGroup.setItems(radioItems);
		radioGroup.setSelectedItem(selectedItem);

		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(container)
				.create()
		);
	}

	protected void addCheckableItem(int titleId,
	                                boolean isSelected,
	                                OnClickListener listener) {
		View itemView = UiUtilities.getInflater(getContext(), nightMode).inflate(R.layout.bottom_sheet_item_with_descr_and_radio_btn, null);
		itemView.findViewById(R.id.icon).setVisibility(View.GONE);
		itemView.findViewById(R.id.description).setVisibility(View.GONE);

		Typeface typeface = FontCache.getRobotoMedium(app);
		String title = getString(titleId);
		SpannableString spannable = UiUtilities.createCustomFontSpannable(typeface, title, title, title);
		int activeColor = ContextCompat.getColor(app, getActiveColorId());
		ForegroundColorSpan colorSpan = new ForegroundColorSpan(activeColor);
		spannable.setSpan(colorSpan, 0, title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		((TextView) itemView.findViewById(R.id.title)).setText(spannable);

		CompoundButton compoundButton = itemView.findViewById(R.id.compound_button);
		compoundButton.setChecked(isSelected);
		UiUtilities.setupCompoundButton(compoundButton, nightMode, UiUtilities.CompoundButtonType.GLOBAL);

		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(itemView)
				.setOnClickListener(listener)
				.create());
	}

	protected void addButtonItem(int titleId, int iconId, OnClickListener listener) {
		Context themedCtx = UiUtilities.getThemedContext(app, nightMode);
		View buttonView = View.inflate(themedCtx, R.layout.bottom_sheet_item_preference_btn, null);
		TextView tvTitle = buttonView.findViewById(R.id.title);
		tvTitle.setText(getString(titleId));

		ImageView ivIcon = buttonView.findViewById(R.id.icon);
		ivIcon.setImageDrawable(app.getUIUtilities().getIcon(iconId, getActiveColorId()));

		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(buttonView)
				.setOnClickListener(listener)
				.create());
	}

	protected void addProgressWithTitleItem(int titleId) {
		items.add(new ProgressWithTitleItem(getString(titleId)));
	}

	protected void addDivider() {
		items.add(new DividerItem(app));
	}

	protected void addSpaceItem(int space) {
		View view = new View(app);
		view.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, space));
		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(view)
				.create());
	}

	protected void addGroupHeaderItem(ProfilesGroup group) {
		Context themedCtx = UiUtilities.getThemedContext(app, nightMode);
		LayoutInflater inflater = UiUtilities.getInflater(themedCtx, nightMode);
		View view = inflater.inflate(R.layout.bottom_sheet_item_title_with_description_72dp, null);

		TextView tvTitle = view.findViewById(R.id.title);
		TextView tvDescription = view.findViewById(R.id.description);
		tvTitle.setText(group.getTitle());
		CharSequence description = group.getDescription(app, nightMode);
		if (description != null) {
			tvDescription.setText(description);
		} else {
			tvDescription.setVisibility(View.GONE);
		}

		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(view)
				.create()
		);
	}

	protected OnClickListener getItemClickListener(final ProfileDataObject profile) {
		return new OnClickListener() {
			@Override
			public void onClick(View view) {
				Bundle args = new Bundle();
				args.putString(PROFILE_KEY_ARG, profile.getStringKey());
				args.putBoolean(PROFILES_LIST_UPDATED_ARG, isProfilesListUpdated(profile));
				Fragment target = getTargetFragment();
				if (target instanceof OnSelectProfileCallback) {
					((OnSelectProfileCallback) target).onProfileSelected(args);
				}
				dismiss();
			}
		};
	}

	protected boolean isProfilesListUpdated(ProfileDataObject profile) {
		return false;
	}

	protected void refreshView() {
		refreshProfiles();
		View mainView = getView();
		Activity activity = getActivity();
		if (activity != null && mainView != null) {
			LinearLayout container = (LinearLayout) mainView.findViewById(useScrollableItemsContainer()
					? R.id.scrollable_items_container : R.id.non_scrollable_items_container);
			if (container != null) {
				container.removeAllViews();
			}
			items.clear();
			createMenuItems(null);
			for (BaseBottomSheetItem item : items) {
				item.inflate(activity, container, nightMode);
			}
			setupHeightAndBackground(mainView);
		}
	}

	protected abstract void refreshProfiles();

	protected boolean isSelected(ProfileDataObject profile) {
		return Algorithms.objectEquals(profile.getStringKey(), selectedItemKey);
	}

	protected int getItemLayoutId(ProfileDataObject profile) {
		return R.layout.bottom_sheet_item_with_descr_and_radio_btn;
	}

	@ColorInt
	protected int getIconColor(ProfileDataObject profile) {
		return profile.getIconColor(nightMode);
	}

	@ColorRes
	protected int getActiveColorId() {
		return nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light;
	}

	@ColorRes
	protected int getDefaultIconColorId() {
		return nightMode ? R.color.icon_color_default_dark : R.color.icon_color_default_light;
	}

	@ColorRes
	protected int getRouteInfoColorId() {
		return nightMode ?
				R.color.route_info_control_icon_color_dark :
				R.color.route_info_control_icon_color_light;
	}

	@Nullable
	protected MapActivity getMapActivity() {
		Activity activity = getActivity();
		if (activity instanceof MapActivity) {
			return (MapActivity) activity;
		}
		return null;
	}

	public interface OnSelectProfileCallback {
		void onProfileSelected(Bundle args);
	}

}
