package net.osmand.plus.profiles;


import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.ProgressWithTitleItem;
import net.osmand.plus.profiles.data.ProfileDataObject;
import net.osmand.plus.profiles.data.RoutingDataObject;
import net.osmand.plus.settings.bottomsheets.BasePreferenceBottomSheet;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton.TextRadioItem;
import net.osmand.plus.widgets.tools.ClickableSpanTouchListener;
import net.osmand.util.Algorithms;

public abstract class SelectProfileBottomSheet extends BasePreferenceBottomSheet {

	public static final String TAG = SelectProfileBottomSheet.class.getSimpleName();

	public static final String SELECTED_KEY = "selected_base";

	public static final String PROFILE_KEY_ARG = "profile_key_arg";
	public static final String USE_LAST_PROFILE_ARG = "use_last_profile_arg";
	public static final String PROFILES_LIST_UPDATED_ARG = "is_profiles_list_updated";
	public static final String DERIVED_PROFILE_ARG = "derived_profile";

	protected String selectedItemKey;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle args = getArguments();
		if (args != null) {
			selectedItemKey = args.getString(SELECTED_KEY, null);
		}
		refreshProfiles();
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		view.findViewById(R.id.dismiss_button).setOnClickListener(v -> {
			onDismissButtonClickAction();
			dismiss();
		});
	}

	protected void addProfileItem(ProfileDataObject profile) {
		View itemView = inflate(getItemLayoutId(profile));

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

	protected void addToggleButton(TextRadioItem selectedItem, TextRadioItem... radioItems) {
		int padding = getDimensionPixelSize(R.dimen.content_padding_small);
		LinearLayout container = (LinearLayout) inflate(R.layout.custom_radio_buttons);
		LinearLayout.MarginLayoutParams params = new LinearLayout.MarginLayoutParams(
				LinearLayout.MarginLayoutParams.MATCH_PARENT, LinearLayout.MarginLayoutParams.WRAP_CONTENT);
		AndroidUtils.setMargins(params, padding, padding, padding, 0);
		container.setLayoutParams(params);

		TextToggleButton radioGroup = new TextToggleButton(app, container, nightMode);
		radioGroup.setItems(radioItems);
		radioGroup.setSelectedItem(selectedItem);

		items.add(new BaseBottomSheetItem.Builder().setCustomView(container).create());
	}

	protected void addCheckableItem(int titleId,
									boolean isSelected,
									OnClickListener listener) {
		View itemView = inflate(R.layout.bottom_sheet_item_with_descr_and_radio_btn);
		itemView.findViewById(R.id.icon).setVisibility(View.GONE);
		itemView.findViewById(R.id.description).setVisibility(View.GONE);

		String title = getString(titleId);
		SpannableString spannable = UiUtilities.createCustomFontSpannable(FontCache.getMediumFont(), title, title, title);
		int activeColor = getColor(getActiveColorId());
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
		View buttonView = inflate(R.layout.bottom_sheet_item_preference_btn);
		TextView tvTitle = buttonView.findViewById(R.id.title);
		tvTitle.setText(getString(titleId));

		ImageView ivIcon = buttonView.findViewById(R.id.icon);
		ivIcon.setImageDrawable(getIcon(iconId, getActiveColorId()));

		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(buttonView)
				.setOnClickListener(listener)
				.create());
	}

	protected void addProgressWithTitleItem(CharSequence title) {
		items.add(new ProgressWithTitleItem(title));
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

	protected void addGroupHeader(CharSequence title) {
		addGroupHeader(title, null);
	}

	protected void addGroupHeader(CharSequence title, CharSequence description) {
		View view = inflate(R.layout.bottom_sheet_item_title_with_description_large);

		TextView tvTitle = view.findViewById(R.id.title);
		TextView tvDescription = view.findViewById(R.id.description);
		tvTitle.setText(title);
		if (description != null) {
			tvDescription.setText(description);
			tvDescription.setOnTouchListener(new ClickableSpanTouchListener());
		} else {
			tvDescription.setVisibility(View.GONE);
		}

		items.add(new BaseBottomSheetItem.Builder().setCustomView(view).create());
	}

	protected void addMessageWithRoundedBackground(@NonNull String message) {
		View view = inflate(R.layout.bottom_sheet_item_description_on_rounded_bg);
		int marginBottom = getDimensionPixelSize(R.dimen.content_padding_half);

		TextView tvMessage = view.findViewById(R.id.title);
		tvMessage.setText(message);

		LinearLayout backgroundView = view.findViewById(R.id.background_view);
		int color = ColorUtilities.getActivityBgColor(app, nightMode);
		Drawable bgDrawable = getPaintedIcon(R.drawable.rectangle_rounded, color);
		AndroidUtils.setBackground(backgroundView, bgDrawable);

		MarginLayoutParams params = (MarginLayoutParams) backgroundView.getLayoutParams();
		AndroidUtils.setMargins(params, params.leftMargin, 0, params.rightMargin, marginBottom);

		items.add(new BaseBottomSheetItem.Builder().setCustomView(view).create());
	}

	protected OnClickListener getItemClickListener(ProfileDataObject profile) {
		return view -> onItemSelected(profile);
	}

	protected void onItemSelected(ProfileDataObject profile) {
		Bundle args = new Bundle();
		args.putString(PROFILE_KEY_ARG, profile.getStringKey());
		args.putBoolean(PROFILES_LIST_UPDATED_ARG, isProfilesListUpdated(profile));
		if (profile instanceof RoutingDataObject) {
			args.putString(DERIVED_PROFILE_ARG, ((RoutingDataObject) profile).getDerivedProfile());
		}
		Fragment target = getTargetFragment();
		if (target instanceof OnSelectProfileCallback) {
			((OnSelectProfileCallback) target).onProfileSelected(args);
		}
		dismiss();
	}

	protected boolean isProfilesListUpdated(ProfileDataObject profile) {
		return false;
	}

	@Override
	public void updateMenuItems() {
		refreshProfiles();
		super.updateMenuItems();
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
		return ColorUtilities.getActiveColorId(nightMode);
	}

	@ColorRes
	protected int getDefaultIconColorId() {
		return ColorUtilities.getDefaultIconColorId(nightMode);
	}

	@ColorRes
	protected int getRouteInfoColorId() {
		return nightMode ?
				R.color.icon_color_default_dark :
				R.color.icon_color_default_light;
	}

	public interface OnSelectProfileCallback {
		void onProfileSelected(Bundle args);
	}
}
