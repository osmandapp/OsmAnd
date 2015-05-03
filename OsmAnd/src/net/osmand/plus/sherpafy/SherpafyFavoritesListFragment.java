package net.osmand.plus.sherpafy;

import java.util.List;

import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.OsmAndListFragment;
import net.osmand.plus.sherpafy.TourInformation.StageFavorite;
import net.osmand.plus.sherpafy.TourInformation.StageFavoriteGroup;
import net.osmand.plus.sherpafy.TourInformation.StageInformation;
import net.osmand.util.MapUtils;
import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ListView;
import android.widget.TextView;


public class SherpafyFavoritesListFragment extends OsmAndListFragment {
	
	OsmandApplication app;
	private SherpafyCustomization customization;
	private TourInformation tour;
	private StageInformation stage;
	private FavoriteAdapter favAdapter;
	private ImageView imageView;

	public SherpafyFavoritesListFragment() {
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
		int k = getArguments().getInt(SherpafyStageInfoFragment.STAGE_PARAM);
		if(tour != null && tour.getStageInformation().size() > k) {
			stage = tour.getStageInformation().get(k);
		}
		setHasOptionsMenu(true);
	}
	
	@Override
	public void onResume() {
		super.onResume();
//		if(tour != null) {
//			getSherlockActivity().getSupportActionBar().setTitle(tour.getName());
//		}
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Object item = getListAdapter().getItem(position);
		if (item instanceof StageFavorite) {
			((TourViewActivity) getActivity()).showFavoriteFragment(stage, (StageFavorite) item);
		}
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			((TourViewActivity) getActivity()).showSelectedItem();
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
		favAdapter = new FavoriteAdapter(stage.getFavorites());
		setListAdapter(favAdapter);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		setListAdapter(null);
	}

	class FavoriteAdapter extends ArrayAdapter<Object> {

		public FavoriteAdapter(List<Object> list) {
			super(getActivity(), R.layout.sherpafy_stage_list_item, list);
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				LayoutInflater inflater = getActivity().getLayoutInflater();
				row = inflater.inflate(R.layout.sherpafy_stage_list_item, parent, false);
			}
			Object ti = getItem(position);
			TextView header = (TextView) row.findViewById(R.id.HeaderText);
			ImageView img = (ImageView) row.findViewById(R.id.Icon);
			TextView text = (TextView) row.findViewById(R.id.Text);
			TextView addtext = (TextView) row.findViewById(R.id.AdditionalText);

			if (ti instanceof StageFavoriteGroup) {
				addtext.setText("");
				text.setTextColor(((StageFavoriteGroup)ti).getColor());
				text.setText(((StageFavoriteGroup)ti).getName());
				header.setVisibility(View.GONE);
				img.setVisibility(View.GONE);
				img.setImageDrawable(null);
			} else if(ti instanceof StageFavorite){
				StageFavorite sf = ((StageFavorite)ti);
				if(stage.startPoint != null && sf.location != null) {
					double d = MapUtils.getDistance(stage.startPoint, sf.location);
					addtext.setText(OsmAndFormatter.getFormattedDistance((float) d, getMyApplication()));
				} else {
					addtext.setText("");
				}
				header.setVisibility(View.VISIBLE);
				header.setText(sf.getName());
				text.setTextColor(StageImageDrawable.MENU_COLOR);
				text.setText(sf.getShortDescription());
				img.setVisibility(View.VISIBLE);
				img.setImageDrawable(new StageImageDrawable(getActivity(), sf.getGroup().getColor(), sf
						.getName().substring(0, 1), 0));

			}
			return row;
		}
	}


}