package net.osmand.plus.liveupdates;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.AppBarLayout;

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
import net.osmand.plus.inapp.InAppPurchases.InAppSubscription;
import net.osmand.plus.liveupdates.LiveUpdatesClearBottomSheet.RefreshLiveUpdates;
import net.osmand.plus.liveupdates.LiveUpdatesHelper.LiveUpdateListener;
import net.osmand.plus.liveupdates.LiveUpdatesHelper.TimeOfDay;
import net.osmand.plus.liveupdates.LiveUpdatesHelper.UpdateFrequency;
import net.osmand.plus.liveupdates.LiveUpdatesSettingsBottomSheet.OnLiveUpdatesForLocalChange;
import net.osmand.plus.liveupdates.LoadLiveMapsTask.LocalIndexInfoAdapter;
import net.osmand.plus.settings.backend.CommonPreference;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.plus.wikipedia.WikipediaDialogFragment;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import static net.osmand.AndroidUtils.getSecondaryTextColorId;
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
import static net.osmand.plus.liveupdates.LiveUpdatesSettingsBottomSheet.getTertiaryTextColorId;
import static net.osmand.plus.monitoring.TripRecordingBottomSheet.getActiveTextColorId;
import static net.osmand.plus.monitoring.TripRecordingBottomSheet.getOsmandIconColorId;
import static net.osmand.plus.monitoring.TripRecordingBottomSheet.getSecondaryIconColorId;

public class LiveUpdatesFragment extends BaseOsmAndDialogFragment implements OnLiveUpdatesForLocalChange, LiveUpdateListener {

	public static final String URL = "https://osmand.net/api/osmlive_status";
	public static final String TAG = LiveUpdatesFragment.class.getSimpleName();
	private final static Log LOG = PlatformUtil.getLog(LiveUpdatesFragment.class);
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
			LiveUpdatesFragment fragment = new LiveUpdatesFragment();
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TAG);
		}
	}

	public static void showUpdateDialog(Activity context, FragmentManager fragmentManager, final LiveUpdateListener listener) {
		List<LocalIndexInfo> mapsToUpdate = listener.getMapsToUpdate();
		if (!Algorithms.isEmpty(mapsToUpdate)) {
			int countEnabled = listener.getMapsToUpdate().size();
			if (countEnabled == 1) {
				runLiveUpdate(context, mapsToUpdate.get(0).getFileName(), false, new Runnable() {
					@Override
					public void run() {
						listener.processFinish();
					}
				});
			} else if (countEnabled > 1) {
				Fragment target = null;
				if (listener instanceof Fragment) {
					target = (Fragment) listener;
				}
				LiveUpdatesUpdateAllBottomSheet.showInstance(fragmentManager, target);
			}
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
		createToolbar((ViewGroup) view.findViewById(R.id.app_bar));

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
						LiveUpdatesSettingsBottomSheet
								.showInstance(getFragmentManager(), LiveUpdatesFragment.this,
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
					showUpdateDialog(getActivity(), getFragmentManager(), LiveUpdatesFragment.this);
					startUpdateDateAsyncTask();
				}
				swipeRefresh.setRefreshing(false);
			}
		});

		View headerView = inflater.inflate(R.layout.list_item_import, listView, false);
		View timeContainer = headerView.findViewById(R.id.item_import_container);
		AndroidUtils.setListItemBackground(app, timeContainer, nightMode);
		AndroidUiHelper.setVisibility(View.VISIBLE, headerView.findViewById(R.id.bottom_divider));
		listView.addHeaderView(headerView);

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
			loadLiveMapsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
	}

	private void stopLoadLiveMapsAsyncTask() {
		if (loadLiveMapsTask != null && loadLiveMapsTask.getStatus() == AsyncTask.Status.RUNNING) {
			loadLiveMapsTask.cancel(false);
		}
	}

	protected void createToolbar(ViewGroup appBar) {
		AppBarLayout appBarLayout = (AppBarLayout) UiUtilities.getInflater(getActivity(), nightMode)
				.inflate(R.layout.global_preferences_toolbar_with_switch, appBar);

		Toolbar toolbar = (Toolbar) appBarLayout.findViewById(R.id.toolbar);
		TextViewEx toolbarTitle = (TextViewEx) toolbar.findViewById(R.id.toolbar_title);
		toolbarTitle.setText(R.string.osm_live);

		View closeButton = toolbar.findViewById(R.id.close_button);
		closeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		FrameLayout iconHelpContainer = toolbar.findViewById(R.id.action_button);
		int iconColorResId = nightMode ? R.color.active_buttons_and_links_text_dark : R.color.active_buttons_and_links_text_light;
		AppCompatImageButton iconHelp = toolbar.findViewById(R.id.action_button_icon);
		Drawable helpDrawable = app.getUIUtilities().getIcon(R.drawable.ic_action_help_online, iconColorResId);
		iconHelp.setImageDrawable(helpDrawable);
		iconHelpContainer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Activity activity = getActivity();
				if (activity != null) {
					WikipediaDialogFragment.showFullArticle(activity, Uri.parse(SUBSCRIPTION_URL), nightMode);
				}
			}
		});

		toolbarSwitchContainer = appBarLayout.findViewById(R.id.toolbar_switch_container);
		updateToolbarSwitch(settings.IS_LIVE_UPDATES_ON.get());
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
		showUpdateDialog(getMyActivity(), getFragmentManager(), this);
		startUpdateDateAsyncTask();
	}

	private void enableLiveUpdates(boolean enable) {
		if (!Algorithms.isEmpty(adapter.mapsList)) {
			AlarmManager alarmMgr = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
			List<LocalIndexInfo> mapsToUpdate = getMapsToUpdate(adapter.mapsList, settings);
			for (LocalIndexInfo li : mapsToUpdate) {
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

	private void expandAllGroups() {
		for (int i = 0; i < adapter.getGroupCount(); i++) {
			listView.expandGroup(i);
		}
	}

	public static int updateCountEnabled(TextView countView, List<LocalIndexInfo> mapsList, OsmandSettings settings) {
		int countEnabled = getMapsToUpdate(mapsList, settings).size();
		if (countView != null) {
			String countText = countEnabled + "/" + mapsList.size();
			countView.setText(countText);
		}
		return countEnabled;
	}

	public static List<LocalIndexInfo> getMapsToUpdate(List<LocalIndexInfo> mapsList, OsmandSettings settings) {
		List<LocalIndexInfo> listToUpdate = new ArrayList<>();
		for (LocalIndexInfo mapToUpdate : mapsList) {
			CommonPreference<Boolean> preference = preferenceForLocalIndex(mapToUpdate.getFileName(), settings);
			if (preference.get()) {
				listToUpdate.add(mapToUpdate);
			}
		}
		return listToUpdate;
	}

	protected class LiveMapsAdapter extends OsmandBaseExpandableListAdapter implements LocalIndexInfoAdapter {
		private final ArrayList<LocalIndexInfo> mapsList = new ArrayList<>();

		@Override
		public void addData(LocalIndexInfo info) {
			mapsList.add(info);
		}

		@Override
		public void clearData() {
			mapsList.clear();
		}

		@Override
		public void onDataUpdated() {
			sort();
		}

		public void sort() {
			Collections.sort(mapsList, new Comparator<LocalIndexInfo>() {
				@Override
				public int compare(LocalIndexInfo o1, LocalIndexInfo o2) {
					CommonPreference<Boolean> preference1 = preferenceForLocalIndex(o1.getFileName(), getSettings());
					CommonPreference<Boolean> preference2 = preferenceForLocalIndex(o2.getFileName(), getSettings());
					int prefSort = preference2.get().compareTo(preference1.get());
					if (prefSort != 0) {
						return prefSort;
					}
					return o1.compareTo(o2);
				}
			});
			notifyDataSetInvalidated();
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
			LayoutInflater inflater = UiUtilities.getInflater(app, nightMode);
			convertView = inflater.inflate(R.layout.list_item_triple_row_icon_and_menu, parent, false);
			LiveMapsViewHolder viewHolder = new LiveMapsViewHolder(convertView);
			convertView.setTag(viewHolder);
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

			TextViewEx countView = ((TextViewEx) view.findViewById(R.id.description));
			AndroidUtils.setTextSecondaryColor(app, countView, nightMode);

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
		private final CompoundButton compoundButton;

		private LiveMapsViewHolder(View view) {
			statusIcon = (AppCompatImageView) view.findViewById(R.id.icon);
			title = (TextView) view.findViewById(R.id.title);
			subTitle = (TextView) view.findViewById(R.id.sub_title);
			description = (TextView) view.findViewById(R.id.description);
			compoundButton = (CompoundButton) view.findViewById(R.id.compound_button);
		}

		public void bindLocalIndexInfo(@NonNull final String item) {
			boolean liveUpdateOn = settings.IS_LIVE_UPDATES_ON.get();
			CommonPreference<Boolean> localUpdateOn = preferenceForLocalIndex(item, settings);
//			IncrementalChangesManager changesManager = app.getResourceManager().getChangesManager();
			compoundButton.setChecked(localUpdateOn.get());
			if (!liveUpdateOn && localUpdateOn.get()) {
				UiUtilities.setupCompoundButton(nightMode, ContextCompat.getColor(app, getTertiaryTextColorId(nightMode)), compoundButton);
			} else {
				UiUtilities.setupCompoundButton(compoundButton, nightMode, CompoundButtonType.GLOBAL);
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
				subTitle.setTextColor(ContextCompat.getColor(app, liveUpdateOn
						? getActiveTextColorId(nightMode) : getSecondaryTextColorId(nightMode)));
				Typeface typeface = FontCache.getFont(app, getString(R.string.font_roboto_medium));
				subTitle.setTypeface(typeface);
			}

			Drawable statusDrawable = AppCompatResources.getDrawable(app, R.drawable.ic_map);
			int resColorId = !localUpdateOn.get() ? getSecondaryIconColorId(nightMode) :
					!liveUpdateOn ? getDefaultIconColorId(nightMode) : getOsmandIconColorId(nightMode);
			int statusColor = ContextCompat.getColor(app, resColorId);
			if (statusDrawable != null) {
				DrawableCompat.setTint(statusDrawable, statusColor);
			}
			statusIcon.setImageDrawable(statusDrawable);

			description.setText(getLastCheckString(item, app));

			if (InAppPurchaseHelper.isSubscribedToLiveUpdates(app)) {
				compoundButton.setEnabled(liveUpdateOn);
				compoundButton.setOnCheckedChangeListener(new SwitchCompat.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						onUpdateLocalIndex(item, isChecked, new Runnable() {
							@Override
							public void run() {
								runSort();
							}
						});
					}
				});
			} else {
				compoundButton.setEnabled(false);
			}
		}
	}

	public static class GetLastUpdateDateTask extends AsyncTask<Void, Void, String> {

		private final OsmandApplication app;
		private final WeakReference<LiveUpdatesFragment> fragment;

		GetLastUpdateDateTask(LiveUpdatesFragment fragment) {
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
			LiveUpdatesFragment f = fragment.get();
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
						LOG.error(e.getMessage());
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
		String description = app.getString(R.string.updated, lastUpdateString);

		if (lastTimeChecked) {
			final long lastCheck = preferenceLastCheck(fileName, settings).get();
			String lastCheckString = formatShortDateTime(app, lastCheck);
			if (!lastUpdateString.equals(app.getString(R.string.shared_string_never))) {
				description = description.concat("\n" + app.getString(R.string.last_time_checked, lastCheckString));
			}
		}
		return description;
	}

	@Override
	public void processFinish() {
		adapter.notifyDataSetChanged();
	}

	@Override
	public List<LocalIndexInfo> getMapsToUpdate() {
		return getMapsToUpdate(adapter.mapsList, settings);
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
			runLiveUpdate(getActivity(), fileName, true, callback);
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
			runLiveUpdate(getActivity(), fileName, userRequested, callback);
		}
	}

	@Override
	public void runSort() {
		if (adapter != null) {
			adapter.onDataUpdated();
		}
	}

	@Override
	public void updateList() {
		if (adapter != null) {
			adapter.notifyDataSetChanged();
		}
	}

	public static String getSupportRegionName(OsmandApplication app, InAppPurchaseHelper purchaseHelper) {
		OsmandSettings settings = app.getSettings();
		String countryName = settings.BILLING_USER_COUNTRY.get();
		if (purchaseHelper != null) {
			List<InAppSubscription> subscriptions = purchaseHelper.getLiveUpdates().getVisibleSubscriptions();
			boolean donationSupported = false;
			for (InAppSubscription s : subscriptions) {
				if (s.isDonationSupported()) {
					donationSupported = true;
					break;
				}
			}
			if (donationSupported) {
				if (Algorithms.isEmpty(countryName)) {
					if (OsmandSettings.BILLING_USER_DONATION_NONE_PARAMETER.equals(settings.BILLING_USER_COUNTRY_DOWNLOAD_NAME.get())) {
						countryName = app.getString(R.string.osmand_team);
					} else {
						countryName = app.getString(R.string.shared_string_world);
					}
				}
			} else {
				countryName = app.getString(R.string.osmand_team);
			}
		} else {
			countryName = app.getString(R.string.osmand_team);
		}
		return countryName;
	}

	public static String getSupportRegionHeader(OsmandApplication app, String supportRegion) {
		return supportRegion.equals(app.getString(R.string.osmand_team)) ?
				app.getString(R.string.default_buttons_support) :
				app.getString(R.string.osm_live_support_region);
	}

	@ColorRes
	public static int getDefaultIconColorId(boolean nightMode) {
		return nightMode ? R.color.icon_color_default_dark : R.color.icon_color_default_light;
	}
}
