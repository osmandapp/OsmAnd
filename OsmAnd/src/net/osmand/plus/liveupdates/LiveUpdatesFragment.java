package net.osmand.plus.liveupdates;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.widget.SwitchCompat;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.LocalIndexHelper;
import net.osmand.plus.activities.LocalIndexInfo;
import net.osmand.plus.activities.OsmandBaseExpandableListAdapter;
import net.osmand.plus.activities.OsmandInAppPurchaseActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.chooseplan.ChoosePlanDialogFragment;
import net.osmand.plus.download.ui.AbstractLoadLocalIndexTask;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseListener;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseTaskType;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscription;
import net.osmand.plus.resources.IncrementalChangesManager;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.osmand.plus.liveupdates.LiveUpdatesHelper.DEFAULT_LAST_CHECK;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.TimeOfDay;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.UpdateFrequency;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.formatDateTime;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.getNameToDisplay;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.getPendingIntent;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceLastCheck;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceLiveUpdatesOn;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceTimeOfDayToUpdate;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceUpdateFrequency;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.runLiveUpdate;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.setAlarmForPendingIntent;

public class LiveUpdatesFragment extends BaseOsmAndFragment implements InAppPurchaseListener {
	public static final int TITLE = R.string.live_updates;
	private static final int SUBSCRIPTION_SETTINGS = 5;
	public static final Comparator<LocalIndexInfo> LOCAL_INDEX_INFO_COMPARATOR = new Comparator<LocalIndexInfo>() {
		@Override
		public int compare(LocalIndexInfo lhs, LocalIndexInfo rhs) {
			return lhs.getName().compareTo(rhs.getName());
		}
	};
	private View subscriptionHeader;
	private ExpandableListView listView;
	private LocalIndexesAdapter adapter;
	private AsyncTask<Void, LocalIndexInfo, List<LocalIndexInfo>> loadLocalIndexesTask;
	private boolean showSettingsOnly;

	private ProgressBar progressBar;
	private boolean processing;

	@Nullable
	public InAppPurchaseHelper getInAppPurchaseHelper() {
		Activity activity = getActivity();
		if (activity instanceof OsmandInAppPurchaseActivity) {
			return ((OsmandInAppPurchaseActivity) activity).getPurchaseHelper();
		} else {
			return null;
		}
	}

	private boolean isProcessing() {
		return processing;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		if (getActivity() instanceof OsmLiveActivity) {
			showSettingsOnly = ((OsmLiveActivity) getActivity()).isShowSettingOnly();
		}
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_live_updates, container, false);
		listView = (ExpandableListView) view.findViewById(android.R.id.list);

		final OsmandApplication app = getMyApplication();
		boolean nightMode = !app.getSettings().isLightContent();
		final SwipeRefreshLayout swipeRefresh = view.findViewById(R.id.swipe_refresh);
		int swipeColor = ContextCompat.getColor(app, nightMode ? R.color.osmand_orange_dark : R.color.osmand_orange);
		swipeRefresh.setColorSchemeColors(swipeColor);
		swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				adapter.showUpdateDialog();
				swipeRefresh.setRefreshing(false);
			}
		});
		
		View bottomShadowView = inflater.inflate(R.layout.card_bottom_divider, listView, false);
		if (!showSettingsOnly) {
			listView.addFooterView(bottomShadowView);
		}
		adapter = new LocalIndexesAdapter(this);
		listView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
			@Override
			public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
				if (!processing && InAppPurchaseHelper.isSubscribedToLiveUpdates(getMyApplication())) {
					final FragmentManager fragmentManager = getChildFragmentManager();
					LiveUpdatesSettingsDialogFragment
							.createInstance(adapter.getChild(groupPosition, childPosition).getFileName())
							.show(fragmentManager, "settings");
					return true;
				} else {
					return false;
				}
			}
		});

		progressBar = (ProgressBar) view.findViewById(R.id.progress);

		if (!Version.isDeveloperVersion(getMyApplication())) {
			subscriptionHeader = inflater.inflate(R.layout.live_updates_header, listView, false);
			updateSubscriptionHeader();
			listView.addHeaderView(subscriptionHeader, "subscriptionHeader", false);
		}
		listView.setAdapter(adapter);

		if (!showSettingsOnly) {
			loadLocalIndexesTask = new LoadLocalIndexTask(adapter, this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
		return view;
	}

	public void updateSubscriptionHeader() {
		if (getActivity() instanceof OsmLiveActivity && subscriptionHeader != null) {
			View subscriptionBanner = subscriptionHeader.findViewById(R.id.subscription_banner);
			View subscriptionInfo = subscriptionHeader.findViewById(R.id.subscription_info);
			if (InAppPurchaseHelper.isSubscribedToLiveUpdates(getMyApplication())) {
				ImageView statusIcon = (ImageView) subscriptionHeader.findViewById(R.id.statusIcon);
				TextView statusTextView = (TextView) subscriptionHeader.findViewById(R.id.statusTextView);
				TextView regionNameHeaderTextView = (TextView) subscriptionHeader.findViewById(R.id.regionHeaderTextView);
				TextView regionNameTextView = (TextView) subscriptionHeader.findViewById(R.id.regionTextView);
				statusTextView.setText(getString(R.string.osm_live_active));
				statusIcon.setImageDrawable(getMyApplication().getUIUtilities().getThemedIcon(R.drawable.ic_action_done));

				regionNameHeaderTextView.setText(R.string.osm_live_support_region);
				String countryName = getSettings().BILLING_USER_COUNTRY.get();
				InAppPurchaseHelper purchaseHelper = getInAppPurchaseHelper();
				if (purchaseHelper != null) {
					InAppSubscription monthlyPurchased = purchaseHelper.getPurchasedMonthlyLiveUpdates();
					if (monthlyPurchased != null && monthlyPurchased.isDonationSupported()) {
						if (Algorithms.isEmpty(countryName)) {
							if (getSettings().BILLING_USER_COUNTRY_DOWNLOAD_NAME.get().equals(OsmandSettings.BILLING_USER_DONATION_NONE_PARAMETER)) {
								regionNameHeaderTextView.setText(R.string.default_buttons_support);
								countryName = getString(R.string.osmand_team);
							} else {
								countryName = getString(R.string.shared_string_world);
							}
						}
					} else {
						regionNameHeaderTextView.setText(R.string.default_buttons_support);
						countryName = getString(R.string.osmand_team);
					}
				} else {
					regionNameHeaderTextView.setText(R.string.default_buttons_support);
					countryName = getString(R.string.osmand_team);
				}
				regionNameTextView.setText(countryName);

				View subscriptionsButton = subscriptionHeader.findViewById(R.id.button_subscriptions);
				View settingsButtonContainer = subscriptionHeader.findViewById(R.id.button_settings_container);
				View settingsButton = subscriptionHeader.findViewById(R.id.button_settings);
				subscriptionsButton.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						ChoosePlanDialogFragment.showOsmLiveInstance(getActivity().getSupportFragmentManager());
					}
				});
				if (isDonationSupported()) {
					settingsButton.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							showDonationSettings();
						}
					});
					settingsButtonContainer.setVisibility(View.VISIBLE);
				} else {
					settingsButton.setOnClickListener(null);
					settingsButtonContainer.setVisibility(View.GONE);
				}

				subscriptionBanner.setVisibility(View.GONE);
				subscriptionInfo.setVisibility(View.VISIBLE);
			} else {
				Button readMoreBtn = (Button) subscriptionHeader.findViewById(R.id.read_more_button);
				readMoreBtn.setEnabled(!processing);
				readMoreBtn.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Uri uri = Uri.parse("https://osmand.net/osm_live");
						Intent goToOsmLive = new Intent(Intent.ACTION_VIEW, uri);
						startActivity(goToOsmLive);
					}
				});
				Button subscriptionButton = (Button) subscriptionHeader.findViewById(R.id.subscription_button);
				subscriptionButton.setEnabled(!processing);
				subscriptionButton.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						FragmentActivity activity = getActivity();
						if (activity != null) {
							ChoosePlanDialogFragment.showOsmLiveInstance(activity.getSupportFragmentManager());
						}
					}
				});

				subscriptionBanner.setVisibility(View.VISIBLE);
				subscriptionInfo.setVisibility(View.GONE);
			}
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		InAppPurchaseHelper purchaseHelper = getInAppPurchaseHelper();
		if (purchaseHelper != null) {
			if (purchaseHelper.getActiveTask() == InAppPurchaseTaskType.REQUEST_INVENTORY) {
				enableProgress();
			}
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (loadLocalIndexesTask != null) {
			loadLocalIndexesTask.cancel(true);
		}
	}

	public void notifyLiveUpdatesChanged() {
		if (getActivity() != null) {
			if (adapter != null && getMyApplication() != null) {
				adapter.notifyLiveUpdatesChanged();
			}
		}
	}

	private boolean isDonationSupported() {
		InAppPurchaseHelper purchaseHelper = getInAppPurchaseHelper();
		if (purchaseHelper != null) {
			InAppSubscription monthlyPurchased = purchaseHelper.getPurchasedMonthlyLiveUpdates();
			return monthlyPurchased != null && monthlyPurchased.isDonationSupported();
		}
		return false;
	}

	private void showDonationSettings() {
		SubscriptionFragment subscriptionFragment = new SubscriptionFragment();
		subscriptionFragment.show(getChildFragmentManager(), SubscriptionFragment.TAG);
	}

	protected class LocalIndexesAdapter extends OsmandBaseExpandableListAdapter {
		public static final int SHOULD_UPDATE_GROUP_POSITION = 0;
		public static final int SHOULD_NOT_UPDATE_GROUP_POSITION = 1;
		final ArrayList<LocalIndexInfo> dataShouldUpdate = new ArrayList<>();
		final ArrayList<LocalIndexInfo> dataShouldNotUpdate = new ArrayList<>();
		final LiveUpdatesFragment fragment;
		final Context ctx;

		public LocalIndexesAdapter(LiveUpdatesFragment fragment) {
			this.fragment = fragment;
			ctx = fragment.getActivity();
		}

		public void add(LocalIndexInfo info) {
			OsmandSettings.CommonPreference<Boolean> preference = preferenceLiveUpdatesOn(
					info.getFileName(), getSettings());
			if (preference.get()) {
				dataShouldUpdate.add(info);
			} else {
				dataShouldNotUpdate.add(info);
			}
		}

		public void notifyLiveUpdatesChanged() {
			Set<LocalIndexInfo> changedSet = new HashSet<>();
			for (LocalIndexInfo localIndexInfo : dataShouldUpdate) {
				OsmandSettings.CommonPreference<Boolean> preference =
						preferenceLiveUpdatesOn(localIndexInfo.getFileName(), getSettings());
				if (!preference.get()) {
					changedSet.add(localIndexInfo);
				}
			}
			dataShouldUpdate.removeAll(changedSet);
			dataShouldNotUpdate.addAll(changedSet);
			changedSet.clear();
			for (LocalIndexInfo localIndexInfo : dataShouldNotUpdate) {
				OsmandSettings.CommonPreference<Boolean> preference =
						preferenceLiveUpdatesOn(localIndexInfo.getFileName(), getSettings());
				if (preference.get()) {
					changedSet.add(localIndexInfo);
				}
			}
			dataShouldUpdate.addAll(changedSet);
			dataShouldNotUpdate.removeAll(changedSet);
			notifyDataSetChanged();
			expandAllGroups();
		}

		public void sort() {
			Collections.sort(dataShouldUpdate, LOCAL_INDEX_INFO_COMPARATOR);
			Collections.sort(dataShouldNotUpdate, LOCAL_INDEX_INFO_COMPARATOR);
		}

		@Override
		public LocalIndexInfo getChild(int groupPosition, int childPosition) {
			if (groupPosition == 0) {
				return dataShouldUpdate.get(childPosition);
			} else if (groupPosition == 1) {
				return dataShouldNotUpdate.get(childPosition);
			} else {
				throw new IllegalArgumentException("unexpected group position:" + groupPosition);
			}
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			// it would be unusable to have 10000 local indexes
			return groupPosition * 10000 + childPosition;
		}

		@Override
		public View getChildView(final int groupPosition, final int childPosition,
								 boolean isLastChild, View convertView, ViewGroup parent) {
			LocalFullMapsViewHolder viewHolder;
			if (convertView == null) {
				LayoutInflater inflater = LayoutInflater.from(ctx);
				convertView = inflater.inflate(R.layout.local_index_live_updates_list_item, parent, false);
				viewHolder = new LocalFullMapsViewHolder(convertView, fragment);
				convertView.setTag(viewHolder);
			} else {
				viewHolder = (LocalFullMapsViewHolder) convertView.getTag();
			}
			viewHolder.bindLocalIndexInfo(getChild(groupPosition, childPosition).getFileName(), isLastChild);
			return convertView;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
			View view = convertView;
			String group = getGroup(groupPosition);
			if (view == null) {
				LayoutInflater inflater = LayoutInflater.from(ctx);
				view = inflater.inflate(R.layout.list_group_title_with_switch, parent, false);
			}
			TextView nameView = ((TextView) view.findViewById(R.id.title));
			nameView.setText(group);

			view.setOnClickListener(null);

			final SwitchCompat liveUpdatesSwitch = (SwitchCompat) view.findViewById(R.id.toggle_item);
			View topShadowView = view.findViewById(R.id.bottomShadowView);
			if (groupPosition == SHOULD_UPDATE_GROUP_POSITION) {
				topShadowView.setVisibility(View.GONE);
				liveUpdatesSwitch.setVisibility(View.VISIBLE);
				OsmandApplication application = (OsmandApplication) ctx.getApplicationContext();
				final OsmandSettings settings = application.getSettings();
				liveUpdatesSwitch.setChecked(settings.IS_LIVE_UPDATES_ON.get());
				liveUpdatesSwitch.setEnabled(!processing);
				liveUpdatesSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						if (isChecked) {
							if (InAppPurchaseHelper.isSubscribedToLiveUpdates(getMyApplication())) {
								switchOnLiveUpdates(settings);
							} else {
								liveUpdatesSwitch.setChecked(false);
								getMyApplication().showToastMessage(getString(R.string.osm_live_ask_for_purchase));
							}
						} else {
							settings.IS_LIVE_UPDATES_ON.set(false);
							enableLiveUpdates(false);
						}
					}

					
				});
			} else {
				topShadowView.setVisibility(View.VISIBLE);
				liveUpdatesSwitch.setVisibility(View.GONE);
			}

			View divider = view.findViewById(R.id.divider);
			if (getChildrenCount(groupPosition) == 0) {
				divider.setVisibility(View.GONE);
			} else {
				divider.setVisibility(View.VISIBLE);
			}
			return view;
		}

		private void switchOnLiveUpdates(final OsmandSettings settings) {
			settings.IS_LIVE_UPDATES_ON.set(true);
			enableLiveUpdates(true);
			showUpdateDialog();
		}
		
		private void showUpdateDialog() {
			if(dataShouldUpdate.size() > 0) {
				if (dataShouldUpdate.size() == 1) {
					runLiveUpdate(getMyApplication(), dataShouldUpdate.get(0).getFileName(), false);
				} else {
					Builder bld = new AlertDialog.Builder(getActivity());
					bld.setMessage(R.string.update_all_maps_now);
					bld.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							for (LocalIndexInfo li : dataShouldUpdate) {
								runLiveUpdate(getMyApplication(), li.getFileName(), false);
							}
							notifyDataSetChanged();
						}
					});
					bld.setNegativeButton(R.string.shared_string_no, null);
					bld.show();
				}
			}
		}
		
		private void enableLiveUpdates(boolean enable) {
			AlarmManager alarmMgr = (AlarmManager) getActivity()
					.getSystemService(Context.ALARM_SERVICE);
			for (LocalIndexInfo li : dataShouldUpdate) {
				String fileName = li.getFileName();
				PendingIntent alarmIntent = getPendingIntent(getActivity(),
						fileName);
				if (enable) {
					final OsmandSettings.CommonPreference<Integer> updateFrequencyPreference =
							preferenceUpdateFrequency(fileName, getSettings());
					final OsmandSettings.CommonPreference<Integer> timeOfDayPreference =
							preferenceTimeOfDayToUpdate(fileName, getSettings());
					UpdateFrequency updateFrequency = UpdateFrequency.values()[updateFrequencyPreference.get()];
					TimeOfDay timeOfDayToUpdate = TimeOfDay.values()[timeOfDayPreference.get()];
					setAlarmForPendingIntent(alarmIntent, alarmMgr, updateFrequency, timeOfDayToUpdate);
				} else {
					alarmMgr.cancel(alarmIntent);
				}
			}
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			if (showSettingsOnly) {
				return 0;
			}else if (groupPosition == SHOULD_UPDATE_GROUP_POSITION) {
				return dataShouldUpdate.size();
			} else if (groupPosition == SHOULD_NOT_UPDATE_GROUP_POSITION) {
				return dataShouldNotUpdate.size();
			} else {
				throw new IllegalArgumentException("unexpected group position:" + groupPosition);
			}
		}

		@Override
		public String getGroup(int groupPosition) {
			if (groupPosition == SHOULD_UPDATE_GROUP_POSITION) {
				return getString(R.string.download_live_updates);
			} else if (groupPosition == SHOULD_NOT_UPDATE_GROUP_POSITION) {
				return getString(R.string.available_maps);
			} else {
				throw new IllegalArgumentException("unexpected group position:" + groupPosition);
			}
		}

		@Override
		public int getGroupCount() {
			if (showSettingsOnly) {
				return 0;
			} else {
				return dataShouldNotUpdate.size() == 0 ? 1 : 2;
			}
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

	private void expandAllGroups() {
		for (int i = 0; i < adapter.getGroupCount(); i++) {
			listView.expandGroup(i);
		}
	}

	private static class LocalFullMapsViewHolder {
		public static final int UPDATES_ENABLED_ITEM_HEIGHT = 72;
		public static final int UPDATES_DISABLED_ITEM_HEIGHT = 50;
		private final ImageView icon;
		private final TextView nameTextView;
		private final TextView subheaderTextView;
		private final TextView descriptionTextView;
		private final ImageButton options;
		private final LiveUpdatesFragment fragment;
		private final View view;
		private final int secondaryColor;
		private final View divider;

		private LocalFullMapsViewHolder(View view, LiveUpdatesFragment context) {
			icon = (ImageView) view.findViewById(R.id.icon);
			nameTextView = (TextView) view.findViewById(R.id.nameTextView);
			subheaderTextView = (TextView) view.findViewById(R.id.subheaderTextView);
			descriptionTextView = (TextView) view.findViewById(R.id.descriptionTextView);
			options = (ImageButton) view.findViewById(R.id.options);
			this.view = view;
			this.fragment = context;

			TypedValue typedValue = new TypedValue();
			Resources.Theme theme = context.getActivity().getTheme();
			theme.resolveAttribute(android.R.attr.textColorSecondary, typedValue, true);
			secondaryColor = typedValue.data;
			divider = view.findViewById(R.id.divider);
		}

		public void bindLocalIndexInfo(@NonNull final String item, boolean isLastChild) {
			OsmandApplication context = fragment.getMyActivity().getMyApplication();
			final OsmandSettings.CommonPreference<Boolean> shouldUpdatePreference =
					preferenceLiveUpdatesOn(item, fragment.getSettings());
			IncrementalChangesManager changesManager = context.getResourceManager().getChangesManager();

			nameTextView.setText(getNameToDisplay(item, fragment.getMyActivity()));
			AbsListView.LayoutParams layoutParams = (AbsListView.LayoutParams) view.getLayoutParams();
			if (shouldUpdatePreference.get()) {
				final Integer frequencyId = preferenceUpdateFrequency(item, fragment.getSettings()).get();
				final Integer timeOfDateToUpdateId = preferenceTimeOfDayToUpdate(item, fragment.getSettings()).get();
				final UpdateFrequency frequency = UpdateFrequency.values()[frequencyId];
				final TimeOfDay timeOfDay = TimeOfDay.values()[timeOfDateToUpdateId];
				subheaderTextView.setVisibility(View.VISIBLE);
				String subheaderText = fragment.getString(frequency.getLocalizedId());
				if (frequency != UpdateFrequency.HOURLY) {
					subheaderText += " • " + fragment.getString(timeOfDay.getLocalizedId());
				}
				subheaderTextView.setText(subheaderText);
				subheaderTextView.setTextColor(fragment.getActivity().getResources()
						.getColor(R.color.osmand_orange));
				icon.setImageDrawable(fragment.getIcon(R.drawable.ic_map, R.color.osmand_orange));
				options.setImageDrawable(getSecondaryColorPaintedIcon(R.drawable.ic_overflow_menu_white));
				layoutParams.height = (int) dpToPx(view.getContext(), UPDATES_ENABLED_ITEM_HEIGHT);
			} else {
				subheaderTextView.setVisibility(View.GONE);
				icon.setImageDrawable(getSecondaryColorPaintedIcon(R.drawable.ic_map));
				options.setImageDrawable(getSecondaryColorPaintedIcon(R.drawable.ic_action_plus));
				layoutParams.height = (int) dpToPx(view.getContext(), UPDATES_DISABLED_ITEM_HEIGHT);
			}
			view.setLayoutParams(layoutParams);

			final String fileNameWithoutExtension =
					Algorithms.getFileNameWithoutExtension(new File(item));
			final long timestamp = changesManager.getTimestamp(fileNameWithoutExtension);
			final long lastCheck = preferenceLastCheck(item, fragment.getSettings()).get();
			OsmandSettings.CommonPreference<Boolean> liveUpdateOn = preferenceLiveUpdatesOn(item, fragment.getSettings());
			if(liveUpdateOn.get() && lastCheck != DEFAULT_LAST_CHECK) {
				String lastCheckString = formatDateTime(fragment.getActivity(), lastCheck );
				descriptionTextView.setText(context.getString(R.string.last_update, lastCheckString));
			} else {
				String lastCheckString = formatDateTime(fragment.getActivity(), timestamp );
				descriptionTextView.setText(context.getString(R.string.last_map_change, lastCheckString));
			}

			if (!fragment.isProcessing() && InAppPurchaseHelper.isSubscribedToLiveUpdates(context)) {
				final View.OnClickListener clickListener = new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						final FragmentManager fragmentManager = fragment.getChildFragmentManager();
						LiveUpdatesSettingsDialogFragment.createInstance(item).show(fragmentManager, "settings");
					}
				};
				options.setOnClickListener(clickListener);
				options.setEnabled(true);
			} else {
				options.setEnabled(false);
			}

			if (isLastChild) {
				divider.setVisibility(View.GONE);
			} else {
				divider.setVisibility(View.VISIBLE);
			}
		}

		private Drawable getSecondaryColorPaintedIcon(@DrawableRes int drawable) {
			return fragment.getMyActivity().getMyApplication().getUIUtilities()
					.getPaintedIcon(drawable, secondaryColor);
		}
	}

	public static class LoadLocalIndexTask
			extends AsyncTask<Void, LocalIndexInfo, List<LocalIndexInfo>>
			implements AbstractLoadLocalIndexTask {

		//private List<LocalIndexInfo> result;
		private LocalIndexesAdapter adapter;
		private LiveUpdatesFragment fragment;
		private LocalIndexHelper helper;

		public LoadLocalIndexTask(LocalIndexesAdapter adapter,
								  LiveUpdatesFragment fragment) {
			this.adapter = adapter;
			this.fragment = fragment;
			helper = new LocalIndexHelper(fragment.getMyActivity().getMyApplication());
		}

		@Override
		protected List<LocalIndexInfo> doInBackground(Void... params) {
			return helper.getLocalFullMaps(this);
		}

		@Override
		public void loadFile(LocalIndexInfo... loaded) {
			publishProgress(loaded);
		}

		@Override
		protected void onProgressUpdate(LocalIndexInfo... values) {
			String fileNameL;
			for (LocalIndexInfo localIndexInfo : values) {
				fileNameL = localIndexInfo.getFileName().toLowerCase();
				if (localIndexInfo.getType() == LocalIndexHelper.LocalIndexType.MAP_DATA
						&& !fileNameL.contains("world") && !fileNameL.startsWith("depth_")) {
					adapter.add(localIndexInfo);
				}
			}
			adapter.notifyDataSetChanged();
			fragment.expandAllGroups();
		}

		@Override
		protected void onPostExecute(List<LocalIndexInfo> result) {
			//this.result = result;
			adapter.sort();
			adapter.notifyLiveUpdatesChanged();
			adapter.notifyDataSetInvalidated();
		}
	}

	private void enableProgress() {
		processing = true;
		progressBar.setVisibility(View.VISIBLE);
		updateSubscriptionHeader();
		adapter.notifyDataSetChanged();
	}

	private void disableProgress() {
		processing = false;
		progressBar.setVisibility(View.INVISIBLE);
		updateSubscriptionHeader();
		adapter.notifyDataSetChanged();
	}

	public static float dpToPx(final Context context, final float dp) {
		return dp * context.getResources().getDisplayMetrics().density;
	}

	@Override
	public void onError(InAppPurchaseTaskType taskType, String error) {
		disableProgress();

		OsmandInAppPurchaseActivity activity = getInAppPurchaseActivity();
		if (activity != null) {
			activity.fireInAppPurchaseErrorOnFragments(getChildFragmentManager(), taskType, error);
		}
	}

	@Override
	public void onGetItems() {
		if (!InAppPurchaseHelper.isSubscribedToLiveUpdates(getMyApplication())) {
			getSettings().IS_LIVE_UPDATES_ON.set(false);
			adapter.enableLiveUpdates(false);
		}
		disableProgress();

		OsmandInAppPurchaseActivity activity = getInAppPurchaseActivity();
		if (activity != null) {
			activity.fireInAppPurchaseGetItemsOnFragments(getChildFragmentManager());
		}
	}

	@Override
	public void onItemPurchased(String sku, boolean active) {
		InAppPurchaseHelper purchaseHelper = getInAppPurchaseHelper();
		if (purchaseHelper != null && purchaseHelper.getLiveUpdates().containsSku(sku)) {
			updateSubscriptionHeader();
		}

		OsmandInAppPurchaseActivity activity = getInAppPurchaseActivity();
		if (activity != null) {
			activity.fireInAppPurchaseItemPurchasedOnFragments(getChildFragmentManager(), sku, active);
		}
	}

	@Override
	public void showProgress(InAppPurchaseTaskType taskType) {
		enableProgress();

		OsmandInAppPurchaseActivity activity = getInAppPurchaseActivity();
		if (activity != null) {
			activity.fireInAppPurchaseShowProgressOnFragments(getChildFragmentManager(), taskType);
		}
	}

	@Override
	public void dismissProgress(InAppPurchaseTaskType taskType) {
		disableProgress();

		OsmandInAppPurchaseActivity activity = getInAppPurchaseActivity();
		if (activity != null) {
			activity.fireInAppPurchaseDismissProgressOnFragments(getChildFragmentManager(), taskType);
		}
	}
}
