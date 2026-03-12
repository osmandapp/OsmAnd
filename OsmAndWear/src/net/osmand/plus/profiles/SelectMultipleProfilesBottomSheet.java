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
import androidx.fragment.app.Fragment;

import net.osmand.CallbackWithObject;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.profiles.data.ProfileDataObject;
import net.osmand.plus.profiles.data.ProfileDataUtils;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.bottomsheets.BasePreferenceBottomSheet;

import java.util.ArrayList;
import java.util.List;

public class SelectMultipleProfilesBottomSheet extends BasePreferenceBottomSheet {

	public static final String TAG = SelectMultipleProfilesBottomSheet.class.getSimpleName();
	public static final String SELECTED_KEYS = "selected_keys";
	public static final String DISABLED_KEYS = "disabled_keys";

	private final List<ProfileDataObject> profiles = new ArrayList<>();
	private List<String> selectedProfiles;
	private List<String> disabledProfiles;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle args = getArguments();
		if (savedInstanceState != null) {
			readBundle(savedInstanceState);
		} else if (args != null) {
			readBundle(args);
		}
	}

	private void readBundle(Bundle bundle) {
		selectedProfiles = bundle.getStringArrayList(SELECTED_KEYS);
		disabledProfiles = bundle.getStringArrayList(DISABLED_KEYS);
		refreshProfiles(getMyApplication());
	}

	private void refreshProfiles(OsmandApplication app) {
		profiles.clear();
		List<ApplicationMode> appModes = ApplicationMode.allPossibleValues();
		profiles.addAll(ProfileDataUtils.getDataObjects(app, appModes));
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

	private void addProfileItem(ProfileDataObject profile) {
		OsmandApplication app = requiredMyApplication();
		View itemView = UiUtilities.getInflater(app, nightMode)
				.inflate(R.layout.bottom_sheet_item_with_descr_and_checkbox_56dp, null);

		int profileColor = profile.getIconColor(nightMode);
		int activeColorId = ColorUtilities.getActiveColorId(nightMode);
		int disableColorId = ColorUtilities.getDefaultIconColorId(nightMode);
		int disableColor = ContextCompat.getColor(app, disableColorId);
		boolean enable = profile.isEnabled();

		TextView tvTitle = itemView.findViewById(R.id.title);
		TextView tvDescription = itemView.findViewById(R.id.description);
		ImageView ivIcon = itemView.findViewById(R.id.icon);
		CompoundButton compoundButton = itemView.findViewById(R.id.compound_button);

		tvTitle.setText(profile.getName());
		tvDescription.setText(profile.getDescription());

		if (!enable) {
			tvTitle.setTextColor(ContextCompat.getColor(app, disableColorId));
			tvDescription.setTextColor(ContextCompat.getColor(app, disableColorId));
		}

		Drawable drawableIcon = app.getUIUtilities().getPaintedIcon(
				profile.getIconRes(), enable ? profileColor : disableColor);
		ivIcon.setImageDrawable(drawableIcon);
		UiUtilities.setupCompoundButton(nightMode, ContextCompat.getColor(app,
				enable ? activeColorId : disableColorId), compoundButton);
		compoundButton.setSaveEnabled(false);
		compoundButton.setChecked(profile.isSelected());

		View.OnClickListener l = !enable ? null : new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String key = profile.getStringKey();
				boolean selected = !profile.isSelected();
				if (selected) {
					selectedProfiles.add(key);
				} else {
					selectedProfiles.remove(key);
				}
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
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putStringArrayList(SELECTED_KEYS, new ArrayList<>(selectedProfiles));
		outState.putStringArrayList(DISABLED_KEYS, new ArrayList<>(disabledProfiles));
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
		Fragment targetFragment = getTargetFragment();
		if (targetFragment instanceof CallbackWithObject) {
			List<String> newSelected = new ArrayList<>();
			for (String profile : selectedProfiles) {
				if (!disabledProfiles.contains(profile)) {
					newSelected.add(profile);
				}
			}
			((CallbackWithObject) targetFragment).processResult(newSelected);
		}
		dismiss();
	}

	public static void showInstance(@NonNull MapActivity mapActivity, Fragment targetFragment,
	                                @Nullable List<String> selectedProfiles,
	                                @Nullable List<String> disabledProfiles,
	                                boolean usedOnMap) {
		SelectMultipleProfilesBottomSheet fragment = new SelectMultipleProfilesBottomSheet();
		Bundle args = new Bundle();
		args.putStringArrayList(SELECTED_KEYS, selectedProfiles != null ?
				new ArrayList<>(selectedProfiles) : new ArrayList<String>());
		args.putStringArrayList(DISABLED_KEYS, disabledProfiles != null ?
				new ArrayList<>(disabledProfiles) : new ArrayList<String>());
		fragment.setArguments(args);
		fragment.setTargetFragment(targetFragment, 0);
		fragment.setUsedOnMap(usedOnMap);
		fragment.show(mapActivity.getSupportFragmentManager(), TAG);
	}

}
