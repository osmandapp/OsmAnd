/**
 * 
 */
package net.osmand.plus.sherpafy;

import java.util.ArrayList;
import java.util.List;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.sherpafy.TourInformation.StageInformation;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;


public class TourStageFragment extends SherlockListFragment  {
	public static final int REQUEST_POI_EDIT = 55;
	private SherpafyCustomization appCtx;
	private boolean lightContent;
	
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		appCtx = (SherpafyCustomization) getApp().getAppCustomization();
		lightContent = getApp().getSettings().isLightContent();
		// ListActivity has a ListView, which you can get with:
		//ListView lv = getListView();
		// Then you can create a listener like so:
//		lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
//			@Override
//			public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id) {
//				TourInformation poi = ((TourAdapter) getListAdapter()).getItem(pos);
//				return false;
//			}
//		});
		if(appCtx.getSelectedTour() != null) {
			setListAdapter(new LocalAdapter(appCtx.getSelectedTour().getStageInformation()));
		} else {
			setListAdapter(new LocalAdapter(new ArrayList<TourInformation.StageInformation>()));
			setEmptyText(getString(R.string.no_tour_selected));
		}
	}
	
	public OsmandApplication getApp(){
		return (OsmandApplication) getSherlockActivity().getApplication();
	}

	@Override
	public void onListItemClick(ListView parent, View v, int position, long id) {
		final StageInformation model = ((LocalAdapter) getListAdapter()).getItem(position);
		// TODO
	}

	private class LocalAdapter extends ArrayAdapter<StageInformation> {
		LocalAdapter(List<StageInformation> list) {
			super(getSherlockActivity(), R.layout.tour_listitem, list);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				LayoutInflater inflater = getSherlockActivity().getLayoutInflater();
				row = inflater.inflate(R.layout.tour_listitem, parent, false);
			}
			TextView label = (TextView) row.findViewById(R.id.label);
			ImageView icon = (ImageView) row.findViewById(R.id.icon);
			ImageView check = (ImageView) row.findViewById(R.id.check_item);
			icon.setImageResource(!lightContent ? R.drawable.ic_action_fav_dark : R.drawable.ic_action_fav_light);
			check.setImageResource(!lightContent ? R.drawable.ic_action_ok_dark : R.drawable.ic_action_ok_light);
			final StageInformation model = getItem(position);
			if(model.getDescription().length() > 0) {
				label.setText(model.getName() +"\n" + model.getDescription());
			} else {
				label.setText(model.getName());
			}
			return (row);
		}

	}

}
