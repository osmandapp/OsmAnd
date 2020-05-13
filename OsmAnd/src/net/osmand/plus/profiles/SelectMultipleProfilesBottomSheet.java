package net.osmand.plus.profiles;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import net.osmand.CallbackWithObject;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.settings.NavigationFragment;
import net.osmand.plus.settings.bottomsheets.BasePreferenceBottomSheet;

import java.util.ArrayList;
import java.util.List;

public class SelectMultipleProfilesBottomSheet extends BasePreferenceBottomSheet {

	public static final String TAG = SelectMultipleProfilesBottomSheet.class.getSimpleName();
	public static final String SELECTED_KEYS = "selected_keys";
	public static final String DISABLED_KEYS = "disabled_keys";

	private List<ProfileDataObject> profiles = new ArrayList<>();
	private CallbackWithObject<List<String>> callback;
	private List<String> selectedProfiles;
	private List<String> disabledProfiles;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle args = getArguments();
		if (args != null) {
			selectedProfiles = args.getStringArrayList(SELECTED_KEYS);
			disabledProfiles = args.getStringArrayList(DISABLED_KEYS);
			refreshProfiles(getMyApplication());
		}
	}

	private void refreshProfiles(OsmandApplication app) {
		profiles.clear();
		profiles.addAll(NavigationFragment.getBaseProfiles(app));
		for (ProfileDataObject profile : profiles) {
			String key = profile.getStringKey();
			profile.setSelected(selectedProfiles.contains(key));
			profile.setEnabled(!disabledProfiles.contains(key));
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new TitleItem(getString(R.string.application_profiles)));

		for (int i = 0; i < profiles.size(); i++) {
			addProfileItem(profiles.get(i));
		}
	}

	private void addProfileItem(final ProfileDataObject profile) {
		OsmandApplication app = requiredMyApplication();
		View itemView = UiUtilities.getInflater(app, nightMode)
				.inflate(R.layout.bottom_sheet_item_with_descr_and_checkbox_56dp, null);

		int profileColorId = profile.getIconColor(nightMode);
		int activeColorId = nightMode ?
				R.color.active_color_primary_dark : R.color.active_color_primary_light;
		int disableColorId = nightMode ?
				R.color.icon_color_default_dark : R.color.icon_color_default_light;
		boolean enable = profile.isEnabled();

		TextView tvTitle = itemView.findViewById(R.id.title);
		TextView tvDescription = itemView.findViewById(R.id.description);
		ImageView ivIcon = itemView.findViewById(R.id.icon);
		final CompoundButton compoundButton = itemView.findViewById(R.id.compound_button);

		tvTitle.setText(profile.getName());
		tvDescription.setText(profile.getDescription());

		if (!enable) {
			tvTitle.setTextColor(ContextCompat.getColor(app, disableColorId));
			tvDescription.setTextColor(ContextCompat.getColor(app, disableColorId));
		}

		Drawable drawableIcon = app.getUIUtilities().getIcon(
				profile.getIconRes(), enable ? profileColorId : disableColorId);
		ivIcon.setImageDrawable(drawableIcon);
		UiUtilities.setupCompoundButton(nightMode, ContextCompat.getColor(app,
				enable ? activeColorId : disableColorId), compoundButton);
		compoundButton.setChecked(profile.isSelected());

		View.OnClickListener l = !enable ? null : new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean selected = !profile.isSelected();
				profile.setSelected(selected);
				compoundButton.setChecked(selected);
			}
		};

		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(itemView)
				.setOnClickListener(l)
				.create());
	}

	@Override
	protected void onDismissButtonClickAction() {
		super.onDismissButtonClickAction();
		dismiss();
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_apply;
	}

	@Override
	protected void onRightBottomButtonClick() {
		if (callback != null) {
			List<String> selectedProfileKeys = new ArrayList<>();
			for (ProfileDataObject profile : profiles) {
				if (profile.isSelected() && profile.isEnabled()) {
					selectedProfileKeys.add(profile.getStringKey());
				}
			}
			callback.processResult(selectedProfileKeys);
		}
		dismiss();
	}

	public void setCallback(CallbackWithObject<List<String>> callback) {
		this.callback = callback;
	}

	public static void showInstance(@NonNull MapActivity mapActivity,
	                                @Nullable List<String> selectedProfiles,
	                                @Nullable List<String> disabledProfiles,
	                                boolean usedOnMap,
	                                CallbackWithObject<List<String>> callback) {
		SelectMultipleProfilesBottomSheet fragment = new SelectMultipleProfilesBottomSheet();
		Bundle args = new Bundle();
		args.putStringArrayList(SELECTED_KEYS, selectedProfiles != null ?
				new ArrayList<>(selectedProfiles) : new ArrayList<String>());
		args.putStringArrayList(DISABLED_KEYS, disabledProfiles != null ?
				new ArrayList<>(disabledProfiles) : new ArrayList<String>());
		fragment.setArguments(args);
		fragment.setUsedOnMap(usedOnMap);
		fragment.setCallback(callback);
		fragment.show(mapActivity.getSupportFragmentManager(), SelectMultipleProfilesBottomSheet.TAG);
	}

}
