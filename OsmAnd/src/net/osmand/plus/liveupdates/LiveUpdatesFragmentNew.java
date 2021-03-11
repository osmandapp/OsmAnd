package net.osmand.plus.liveupdates;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import net.osmand.AndroidNetworkUtils;
import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.UiUtilities.CompoundButtonType;
import net.osmand.plus.activities.LocalIndexInfo;
import net.osmand.plus.activities.OsmandBaseExpandableListAdapter;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.liveupdates.LiveUpdatesClearDialogFragment.RefreshLiveUpdates;
import net.osmand.plus.liveupdates.LiveUpdatesHelper.TimeOfDay;
import net.osmand.plus.liveupdates.LiveUpdatesHelper.UpdateFrequency;
import net.osmand.plus.liveupdates.LiveUpdatesSettingsDialogFragmentNew.OnLiveUpdatesForLocalChange;
import net.osmand.plus.liveupdates.LoadLiveMapsTask.LocalIndexInfoAdapter;
import net.osmand.plus.liveupdates.PerformLiveUpdateAsyncTask.LiveUpdateListener;
import net.osmand.plus.settings.backend.CommonPreference;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import static net.osmand.plus.liveupdates.LiveUpdatesHelper.formatShortDateTime;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.getNameToDisplay;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.getPendingIntent;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceForLocalIndex;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceLastCheck;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceLatestUpdateAvailable;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceTimeOfDayToUpdate;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceUpdateFrequency;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.runLiveUpdate;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.setAlarmForPendingIntent;
import static net.osmand.plus.liveupdates.LiveUpdatesSettingsDialogFragmentNew.getTertiaryTextColorId;
import static net.osmand.plus.monitoring.TripRecordingActiveBottomSheet.getActiveTextColorId;
import static net.osmand.plus.monitoring.TripRecordingActiveBottomSheet.getOsmandIconColorId;
import static net.osmand.plus.monitoring.TripRecordingActiveBottomSheet.getSecondaryIconColorId;
import static net.osmand.plus.monitoring.TripRecordingActiveBottomSheet.getSecondaryTextColorId;

public class LiveUpdatesFragmentNew extends BaseOsmAndDialogFragment implements OnLiveUpdatesForLocalChange {

	public static final String URL = "https://osmand.net/api/osmlive_status";
	public static final String TAG = LiveUpdatesFragmentNew.class.getSimpleName();
	private final static Log LOG = PlatformUtil.getLog(LiveUpdatesFragmentNew.class);
	private static final String SUBSCRIPTION_URL = "https://osmand.net/features/subscription";

	private OsmandApplication app;
	private OsmandSettings settings;
	private boolean nightMode;

	private View toolbarSwitchContainer;
	private ExpandableListView listView;
	private TextViewEx descriptionTime;
	private LiveMapsAdapter adapter;

	private GetLastUpdateDateTask getLastUpdateDateTask;
	private LoadLiveMapsTask loadLiveMapsTask;

	public static void showInstance(@NonNull FragmentManager fragmentManager, Fragment target) {
		if (!fragmentManager.isStateSaved()) {
			LiveUpdatesFragmentNew fragment = new LiveUpdatesFragmentNew();
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TAG);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = getMyApplication();
		settings = getSettings();
		nightMode = isNightMode(false);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_live_updates, container, false);

		Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
		toolbar.setTitle(R.string.osm_live);
		int iconColorResId = nightMode ? R.color.active_buttons_and_links_text_dark : R.color.active_buttons_and_links_text_light;
		Drawable icBack = app.getUIUtilities().getIcon(AndroidUtils.getNavigationIconResId(app), iconColorResId);
		DrawableCompat.setTint(icBack, ContextCompat.getColor(app, iconColorResId));
		toolbar.setNavigationIcon(icBack);
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
		ImageButton iconHelp = toolbar.findViewById(R.id.toolbar_action);
		Drawable helpDrawable = app.getUIUtilities().getIcon(R.drawable.ic_action_help, iconColorResId);
		iconHelp.setImageDrawable(helpDrawable);
		iconHelp.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(SUBSCRIPTION_URL));
				if (AndroidUtils.isIntentSafe(app, intent)) {
					startActivity(intent);
				}
			}
		});

		listView = (ExpandableListView) view.findViewById(android.R.id.list);
		adapter = new LiveMapsAdapter();
		listView.setAdapter(adapter);
		expandAllGroups();

		View bottomShadowView = inflater.inflate(R.layout.card_bottom_divider, listView, false);
		listView.addFooterView(bottomShadowView);
		listView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
			@Override
			public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
				if (InAppPurchaseHelper.isSubscribedToLiveUpdates(app) && settings.IS_LIVE_UPDATES_ON.get()) {
					if (getFragmentManager() != null) {
						LiveUpdatesSettingsDialogFragmentNew
								.showInstance(getFragmentManager(), LiveUpdatesFragmentNew.this,
										adapter.getChild(groupPosition, childPosition).getFileName());
					}
					return true;
				} else {
					return false;
				}
			}
		});

		final SwipeRefreshLayout swipeRefresh = view.findViewById(R.id.swipe_refresh);
		int swipeColor = ContextCompat.getColor(app, nightMode ? R.color.osmand_orange_dark : R.color.osmand_orange);
		swipeRefresh.setColorSchemeColors(swipeColor);
		swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				if (settings.IS_LIVE_UPDATES_ON.get()) {
					showUpdateDialog();
				}
				swipeRefresh.setRefreshing(false);
			}
		});

		toolbarSwitchContainer = view.findViewById(R.id.toolbar_switch_container);
		updateToolbarSwitch(settings.IS_LIVE_UPDATES_ON.get());

		View timeContainer = view.findViewById(R.id.item_import_container);
		AndroidUtils.setListItemBackground(app, timeContainer, nightMode);

		AppCompatImageView descriptionIcon = timeContainer.findViewById(R.id.icon);
		Drawable icon = UiUtilities.createTintedDrawable(app, R.drawable.ic_action_time,
				ContextCompat.getColor(app, nightMode ? R.color.icon_color_default_dark : R.color.icon_color_default_light));
		descriptionIcon.setImageDrawable(icon);

		TextViewEx title = timeContainer.findViewById(R.id.title);
		AndroidUtils.setTextSecondaryColor(app, title, nightMode);
		title.setText(R.string.latest_openstreetmap_update);
		title.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.default_desc_text_size));
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			title.setLetterSpacing(AndroidUtils.getFloatValueFromRes(app, R.dimen.description_letter_spacing));
		}

		descriptionTime = timeContainer.findViewById(R.id.sub_title);
		AndroidUtils.setTextPrimaryColor(app, descriptionTime, nightMode);
		Typeface typeface = FontCache.getFont(app, getString(R.string.font_roboto_medium));
		descriptionTime.setTypeface(typeface);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			descriptionTime.setLetterSpacing(AndroidUtils.getFloatValueFromRes(app, R.dimen.description_letter_spacing));
		}

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		startUpdateDateAsyncTask();
		startLoadLiveMapsAsyncTask();
	}

	@Override
	public void onPause() {
		super.onPause();
		stopUpdateDateAsyncTask();
		stopLoadLiveMapsAsyncTask();
	}

	@Override
	public void onDismiss(@NonNull DialogInterface dialog) {
		super.onDismiss(dialog);
		Fragment target = getTargetFragment();
		if (target instanceof RefreshLiveUpdates) {
			((RefreshLiveUpdates) target).onUpdateStates(app);
		}
	}

	private void startUpdateDateAsyncTask() {
		getLastUpdateDateTask = new GetLastUpdateDateTask(this);
		getLastUpdateDateTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private void stopUpdateDateAsyncTask() {
		if (getLastUpdateDateTask != null) {
			getLastUpdateDateTask.cancel(true);
		}
	}

	private void startLoadLiveMapsAsyncTask() {
		if (loadLiveMapsTask == null) {
			loadLiveMapsTask = new LoadLiveMapsTask(adapter, app);
			loadLiveMapsTask.setSort(true);
			loadLiveMapsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
	}

	private void stopLoadLiveMapsAsyncTask() {
		if (loadLiveMapsTask != null && loadLiveMapsTask.getStatus() == AsyncTask.Status.RUNNING) {
			loadLiveMapsTask.cancel(false);
		}
	}

	private void updateToolbarSwitch(final boolean isChecked) {
		int switchColor = ContextCompat.getColor(app,
				isChecked ? getActiveTextColorId(nightMode) : getSecondaryTextColorId(nightMode));
		AndroidUtils.setBackground(toolbarSwitchContainer, new ColorDrawable(switchColor));

		SwitchCompat switchView = toolbarSwitchContainer.findViewById(R.id.switchWidget);
		switchView.setChecked(isChecked);
		UiUtilities.setupCompoundButton(switchView, nightMode, CompoundButtonType.TOOLBAR);

		toolbarSwitchContainer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				boolean visible = !isChecked;
				if (visible) {
					if (InAppPurchaseHelper.isSubscribedToLiveUpdates(app)) {
						switchOnLiveUpdates();
						updateToolbarSwitch(true);
					} else {
						app.showToastMessage(getString(R.string.osm_live_ask_for_purchase));
						updateToolbarSwitch(false);
					}
				} else {
					settings.IS_LIVE_UPDATES_ON.set(false);
					enableLiveUpdates(false);
					updateToolbarSwitch(false);
				}
				updateList();
			}
		});

		TextView title = toolbarSwitchContainer.findViewById(R.id.switchButtonText);
		title.setText(isChecked ? R.string.shared_string_enabled : R.string.shared_string_disabled);
	}

	private void switchOnLiveUpdates() {
		settings.IS_LIVE_UPDATES_ON.set(true);
		enableLiveUpdates(true);
		showUpdateDialog();
	}

	private void showUpdateDialog() {
		startUpdateDateAsyncTask();
		if (!Algorithms.isEmpty(adapter.mapsList)) {
			final LiveUpdateListener listener = new LiveUpdateListener() {
				@Override
				public void processFinish() {
					adapter.notifyDataSetChanged();
				}
			};
			if (adapter.countEnabled == 1) {
				LocalIndexInfo li = adapter.mapsList.get(0);
				runLiveUpdate(getActivity(), li.getFileName(), false, listener);
			} else if (adapter.countEnabled > 1) {
				AlertDialog.Builder bld = new AlertDialog.Builder(getMyActivity());
				bld.setMessage(R.string.update_all_maps_now);
				bld.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						for (LocalIndexInfo li : adapter.mapsList) {
							CommonPreference<Boolean> localUpdateOn = preferenceForLocalIndex(li.getFileName(), settings);
							if (localUpdateOn.get()) {
								runLiveUpdate(getActivity(), li.getFileName(), false, listener);
							}
						}
					}
				});
				bld.setNegativeButton(R.string.shared_string_no, null);
				bld.show();
			}
		}
	}

	private void enableLiveUpdates(boolean enable) {
		if (!Algorithms.isEmpty(adapter.mapsList)) {
			AlarmManager alarmMgr = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
			for (LocalIndexInfo li : adapter.mapsList) {
				CommonPreference<Boolean> localUpdateOn = preferenceForLocalIndex(li.getFileName(), settings);
				if (localUpdateOn.get()) {
					String fileName = li.getFileName();
					PendingIntent alarmIntent = getPendingIntent(app, fileName);
					if (enable) {
						final CommonPreference<Integer> updateFrequencyPreference =
								preferenceUpdateFrequency(fileName, settings);
						final CommonPreference<Integer> timeOfDayPreference =
								preferenceTimeOfDayToUpdate(fileName, settings);
						UpdateFrequency updateFrequency = UpdateFrequency.values()[updateFrequencyPreference.get()];
						TimeOfDay timeOfDayToUpdate = TimeOfDay.values()[timeOfDayPreference.get()];
						setAlarmForPendingIntent(alarmIntent, alarmMgr, updateFrequency, timeOfDayToUpdate);
					} else {
						alarmMgr.cancel(alarmIntent);
					}
				}
			}
		}
	}

	private void expandAllGroups() {
		for (int i = 0; i < adapter.getGroupCount(); i++) {
			listView.expandGroup(i);
		}
	}

	public void notifyLiveUpdatesChanged() {
		if (getActivity() != null) {
			runSort();
		}
	}

	protected class LiveMapsAdapter extends OsmandBaseExpandableListAdapter implements LocalIndexInfoAdapter {
		private final ArrayList<LocalIndexInfo> mapsList = new ArrayList<>();
		private int countEnabled = 0;
		private TextViewEx countView;

		@Override
		public void addData(LocalIndexInfo info) {
			mapsList.add(info);
		}

		@Override
		public void clearData() {
		}

		@Override
		public void sort() {
			countEnabled = 0;
			for (LocalIndexInfo map : mapsList) {
				CommonPreference<Boolean> preference = preferenceForLocalIndex(map.getFileName(), getSettings());
				if (preference.get()) {
					countEnabled++;
				}
			}
			updateCountEnabled();

			Collections.sort(mapsList);
			Collections.sort(mapsList, new Comparator<LocalIndexInfo>() {
				@Override
				public int compare(LocalIndexInfo o1, LocalIndexInfo o2) {
					CommonPreference<Boolean> preference1 = preferenceForLocalIndex(o1.getFileName(), getSettings());
					CommonPreference<Boolean> preference2 = preferenceForLocalIndex(o2.getFileName(), getSettings());
					return preference2.get().compareTo(preference1.get());
				}
			});
			notifyDataSetInvalidated();
		}

		@Override
		public void updateCountEnabled() {
			if (countView != null) {
				String countText = countEnabled + "/" + mapsList.size();
				countView.setText(countText);
			}
		}

		@Override
		public LocalIndexInfo getChild(int groupPosition, int childPosition) {
			return mapsList.get(childPosition);
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return groupPosition * 10000 + childPosition;    // it would be unusable to have 10000 local indexes
		}

		@Override
		public View getChildView(final int groupPosition, final int childPosition,
								 boolean isLastChild, View convertView, ViewGroup parent) {
			LiveMapsViewHolder viewHolder;
//			if (convertView == null) {
			LayoutInflater inflater = UiUtilities.getInflater(app, nightMode);
			convertView = inflater.inflate(R.layout.list_item_triple_row_icon_and_menu, parent, false);
			viewHolder = new LiveMapsViewHolder(convertView);
			convertView.setTag(viewHolder);
//			} else {
//				viewHolder = (LiveMapsViewHolder) convertView.getTag();
//			}
			viewHolder.bindLocalIndexInfo(getChild(groupPosition, childPosition).getFileName());
			return convertView;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
			View view = convertView;
			if (view == null) {
				LayoutInflater inflater = UiUtilities.getInflater(app, nightMode);
				view = inflater.inflate(R.layout.list_group_title_with_right_descr, parent, false);
			}
			view.setOnClickListener(null);
			View topShadowView = view.findViewById(R.id.bottomShadowView);
			if (groupPosition == 0) {
				topShadowView.setVisibility(View.GONE);
			} else {
				topShadowView.setVisibility(View.VISIBLE);
			}

			TextViewEx titleView = ((TextViewEx) view.findViewById(R.id.title));
			titleView.setText(getGroup(groupPosition));

			countView = ((TextViewEx) view.findViewById(R.id.description));
			AndroidUtils.setTextSecondaryColor(app, countView, nightMode);
			updateCountEnabled();

			return view;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			return mapsList.size();
		}

		@Override
		public String getGroup(int groupPosition) {
			return getString(R.string.available_maps);
		}

		@Override
		public int getGroupCount() {
			return 1;
		}

		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}
	}

	private class LiveMapsViewHolder {
		private final ImageView statusIcon;
		private final TextView title;
		private final TextView subTitle;
		private final TextView description;
		private final CompoundButton option;

		private LiveMapsViewHolder(View view) {
			statusIcon = (AppCompatImageView) view.findViewById(R.id.icon);
			title = (TextView) view.findViewById(R.id.title);
			subTitle = (TextView) view.findViewById(R.id.sub_title);
			description = (TextView) view.findViewById(R.id.description);
			option = (CompoundButton) view.findViewById(R.id.compound_button);
		}

		public void bindLocalIndexInfo(@NonNull final String item) {
			boolean liveUpdateOn = settings.IS_LIVE_UPDATES_ON.get();
			CommonPreference<Boolean> localUpdateOn = preferenceForLocalIndex(item, settings);
//			IncrementalChangesManager changesManager = app.getResourceManager().getChangesManager();
			option.setChecked(localUpdateOn.get());
			if (!liveUpdateOn && localUpdateOn.get()) {
				UiUtilities.setupCompoundButton(nightMode, ContextCompat.getColor(app, getTertiaryTextColorId(nightMode)), option);
			} else {
				UiUtilities.setupCompoundButton(option, nightMode, CompoundButtonType.GLOBAL);
			}

			title.setText(getNameToDisplay(item, app));

			AndroidUiHelper.updateVisibility(subTitle, localUpdateOn.get());
			if (localUpdateOn.get()) {
				int frequencyId = preferenceUpdateFrequency(item, settings).get();
				final UpdateFrequency frequency = UpdateFrequency.values()[frequencyId];
				String subTitleText = getString(frequency.getLocalizedId());
				/*int timeOfDateToUpdateId = preferenceTimeOfDayToUpdate(item, settings).get();
				final TimeOfDay timeOfDay = TimeOfDay.values()[timeOfDateToUpdateId];
				if (frequency != UpdateFrequency.HOURLY) {
					subTitleText += " • " + getString(timeOfDay.getLocalizedId());
				}*/
				subTitle.setText(subTitleText);
				subTitle.setTextColor(ContextCompat.getColor(app, liveUpdateOn ? getActiveTextColorId(nightMode) : getSecondaryTextColorId(nightMode)));
				Typeface typeface = FontCache.getFont(app, getString(R.string.font_roboto_medium));
				subTitle.setTypeface(typeface);
			}

			Drawable statusDrawable = ContextCompat.getDrawable(app, R.drawable.ic_map);
			int resColorId = !localUpdateOn.get() ? getSecondaryIconColorId(nightMode) :
					!liveUpdateOn ? getDefaultIconColorId(nightMode) : getOsmandIconColorId(nightMode);
			int statusColor = ContextCompat.getColor(app, resColorId);
			if (statusDrawable != null) {
				DrawableCompat.setTint(statusDrawable, statusColor);
			}
			statusIcon.setImageDrawable(statusDrawable);

			description.setText(getLastCheckString(item, app));

			if (InAppPurchaseHelper.isSubscribedToLiveUpdates(app)) {
				option.setEnabled(liveUpdateOn);
				option.setOnCheckedChangeListener(new SwitchCompat.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						onUpdateLocalIndex(item, isChecked, null);
					}
				});
			} else {
				option.setEnabled(false);
			}
		}
	}

	public static class GetLastUpdateDateTask extends AsyncTask<Void, Void, String> {

		private final OsmandApplication app;
		private final WeakReference<LiveUpdatesFragmentNew> fragment;

		GetLastUpdateDateTask(LiveUpdatesFragmentNew fragment) {
			this.fragment = new WeakReference<>(fragment);
			app = fragment.getMyApplication();
		}

		@Override
		protected String doInBackground(Void... params) {
			try {
				return AndroidNetworkUtils.sendRequest(app, URL, null,
						"Requesting map updates info...", false, false);
			} catch (Exception e) {
				LOG.error("Error: " + "Requesting map updates info error", e);
				return null;
			}
		}

		@Override
		protected void onPostExecute(String response) {
			LiveUpdatesFragmentNew f = fragment.get();
			if (response != null && f != null) {
				TextViewEx descriptionTime = f.descriptionTime;
				if (descriptionTime != null) {
					SimpleDateFormat source = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
					source.setTimeZone(TimeZone.getTimeZone("UTC"));
					SimpleDateFormat dest = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
					dest.setTimeZone(TimeZone.getDefault());
					try {
						LOG.debug("response = " + response);
						Date parsed = source.parse(response);
						if (parsed != null) {
							long dateTime = parsed.getTime();
							LOG.debug("dateTime = " + dateTime);
							descriptionTime.setText(dest.format(parsed));
						}
					} catch (ParseException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	protected static String getLastCheckString(String fileName, OsmandApplication app) {
		return getLastCheckString(fileName, app, false);
	}

	protected static String getLastCheckString(String fileName, OsmandApplication app, boolean lastTimeChecked) {
		OsmandSettings settings = app.getSettings();

		final long lastUpdate = preferenceLatestUpdateAvailable(fileName, settings).get();
		String lastUpdateString = formatShortDateTime(app, lastUpdate);
		String description = app.getResources().getString(R.string.updated, lastUpdateString);

		if (lastTimeChecked) {
			final long lastCheck = preferenceLastCheck(fileName, settings).get();
			String lastCheckString = formatShortDateTime(app, lastCheck);
			if (!lastUpdateString.equals(app.getResources().getString(R.string.shared_string_never))) {
				description = description.concat("\n" + app.getResources().getString(R.string.last_time_checked, lastCheckString));
			}
		}
		return description;
	}

	@Override
	public boolean onUpdateLocalIndex(String fileName, boolean newValue, final Runnable callback) {

		int frequencyId = preferenceUpdateFrequency(fileName, settings).get();
		int timeOfDateToUpdateId = preferenceTimeOfDayToUpdate(fileName, settings).get();
		final AlarmManager alarmManager = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
		final PendingIntent alarmIntent = getPendingIntent(app, fileName);

		final CommonPreference<Boolean> liveUpdatePreference = preferenceForLocalIndex(fileName, settings);
		liveUpdatePreference.set(newValue);
		if (settings.IS_LIVE_UPDATES_ON.get() && liveUpdatePreference.get()) {
			runLiveUpdate(getActivity(), fileName, true, new LiveUpdateListener() {
				@Override
				public void processFinish() {
					runSort();
					if (callback != null) {
						callback.run();
					}
				}
			});
			UpdateFrequency updateFrequency = UpdateFrequency.values()[frequencyId];
			TimeOfDay timeOfDayToUpdate = TimeOfDay.values()[timeOfDateToUpdateId];
			setAlarmForPendingIntent(alarmIntent, alarmManager, updateFrequency, timeOfDayToUpdate);
		} else {
			alarmManager.cancel(alarmIntent);
			runSort();
		}

		return true;
	}

	@Override
	public void forceUpdateLocal(String fileName, boolean userRequested, final Runnable callback) {
		if (settings.IS_LIVE_UPDATES_ON.get()) {
			runLiveUpdate(getActivity(), fileName, userRequested, new LiveUpdateListener() {
				@Override
				public void processFinish() {
					updateList();
					if (callback != null) {
						callback.run();
					}
				}
			});
		}
	}

	@Override
	public void runSort() {
		if (adapter != null) {
			adapter.sort();
		}
	}

	@Override
	public void updateList() {
		if (adapter != null) {
			adapter.notifyDataSetChanged();
		}
	}

	@ColorRes
	public static int getDefaultIconColorId(boolean nightMode) {
		return nightMode ? R.color.icon_color_default_dark : R.color.icon_color_default_light;
	}

}
