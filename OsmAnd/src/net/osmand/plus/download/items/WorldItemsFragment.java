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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.WorldRegion;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.newimplementation.DownloadsUiHelper;

import org.apache.commons.logging.Log;

public class WorldItemsFragment extends Fragment {
	public static final String TAG = "WorldItemsFragment";
	private static final Log LOG = PlatformUtil.getLog(WorldItemsFragment.class);

	public static final int RELOAD_ID = 0;

	private ItemsListBuilder builder;
	private WorldRegionsAdapter worldRegionsAdapter;
	private WorldMapsAdapter worldMapsAdapter;
	private VoicePromtsAdapter voicePromtsAdapter;

	private TextView worldRegionsTextView;
	private ListView worldRegionsListView;
	private TextView worldMapsTextView;
	private ListView worldMapsListView;
	private TextView voicePromtsTextView;
	private ListView voicePromtsListView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.download_index_fragment, container, false);

		builder = new ItemsListBuilder(getMyApplication(), getMyApplication().getWorldRegion());

		worldRegionsTextView = (TextView) view.findViewById(R.id.list_world_regions_title);
		worldRegionsTextView.setText("World regions".toUpperCase());
		worldRegionsListView = (ListView) view.findViewById(R.id.list_world_regions);
		worldRegionsAdapter = new WorldRegionsAdapter(getActivity(), getMyApplication());
		worldRegionsListView.setAdapter(worldRegionsAdapter);
		worldRegionsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				WorldRegion region = worldRegionsAdapter.getItem(position);
				DownloadsUiHelper.showDialog(getActivity(), RegionDialogFragment.createInstance(region));
			}
		});

		worldMapsTextView = (TextView) view.findViewById(R.id.list_world_maps_title);
		worldMapsTextView.setText("World maps".toUpperCase());
		worldMapsListView = (ListView) view.findViewById(R.id.list_world_maps);
		worldMapsAdapter = new WorldMapsAdapter(getActivity());
		worldMapsListView.setAdapter(worldMapsAdapter);

		voicePromtsTextView = (TextView) view.findViewById(R.id.list_voice_promts_title);
		voicePromtsTextView.setText("Voice promts".toUpperCase());
		voicePromtsListView = (ListView) view.findViewById(R.id.list_voice_promts);
		voicePromtsAdapter = new VoicePromtsAdapter(getActivity(), getMyApplication());
		voicePromtsListView.setAdapter(voicePromtsAdapter);

		onCategorizationFinished();

		DownloadsUiHelper.initFreeVersionBanner(view,
				getMyApplication(), getResources());

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
			updateVisibility(worldRegionsAdapter, worldRegionsTextView, worldRegionsListView);
		}
	}

	private void fillWorldMapsAdapter() {
		if (worldMapsAdapter != null) {
			worldMapsAdapter.clear();
			worldMapsAdapter.addAll(builder.getRegionMapItems());
			updateVisibility(worldMapsAdapter, worldMapsTextView, worldMapsListView);
		}
	}

	private void fillVoicePromtsAdapter() {
		if (voicePromtsAdapter != null) {
			voicePromtsAdapter.clear();
			//voicePromtsAdapter.addAll(cats);
			updateVisibility(voicePromtsAdapter, voicePromtsTextView, voicePromtsListView);
		}
	}

	private void updateVisibility(ArrayAdapter adapter, TextView textView, ListView listView) {
		if (adapter.isEmpty()) {
			textView.setVisibility(View.GONE);
			listView.setVisibility(View.GONE);
		} else {
			textView.setVisibility(View.VISIBLE);
			listView.setVisibility(View.VISIBLE);
			setListViewHeightBasedOnChildren(listView);
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
			fillWorldMapsAdapter();
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

	private static class WorldMapsAdapter extends ArrayAdapter<ItemsListBuilder.ResourceItem> {

		public WorldMapsAdapter(Context context) {
			super(context, R.layout.simple_list_menu_item);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ItemViewHolder viewHolder;
			if (convertView == null) {
				convertView = LayoutInflater.from(parent.getContext())
						.inflate(R.layout.two_line_with_images_list_item, parent, false);
				viewHolder = new ItemViewHolder(convertView);
				convertView.setTag(viewHolder);
			} else {
				viewHolder = (ItemViewHolder) convertView.getTag();
			}
			ItemsListBuilder.ResourceItem item = getItem(position);
			viewHolder.bindIndexItem(item.getIndexItem(), (DownloadActivity) getContext(), false, false);
			return convertView;
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
