package net.osmand.plus.liveupdates;

import static android.graphics.Typeface.DEFAULT;
import static net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.*;
import static net.osmand.plus.settings.bottomsheets.BooleanPreferenceBottomSheet.getCustomButtonView;
import static net.osmand.plus.settings.bottomsheets.BooleanPreferenceBottomSheet.updateCustomButtonView;
import static net.osmand.plus.utils.UiUtilities.CompoundButtonType.TOOLBAR;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.CompoundButton;
import android.widget.FrameLayout.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescriptionDifHeight;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.liveupdates.LiveUpdatesClearBottomSheet.RefreshLiveUpdates;
import net.osmand.plus.resources.IncrementalChangesManager;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.plus.widgets.multistatetoggle.RadioItem.OnRadioItemClickListener;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton.TextRadioItem;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

public class LiveUpdatesSettingsBottomSheet extends MenuBottomSheetDialogFragment
		implements RefreshLiveUpdates, DownloadEvents {

	public static final String TAG = LiveUpdatesSettingsBottomSheet.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(LiveUpdatesSettingsBottomSheet.class);
	private static final String LOCAL_INDEX_FILE_NAME = "local_index_file_name";

	private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd, HH:mm", Locale.US);

	private BaseBottomSheetItem itemTitle;
	private BaseBottomSheetItem itemUpdateTimeInfo;
	private BaseBottomSheetItem itemSwitchLiveUpdate;
	private BaseBottomSheetItem itemFrequencyHelpMessage;
	private BaseBottomSheetItem itemClear;
	private BaseBottomSheetItem itemViaWiFi;
	private TextToggleButton frequencyToggleButton;
	private TextToggleButton timeOfDayToggleButton;

	private String fileName;
	private OnLiveUpdatesForLocalChange onLiveUpdatesForLocalChange;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		if (getTargetFragment() instanceof OnLiveUpdatesForLocalChange) {
			onLiveUpdatesForLocalChange = (OnLiveUpdatesForLocalChange) getTargetFragment();
		}
		if (savedInstanceState != null && savedInstanceState.containsKey(LOCAL_INDEX_FILE_NAME)) {
			fileName = savedInstanceState.getString(LOCAL_INDEX_FILE_NAME);
		}

		CommonPreference<Boolean> localUpdatePreference = preferenceForLocalIndex(fileName, settings);
		CommonPreference<Boolean> downloadViaWiFiPreference = preferenceDownloadViaWiFi(fileName, settings);
		CommonPreference<Integer> frequencyPreference = preferenceUpdateFrequency(fileName, settings);
		CommonPreference<Integer> timeOfDayPreference = preferenceTimeOfDayToUpdate(fileName, settings);

		int dp6 = getDimensionPixelSize(R.dimen.content_padding_small_half);
		int dp8 = getDimensionPixelSize(R.dimen.content_padding_half);
		int dp12 = getDimensionPixelSize(R.dimen.content_padding_small);
		int dp16 = getDimensionPixelSize(R.dimen.content_padding);
		int dp24 = getDimensionPixelSize(R.dimen.dialog_content_margin);
		int dp36 = getDimensionPixelSize(R.dimen.dialog_button_height);

		itemTitle = new SimpleBottomSheetItem.Builder()
				.setTitle(getNameToDisplay(fileName, app))
				.setTitleColorId(ColorUtilities.getPrimaryTextColorId(nightMode))
				.setLayoutId(R.layout.bottom_sheet_item_title_big)
				.create();
		items.add(itemTitle);

		itemUpdateTimeInfo = new BaseBottomSheetItem.Builder()
				.setCustomView(inflate(R.layout.live_update_time_info))
				.create();
		items.add(itemUpdateTimeInfo);
		refreshUpdateTimeInfo();

		Context context = requireContext();
		View itemLiveUpdate = getCustomButtonView(context, null, localUpdatePreference.get(), nightMode);
		View itemLiveUpdateButton = itemLiveUpdate.findViewById(R.id.button_container);
		CompoundButton button = itemLiveUpdateButton.findViewById(R.id.compound_button);
		UiUtilities.setupCompoundButton(button, nightMode, TOOLBAR);
		itemLiveUpdateButton.setMinimumHeight(getDimensionPixelSize(R.dimen.bottom_sheet_selected_item_title_height));
		itemSwitchLiveUpdate = new BottomSheetItemWithCompoundButton.Builder()
				.setChecked(localUpdatePreference.get())
				.setTitle(getStateText(localUpdatePreference.get()))
				.setTitleColorId(ColorUtilities.getActiveTabTextColorId(nightMode))
				.setCustomView(itemLiveUpdate)
				.setOnClickListener(view -> {
					BottomSheetItemWithCompoundButton item = (BottomSheetItemWithCompoundButton) itemSwitchLiveUpdate;
					boolean checked = item.isChecked();
					item.setChecked(!checked);
					if (onLiveUpdatesForLocalChange != null
							&& onLiveUpdatesForLocalChange.onUpdateLocalIndex(fileName, !checked, () -> {
								refreshUpdateTimeInfo();
								refreshUpdateFrequencyMessage();
								updateFileSize();
								Fragment target = getTargetFragment();
								if (target instanceof LiveUpdatesFragment) {
									((LiveUpdatesFragment) target).runSort();
								}
							})) {
						item.setTitle(getStateText(!checked));
						updateCustomButtonView(context, null, item.getView(), !checked, nightMode);
						CommonPreference<Boolean> localUpdatePreference1 = preferenceForLocalIndex(fileName, settings);
						frequencyToggleButton.setItemsEnabled(localUpdatePreference1.get());
						timeOfDayToggleButton.setItemsEnabled(localUpdatePreference1.get());
						setStateViaWiFiButton(localUpdatePreference1);
					}
				})
				.create();
		items.add(itemSwitchLiveUpdate);

		TextViewEx frequencyTitle = (TextViewEx) inflate(R.layout.bottom_sheet_item_title);
		frequencyTitle.setMinHeight(dp24);
		frequencyTitle.setMinimumHeight(dp24);
		frequencyTitle.setText(R.string.update_frequency);
		frequencyTitle.setTypeface(DEFAULT);
		AndroidUtils.setPadding(frequencyTitle, dp16, dp12, dp16, dp12);
		AndroidUtils.setTextPrimaryColor(app, frequencyTitle, nightMode);
		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(frequencyTitle)
				.create());

		LinearLayout itemFrequencyButtons = (LinearLayout) inflate(R.layout.custom_radio_buttons);
		LinearLayout.MarginLayoutParams itemFrequencyParams = new LinearLayout.MarginLayoutParams(
				LinearLayout.MarginLayoutParams.MATCH_PARENT, LinearLayout.MarginLayoutParams.WRAP_CONTENT);
		AndroidUtils.setMargins(itemFrequencyParams, dp16, 0, dp16, 0);
		itemFrequencyButtons.setLayoutParams(itemFrequencyParams);

		String hourly = getString(R.string.hourly);
		String daily = getString(R.string.daily);
		String weekly = getString(R.string.weekly);
		TextRadioItem hourlyButton = new TextRadioItem(hourly);
		TextRadioItem dailyButton = new TextRadioItem(daily);
		TextRadioItem weeklyButton = new TextRadioItem(weekly);
		frequencyToggleButton = new TextToggleButton(app, itemFrequencyButtons, nightMode);
		frequencyToggleButton.setItems(hourlyButton, dailyButton, weeklyButton);
		setSelectedRadioItem(frequencyToggleButton, frequencyPreference.get(), hourlyButton, dailyButton, weeklyButton);
		frequencyToggleButton.setItemsEnabled(localUpdatePreference.get());

		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(itemFrequencyButtons)
				.create());

		TextViewEx timeOfDayTitle = (TextViewEx) inflate(R.layout.bottom_sheet_item_title);
		timeOfDayTitle.setMinHeight(dp24);
		timeOfDayTitle.setMinimumHeight(dp24);
		timeOfDayTitle.setText(R.string.update_time);
		timeOfDayTitle.setTypeface(DEFAULT);
		AndroidUtils.setPadding(timeOfDayTitle, dp16, dp16, dp16, dp6);
		AndroidUtils.setTextPrimaryColor(app, timeOfDayTitle, nightMode);
		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(timeOfDayTitle)
				.create());

		LinearLayout itemTimeOfDayButtons = (LinearLayout) inflate(R.layout.custom_radio_buttons);
		LinearLayout.MarginLayoutParams itemTimeOfDayParams = new LinearLayout.MarginLayoutParams(
				LinearLayout.MarginLayoutParams.MATCH_PARENT, LinearLayout.MarginLayoutParams.WRAP_CONTENT);
		AndroidUtils.setMargins(itemTimeOfDayParams, dp16, 0, dp16, 0);
		itemTimeOfDayButtons.setLayoutParams(itemTimeOfDayParams);

		String morning = getString(R.string.morning);
		String night = getString(R.string.night);
		TextRadioItem morningButton = new TextRadioItem(morning);
		TextRadioItem nightButton = new TextRadioItem(night);
		timeOfDayToggleButton = new TextToggleButton(app, itemTimeOfDayButtons, nightMode);
		timeOfDayToggleButton.setItems(morningButton, nightButton);
		setSelectedRadioItem(timeOfDayToggleButton, timeOfDayPreference.get(), morningButton, nightButton);
		timeOfDayToggleButton.setItemsEnabled(localUpdatePreference.get());
		refreshTimeOfDayLayout(frequencyPreference.get(), itemTimeOfDayButtons, timeOfDayTitle);

		morningButton.setOnClickListener(getTimeOfDayButtonListener(TimeOfDay.MORNING));
		nightButton.setOnClickListener(getTimeOfDayButtonListener(TimeOfDay.NIGHT));

		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(itemTimeOfDayButtons)
				.create());

		hourlyButton.setOnClickListener(getFrequencyButtonListener(UpdateFrequency.HOURLY, itemTimeOfDayButtons, timeOfDayTitle));
		dailyButton.setOnClickListener(getFrequencyButtonListener(UpdateFrequency.DAILY, itemTimeOfDayButtons, timeOfDayTitle));
		weeklyButton.setOnClickListener(getFrequencyButtonListener(UpdateFrequency.WEEKLY, itemTimeOfDayButtons, timeOfDayTitle));

		itemFrequencyHelpMessage = new BottomSheetItemWithDescriptionDifHeight.Builder()
				.setMinHeight(0)
				.setDescription(getString(getUpdateFrequency().descId))
				.setDescriptionColorId(ColorUtilities.getSecondaryTextColorId(nightMode))
				.setLayoutId(R.layout.bottom_sheet_item_description)
				.create();
		items.add(itemFrequencyHelpMessage);

		LinearLayout itemUpdateNowButton =
				(LinearLayout) inflate(R.layout.bottom_sheet_button_with_icon_center);
		LinearLayout.MarginLayoutParams itemUpdateNowParams = new LinearLayout.MarginLayoutParams(
				LinearLayout.MarginLayoutParams.MATCH_PARENT, dp36);
		AndroidUtils.setMargins(itemUpdateNowParams, dp12, dp16, dp16, dp12);
		itemUpdateNowButton.setLayoutParams(itemUpdateNowParams);
		((AppCompatImageView) itemUpdateNowButton.findViewById(R.id.button_icon)).setImageDrawable(
				AppCompatResources.getDrawable(app, R.drawable.ic_action_update));
		UiUtilities.setupDialogButton(nightMode, itemUpdateNowButton, DialogButtonType.SECONDARY, getString(R.string.update_now));
		itemUpdateNowButton.setMinimumHeight(AndroidUtils.dpToPx(app, dp36));

		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(itemUpdateNowButton)
				.setOnClickListener(v -> {
					if (!settings.isInternetConnectionAvailable()) {
						app.showShortToastMessage(R.string.no_internet_connection);
					} else {
						if (onLiveUpdatesForLocalChange != null) {
							Runnable runnable = () -> {
								if (isAdded()) {
									refreshUpdateTimeInfo();
									refreshUpdateFrequencyMessage();
									updateFileSize();
									Fragment target = getTargetFragment();
									if (target instanceof LiveUpdatesFragment) {
										((LiveUpdatesFragment) target).updateList();
									}
								}
							};
							onLiveUpdatesForLocalChange.forceUpdateLocal(fileName, true, runnable);
						}
					}
				})
				.create());

		items.add(createDividerItem());

		int iconDeleteColor = ColorUtilities.getColor(app, R.color.color_osm_edit_delete);
		Drawable iconDelete = AppCompatResources.getDrawable(app, R.drawable.ic_action_delete_dark);

		itemClear = new BottomSheetItemWithDescription.Builder()
				.setDescription(getUpdatesSizeStr())
				.setIcon(UiUtilities.tintDrawable(iconDelete, iconDeleteColor))
				.setTitle(getString(R.string.updates_size))
				.setLayoutId(R.layout.bottom_sheet_item_with_descr_icon_right)
				.setOnClickListener(v -> {
					if (getUpdatesSize() > 0) {
						if (getFragmentManager() != null) {
							LiveUpdatesClearBottomSheet.showInstance(getFragmentManager(),
									LiveUpdatesSettingsBottomSheet.this, fileName);
						}
					}
				})
				.create();
		items.add(itemClear);

		items.add(createDividerItem());

		itemViaWiFi = new BottomSheetItemWithCompoundButton.Builder()
				.setChecked(downloadViaWiFiPreference.get())
				.setDescription(getStateText(downloadViaWiFiPreference.get()))
				.setIconHidden(true)
				.setTitle(getString(R.string.only_download_over_wifi))
				.setLayoutId(R.layout.bottom_sheet_item_with_descr_and_switch_56dp)
				.setOnClickListener(v -> {
					if (preferenceForLocalIndex(fileName, settings).get()) {
						BottomSheetItemWithCompoundButton item = (BottomSheetItemWithCompoundButton) itemViaWiFi;
						boolean checked = item.isChecked();
						item.setChecked(!checked);
						item.setDescription(getStateText(!checked));
						preferenceDownloadViaWiFi(fileName, settings).set(!checked);
					}
				})
				.create();
		items.add(itemViaWiFi);

		items.add(new DividerSpaceItem(app, getDimensionPixelSize(R.dimen.context_menu_padding_margin_large)));
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, parent, savedInstanceState);

		TextViewEx titleView = (TextViewEx) itemTitle.getView();
		titleView.setPadding(titleView.getPaddingLeft(), getDimensionPixelSize(R.dimen.content_padding_small),
				titleView.getPaddingRight(), getDimensionPixelSize(R.dimen.list_item_button_padding));
		int titleHeight = getDimensionPixelSize(R.dimen.bottom_sheet_descr_height);
		titleView.setMinimumHeight(titleHeight);

		TextView frequencyHelpMessageView = (TextView) itemFrequencyHelpMessage.getView();
		frequencyHelpMessageView.getLayoutParams().height = LinearLayout.LayoutParams.WRAP_CONTENT;
		int dp16 = getDimensionPixelSize(R.dimen.content_padding);
		AndroidUtils.setPadding(frequencyHelpMessageView, dp16, dp16 / 2, dp16, 0);

		CommonPreference<Boolean> localUpdatePreference = preferenceForLocalIndex(fileName, settings);
		setStateViaWiFiButton(localUpdatePreference);
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		lockMaxViewHeight();
	}

	private void lockMaxViewHeight() {
		Activity activity = getActivity();
		View mainView = getView();
		if (activity != null && AndroidUiHelper.isOrientationPortrait(activity) && mainView != null) {
			View scrollView = mainView.findViewById(R.id.scroll_view);
			scrollView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

				@Override
				public void onGlobalLayout() {
					scrollView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
					LayoutParams newParams = new LayoutParams(LayoutParams.MATCH_PARENT, scrollView.getHeight());
					scrollView.setLayoutParams(newParams);
				}
			});
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(LOCAL_INDEX_FILE_NAME, fileName);
	}

	private void setStateViaWiFiButton(CommonPreference<Boolean> localUpdatePreference) {
		if (itemViaWiFi != null) {
			BottomSheetItemWithCompoundButton item = (BottomSheetItemWithCompoundButton) itemViaWiFi;
			if (item.getView() != null) {
				TextView title = item.getView().findViewById(R.id.title);
				TextView description = item.getView().findViewById(R.id.description);
				CompoundButton compoundButton = item.getView().findViewById(R.id.compound_button);
				if (localUpdatePreference.get()) {
					AndroidUtils.setTextPrimaryColor(app, title, nightMode);
					AndroidUtils.setTextSecondaryColor(app, description, nightMode);
					compoundButton.setEnabled(true);
				} else {
					AndroidUtils.setTextSecondaryColor(app, title, nightMode);
					description.setTextColor(ContextCompat.getColor(app, ColorUtilities.getTertiaryTextColorId(nightMode)));
					compoundButton.setEnabled(false);
				}
			}
		}
	}

	private void refreshUpdateFrequencyMessage() {
		if (itemFrequencyHelpMessage != null) {
			String updateFrequencyMessage = getString(getUpdateFrequency().descId);
			((BottomSheetItemWithDescription) itemFrequencyHelpMessage).setDescription(updateFrequencyMessage);
		}
	}

	private void updateFileSize() {
		if (itemClear != null) {
			((BottomSheetItemWithDescription) itemClear).setDescription(getUpdatesSizeStr());
		}
	}

	private void refreshUpdateTimeInfo() {
		if (itemUpdateTimeInfo != null) {
			long lastUpdateTimeMillis = preferenceLastSuccessfulUpdateCheck(fileName, settings).get();
			CharSequence lastUpdateTimeStr = getLastUpdateTimeInfo(lastUpdateTimeMillis);
			CharSequence nextUpdateTimeStr = getNextUpdateTimeInfo(lastUpdateTimeMillis);

			View view = itemUpdateTimeInfo.getView();
			TextView lastUpdateInfo = view.findViewById(R.id.last_update_info);
			TextView nextUpdateInfo = view.findViewById(R.id.next_update_info);

			lastUpdateInfo.setText(lastUpdateTimeStr);
			nextUpdateInfo.setText(nextUpdateTimeStr);
			AndroidUiHelper.updateVisibility(nextUpdateInfo, !Algorithms.isEmpty(nextUpdateTimeStr));
		}
	}

	@NonNull
	private CharSequence getLastUpdateTimeInfo(long lastUpdateTimeMillis) {
		boolean noUpdatesYet = lastUpdateTimeMillis == DEFAULT_LAST_CHECK;
		if (noUpdatesYet) {
			return getString(R.string.shared_string_never);
		}

		String lastUpdateTime = getString(R.string.updated_map_time, formatShortDateTime(app, lastUpdateTimeMillis));
		long lastOsmUpdateTimestamp = preferenceLastOsmChange(fileName, settings).get();
		if (lastOsmUpdateTimestamp <= 0) {
			lastOsmUpdateTimestamp = preferenceLastCheck(fileName, settings).get();
		}
		if (lastOsmUpdateTimestamp <= 0) {
			return lastUpdateTime;
		}

		String formattedOsmUpdateTime = dateFormat.format(new Date(lastOsmUpdateTimestamp));
		String lastOsmChangesTime = getString(R.string.includes_osm_changes_until, formattedOsmUpdateTime);
		String updateInfo = getString(R.string.ltr_or_rtl_combine_via_space, lastUpdateTime, lastOsmChangesTime);
		SpannableString spannable = new SpannableString(updateInfo);
		CustomTypefaceSpan span = new CustomTypefaceSpan(FontCache.getMediumFont());
		int start = updateInfo.indexOf(formattedOsmUpdateTime);
		int end = start + formattedOsmUpdateTime.length();
		spannable.setSpan(span, start, end, 0);

		return spannable;
	}

	@NonNull
	private String getNextUpdateTimeInfo(long lastUpdateTimeMillis) {
		boolean noUpdatesYet = lastUpdateTimeMillis == DEFAULT_LAST_CHECK;
		if (noUpdatesYet) {
			return "";
		}

		UpdateFrequency updateFrequency = getUpdateFrequency();
		TimeOfDay timeOfDay = getTimeOfDayToUpdate();
		long nextUpdateTimeMillis = getNextUpdateTimeMillis(lastUpdateTimeMillis, updateFrequency, timeOfDay);

		String nextUpdateTime = LiveUpdatesHelper.getNextUpdateTime(app, nextUpdateTimeMillis);
		String nextUpdateDate = getNextUpdateDate(app, nextUpdateTimeMillis);
		return DateUtils.isToday(nextUpdateTimeMillis)
				? getString(R.string.next_live_update_time, nextUpdateTime)
				: getString(R.string.next_live_update_date_and_time, nextUpdateDate, nextUpdateTime);
	}

	@NonNull
	private UpdateFrequency getUpdateFrequency() {
		CommonPreference<Integer> updateFrequencyPref = preferenceUpdateFrequency(fileName, settings);
		return UpdateFrequency.values()[updateFrequencyPref.get()];
	}

	@NonNull
	private TimeOfDay getTimeOfDayToUpdate() {
		CommonPreference<Integer> timeOfDayToUpdatePref = preferenceTimeOfDayToUpdate(fileName, settings);
		return TimeOfDay.values()[timeOfDayToUpdatePref.get()];
	}

	private long getUpdatesSize() {
		IncrementalChangesManager changesManager = app.getResourceManager().getChangesManager();
		String fileNameWithoutExt = Algorithms.getFileNameWithoutExtension(fileName);
		return changesManager.getUpdatesSize(fileNameWithoutExt);
	}

	private String getUpdatesSizeStr() {
		long updatesSize = getUpdatesSize();
		return updatesSize > 0
				? AndroidUtils.formatSize(app, updatesSize)
				: getString(R.string.ltr_or_rtl_combine_via_space, "0.0", "kB");
	}

	@NonNull
	private BaseBottomSheetItem createDividerItem() {
		DividerItem dividerItem = new DividerItem(app);
		int start = getDimensionPixelSize(R.dimen.content_padding);
		int vertical = getDimensionPixelSize(R.dimen.content_padding_small_half);
		dividerItem.setMargins(start, vertical, 0, vertical);
		return dividerItem;
	}

	private void setSelectedRadioItem(TextToggleButton toggleButton, int position, TextRadioItem... buttons) {
		toggleButton.setSelectedItem(buttons[position]);
	}

	private void refreshTimeOfDayLayout(int position, View... timeOfDayLayouts) {
		switch (UpdateFrequency.values()[position]) {
			case HOURLY:
				for (View timeOfDayLayout : timeOfDayLayouts) {
					AndroidUiHelper.updateVisibility(timeOfDayLayout, false);
				}
				break;
			case DAILY:
			case WEEKLY:
				for (View timeOfDayLayout : timeOfDayLayouts) {
					AndroidUiHelper.updateVisibility(timeOfDayLayout, true);
				}
				break;
		}
	}

	private OnRadioItemClickListener getFrequencyButtonListener(
			@NonNull UpdateFrequency type, View... timeOfDayLayouts) {
		return (radioItem, view) -> {
			CommonPreference<Integer> frequencyPreference = preferenceUpdateFrequency(fileName, settings);
			setOnRadioItemClick(frequencyPreference, type.ordinal(), timeOfDayLayouts);
			return true;
		};
	}

	private OnRadioItemClickListener getTimeOfDayButtonListener(@NonNull TimeOfDay type) {
		return (radioItem, view) -> {
			CommonPreference<Integer> timeOfDayPreference = preferenceTimeOfDayToUpdate(fileName, settings);
			setOnRadioItemClick(timeOfDayPreference, type.ordinal());
			return true;
		};
	}

	private void setOnRadioItemClick(CommonPreference<Integer> preference, int newValue, View... timeOfDayLayouts) {
		CommonPreference<Boolean> liveUpdatePreference = preferenceForLocalIndex(fileName, settings);
		if (liveUpdatePreference.get()) {
			preference.set(newValue);
			if (!Algorithms.isEmpty(Arrays.asList(timeOfDayLayouts))) {
				refreshTimeOfDayLayout(newValue, timeOfDayLayouts);
			}
			refreshUpdateFrequencyMessage();
			OnLiveUpdatesForLocalChange confirmationInterface = (OnLiveUpdatesForLocalChange) getTargetFragment();
			if (confirmationInterface != null) {
				confirmationInterface.updateList();
			}
		}
	}

	@Override
	public void onUpdateStates(Context context) {
		OnLiveUpdatesForLocalChange confirmationInterface = (OnLiveUpdatesForLocalChange) getTargetFragment();
		if (confirmationInterface != null) {
			confirmationInterface.updateList();
		}
		refreshUpdateTimeInfo();
		updateFileSize();
	}

	@Override
	public void downloadHasFinished() {
		updateFileSize();
	}

	public interface OnLiveUpdatesForLocalChange {

		boolean onUpdateLocalIndex(String fileName, boolean newValue, Runnable callback);

		void forceUpdateLocal(String fileName, boolean userRequested, Runnable callback);

		void runSort();

		void updateList();
	}

	public String getStateText(boolean isEnabled) {
		return getString(isEnabled ? R.string.shared_string_enabled : R.string.shared_string_disabled);
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, Fragment target, String fileName) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			LiveUpdatesSettingsBottomSheet fragment = new LiveUpdatesSettingsBottomSheet();
			fragment.usedOnMap = false;
			fragment.setTargetFragment(target, 0);
			fragment.fileName = fileName;
			fragment.show(fragmentManager, TAG);
		}
	}
}
