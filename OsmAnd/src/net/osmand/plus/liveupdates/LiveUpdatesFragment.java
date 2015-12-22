package net.osmand.plus.liveupdates;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.SwitchCompat;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.LocalIndexHelper;
import net.osmand.plus.activities.LocalIndexInfo;
import net.osmand.plus.activities.OsmandBaseExpandableListAdapter;
import net.osmand.plus.download.AbstractDownloadActivity;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.ui.AbstractLoadLocalIndexTask;
import net.osmand.plus.resources.IncrementalChangesManager;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.osmand.plus.liveupdates.LiveUpdatesHelper.UpdateFrequency;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.formatDateTime;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.getNameToDisplay;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceLiveUpdatesOn;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceUpdateFrequency;

public class LiveUpdatesFragment extends Fragment {
	public static final String TITILE = "Live Updates";
	public static final Comparator<LocalIndexInfo> LOCAL_INDEX_INFO_COMPARATOR = new Comparator<LocalIndexInfo>() {
		@Override
		public int compare(LocalIndexInfo lhs, LocalIndexInfo rhs) {
			return lhs.getName().compareTo(rhs.getName());
		}
	};
	private ExpandableListView listView;
	private LocalIndexesAdapter adapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_live_updates, container, false);
		listView = (ExpandableListView) view.findViewById(android.R.id.list);
//		View header = inflater.inflate(R.layout.live_updates_header, listView, false);

		adapter = new LocalIndexesAdapter(this);
		listView.setAdapter(adapter);
		View topShadowView = inflater.inflate(R.layout.shadow_top, listView, false);
		listView.addHeaderView(topShadowView);
		View bottomShadowView = inflater.inflate(R.layout.shadow_bottom, listView, false);
		listView.addFooterView(bottomShadowView);
		new LoadLocalIndexTask(adapter, this).execute();
		return view;
	}

	private OsmandSettings getSettings() {
		return getMyActivity().getMyApplication().getSettings();
	}

	private AbstractDownloadActivity getMyActivity() {
		return (AbstractDownloadActivity) getActivity();
	}

	public void notifyLiveUpdatesChanged() {
		adapter.notifyLiveUpdatesChanged();
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
			OsmandSettings.CommonPreference<Boolean> preference = preferenceLiveUpdatesOn(info,
					getSettings());
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
						preferenceLiveUpdatesOn(localIndexInfo, getSettings());
				if (!preference.get()) {
					changedSet.add(localIndexInfo);
				}
			}
			dataShouldUpdate.removeAll(changedSet);
			dataShouldNotUpdate.addAll(changedSet);
			changedSet.clear();
			for (LocalIndexInfo localIndexInfo : dataShouldNotUpdate) {
				OsmandSettings.CommonPreference<Boolean> preference =
						preferenceLiveUpdatesOn(localIndexInfo, getSettings());
				if (preference.get()) {
					changedSet.add(localIndexInfo);
				}
			}
			dataShouldUpdate.addAll(changedSet);
			dataShouldNotUpdate.removeAll(changedSet);
			notifyDataSetChanged();
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
			viewHolder.bindLocalIndexInfo(getChild(groupPosition, childPosition));
			return convertView;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
			View v = convertView;
			String group = getGroup(groupPosition);
			if (v == null) {
				LayoutInflater inflater = LayoutInflater.from(ctx);
				v = inflater.inflate(R.layout.list_group_title_with_switch, parent, false);
			}
			TextView nameView = ((TextView) v.findViewById(R.id.section_name));
			nameView.setText(group);

			v.setOnClickListener(null);

			TypedValue typedValue = new TypedValue();
			Resources.Theme theme = ctx.getTheme();
			theme.resolveAttribute(R.attr.ctx_menu_info_view_bg, typedValue, true);
			v.setBackgroundColor(typedValue.data);

			SwitchCompat liveUpdatesSwitch = (SwitchCompat) v.findViewById(R.id.liveUpdatesSwitch);
			View topShadowView = v.findViewById(R.id.bottomShadowView);
			if (groupPosition == SHOULD_UPDATE_GROUP_POSITION) {
				topShadowView.setVisibility(View.GONE);
				liveUpdatesSwitch.setVisibility(View.VISIBLE);
				OsmandApplication application = (OsmandApplication) ctx.getApplicationContext();
				final OsmandSettings settings = application.getSettings();
				liveUpdatesSwitch.setChecked(settings.IS_LIVE_UPDATES_ON.get());
				liveUpdatesSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						settings.IS_LIVE_UPDATES_ON.set(isChecked);
						int liveUpdatesStateId = isChecked ? R.string.shared_string_on
								: R.string.shared_string_off;
					}
				});
			} else {
				topShadowView.setVisibility(View.VISIBLE);
				liveUpdatesSwitch.setVisibility(View.GONE);
			}
			return v;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			if (groupPosition == SHOULD_UPDATE_GROUP_POSITION) {
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
				return getString(R.string.live_updates);
			} else if (groupPosition == SHOULD_NOT_UPDATE_GROUP_POSITION) {
				return getString(R.string.available_maps);
			} else {
				throw new IllegalArgumentException("unexpected group position:" + groupPosition);
			}
		}

		@Override
		public int getGroupCount() {
			return 2;
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
		private final ImageView icon;
		private final TextView nameTextView;
		private final TextView subheaderTextView;
		private final TextView descriptionTextView;
		private final ImageButton options;
		private final LiveUpdatesFragment fragment;
		private final View view;
		private final int secondaryColor;

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
		}

		public void bindLocalIndexInfo(final LocalIndexInfo item) {
			OsmandApplication context = fragment.getMyActivity().getMyApplication();
			final OsmandSettings.CommonPreference<Boolean> shouldUpdatePreference =
					preferenceLiveUpdatesOn(item, fragment.getSettings());
			IncrementalChangesManager changesManager = context.getResourceManager().getChangesManager();
			final String fileNameWithoutExtension =
					Algorithms.getFileNameWithoutExtension(new File(item.getFileName()));

			nameTextView.setText(getNameToDisplay(item, fragment.getMyActivity()));
			if (shouldUpdatePreference.get()) {
				final Integer frequencyId = preferenceUpdateFrequency(item, fragment.getSettings()).get();
				final UpdateFrequency frequency = UpdateFrequency.values()[frequencyId];
				subheaderTextView.setText(frequency.toString());
				subheaderTextView.setTextColor(fragment.getActivity().getResources()
						.getColor(R.color.osmand_orange));
				icon.setImageDrawable(context.getIconsCache().getIcon(R.drawable.ic_map, R.color.osmand_orange));
				options.setImageDrawable(getSecondaryColorPaintedIcon(R.drawable.ic_overflow_menu_white));
			} else {
				String size;
				long updatesSize = changesManager.getUpdatesSize(fileNameWithoutExtension);
				updatesSize /= (1 << 10);
				if (updatesSize > 100) {
					size = DownloadActivity.formatMb.format(new Object[]{(float) updatesSize / (1 << 10)});
				} else {
					size = updatesSize + " KB";
				}
				subheaderTextView.setText(size);
				subheaderTextView.setTextColor(secondaryColor);
				icon.setImageDrawable(getSecondaryColorPaintedIcon(R.drawable.ic_map));
				options.setImageDrawable(getSecondaryColorPaintedIcon(R.drawable.ic_action_plus));
			}

			final long timestamp = changesManager.getTimestamp(fileNameWithoutExtension);
			String formattedDate = formatDateTime(fragment.getActivity(), timestamp);
			descriptionTextView.setText(context.getString(R.string.last_update, formattedDate));

			final View.OnClickListener clickListener = new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					final FragmentManager fragmentManager = fragment.getChildFragmentManager();
					LiveUpdatesSettingsDialogFragment.createInstance(item).show(fragmentManager, "settings");
				}
			};
			options.setOnClickListener(clickListener);
			view.setOnClickListener(clickListener);
		}

		private Drawable getSecondaryColorPaintedIcon(@DrawableRes int drawable) {
			return fragment.getMyActivity().getMyApplication().getIconsCache()
					.getPaintedContentIcon(drawable, secondaryColor);
		}
	}

	public static class LoadLocalIndexTask
			extends AsyncTask<Void, LocalIndexInfo, List<LocalIndexInfo>>
			implements AbstractLoadLocalIndexTask {

		private List<LocalIndexInfo> result;
		private LocalIndexesAdapter adapter;
		private LiveUpdatesFragment fragment;

		public LoadLocalIndexTask(LocalIndexesAdapter adapter,
								  LiveUpdatesFragment fragment) {
			this.adapter = adapter;
			this.fragment = fragment;
		}

		@Override
		protected List<LocalIndexInfo> doInBackground(Void... params) {
			LocalIndexHelper helper = new LocalIndexHelper(fragment.getMyActivity().getMyApplication());
			return helper.getLocalFullMaps(this);
		}

		@Override
		public void loadFile(LocalIndexInfo... loaded) {
			publishProgress(loaded);
		}

		@Override
		protected void onProgressUpdate(LocalIndexInfo... values) {
			for (LocalIndexInfo localIndexInfo : values) {
				if (localIndexInfo.getType() == LocalIndexHelper.LocalIndexType.MAP_DATA
						&& !(localIndexInfo.getFileName().toLowerCase().contains("world"))) {
					adapter.add(localIndexInfo);
				}
			}
			adapter.notifyDataSetChanged();
			fragment.expandAllGroups();
		}

		@Override
		protected void onPostExecute(List<LocalIndexInfo> result) {
			this.result = result;
			adapter.sort();
		}
	}
}
