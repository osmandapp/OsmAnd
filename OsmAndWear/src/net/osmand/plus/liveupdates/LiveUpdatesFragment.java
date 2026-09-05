package net.osmand.plus.liveupdates;

import static net.osmand.IndexConstants.BINARY_MAP_INDEX_EXT;
import static net.osmand.IndexConstants.BINARY_ROAD_MAP_INDEX_EXT;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.*;
import static net.osmand.plus.plugins.monitoring.TripRecordingBottomSheet.getOsmandIconColorId;
import static net.osmand.plus.plugins.monitoring.TripRecordingBottomSheet.getSecondaryIconColorId;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
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

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.AppBarLayout;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.base.OsmandBaseExpandableListAdapter;
import net.osmand.plus.chooseplan.ChoosePlanFragment;
import net.osmand.plus.chooseplan.OsmAndFeature;
import net.osmand.plus.download.local.LocalItem;
import net.osmand.plus.download.local.LocalItemUtils;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.inapp.InAppPurchaseUtils;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscription;
import net.osmand.plus.liveupdates.LiveUpdatesClearBottomSheet.RefreshLiveUpdates;
import net.osmand.plus.liveupdates.LiveUpdatesSettingsBottomSheet.OnLiveUpdatesForLocalChange;
import net.osmand.plus.liveupdates.LoadLiveMapsTask.LocalIndexInfoAdapter;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.AndroidNetworkUtils;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UiUtilities.CompoundButtonType;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class LiveUpdatesFragment extends BaseOsmAndDialogFragment implements OnLiveUpdatesForLocalChange, LiveUpdateListener {

	public static final String URL = "https://osmand.net/api/osmlive_status";
	public static final String TAG = LiveUpdatesFragment.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(LiveUpdatesFragment.class);

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

	public static void showUpdateDialog(Activity activity, FragmentManager fragmentManager, LiveUpdateListener listener) {
		List<LocalItem> mapsToUpdate = listener.getMapsToUpdate();
		if (!Algorithms.isEmpty(mapsToUpdate)) {
			int countEnabled = listener.getMapsToUpdate().size();
			if (countEnabled == 1) {
				runLiveUpdate(activity, getFileNameWithoutRoadSuffix(mapsToUpdate.get(0)), false, listener::processFinish);
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
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		updateNightMode();
		View view = themedInflater.inflate(R.layout.fragment_live_updates, container, false);
		createToolbar(view.findViewById(R.id.app_bar));

		listView = view.findViewById(android.R.id.list);

		View headerView = inflater.inflate(R.layout.list_item_import, listView, false);
		View bottomShadowView = inflater.inflate(R.layout.card_bottom_divider, listView, false);

		listView.addHeaderView(headerView);
		listView.addFooterView(bottomShadowView);

		adapter = new LiveMapsAdapter();
		listView.setAdapter(adapter);
		expandAllGroups();

		listView.setOnChildClickListener((parent, v, groupPosition, childPosition, id) -> {
			if (InAppPurchaseUtils.isLiveUpdatesAvailable(app) && settings.IS_LIVE_UPDATES_ON.get()) {
				if (getFragmentManager() != null) {
					LiveUpdatesSettingsBottomSheet
							.showInstance(getFragmentManager(), LiveUpdatesFragment.this,
									getFileNameWithoutRoadSuffix(adapter.getChild(groupPosition, childPosition)));
				}
				return true;
			} else {
				return false;
			}
		});

		SwipeRefreshLayout swipeRefresh = view.findViewById(R.id.swipe_refresh);
		int swipeColor = ContextCompat.getColor(app, nightMode ? R.color.osmand_orange_dark : R.color.osmand_orange);
		swipeRefresh.setColorSchemeColors(swipeColor);
		swipeRefresh.setOnRefreshListener(() -> {
			if (settings.IS_LIVE_UPDATES_ON.get()) {
				showUpdateDialog(getActivity(), getFragmentManager(), LiveUpdatesFragment.this);
				startUpdateDateAsyncTask();
			}
			swipeRefresh.setRefreshing(false);
		});

		View timeContainer = headerView.findViewById(R.id.item_import_container);
		AndroidUtils.setListItemBackground(app, timeContainer, nightMode);
		AndroidUiHelper.setVisibility(View.VISIBLE, headerView.findViewById(R.id.bottom_divider));

		AppCompatImageView descriptionIcon = timeContainer.findViewById(R.id.icon);
		Drawable icon = UiUtilities.createTintedDrawable(app, R.drawable.ic_action_time,
				ColorUtilities.getDefaultIconColor(app, nightMode));
		descriptionIcon.setImageDrawable(icon);

		TextViewEx title = timeContainer.findViewById(R.id.title);
		AndroidUtils.setTextSecondaryColor(app, title, nightMode);
		title.setText(R.string.latest_openstreetmap_update);
		title.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.default_desc_text_size));
		title.setLetterSpacing(AndroidUtils.getFloatValueFromRes(app, R.dimen.description_letter_spacing));

		descriptionTime = timeContainer.findViewById(R.id.sub_title);
		AndroidUtils.setTextPrimaryColor(app, descriptionTime, nightMode);
		descriptionTime.setTypeface(FontCache.getMediumFont());
		descriptionTime.setLetterSpacing(AndroidUtils.getFloatValueFromRes(app, R.dimen.description_letter_spacing));

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

	protected void createToolbar(@NonNull ViewGroup appBar) {
		AppBarLayout appBarLayout = (AppBarLayout) UiUtilities.getInflater(getActivity(), nightMode)
				.inflate(R.layout.global_preferences_toolbar_with_switch, appBar);

		Toolbar toolbar = appBarLayout.findViewById(R.id.toolbar);
		TextViewEx toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
		toolbarTitle.setText(R.string.osm_live);

		ImageView closeButton = toolbar.findViewById(R.id.close_button);
		UiUtilities.rotateImageByLayoutDirection(closeButton);
		closeButton.setOnClickListener(v -> dismiss());

		LayoutInflater inflater = UiUtilities.getInflater(toolbar.getContext(), nightMode);
		ViewGroup container = toolbar.findViewById(R.id.actions_container);

		int colorId = ColorUtilities.getActiveButtonsAndLinksTextColorId(nightMode);
		ImageButton button = (ImageButton) inflater.inflate(R.layout.action_button, container, false);
		button.setImageDrawable(getIcon(R.drawable.ic_action_help_online, colorId));
		button.setOnClickListener(view -> {
			Activity activity = getActivity();
			if (activity != null) {
				String docsUrl = getString(R.string.docs_osmand_live);
				AndroidUtils.openUrl(activity, Uri.parse(docsUrl), nightMode);
			}
		});
		container.addView(button);

		toolbarSwitchContainer = appBarLayout.findViewById(R.id.toolbar_switch_container);
		updateToolbarSwitch(settings.IS_LIVE_UPDATES_ON.get());
	}

	private void updateToolbarSwitch(boolean isChecked) {
		int switchColor = ContextCompat.getColor(app,
				isChecked ? ColorUtilities.getActiveColorId(nightMode) : ColorUtilities.getSecondaryTextColorId(nightMode));
		AndroidUtils.setBackground(toolbarSwitchContainer, new ColorDrawable(switchColor));

		SwitchCompat switchView = toolbarSwitchContainer.findViewById(R.id.switchWidget);
		switchView.setChecked(isChecked);
		UiUtilities.setupCompoundButton(switchView, nightMode, CompoundButtonType.TOOLBAR);

		toolbarSwitchContainer.setOnClickListener(view -> {
			boolean visible = !isChecked;
			if (visible) {
				if (InAppPurchaseUtils.isLiveUpdatesAvailable(app)) {
					switchOnLiveUpdates();
					updateToolbarSwitch(true);
				} else {
					updateToolbarSwitch(false);
					app.showToastMessage(getString(R.string.osm_live_ask_for_purchase));

					FragmentActivity activity = getActivity();
					if (activity != null) {
						ChoosePlanFragment.showInstance(activity, OsmAndFeature.HOURLY_MAP_UPDATES);
					}
				}
			} else {
				settings.IS_LIVE_UPDATES_ON.set(false);
				enableLiveUpdates(false);
				updateToolbarSwitch(false);
			}
			updateList();
		});

		TextView title = toolbarSwitchContainer.findViewById(R.id.switchButtonText);
		title.setText(isChecked ? R.string.shared_string_enabled : R.string.shared_string_disabled);
	}

	private void switchOnLiveUpdates() {
		settings.IS_LIVE_UPDATES_ON.set(true);
		enableLiveUpdates(true);
		showUpdateDialog(getActivity(), getFragmentManager(), this);
		startUpdateDateAsyncTask();
	}

	private void enableLiveUpdates(boolean enable) {
		if (!Algorithms.isEmpty(adapter.localItems)) {
			AlarmManager alarmMgr = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
			List<LocalItem> mapsToUpdate = getMapsToUpdate(adapter.localItems, settings);
			for (LocalItem item : mapsToUpdate) {
				String fileName = getFileNameWithoutRoadSuffix(item);
				PendingIntent alarmIntent = getPendingIntent(app, fileName);
				if (enable) {
					CommonPreference<Integer> updateFrequencyPreference =
							preferenceUpdateFrequency(fileName, settings);
					CommonPreference<Integer> timeOfDayPreference =
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

	public static int updateCountEnabled(TextView countView, List<LocalItem> mapsList, OsmandSettings settings) {
		int countEnabled = getMapsToUpdate(mapsList, settings).size();
		if (countView != null) {
			String countText = countEnabled + "/" + mapsList.size();
			countView.setText(countText);
		}
		return countEnabled;
	}

	public static List<LocalItem> getMapsToUpdate(List<LocalItem> mapsList, OsmandSettings settings) {
		List<LocalItem> listToUpdate = new ArrayList<>();
		for (LocalItem mapToUpdate : mapsList) {
			CommonPreference<Boolean> preference = preferenceForLocalIndex(getFileNameWithoutRoadSuffix(mapToUpdate), settings);
			if (preference.get()) {
				listToUpdate.add(mapToUpdate);
			}
		}
		return listToUpdate;
	}

	protected class LiveMapsAdapter extends OsmandBaseExpandableListAdapter implements LocalIndexInfoAdapter {
		private final List<LocalItem> localItems = new ArrayList<>();

		@Override
		public void addData(@NonNull List<LocalItem> indexes) {
			if (LocalItemUtils.addUnique(localItems, indexes)) {
				notifyDataSetChanged();
			}
		}

		@Override
		public void clearData() {
			localItems.clear();
			notifyDataSetChanged();
		}

		@Override
		public void onDataUpdated() {
			sort();
		}

		public void sort() {
			Collections.sort(localItems, (o1, o2) -> {
				CommonPreference<Boolean> preference1 = preferenceForLocalIndex(getFileNameWithoutRoadSuffix(o1), settings);
				CommonPreference<Boolean> preference2 = preferenceForLocalIndex(getFileNameWithoutRoadSuffix(o2), settings);
				int prefSort = preference2.get().compareTo(preference1.get());
				if (prefSort != 0) {
					return prefSort;
				}
				return o1.compareTo(o2);
			});
			notifyDataSetInvalidated();
		}

		@Override
		public LocalItem getChild(int groupPosition, int childPosition) {
			return localItems.get(childPosition);
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return groupPosition * 10000 + childPosition;    // it would be unusable to have 10000 local indexes
		}

		@Override
		public View getChildView(int groupPosition, int childPosition,
		                         boolean isLastChild, View convertView, ViewGroup parent) {
			LayoutInflater inflater = UiUtilities.getInflater(getContext(), nightMode);
			convertView = inflater.inflate(R.layout.list_item_triple_row_icon_and_menu, parent, false);
			ImageView secondaryIcon = convertView.findViewById(R.id.secondary_icon);
			UiUtilities.rotateImageByLayoutDirection(secondaryIcon);
			LiveMapsViewHolder viewHolder = new LiveMapsViewHolder(convertView);
			convertView.setTag(viewHolder);
			viewHolder.bindLocalItem(getFileNameWithoutRoadSuffix(getChild(groupPosition, childPosition)));
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

			TextViewEx titleView = view.findViewById(R.id.title);
			titleView.setText(getGroup(groupPosition));

			TextViewEx countView = view.findViewById(R.id.description);
			AndroidUtils.setTextSecondaryColor(app, countView, nightMode);

			return view;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			return localItems.size();
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
			statusIcon = view.findViewById(R.id.icon);
			title = view.findViewById(R.id.title);
			subTitle = view.findViewById(R.id.sub_title);
			description = view.findViewById(R.id.description);
			compoundButton = view.findViewById(R.id.compound_button);
		}

		public void bindLocalItem(@NonNull String item) {
			boolean liveUpdateOn = settings.IS_LIVE_UPDATES_ON.get();
			CommonPreference<Boolean> localUpdateOn = preferenceForLocalIndex(item, settings);
//			IncrementalChangesManager changesManager = app.getResourceManager().getChangesManager();
			compoundButton.setChecked(localUpdateOn.get());
			if (!liveUpdateOn && localUpdateOn.get()) {
				UiUtilities.setupCompoundButton(nightMode, ContextCompat.getColor(app, ColorUtilities.getTertiaryTextColorId(nightMode)), compoundButton);
			} else {
				UiUtilities.setupCompoundButton(compoundButton, nightMode, CompoundButtonType.GLOBAL);
			}

			title.setText(getNameToDisplay(item, app));

			AndroidUiHelper.updateVisibility(subTitle, localUpdateOn.get());
			if (localUpdateOn.get()) {
				int frequencyId = preferenceUpdateFrequency(item, settings).get();
				UpdateFrequency frequency = UpdateFrequency.values()[frequencyId];
				String subTitleText = getString(frequency.titleId);
				subTitle.setText(subTitleText);
				subTitle.setTextColor(ContextCompat.getColor(app, liveUpdateOn
						? ColorUtilities.getActiveColorId(nightMode) : ColorUtilities.getSecondaryTextColorId(nightMode)));
				subTitle.setTypeface(FontCache.getMediumFont());
			}

			Drawable statusDrawable = AppCompatResources.getDrawable(app, R.drawable.ic_map);
			int resColorId = !localUpdateOn.get() ? getSecondaryIconColorId(nightMode) :
					!liveUpdateOn ? ColorUtilities.getDefaultIconColorId(nightMode) : getOsmandIconColorId(nightMode);
			int statusColor = ContextCompat.getColor(app, resColorId);
			if (statusDrawable != null) {
				DrawableCompat.setTint(statusDrawable, statusColor);
			}
			statusIcon.setImageDrawable(statusDrawable);

			description.setText(getFormattedLastSuccessfulCheck(item));

			if (InAppPurchaseUtils.isLiveUpdatesAvailable(app)) {
				compoundButton.setEnabled(liveUpdateOn);
				compoundButton.setOnCheckedChangeListener((buttonView, isChecked) -> onUpdateLocalIndex(item, isChecked, LiveUpdatesFragment.this::runSort));
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
			app = fragment.app;
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

	@NonNull
	private String getFormattedLastSuccessfulCheck(@NonNull String fileName) {
		long lastUpdate = preferenceLastSuccessfulUpdateCheck(fileName, settings).get();
		String lastUpdateString = formatShortDateTime(app, lastUpdate);
		return app.getString(R.string.updated, lastUpdateString);
	}

	@Override
	public void processFinish() {
		adapter.notifyDataSetChanged();
	}

	@Override
	public List<LocalItem> getMapsToUpdate() {
		return getMapsToUpdate(adapter.localItems, settings);
	}

	@Override
	public boolean onUpdateLocalIndex(String fileName, boolean newValue, Runnable callback) {
		int frequencyId = preferenceUpdateFrequency(fileName, settings).get();
		int timeOfDateToUpdateId = preferenceTimeOfDayToUpdate(fileName, settings).get();
		AlarmManager alarmManager = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
		PendingIntent alarmIntent = getPendingIntent(app, fileName);

		CommonPreference<Boolean> liveUpdatePreference = preferenceForLocalIndex(fileName, settings);
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
	public void forceUpdateLocal(String fileName, boolean userRequested, Runnable callback) {
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

	@NonNull
	public static String getFileNameWithoutRoadSuffix(@NonNull LocalItem item) {
		String fileName = item.getFileName();
		if (fileName.endsWith(BINARY_ROAD_MAP_INDEX_EXT)) {
			return fileName.substring(0, fileName.lastIndexOf(BINARY_ROAD_MAP_INDEX_EXT)) + BINARY_MAP_INDEX_EXT;
		}
		return fileName;
	}

	public static String getSupportRegionName(OsmandApplication app, InAppPurchaseHelper purchaseHelper) {
		OsmandSettings settings = app.getSettings();
		String countryName = settings.BILLING_USER_COUNTRY.get();
		if (purchaseHelper != null) {
			List<InAppSubscription> subscriptions = purchaseHelper.getSubscriptions().getVisibleSubscriptions();
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
}
