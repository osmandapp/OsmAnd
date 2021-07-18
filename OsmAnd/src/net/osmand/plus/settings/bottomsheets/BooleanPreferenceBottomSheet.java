package net.osmand.plus.settings.bottomsheets;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.BooleanPreference;
import net.osmand.plus.settings.backend.OsmandPreference;
import net.osmand.plus.settings.fragments.ApplyQueryType;
import net.osmand.plus.settings.fragments.OnConfirmPreferenceChange;
import net.osmand.plus.settings.fragments.OnPreferenceChanged;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;

import org.apache.commons.logging.Log;

import static net.osmand.plus.monitoring.TripRecordingBottomSheet.getSecondaryIconColorId;

public class BooleanPreferenceBottomSheet extends BasePreferenceBottomSheet {

	public static final String TAG = BooleanPreferenceBottomSheet.class.getSimpleName();

	private static final Log LOG = PlatformUtil.getLog(BooleanPreferenceBottomSheet.class);

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		final OsmandApplication app = getMyApplication();
		if (app == null) {
			return;
		}
		final SwitchPreferenceEx switchPreference = getSwitchPreferenceEx();
		if (switchPreference == null) {
			return;
		}
		OsmandPreference preference = app.getSettings().getPreference(switchPreference.getKey());
		if (!(preference instanceof BooleanPreference)) {
			return;
		}
		Context themedCtx = UiUtilities.getThemedContext(app, nightMode);

		String title = switchPreference.getTitle().toString();
		items.add(new TitleItem(title));

		final BooleanPreference pref = (BooleanPreference) preference;
		CharSequence summaryOn = switchPreference.getSummaryOn();
		CharSequence summaryOff = switchPreference.getSummaryOff();
		final String on = summaryOn == null || summaryOn.toString().isEmpty()
				? getString(R.string.shared_string_enabled) : summaryOn.toString();
		final String off = summaryOff == null || summaryOff.toString().isEmpty()
				? getString(R.string.shared_string_disabled) : summaryOff.toString();
		final int activeColor = AndroidUtils.resolveAttribute(themedCtx, R.attr.active_color_basic);
		final int disabledColor = AndroidUtils.resolveAttribute(themedCtx, android.R.attr.textColorSecondary);
		boolean checked = switchPreference.isChecked();

		final BottomSheetItemWithCompoundButton[] preferenceBtn = new BottomSheetItemWithCompoundButton[1];
		preferenceBtn[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
				.setChecked(checked)
				.setTitle(checked ? on : off)
				.setTitleColorId(checked ? activeColor : disabledColor)
				.setCustomView(getCustomButtonView(app, getAppMode(), checked, nightMode))
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						boolean newValue = !switchPreference.isChecked();
						Fragment targetFragment = getTargetFragment();
						if (targetFragment instanceof OnConfirmPreferenceChange) {
							ApplyQueryType applyQueryType = getApplyQueryType();
							if (applyQueryType == ApplyQueryType.SNACK_BAR) {
								applyQueryType = ApplyQueryType.NONE;
							}
							OnConfirmPreferenceChange confirmationInterface =
									(OnConfirmPreferenceChange) targetFragment;
							if (confirmationInterface.onConfirmPreferenceChange(
									switchPreference.getKey(), newValue, applyQueryType)) {
								switchPreference.setChecked(newValue);
								preferenceBtn[0].setTitle(newValue ? on : off);
								preferenceBtn[0].setChecked(newValue);
								preferenceBtn[0].setTitleColorId(newValue ? activeColor : disabledColor);
								updateCustomButtonView(app, getAppMode(), v, newValue, nightMode);

								if (targetFragment instanceof OnPreferenceChanged) {
									((OnPreferenceChanged) targetFragment).onPreferenceChanged(switchPreference.getKey());
								}
							}
						}
					}
				})
				.create();
		if (isProfileDependent()) {
			preferenceBtn[0].setCompoundButtonColor(getAppMode().getProfileColor(nightMode));
		}
		items.add(preferenceBtn[0]);

		String description = switchPreference.getDescription();
		if (description != null) {
			BaseBottomSheetItem preferenceDescription = new BottomSheetItemWithDescription.Builder()
					.setDescription(description)
					.setLayoutId(R.layout.bottom_sheet_item_descr)
					.create();
			items.add(preferenceDescription);
		}
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	public static View getCustomButtonView(OsmandApplication app, ApplicationMode mode, boolean checked, boolean nightMode) {
		View customView = UiUtilities.getInflater(app, nightMode).inflate(R.layout.bottom_sheet_item_preference_switch, null);
		updateCustomButtonView(app, mode, customView, checked, nightMode);
		return customView;
	}

	public static void updateCustomButtonView(OsmandApplication app, ApplicationMode mode, View customView, boolean checked, boolean nightMode) {
		Context themedCtx = UiUtilities.getThemedContext(app, nightMode);
		boolean isLayoutRtl = AndroidUtils.isLayoutRtl(themedCtx);
		LinearLayout buttonView = customView.findViewById(R.id.button_container);

		int bgColor;
		int selectedColor;
		if (mode != null) {
			int color = checked ? mode.getProfileColor(nightMode) : AndroidUtils.getColorFromAttr(themedCtx, R.attr.divider_color_basic);
			bgColor = UiUtilities.getColorWithAlpha(color, checked ? 0.1f : 0.5f);
			selectedColor = UiUtilities.getColorWithAlpha(color, checked ? 0.3f : 0.5f);
		} else {
			bgColor = ContextCompat.getColor(app, checked
					? getActiveColorId(nightMode) : getSecondaryIconColorId(nightMode));
			selectedColor = UiUtilities.getColorWithAlpha(
					ContextCompat.getColor(app, getActiveColorId(nightMode)), checked ? 0.3f : 0.5f);
		}

		int bgResId = isLayoutRtl ? R.drawable.rectangle_rounded_left : R.drawable.rectangle_rounded_right;
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
			int selectableResId = isLayoutRtl ?
					R.drawable.ripple_rectangle_rounded_left :
					R.drawable.ripple_rectangle_rounded_right;
			Drawable bgDrawable = app.getUIUtilities().getPaintedIcon(bgResId, bgColor);
			Drawable selectable = app.getUIUtilities().getPaintedIcon(selectableResId, selectedColor);
			Drawable[] layers = {bgDrawable, selectable};
			AndroidUtils.setBackground(buttonView, new LayerDrawable(layers));
		} else {
			Drawable bgDrawable = app.getUIUtilities().getPaintedIcon(bgResId, bgColor);
			AndroidUtils.setBackground(buttonView, bgDrawable);
		}
	}

	private SwitchPreferenceEx getSwitchPreferenceEx() {
		return (SwitchPreferenceEx) getPreference();
	}

	public static void showInstance(@NonNull FragmentManager fm, String prefId, Fragment target, boolean usedOnMap,
									@Nullable ApplicationMode appMode, ApplyQueryType applyQueryType,
									boolean profileDependent) {
		try {
			if (fm.findFragmentByTag(BooleanPreferenceBottomSheet.TAG) == null) {
				Bundle args = new Bundle();
				args.putString(PREFERENCE_ID, prefId);

				BooleanPreferenceBottomSheet fragment = new BooleanPreferenceBottomSheet();
				fragment.setArguments(args);
				fragment.setUsedOnMap(usedOnMap);
				fragment.setAppMode(appMode);
				fragment.setApplyQueryType(applyQueryType);
				fragment.setTargetFragment(target, 0);
				fragment.setProfileDependent(profileDependent);
				fragment.show(fm, BooleanPreferenceBottomSheet.TAG);
			}
		} catch (RuntimeException e) {
			LOG.error("showInstance", e);
		}
	}
}