package net.osmand.plus.liveupdates;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.ShortDescriptionItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.liveupdates.LiveUpdatesClearBottomSheet.RefreshLiveUpdates;
import net.osmand.plus.liveupdates.LiveUpdatesHelper.TimeOfDay;
import net.osmand.plus.liveupdates.LiveUpdatesHelper.UpdateFrequency;
import net.osmand.plus.resources.IncrementalChangesManager;
import net.osmand.plus.settings.backend.CommonPreference;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.widgets.MultiStateToggleButton;
import net.osmand.plus.widgets.MultiStateToggleButton.OnRadioItemClickListener;
import net.osmand.plus.widgets.MultiStateToggleButton.RadioItem;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.Arrays;

import static net.osmand.AndroidUtils.getPrimaryTextColorId;
import static net.osmand.AndroidUtils.getSecondaryTextColorId;
import static net.osmand.plus.UiUtilities.CompoundButtonType.TOOLBAR;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.formatHelpDateTime;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.formatShortDateTime;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.getNameToDisplay;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceDownloadViaWiFi;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceForLocalIndex;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceLastCheck;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceLatestUpdateAvailable;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceTimeOfDayToUpdate;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceUpdateFrequency;
import static net.osmand.plus.settings.bottomsheets.BooleanPreferenceBottomSheet.getCustomButtonView;
import static net.osmand.plus.settings.bottomsheets.BooleanPreferenceBottomSheet.updateCustomButtonView;

public class LiveUpdatesSettingsBottomSheet extends MenuBottomSheetDialogFragment implements RefreshLiveUpdates {

	public static final String TAG = LiveUpdatesSettingsBottomSheet.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(LiveUpdatesSettingsBottomSheet.class);
	private static final String LOCAL_INDEX_FILE_NAME = "local_index_file_name";

	private OsmandApplication app;
	private OsmandSettings settings;

	private BaseBottomSheetItem itemTitle;
	private BaseBottomSheetItem itemLastCheck;
	private BaseBottomSheetItem itemSwitchLiveUpdate;
	private BaseBottomSheetItem itemFrequencyHelpMessage;
	private BaseBottomSheetItem itemClear;
	private BaseBottomSheetItem itemViaWiFi;
	private MultiStateToggleButton frequencyToggleButton;
	private MultiStateToggleButton timeOfDayToggleButton;

	private String fileName;
	private OnLiveUpdatesForLocalChange onLiveUpdatesForLocalChange;

	public static void showInstance(@NonNull FragmentManager fragmentManager, Fragment target, String fileName) {
		if (!fragmentManager.isStateSaved()) {
			LiveUpdatesSettingsBottomSheet fragment = new LiveUpdatesSettingsBottomSheet();
			fragment.setTargetFragment(target, 0);
			fragment.fileName = fileName;
			fragment.show(fragmentManager, TAG);
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		app = getMyApplication();
		settings = app.getSettings();
		LayoutInflater inflater = UiUtilities.getInflater(app, nightMode);
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
		int dp4 = getDimen(R.dimen.list_item_button_padding);
		int dp12 = getDimen(R.dimen.content_padding_small);
		int dp16 = getDimen(R.dimen.content_padding);
		int dp36 = getDimen(R.dimen.dialog_button_height);
		int dp48 = getDimen(R.dimen.context_menu_buttons_bottom_height);

		itemTitle = new SimpleBottomSheetItem.Builder()
				.setTitle(getNameToDisplay(fileName, app))
				.setTitleColorId(getPrimaryTextColorId(nightMode))
				.setLayoutId(R.layout.bottom_sheet_item_title_big)
				.create();
		items.add(itemTitle);

		itemLastCheck = new ShortDescriptionItem.Builder()
				.setDescription(getLastCheckString())
				.setDescriptionColorId(getSecondaryTextColorId(nightMode))
				.setDescriptionMaxLines(2)
				.setLayoutId(R.layout.bottom_sheet_item_description)
				.create();
		items.add(itemLastCheck);

		View itemLiveUpdate = getCustomButtonView(app, null, localUpdatePreference.get(), nightMode);
		View itemLiveUpdateButton = itemLiveUpdate.findViewById(R.id.button_container);
		CompoundButton button = (CompoundButton) itemLiveUpdateButton.findViewById(R.id.compound_button);
		UiUtilities.setupCompoundButton(button, nightMode, TOOLBAR);
		itemLiveUpdateButton.setMinimumHeight(getDimen(R.dimen.bottom_sheet_selected_item_title_height));
		itemSwitchLiveUpdate = new BottomSheetItemWithCompoundButton.Builder()
				.setChecked(localUpdatePreference.get())
				.setTitle(getStateText(localUpdatePreference.get()))
				.setTitleColorId(getActiveTabTextColorId(nightMode))
				.setCustomView(itemLiveUpdate)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						BottomSheetItemWithCompoundButton item = (BottomSheetItemWithCompoundButton) itemSwitchLiveUpdate;
						boolean checked = item.isChecked();
						item.setChecked(!checked);
						if (onLiveUpdatesForLocalChange != null
								&& onLiveUpdatesForLocalChange.onUpdateLocalIndex(fileName, !checked, new Runnable() {
							@Override
							public void run() {
								updateLastCheck();
								updateFrequencyHelpMessage();
								updateFileSize();
								Fragment target = getTargetFragment();
								if (target instanceof LiveUpdatesFragment) {
									((LiveUpdatesFragment) target).runSort();
								}
							}
						})) {
							item.setTitle(getStateText(!checked));
							updateCustomButtonView(app, null, item.getView(), !checked, nightMode);
							CommonPreference<Boolean> localUpdatePreference = preferenceForLocalIndex(fileName, settings);
							frequencyToggleButton.updateView(localUpdatePreference.get());
							timeOfDayToggleButton.updateView(localUpdatePreference.get());
							setStateViaWiFiButton(localUpdatePreference);
						}
					}
				})
				.create();
		items.add(itemSwitchLiveUpdate);

		Typeface typefaceRegular = FontCache.getRobotoRegular(app);
		TextViewEx timeOfDayTitle = (TextViewEx) inflater.inflate(R.layout.bottom_sheet_item_title, null);
		timeOfDayTitle.setHeight(dp48);
		timeOfDayTitle.setMinimumHeight(dp48);
		timeOfDayTitle.setText(R.string.update_time);
		timeOfDayTitle.setTypeface(typefaceRegular);
		AndroidUtils.setPadding(timeOfDayTitle, timeOfDayTitle.getPaddingLeft(), dp4, timeOfDayTitle.getPaddingRight(), dp4);
		AndroidUtils.setTextPrimaryColor(app, timeOfDayTitle, nightMode);
		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(timeOfDayTitle)
				.create());

		LinearLayout itemTimeOfDayButtons = (LinearLayout) inflater.inflate(R.layout.custom_radio_buttons, null);
		LinearLayout.MarginLayoutParams itemTimeOfDayParams = new LinearLayout.MarginLayoutParams(
				LinearLayout.MarginLayoutParams.MATCH_PARENT, LinearLayout.MarginLayoutParams.WRAP_CONTENT);
		AndroidUtils.setMargins(itemTimeOfDayParams, dp16, 0, dp16, 0);
		itemTimeOfDayButtons.setLayoutParams(itemTimeOfDayParams);

		String morning = getString(R.string.morning);
		String night = getString(R.string.night);
		RadioItem morningButton = new RadioItem(morning);
		RadioItem nightButton = new RadioItem(night);
		timeOfDayToggleButton = new MultiStateToggleButton(app, itemTimeOfDayButtons, nightMode);
		timeOfDayToggleButton.setItems(morningButton, nightButton);
		setSelectedRadioItem(timeOfDayToggleButton, timeOfDayPreference.get(), morningButton, nightButton);
		timeOfDayToggleButton.updateView(localUpdatePreference.get());
		refreshTimeOfDayLayout(frequencyPreference.get(), itemTimeOfDayButtons, timeOfDayTitle);

		morningButton.setOnClickListener(getTimeOfDayButtonListener(TimeOfDay.MORNING));
		nightButton.setOnClickListener(getTimeOfDayButtonListener(TimeOfDay.NIGHT));

		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(itemTimeOfDayButtons)
				.create()
		);

		TextViewEx frequencyTitle = (TextViewEx) inflater.inflate(R.layout.bottom_sheet_item_title, null);
		frequencyTitle.setHeight(dp48);
		frequencyTitle.setMinimumHeight(dp48);
		frequencyTitle.setText(R.string.update_frequency);
		frequencyTitle.setTypeface(typefaceRegular);
		AndroidUtils.setPadding(frequencyTitle, frequencyTitle.getPaddingLeft(), dp4, frequencyTitle.getPaddingRight(), dp4);
		AndroidUtils.setTextPrimaryColor(app, frequencyTitle, nightMode);
		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(frequencyTitle)
				.create());

		LinearLayout itemFrequencyButtons = (LinearLayout) inflater.inflate(R.layout.custom_radio_buttons, null);
		LinearLayout.MarginLayoutParams itemFrequencyParams = new LinearLayout.MarginLayoutParams(
				LinearLayout.MarginLayoutParams.MATCH_PARENT, LinearLayout.MarginLayoutParams.WRAP_CONTENT);
		AndroidUtils.setMargins(itemFrequencyParams, dp16, 0, dp16, dp12);
		itemFrequencyButtons.setLayoutParams(itemFrequencyParams);

		String hourly = getString(R.string.hourly);
		String daily = getString(R.string.daily);
		String weekly = getString(R.string.weekly);
		RadioItem hourlyButton = new RadioItem(hourly);
		RadioItem dailyButton = new RadioItem(daily);
		RadioItem weeklyButton = new RadioItem(weekly);
		frequencyToggleButton = new MultiStateToggleButton(app, itemFrequencyButtons, nightMode);
		frequencyToggleButton.setItems(hourlyButton, dailyButton, weeklyButton);
		setSelectedRadioItem(frequencyToggleButton, frequencyPreference.get(), hourlyButton, dailyButton, weeklyButton);
		frequencyToggleButton.updateView(localUpdatePreference.get());

		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(itemFrequencyButtons)
				.create());

		hourlyButton.setOnClickListener(getFrequencyButtonListener(UpdateFrequency.HOURLY, itemTimeOfDayButtons, timeOfDayTitle));
		dailyButton.setOnClickListener(getFrequencyButtonListener(UpdateFrequency.DAILY, itemTimeOfDayButtons, timeOfDayTitle));
		weeklyButton.setOnClickListener(getFrequencyButtonListener(UpdateFrequency.WEEKLY, itemTimeOfDayButtons, timeOfDayTitle));

		itemFrequencyHelpMessage = new ShortDescriptionItem.Builder()
				.setDescription(getFrequencyHelpMessage())
				.setDescriptionColorId(getSecondaryTextColorId(nightMode))
				.setLayoutId(R.layout.bottom_sheet_item_description)
				.create();
		items.add(itemFrequencyHelpMessage);

		LinearLayout itemUpdateNowButton =
				(LinearLayout) inflater.inflate(R.layout.bottom_sheet_button_with_icon_center, null);
		LinearLayout.MarginLayoutParams itemUpdateNowParams = new LinearLayout.MarginLayoutParams(
				LinearLayout.MarginLayoutParams.MATCH_PARENT, dp36);
		AndroidUtils.setMargins(itemUpdateNowParams, dp12, 0, dp16, dp12);
		itemUpdateNowButton.setLayoutParams(itemUpdateNowParams);
		((AppCompatImageView) itemUpdateNowButton.findViewById(R.id.button_icon)).setImageDrawable(
				AppCompatResources.getDrawable(app, R.drawable.ic_action_update));
		UiUtilities.setupDialogButton(nightMode, itemUpdateNowButton, UiUtilities.DialogButtonType.SECONDARY, getString(R.string.update_now));
		itemUpdateNowButton.setMinimumHeight(AndroidUtils.dpToPx(app, dp36));

		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(itemUpdateNowButton)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (!settings.isInternetConnectionAvailable()) {
							app.showShortToastMessage(R.string.no_internet_connection);
						} else {
							if (onLiveUpdatesForLocalChange != null) {
								onLiveUpdatesForLocalChange.forceUpdateLocal(fileName, true, new Runnable() {
									@Override
									public void run() {
										updateLastCheck();
										updateFrequencyHelpMessage();
										updateFileSize();
										Fragment target = getTargetFragment();
										if (target instanceof LiveUpdatesFragment) {
											((LiveUpdatesFragment) target).updateList();
										}
									}
								});
							}
						}
					}
				})
				.create());

		items.add(createDividerItem());

		int iconDeleteColor = ContextCompat.getColor(app, R.color.color_osm_edit_delete);
		Drawable iconDelete = AppCompatResources.getDrawable(app, R.drawable.ic_action_delete_dark);

		itemClear = new BottomSheetItemWithDescription.Builder()
				.setDescription(getUpdatesSizeStr())
				.setIcon(UiUtilities.tintDrawable(iconDelete, iconDeleteColor))
				.setTitle(getString(R.string.updates_size))
				.setLayoutId(R.layout.bottom_sheet_item_with_descr_icon_right)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (getUpdatesSize() > 0) {
							if (getFragmentManager() != null) {
								LiveUpdatesClearBottomSheet.showInstance(getFragmentManager(),
										LiveUpdatesSettingsBottomSheet.this, fileName);
							}
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
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (preferenceForLocalIndex(fileName, settings).get()) {
							BottomSheetItemWithCompoundButton item = (BottomSheetItemWithCompoundButton) itemViaWiFi;
							boolean checked = item.isChecked();
							item.setChecked(!checked);
							item.setDescription(getStateText(!checked));
							preferenceDownloadViaWiFi(fileName, settings).set(!checked);
						}
					}
				})
				.create();
		items.add(itemViaWiFi);

		items.add(new DividerSpaceItem(app, getDimen(R.dimen.context_menu_padding_margin_large)));
	}


	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, parent, savedInstanceState);

		int titleHeight = getDimen(R.dimen.bottom_sheet_descr_height);
		TextViewEx titleView = (TextViewEx) itemTitle.getView();
		titleView.setMinimumHeight(titleHeight);
		titleView.getLayoutParams().height = titleHeight;
		titleView.setPadding(titleView.getPaddingLeft(), getDimen(R.dimen.content_padding_small),
				titleView.getPaddingRight(), getDimen(R.dimen.list_item_button_padding));

		int descriptionHeight = getDimen(R.dimen.bottom_sheet_title_height);
		TextView descriptionView = (TextView) itemLastCheck.getView();
		descriptionView.setMinimumHeight(descriptionHeight);
		descriptionView.getLayoutParams().height = descriptionHeight;

		int frequencyHelpMessageHeight = getDimen(R.dimen.context_menu_progress_min_height);
		TextView frequencyHelpMessageView = (TextView) itemFrequencyHelpMessage.getView();
		frequencyHelpMessageView.setMinimumHeight(frequencyHelpMessageHeight);
		frequencyHelpMessageView.getLayoutParams().height = frequencyHelpMessageHeight;

		CommonPreference<Boolean> localUpdatePreference = preferenceForLocalIndex(fileName, settings);
		setStateViaWiFiButton(localUpdatePreference);
		return view;
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
					description.setTextColor(ContextCompat.getColor(app, getTertiaryTextColorId(nightMode)));
					compoundButton.setEnabled(false);
				}
			}
		}
	}

	private void updateLastCheck() {
		if (itemLastCheck != null) {
			((BottomSheetItemWithDescription) itemLastCheck).setDescription(getLastCheckString());
		}
	}

	private void updateFrequencyHelpMessage() {
		if (itemFrequencyHelpMessage != null) {
			((BottomSheetItemWithDescription) itemFrequencyHelpMessage).setDescription(getFrequencyHelpMessage());
		}
	}

	private void updateFileSize() {
		if (itemClear != null) {
			((BottomSheetItemWithDescription) itemClear).setDescription(getUpdatesSizeStr());
		}
	}

	protected SpannableString getLastCheckString() {
		final long lastUpdate = preferenceLatestUpdateAvailable(fileName, settings).get();
		String updatedTimeStr = getString(R.string.updated, formatShortDateTime(app, lastUpdate));
		if (!updatedTimeStr.contains(getString(R.string.shared_string_never))) {
			final long lastCheck = preferenceLastCheck(fileName, settings).get();
			String lastCheckStr = getString(R.string.last_time_checked, formatShortDateTime(app, lastCheck));
			updatedTimeStr = updatedTimeStr.concat("\n").concat(lastCheckStr);
			SpannableString spanStr = new SpannableString(updatedTimeStr);
			Typeface typeface = FontCache.getRobotoMedium(getContext());
			int start = updatedTimeStr.indexOf(" — ");
			if (start != -1) {
				int end = updatedTimeStr.indexOf(lastCheckStr) - 1;
				spanStr.setSpan(new CustomTypefaceSpan(typeface), start, end, 0);
				start = updatedTimeStr.lastIndexOf(" — ");
				if (start != -1) {
					end = updatedTimeStr.length();
					spanStr.setSpan(new CustomTypefaceSpan(typeface), start, end, 0);
				}
			}
			return spanStr;
		}
		return new SpannableString(updatedTimeStr);
	}

	protected String getFrequencyHelpMessage() {
		CommonPreference<Integer> updateFrequency = preferenceUpdateFrequency(fileName, settings);
		CommonPreference<Integer> timeOfDayToUpdate = preferenceTimeOfDayToUpdate(fileName, settings);
		final long lastUpdate = preferenceLatestUpdateAvailable(fileName, settings).get();
		return formatHelpDateTime(app, UpdateFrequency.values()[updateFrequency.get()],
				TimeOfDay.values()[timeOfDayToUpdate.get()], lastUpdate);
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


	private BaseBottomSheetItem createDividerItem() {
		DividerItem dividerItem = new DividerItem(app);
		int start = getDimen(R.dimen.content_padding);
		int vertical = getDimen(R.dimen.content_padding_small_half);
		dividerItem.setMargins(start, vertical, 0, vertical);
		return dividerItem;
	}

	private void setSelectedRadioItem(MultiStateToggleButton toggleButton, int position, RadioItem... buttons) {
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
			@NonNull final UpdateFrequency type, final View... timeOfDayLayouts) {
		return new OnRadioItemClickListener() {
			@Override
			public boolean onRadioItemClick(RadioItem radioItem, View view) {
				final CommonPreference<Integer> frequencyPreference = preferenceUpdateFrequency(fileName, settings);
				setOnRadioItemClick(frequencyPreference, type.ordinal(), timeOfDayLayouts);
				return true;
			}
		};
	}

	private OnRadioItemClickListener getTimeOfDayButtonListener(@NonNull final TimeOfDay type) {
		return new OnRadioItemClickListener() {
			@Override
			public boolean onRadioItemClick(RadioItem radioItem, View view) {
				final CommonPreference<Integer> timeOfDayPreference = preferenceTimeOfDayToUpdate(fileName, settings);
				setOnRadioItemClick(timeOfDayPreference, type.ordinal());
				return true;
			}
		};
	}

	private void setOnRadioItemClick(CommonPreference<Integer> preference, int newValue, View... timeOfDayLayouts) {
		CommonPreference<Boolean> liveUpdatePreference = preferenceForLocalIndex(fileName, settings);
		if (liveUpdatePreference.get()) {
			preference.set(newValue);
			if (!Algorithms.isEmpty(Arrays.asList(timeOfDayLayouts))) {
				refreshTimeOfDayLayout(newValue, timeOfDayLayouts);
			}
			updateFrequencyHelpMessage();
			OnLiveUpdatesForLocalChange confirmationInterface = (OnLiveUpdatesForLocalChange) getTargetFragment();
			if (confirmationInterface != null) {
				confirmationInterface.updateList();
			}
		}
	}

	@Override
	public void onUpdateStates(Context context) {
		final OnLiveUpdatesForLocalChange confirmationInterface = (OnLiveUpdatesForLocalChange) getTargetFragment();
		if (confirmationInterface != null) {
			confirmationInterface.updateList();
		}
		updateLastCheck();
		updateFileSize();
	}

	public interface OnLiveUpdatesForLocalChange {

		boolean onUpdateLocalIndex(String fileName, boolean newValue, Runnable callback);

		void forceUpdateLocal(String fileName, boolean userRequested, Runnable callback);

		void runSort();

		void updateList();
	}

	public int getDimen(@DimenRes int id) {
		return getResources().getDimensionPixelSize(id);
	}

	public String getStateText(boolean isEnabled) {
		return getString(isEnabled ? R.string.shared_string_enabled : R.string.shared_string_disabled);
	}

	@ColorRes
	public static int getActiveTabTextColorId(boolean nightMode) {
		return nightMode ? R.color.text_color_tab_active_dark : R.color.text_color_tab_active_light;
	}

	@ColorRes
	public static int getActivePrimaryColorId(boolean nightMode) {
		return nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light;
	}

	@ColorRes
	public static int getTertiaryTextColorId(boolean nightMode) {
		return nightMode ? R.color.text_color_tertiary_dark : R.color.text_color_tertiary_light;
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}
}
