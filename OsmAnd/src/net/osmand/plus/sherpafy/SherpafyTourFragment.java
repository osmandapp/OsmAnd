package net.osmand.plus.sherpafy;

import java.util.ArrayList;
import java.util.List;

import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.OsmAndListFragment;
import net.osmand.plus.activities.actions.ShareDialog;
import net.osmand.plus.sherpafy.TourInformation.StageInformation;
import net.osmand.util.Algorithms;
import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ListView;
import android.widget.TextView;


public class SherpafyTourFragment extends OsmAndListFragment {
	private static final int SHARE_ID = 6;
	private static final int START = 7;
	OsmandApplication app;
	private SherpafyCustomization customization;
	private TourInformation tour;
	private StageAdapter stageAdapter;
	private ImageView imageView;

	public SherpafyTourFragment() {
	}

	private enum StageItemType {
		OVERVIEW, INSTRUCTIONS, GALLERY, STAGE, TEXT
	}

	private static class StageItem {

		boolean stage;
		String txt;
		String header;
		Object type;

		public StageItem(Object type, String header, String txt, boolean stage) {
			this.type = type;
			this.txt = txt;
			this.stage = stage;
			this.header = header;
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		app = (OsmandApplication) getActivity().getApplication();
		customization = (SherpafyCustomization) app.getAppCustomization();
		String id = getArguments().getString("TOUR");
		for (TourInformation ti : customization.getTourInformations()) {
			if (ti.getId().equals(id)) {
				tour = ti;
				break;
			}
		}
		setHasOptionsMenu(true);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		if(tour != null) {
			((ActionBarActivity) getActivity()).getSupportActionBar().setTitle(tour.getName());
		}
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		if (position > 0) {
			StageItem si = (StageItem) getListAdapter().getItem(position - 1);
			if (si.type instanceof StageInformation) {
				((TourViewActivity) getActivity()).selectMenu(si.type);
			} else {
				if (si.type == StageItemType.GALLERY) {
					// ((TourViewActivity) getSherlockActivity()).showGallery(tour);
				} else if (si.type == StageItemType.OVERVIEW) {
					((TourViewActivity) getActivity()).showDetailedOverview(si.header, tour.getFulldescription());
				} else if (si.type == StageItemType.INSTRUCTIONS) {
					((TourViewActivity) getActivity()).showDetailedInstructions(si.header, tour.getInstructions());
				}
			}
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		// createMenuItem(menu, ACTION_GO_TO_MAP, R.string.start_tour, 0, 0,/* R.drawable.ic_action_marker_light, */
		// MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		if (tour != null) {
			boolean current = customization.getSelectedTour() == tour;
			OnMenuItemClickListener oic = new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					return onOptionsItemSelected(item);
				}
			};
			((TourViewActivity) getActivity()).createMenuItem(menu, START,
					current ? R.string.continue_tour : R.string.start_tour , 
					0, 
					MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT, oic);
			((TourViewActivity) getActivity()).createMenuItem(menu, SHARE_ID, R.string.shared_string_share,
					R.drawable.ic_action_gshare_dark,
					MenuItem.SHOW_AS_ACTION_IF_ROOM, oic);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == SHARE_ID) {
			ShareDialog sd = new ShareDialog(getActivity());
			if (this.tour.getHomeUrl().equals("")) {
				sd.shareURLOrText(null, this.tour.getName(), null);
			} else {
				sd.shareURLOrText(this.tour.getHomeUrl(), this.tour.getName() + " " + this.tour.getHomeUrl(), null);
			}
			sd.showDialog();
			return true;
		} else if(item.getItemId() == START) {
			((TourViewActivity) getActivity()).startTour(tour);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = super.onCreateView(inflater, container, savedInstanceState);
		imageView = new ImageView(getActivity());
		imageView.setLayoutParams(new AbsListView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		imageView.setScaleType(ScaleType.CENTER_CROP);
		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		List<StageItem> items = new ArrayList<SherpafyTourFragment.StageItem>();
		items.add(new StageItem(StageItemType.TEXT, "", getString(R.string.sherpafy_tour_info_txt), false));
		items.add(new StageItem(StageItemType.OVERVIEW, getString(R.string.sherpafy_overview),
				getString(R.string.sherpafy_overview_desr), false));
		items.add(new StageItem(StageItemType.INSTRUCTIONS, getString(R.string.sherpafy_instructions),
				getString(R.string.sherpafy_instructions_desr), false));
//		items.add(new StageItem(StageItemType.GALLERY, getString(R.string.sherpafy_gallery),
//				getString(R.string.sherpafy_gallery_descr), false));
		items.add(new StageItem(StageItemType.TEXT, "", getString(R.string.sherpafy_stages_txt), true));
		if (tour != null) {
			for (StageInformation si : tour.getStageInformation()) {
				StageItem it = new StageItem(si, si.getName(), si.getShortDescription(), true);
				items.add(it);
			}
			stageAdapter = new StageAdapter(items);
			imageView.setImageBitmap(tour.getImageBitmap());
			if (imageView != null) {
				getListView().addHeaderView(imageView);
			}
			setListAdapter(stageAdapter);
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		setListAdapter(null);
	}

	class StageAdapter extends ArrayAdapter<StageItem> {

		public StageAdapter(List<StageItem> list) {
			super(getActivity(), R.layout.sherpafy_stage_list_item, list);
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				LayoutInflater inflater = getActivity().getLayoutInflater();
				row = inflater.inflate(R.layout.sherpafy_stage_list_item, parent, false);
			}
			StageItem ti = getItem(position);
			TextView header = (TextView) row.findViewById(R.id.HeaderText);
			ImageView img = (ImageView) row.findViewById(R.id.Icon);
			TextView text = (TextView) row.findViewById(R.id.Text);
			TextView addtext = (TextView) row.findViewById(R.id.AdditionalText);

			if (ti.type instanceof StageInformation) {
				double d = ((StageInformation) ti.type).getDistance();
				if (d > 0) {
					addtext.setText(OsmAndFormatter.getFormattedDistance((float) d, getMyApplication()));
				} else {
					addtext.setText("");
				}
			} else {
				addtext.setText("");
			}
			if (Algorithms.isEmpty(ti.header)) {
				header.setVisibility(View.GONE);
			} else {
				header.setVisibility(View.VISIBLE);
				header.setText(ti.header);
			}
			if (Algorithms.isEmpty(ti.txt)) {
				text.setVisibility(View.GONE);
			} else {
				text.setVisibility(View.VISIBLE);
				text.setText(ti.txt);
				if (ti.type == StageItemType.TEXT) {
					text.setTextColor(ti.stage ? StageImageDrawable.STAGE_COLOR : StageImageDrawable.INFO_COLOR);
				} else {
					text.setTextColor(StageImageDrawable.MENU_COLOR);
				}
			}
			if (ti.type == StageItemType.TEXT) {
				img.setVisibility(View.GONE);
				img.setImageDrawable(null);
			} else if (ti.type instanceof StageInformation) {
				StageInformation si = (StageInformation) ti.type;
				img.setVisibility(View.VISIBLE);
				img.setImageDrawable(new StageImageDrawable(getActivity(), StageImageDrawable.STAGE_COLOR, (si
						.getOrder() + 1) + "", 0));
			} else {
				img.setVisibility(View.VISIBLE);
				img.setImageDrawable(new StageImageDrawable(getActivity(), StageImageDrawable.INFO_COLOR, ti.header
						.substring(0, 1) + "", 0));
			}
			return row;
		}
	}

}