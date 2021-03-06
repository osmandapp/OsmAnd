package net.osmand.plus.liveupdates;

import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

import androidx.annotation.ColorRes;
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
import net.osmand.plus.liveupdates.LiveUpdatesClearDialogFragment.OnRefreshLiveUpdates;
import net.osmand.plus.liveupdates.LiveUpdatesHelper.TimeOfDay;
import net.osmand.plus.liveupdates.LiveUpdatesHelper.UpdateFrequency;
import net.osmand.plus.resources.IncrementalChangesManager;
import net.osmand.plus.settings.backend.CommonPreference;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.widgets.MultiStateToggleButton;
import net.osmand.plus.widgets.MultiStateToggleButton.OnRadioItemClickListener;
import net.osmand.plus.widgets.MultiStateToggleButton.RadioItem;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.Arrays;

import static net.osmand.plus.liveupdates.LiveUpdatesHelper.formatHelpDateTime;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.formatShortDateTime;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.getNameToDisplay;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceDownloadViaWiFi;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceForLocalIndex;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceLastCheck;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceLatestUpdateAvailable;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceTimeOfDayToUpdate;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceUpdateFrequency;
import static net.osmand.plus.monitoring.TripRecordingActiveBottomSheet.getSecondaryTextColorId;
import static net.osmand.plus.settings.bottomsheets.BooleanPreferenceBottomSheet.getCustomButtonView;
import static net.osmand.plus.settings.bottomsheets.BooleanPreferenceBottomSheet.updateCustomButtonView;

public class LiveUpdatesSettingsDialogFragmentNew extends MenuBottomSheetDialogFragment implements OnRefreshLiveUpdates {

	public static final String TAG = LiveUpdatesSettingsDialogFragmentNew.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(LiveUpdatesSettingsDialogFragmentNew.class);
	private static final String LOCAL_INDEX_FILE_NAME = "local_index_file_name";

	private OsmandApplication app;
	private OsmandSettings settings;

	private String fileName;
	private int itemLastCheckIndex = -1;
	private int itemLiveUpdateIndex = -1;
	private int itemFrequencyButtonsIndex = -1;
	private int itemTimeOfDayButtonsIndex = -1;
	private int itemFrequencyHelpMessageIndex = -1;
	private int itemSizeIndex = -1;
	private int itemOnWiFiIndex = -1;

	public static void showInstance(@NonNull FragmentManager fragmentManager, Fragment target, String fileName) {
		if (!fragmentManager.isStateSaved()) {
			LiveUpdatesSettingsDialogFragmentNew fragment = new LiveUpdatesSettingsDialogFragmentNew();
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
		final OnLiveUpdatesForLocalChange confirmationInterface = (OnLiveUpdatesForLocalChange) getTargetFragment();
		final CommonPreference<Boolean> liveUpdatePreference = preferenceForLocalIndex(fileName, settings);
		final CommonPreference<Boolean> downloadViaWiFiPreference = preferenceDownloadViaWiFi(fileName, settings);
		final CommonPreference<Integer> frequencyPreference = preferenceUpdateFrequency(fileName, settings);
		final CommonPreference<Integer> timeOfDayPreference = preferenceTimeOfDayToUpdate(fileName, settings);
		final String on = getString(R.string.shared_string_enabled);
		final String off = getString(R.string.shared_string_disabled);
		int dp4 = getResources().getDimensionPixelSize(R.dimen.context_menu_buttons_padding_bottom);
		int dp6 = getResources().getDimensionPixelSize(R.dimen.context_menu_padding_margin_tiny);
		int dp8 = getResources().getDimensionPixelSize(R.dimen.context_menu_padding_margin_small);
		int dp12 = getResources().getDimensionPixelSize(R.dimen.content_padding_small);
		int dp20 = getResources().getDimensionPixelSize(R.dimen.context_menu_padding_margin_large);
		int dp40 = getResources().getDimensionPixelSize(R.dimen.list_header_height);
		int dp56 = getResources().getDimensionPixelSize(R.dimen.bottom_sheet_selected_item_title_height);

		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(LOCAL_INDEX_FILE_NAME)) {
				fileName = savedInstanceState.getString(LOCAL_INDEX_FILE_NAME);
			}
		}

		items.add(new SimpleBottomSheetItem.Builder()
				.setTitle(getNameToDisplay(fileName, app))
				.setTitleColorId(getPrimaryTextColorId(nightMode))
				.setLayoutId(R.layout.bottom_sheet_item_title_big)
				.create());

		items.add(new ShortDescriptionItem.Builder()
				.setDescription(getLastCheckString())
				.setDescriptionColorId(getSecondaryTextColorId(nightMode))
				.setDescriptionMaxLines(2)
				.setLayoutId(R.layout.bottom_sheet_item_description)
				.create());
		itemLastCheckIndex = items.size() - 1;

		View itemLiveUpdate = getCustomButtonView(app, null, liveUpdatePreference.get(), nightMode);
		View itemLiveUpdateButton = itemLiveUpdate.findViewById(R.id.button_container);
		itemLiveUpdateButton.setMinimumHeight(dp56);
		items.add(new BottomSheetItemWithCompoundButton.Builder()
				.setChecked(liveUpdatePreference.get())
				.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						if (confirmationInterface != null
								&& confirmationInterface.onUpdateLocalIndex(fileName, isChecked, new Runnable() {
							@Override
							public void run() {
								if (itemLastCheckIndex != -1) {
									((BottomSheetItemWithDescription) items.get(itemLastCheckIndex))
											.setDescription(getLastCheckString());
								}
								if (itemFrequencyHelpMessageIndex != -1) {
									((BottomSheetItemWithDescription) items.get(itemFrequencyHelpMessageIndex))
											.setDescription(getFrequencyHelpMessage());
								}
								if (itemSizeIndex != -1) {
									((BottomSheetItemWithDescription) items.get(itemSizeIndex))
											.setDescription(getUpdatesSizeStr());
								}
							}
						})) {
							if (itemLiveUpdateIndex != -1) {
								BottomSheetItemWithCompoundButton button = (BottomSheetItemWithCompoundButton) items.get(itemLiveUpdateIndex);
								button.setTitle(isChecked ? on : off);
								button.setChecked(isChecked);
								updateCustomButtonView(app, null, button.getView(), isChecked, nightMode);
							}
							for (int i = 0; i < items.size(); i++) {
								if (i == itemOnWiFiIndex || i == itemFrequencyButtonsIndex || i == itemTimeOfDayButtonsIndex) {
									items.get(i).getView().setEnabled(isChecked);
								}
							}
						}
					}
				})
				.setTitle(liveUpdatePreference.get() ? on : off)
				.setTitleColorId(getActiveTabTextColorId(nightMode))
				.setCustomView(itemLiveUpdate)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						if (itemLiveUpdateIndex != -1) {
							BottomSheetItemWithCompoundButton button = (BottomSheetItemWithCompoundButton) items.get(itemLiveUpdateIndex);
							button.setChecked(!button.isChecked());
						}
					}
				})
				.create());
		itemLiveUpdateIndex = items.size() - 1;

		TextViewEx frequencyTitle = (TextViewEx) inflater.inflate(R.layout.bottom_sheet_item_title, null);
		frequencyTitle.setHeight(AndroidUtils.dpToPx(app, 48));
		frequencyTitle.setMinimumHeight(AndroidUtils.dpToPx(app, 48));
		frequencyTitle.setText(R.string.update_frequency);
		AndroidUtils.setPadding(frequencyTitle, frequencyTitle.getPaddingLeft(), 0, frequencyTitle.getPaddingRight(), 0);
		AndroidUtils.setTextPrimaryColor(app, frequencyTitle, nightMode);
		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(frequencyTitle)
				.create());

		LinearLayout itemFrequencyButtons = (LinearLayout) inflater.inflate(R.layout.custom_radio_buttons, null);
		LinearLayout.MarginLayoutParams itemFrequencyParams = new LinearLayout.MarginLayoutParams(LinearLayout.MarginLayoutParams.MATCH_PARENT, LinearLayout.MarginLayoutParams.WRAP_CONTENT);
		AndroidUtils.setMargins(itemFrequencyParams, AndroidUtils.dpToPx(app, 16), 0, AndroidUtils.dpToPx(app, 16), 20);
		itemFrequencyButtons.setLayoutParams(itemFrequencyParams);

		String hourly = getString(R.string.hourly);
		String daily = getString(R.string.daily);
		String weekly = getString(R.string.weekly);
		RadioItem hourlyButton = new RadioItem(hourly);
		RadioItem dailyButton = new RadioItem(daily);
		RadioItem weeklyButton = new RadioItem(weekly);
		MultiStateToggleButton frequencyToggleButton = new MultiStateToggleButton(app, itemFrequencyButtons, nightMode);
		frequencyToggleButton.setItems(hourlyButton, dailyButton, weeklyButton);
		setSelectedRadioItem(frequencyToggleButton, frequencyPreference.get(), hourlyButton, dailyButton, weeklyButton);

		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(itemFrequencyButtons)
				.create());
		itemFrequencyButtonsIndex = items.size() - 1;

		TextViewEx timeOfDayTitle = (TextViewEx) inflater.inflate(R.layout.bottom_sheet_item_title, null);
		timeOfDayTitle.setHeight(dp40);
		timeOfDayTitle.setMinimumHeight(dp40);
		timeOfDayTitle.setText(R.string.update_time);
		AndroidUtils.setPadding(timeOfDayTitle, timeOfDayTitle.getPaddingLeft(), 0, timeOfDayTitle.getPaddingRight(), 0);
		AndroidUtils.setTextPrimaryColor(app, timeOfDayTitle, nightMode);
		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(timeOfDayTitle)
				.create());

		LinearLayout itemTimeOfDayButtons = (LinearLayout) inflater.inflate(R.layout.custom_radio_buttons, null);
		LinearLayout.MarginLayoutParams itemTimeOfDayParams = new LinearLayout.MarginLayoutParams(LinearLayout.MarginLayoutParams.MATCH_PARENT, LinearLayout.MarginLayoutParams.WRAP_CONTENT);
		AndroidUtils.setMargins(itemTimeOfDayParams, AndroidUtils.dpToPx(app, 16), 0, AndroidUtils.dpToPx(app, 16), AndroidUtils.dpToPx(app, 10));
		itemTimeOfDayButtons.setLayoutParams(itemTimeOfDayParams);

		String morning = getString(R.string.morning);
		String night = getString(R.string.night);
		RadioItem morningButton = new RadioItem(morning);
		RadioItem nightButton = new RadioItem(night);
		MultiStateToggleButton timeOfDayToggleButton = new MultiStateToggleButton(app, itemTimeOfDayButtons, nightMode);
		timeOfDayToggleButton.setItems(morningButton, nightButton);
		setSelectedRadioItem(timeOfDayToggleButton, timeOfDayPreference.get(), morningButton, nightButton);
		refreshTimeOfDayLayout(frequencyPreference.get(), itemTimeOfDayButtons, timeOfDayTitle);

		hourlyButton.setOnClickListener(getFrequencyButtonListener(UpdateFrequency.HOURLY, itemTimeOfDayButtons, timeOfDayTitle));
		dailyButton.setOnClickListener(getFrequencyButtonListener(UpdateFrequency.DAILY, itemTimeOfDayButtons, timeOfDayTitle));
		weeklyButton.setOnClickListener(getFrequencyButtonListener(UpdateFrequency.WEEKLY, itemTimeOfDayButtons, timeOfDayTitle));
		morningButton.setOnClickListener(getTimeOfDayButtonListener(TimeOfDay.MORNING));
		nightButton.setOnClickListener(getTimeOfDayButtonListener(TimeOfDay.NIGHT));

		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(itemTimeOfDayButtons)
				.create()
		);
		itemTimeOfDayButtonsIndex = items.size() - 1;

		items.add(new ShortDescriptionItem.Builder()
				.setDescription(getFrequencyHelpMessage())
				.setDescriptionColorId(getSecondaryTextColorId(nightMode))
				.setLayoutId(R.layout.bottom_sheet_item_description)
				.create());
		itemFrequencyHelpMessageIndex = items.size() - 1;

		LinearLayout itemUpdateNowButton = (LinearLayout) inflater.inflate(R.layout.bottom_sheet_button_with_icon_center, null);
		LinearLayout.MarginLayoutParams itemUpdateNowParams = new LinearLayout.MarginLayoutParams(LinearLayout.MarginLayoutParams.MATCH_PARENT, AndroidUtils.dpToPx(app, 36));
		AndroidUtils.setMargins(itemUpdateNowParams, AndroidUtils.dpToPx(app, 12), AndroidUtils.dpToPx(app, 12), AndroidUtils.dpToPx(app, 16), AndroidUtils.dpToPx(app, 12));
		itemUpdateNowButton.setLayoutParams(itemUpdateNowParams);
		((AppCompatImageView) itemUpdateNowButton.findViewById(R.id.button_icon)).setImageDrawable(
				ContextCompat.getDrawable(app, R.drawable.ic_action_update));
		UiUtilities.setupDialogButton(nightMode, itemUpdateNowButton, UiUtilities.DialogButtonType.SECONDARY, app.getResources().getString(R.string.update_now));
		itemUpdateNowButton.setMinimumHeight(AndroidUtils.dpToPx(app, app.getResources().getDimension(R.dimen.dialog_button_height)));

		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(itemUpdateNowButton)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (!settings.isInternetConnectionAvailable()) {
							app.showShortToastMessage(R.string.no_internet_connection);
						} else {
							if (confirmationInterface != null) {
								confirmationInterface.forceUpdateLocal(fileName, true, new Runnable() {
									@Override
									public void run() {
										if (itemLastCheckIndex != -1) {
											((BottomSheetItemWithDescription) items.get(itemLastCheckIndex))
													.setDescription(getLastCheckString());
										}
										if (itemFrequencyHelpMessageIndex != -1) {
											((BottomSheetItemWithDescription) items.get(itemFrequencyHelpMessageIndex))
													.setDescription(getFrequencyHelpMessage());
										}
										if (itemSizeIndex != -1) {
											((BottomSheetItemWithDescription) items.get(itemSizeIndex))
													.setDescription(getUpdatesSizeStr());
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

		items.add(
				new BottomSheetItemWithDescription.Builder()
						.setDescription(getUpdatesSizeStr())
						.setIcon(UiUtilities.tintDrawable(iconDelete, iconDeleteColor))
						.setTitle(app.getResources().getString(R.string.updates_size))
						.setLayoutId(R.layout.bottom_sheet_item_with_descr_icon_right)
						.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								if (getUpdatesSize() > 0) {
									if (getFragmentManager() != null) {
										LiveUpdatesClearDialogFragment
												.showInstance(getFragmentManager(),
														LiveUpdatesSettingsDialogFragmentNew.this, fileName);
									}
								}
							}
						})
						.create()
		);
		itemSizeIndex = items.size() - 1;

		items.add(createDividerItem());

		items.add(
				new BottomSheetItemWithCompoundButton.Builder()
						.setChecked(downloadViaWiFiPreference.get())
						.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
							@Override
							public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
								downloadViaWiFiPreference.set(isChecked);
							}
						})
						.setDescription(downloadViaWiFiPreference.get() ? on : off)
						.setIconHidden(true)
						.setTitle(app.getResources().getString(R.string.only_download_over_wifi))
						.setLayoutId(R.layout.bottom_sheet_item_with_descr_and_switch_56dp)
						.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								if (liveUpdatePreference.get() && itemOnWiFiIndex != -1) {
									BottomSheetItemWithCompoundButton button = (BottomSheetItemWithCompoundButton) items.get(itemOnWiFiIndex);
									button.setChecked(!button.isChecked());
									button.setDescription(downloadViaWiFiPreference.get() ? on : off);
								}
							}
						})
						.create()
		);
		itemOnWiFiIndex = items.size() - 1;

		items.add(new DividerSpaceItem(app, dp20));

	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(LOCAL_INDEX_FILE_NAME, fileName);
	}


	protected SpannableString getLastCheckString() {
		final long lastUpdate = preferenceLatestUpdateAvailable(fileName, settings).get();
		String updatedTimeStr = app.getResources().getString(R.string.updated, formatShortDateTime(app, lastUpdate));
		SpannableString updatedTimeSpannable = SpannableString.valueOf(updatedTimeStr);

		if (!updatedTimeStr.contains(app.getResources().getString(R.string.shared_string_never))) {
			SpannableString fullUpdatedTimeSpannable = UiUtilities.createSpannableString(updatedTimeStr,
					new StyleSpan(Typeface.BOLD), updatedTimeStr.substring(updatedTimeStr.indexOf(" - "), updatedTimeStr.length() - 1));

			final long lastCheck = preferenceLastCheck(fileName, settings).get();
			String lastCheckStr = "\n" + app.getResources().getString(R.string.last_time_checked, formatShortDateTime(app, lastCheck));

			SpannableString lastCheckTimeSpannable = UiUtilities.createSpannableString(lastCheckStr,
					new StyleSpan(Typeface.BOLD), lastCheckStr.substring(lastCheckStr.lastIndexOf(" - ")));

			return SpannableString.valueOf(TextUtils.concat(fullUpdatedTimeSpannable, lastCheckTimeSpannable));
		}
		return updatedTimeSpannable;
	}

	protected String getFrequencyHelpMessage() {
		CommonPreference<Integer> updateFrequency = preferenceUpdateFrequency(fileName, settings);
		CommonPreference<Integer> timeOfDayToUpdate = preferenceTimeOfDayToUpdate(fileName, settings);
		final long lastUpdate = preferenceLatestUpdateAvailable(fileName, settings).get();
		return formatHelpDateTime(app, UpdateFrequency.values()[updateFrequency.get()], TimeOfDay.values()[timeOfDayToUpdate.get()], lastUpdate);
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
				: app.getResources().getString(R.string.ltr_or_rtl_combine_via_space, "0.0", "kB");
	}


	private BaseBottomSheetItem createDividerItem() {
		DividerItem dividerItem = new DividerItem(app);
		int start = app.getResources().getDimensionPixelSize(R.dimen.content_padding);
		int vertical = getResources().getDimensionPixelSize(R.dimen.content_padding_small_half);
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

	private OnRadioItemClickListener getFrequencyButtonListener(@NonNull final UpdateFrequency type, final View... timeOfDayLayouts) {
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
			if (itemFrequencyHelpMessageIndex != -1) {
				((BottomSheetItemWithDescription) items.get(itemFrequencyHelpMessageIndex))
						.setDescription(getFrequencyHelpMessage());
			}
			OnLiveUpdatesForLocalChange confirmationInterface = (OnLiveUpdatesForLocalChange) getTargetFragment();
			if (confirmationInterface != null) {
				confirmationInterface.updateList();
			}
		}
	}

	@Override
	public void onUpdateStates() {
		final OnLiveUpdatesForLocalChange confirmationInterface = (OnLiveUpdatesForLocalChange) getTargetFragment();
		if (confirmationInterface != null) {
			confirmationInterface.updateList();
		}
		if (itemSizeIndex != -1) {
			((BottomSheetItemWithDescription) items.get(itemSizeIndex))
					.setDescription(getUpdatesSizeStr());
		}
	}

	public interface OnLiveUpdatesForLocalChange {

		boolean onUpdateLocalIndex(String fileName, boolean newValue, Runnable callback);

		void forceUpdateLocal(String fileName, boolean userRequested, Runnable callback);

		void runSort();

		void updateList();
	}

	@ColorRes
	public static int getPrimaryTextColorId(boolean nightMode) {
		return nightMode ? R.color.text_color_primary_dark : R.color.text_color_primary_light;
	}

	@ColorRes
	public static int getActiveTabTextColorId(boolean nightMode) {
		return nightMode ? R.color.text_color_tab_active_dark : R.color.text_color_tab_active_light;
	}

	@ColorRes
	public static int getActivePrimaryColorId(boolean nightMode) {
		return nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light;
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

}
