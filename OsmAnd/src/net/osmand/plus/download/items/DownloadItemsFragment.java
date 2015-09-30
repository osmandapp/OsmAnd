package net.osmand.plus.download.items;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.WorldRegion;
import net.osmand.plus.download.DownloadActivity;

import org.apache.commons.logging.Log;

import java.text.MessageFormat;
import java.util.Locale;

public class DownloadItemsFragment extends Fragment {
	private static final Log LOG = PlatformUtil.getLog(DownloadItemsFragment.class);
	private static final MessageFormat formatGb = new MessageFormat("{0, number,<b>#.##</b>} GB", Locale.US);

	public static final int RELOAD_ID = 0;

	private ItemsListBuilder builder;
	private WorldRegionsAdapter worldRegionsAdapter;
	private WorldMapAdapter worldMapAdapter;
	private VoicePromtsAdapter voicePromtsAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.download_index_fragment, container, false);

		builder = new ItemsListBuilder(getMyApplication(), getMyApplication().getWorldRegion());
		boolean hasBuilt = builder.build();

		ListView worldRegionsListView = (ListView) view.findViewById(R.id.list_world_regions);
		worldRegionsAdapter = new WorldRegionsAdapter(getActivity(), getMyApplication());
		worldRegionsListView.setAdapter(worldRegionsAdapter);
		if (hasBuilt) {
			fillWorldRegionsAdapter();
		}

		ListView worldMapListView = (ListView) view.findViewById(R.id.list_world_map);
		worldMapAdapter = new WorldMapAdapter(getActivity(), getMyApplication());
		worldMapListView.setAdapter(worldMapAdapter);
		if (hasBuilt) {
			fillWorldMapAdapter();
		}

		ListView voicePromtsListView = (ListView) view.findViewById(R.id.list_voice_promts);
		voicePromtsAdapter = new VoicePromtsAdapter(getActivity(), getMyApplication());
		voicePromtsListView.setAdapter(voicePromtsAdapter);
		if (hasBuilt) {
			fillVoicePromtsAdapter();
		}

		return view;
	}

	public static void setListViewHeightBasedOnChildren(ListView listView) {
		ListAdapter listAdapter = listView.getAdapter();
		if (listAdapter == null) {
			// pre-condition
			return;
		}

		int totalHeight = 0;
		for (int i = 0; i < listAdapter.getCount(); i++) {
			View listItem = listAdapter.getView(i, null, listView);
			listItem.measure(0, 0);
			totalHeight += listItem.getMeasuredHeight();
		}

		ViewGroup.LayoutParams params = listView.getLayoutParams();
		params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
		listView.setLayoutParams(params);
		listView.requestLayout();
	}

	public OsmandApplication getMyApplication() {
		return (OsmandApplication)getActivity().getApplication();
	}

	private void fillWorldRegionsAdapter() {
		if (worldRegionsAdapter != null) {
			worldRegionsAdapter.clear();
			worldRegionsAdapter.addAll(builder.getRegionsFromAllItems());
			setListViewHeightBasedOnChildren((ListView) getView().findViewById(R.id.list_world_regions));
		}
	}

	private void fillWorldMapAdapter() {
		if (worldMapAdapter != null) {
			worldMapAdapter.clear();
			worldMapAdapter.addAll(builder.getRegionMapItems());
			setListViewHeightBasedOnChildren((ListView) getView().findViewById(R.id.list_world_map));
		}
	}

	private void fillVoicePromtsAdapter() {
		if (voicePromtsAdapter != null) {
			voicePromtsAdapter.clear();
			//voicePromtsAdapter.addAll(cats);
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		MenuItem item = menu.add(0, RELOAD_ID, 0, R.string.shared_string_refresh);
		item.setIcon(R.drawable.ic_action_refresh_dark);
		MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == RELOAD_ID) {
			// re-create the thread
			DownloadActivity.downloadListIndexThread.runReloadIndexFiles();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private DownloadActivity getDownloadActivity() {
		return (DownloadActivity) getActivity();
	}

	public void onCategorizationFinished() {
		if (builder.build()) {
			fillWorldRegionsAdapter();
			fillWorldMapAdapter();
			fillVoicePromtsAdapter();
		}
	}

	private static class WorldRegionsAdapter extends ArrayAdapter<WorldRegion> {
		private final OsmandApplication osmandApplication;

		public WorldRegionsAdapter(Context context, OsmandApplication osmandApplication) {
			super(context, R.layout.simple_list_menu_item);
			this.osmandApplication = osmandApplication;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder viewHolder;
			if (convertView == null) {
				convertView = LayoutInflater.from(parent.getContext())
						.inflate(R.layout.simple_list_menu_item, parent, false);
				viewHolder = new ViewHolder();
				viewHolder.textView = (TextView) convertView.findViewById(R.id.title);
				convertView.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder) convertView.getTag();
			}
			Drawable iconLeft = osmandApplication.getIconsCache()
					.getContentIcon(R.drawable.ic_world_globe_dark);
			viewHolder.textView.setCompoundDrawablesWithIntrinsicBounds(iconLeft, null, null, null);
			viewHolder.textView.setText(getItem(position).getName());
			return convertView;
		}

		private static class ViewHolder {
			TextView textView;
		}
	}

	private static class WorldMapAdapter extends ArrayAdapter<ItemsListBuilder.ResourceItem> {
		private final OsmandApplication osmandApplication;

		public WorldMapAdapter(Context context, OsmandApplication osmandApplication) {
			super(context, R.layout.simple_list_menu_item);
			this.osmandApplication = osmandApplication;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder viewHolder;
			if (convertView == null) {
				convertView = LayoutInflater.from(parent.getContext())
						.inflate(R.layout.simple_list_menu_item, parent, false);
				viewHolder = new ViewHolder();
				viewHolder.textView = (TextView) convertView.findViewById(R.id.title);
				convertView.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder) convertView.getTag();
			}
			Drawable iconLeft = osmandApplication.getIconsCache()
					.getContentIcon(R.drawable.ic_world_globe_dark);
			viewHolder.textView.setCompoundDrawablesWithIntrinsicBounds(iconLeft, null, null, null);
			viewHolder.textView.setText(getItem(position).getTitle());
			return convertView;
		}

		private static class ViewHolder {
			TextView textView;
		}
	}

	private static class VoicePromtsAdapter extends ArrayAdapter {
		private final OsmandApplication osmandApplication;

		public VoicePromtsAdapter(Context context, OsmandApplication osmandApplication) {
			super(context, R.layout.simple_list_menu_item);
			this.osmandApplication = osmandApplication;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder viewHolder;
			if (convertView == null) {
				convertView = LayoutInflater.from(parent.getContext())
						.inflate(R.layout.simple_list_menu_item, parent, false);
				viewHolder = new ViewHolder();
				viewHolder.textView = (TextView) convertView.findViewById(R.id.title);
				convertView.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder) convertView.getTag();
			}
			Drawable iconLeft = osmandApplication.getIconsCache()
					.getContentIcon(R.drawable.ic_world_globe_dark);
			viewHolder.textView.setCompoundDrawablesWithIntrinsicBounds(iconLeft, null, null, null);
			viewHolder.textView.setText(getItem(position).toString());
			return convertView;
		}

		private static class ViewHolder {
			TextView textView;
		}
	}

}
