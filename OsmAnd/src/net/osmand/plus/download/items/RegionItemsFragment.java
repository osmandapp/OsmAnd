package net.osmand.plus.download.items;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.WorldRegion;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.srtmplugin.SRTMPlugin;

import org.apache.commons.logging.Log;

import java.text.MessageFormat;
import java.util.Locale;

public class RegionItemsFragment extends Fragment {
	public static final String TAG = "RegionItemsFragment";
	private static final Log LOG = PlatformUtil.getLog(RegionItemsFragment.class);
	private static final MessageFormat formatGb = new MessageFormat("{0, number,<b>#.##</b>} GB", Locale.US);

	private ItemsListBuilder builder;
	private RegionsAdapter regionsAdapter;
	private MapsAdapter mapsAdapter;
	private VoicePromtsAdapter voicePromtsAdapter;

	private TextView mapsTextView;
	private ListView mapsListView;
	private TextView regionsTextView;
	private ListView regionsListView;
	private TextView voicePromtsTextView;
	private ListView voicePromtsListView;

	private static final String REGION_KEY = "world_region_key";
	private WorldRegion region;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		WorldRegion region = null;
		if (savedInstanceState != null) {
			Object regionObj = savedInstanceState.getSerializable(REGION_KEY);
			if (regionObj != null) {
				region = (WorldRegion)regionObj;
			}
		}
		if (region == null) {
			Object regionObj = getArguments().getSerializable(REGION_KEY);
			if (regionObj != null) {
				region = (WorldRegion)regionObj;
			}
		}

		this.region = region;

		View view = inflater.inflate(R.layout.local_items_fragment, container, false);

		builder = new ItemsListBuilder(getMyApplication(), this.region);

		mapsTextView = (TextView) view.findViewById(R.id.list_world_regions_title);
		mapsTextView.setText("Region maps".toUpperCase());
		mapsListView = (ListView) view.findViewById(R.id.list_world_regions);
		mapsAdapter = new MapsAdapter(getActivity());
		mapsListView.setAdapter(mapsAdapter);

		regionsTextView = (TextView) view.findViewById(R.id.list_world_maps_title);
		regionsTextView.setText("Additional maps".toUpperCase());
		regionsListView = (ListView) view.findViewById(R.id.list_world_maps);
		regionsAdapter = new RegionsAdapter(getActivity());
		regionsListView.setAdapter(regionsAdapter);
		regionsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Object obj = regionsAdapter.getItem(position);
				if (obj instanceof WorldRegion) {
					WorldRegion region = (WorldRegion) obj;
					((RegionDialogFragment) getParentFragment())
							.onRegionSelected(region);
				}
			}
		});

		voicePromtsTextView = (TextView) view.findViewById(R.id.list_voice_promts_title);
		voicePromtsTextView.setText("Voice promts".toUpperCase());
		voicePromtsListView = (ListView) view.findViewById(R.id.list_voice_promts);
		voicePromtsAdapter = new VoicePromtsAdapter(getActivity(), getMyApplication());
		voicePromtsListView.setAdapter(voicePromtsAdapter);

		if (builder.build()) {
			fillMapsAdapter();
			fillRegionsAdapter();
			fillVoicePromtsAdapter();
		}

		return view;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putSerializable(REGION_KEY, region);
		super.onSaveInstanceState(outState);
	}

	public OsmandApplication getMyApplication() {
		return (OsmandApplication)getActivity().getApplication();
	}

	private void fillRegionsAdapter() {
		if (regionsAdapter != null) {
			regionsAdapter.clear();
			regionsAdapter.addAll(builder.getAllResourceItems());
			updateVisibility(regionsAdapter, regionsTextView, regionsListView);
		}
	}

	private void fillMapsAdapter() {
		if (mapsAdapter != null) {
			mapsAdapter.clear();
			mapsAdapter.addAll(builder.getRegionMapItems());
			updateVisibility(mapsAdapter, mapsTextView, mapsListView);
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
		}
	}

	private DownloadActivity getDownloadActivity() {
		return (DownloadActivity) getActivity();
	}

	public static RegionItemsFragment createInstance(WorldRegion region) {
		Bundle bundle = new Bundle();
		bundle.putSerializable(REGION_KEY, region);
		RegionItemsFragment fragment = new RegionItemsFragment();
		fragment.setArguments(bundle);
		return fragment;
	}

	private static class MapsAdapter extends ArrayAdapter<ItemsListBuilder.ResourceItem> {
		private boolean srtmDisabled;

		public MapsAdapter(Context context) {
			super(context, R.layout.simple_list_menu_item);
			srtmDisabled = OsmandPlugin.getEnabledPlugin(SRTMPlugin.class) == null;
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
			viewHolder.setSrtmDisabled(srtmDisabled);

			ItemsListBuilder.ResourceItem item = getItem(position);
			viewHolder.bindIndexItem(item.getIndexItem(), (DownloadActivity) getContext(), true, false);
			return convertView;
		}
	}

	private static class RegionsAdapter extends ArrayAdapter {
		private boolean srtmDisabled;

		public RegionsAdapter(Context context) {
			super(context, R.layout.two_line_with_images_list_item);
			srtmDisabled = OsmandPlugin.getEnabledPlugin(SRTMPlugin.class) == null;
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
			viewHolder.setSrtmDisabled(srtmDisabled);

			Object item = getItem(position);
			if (item instanceof WorldRegion) {
				viewHolder.bindRegion((WorldRegion) item, (DownloadActivity) getContext());
			} else if (item instanceof ItemsListBuilder.ResourceItem) {
				viewHolder.bindIndexItem(((ItemsListBuilder.ResourceItem) item).getIndexItem(),
						(DownloadActivity) getContext(), false, true);
			} else {
				throw new IllegalArgumentException("Item must be of type WorldRegion or " +
						"IndexItem but is of type:" + item.getClass());
			}
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
			RegionItemViewHolder viewHolder;
			if (convertView == null) {
				convertView = LayoutInflater.from(parent.getContext())
						.inflate(R.layout.simple_list_menu_item, parent, false);
				viewHolder = new RegionItemViewHolder();
				viewHolder.textView = (TextView) convertView.findViewById(R.id.title);
				convertView.setTag(viewHolder);
			} else {
				viewHolder = (RegionItemViewHolder) convertView.getTag();
			}
			Drawable iconLeft = osmandApplication.getIconsCache()
					.getContentIcon(R.drawable.ic_world_globe_dark);
			viewHolder.textView.setCompoundDrawablesWithIntrinsicBounds(iconLeft, null, null, null);
			viewHolder.textView.setText(getItem(position).toString());
			return convertView;
		}
	}

	private static class RegionItemViewHolder {
		TextView textView;
	}
}
