/**
 * 
 */
package net.osmand.plus.sherpafy;

import java.util.List;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.sherpafy.TourCommonActivity.TourFragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;


public class TourSelectionFragment extends SherlockListFragment implements TourFragment {
	public static final int REQUEST_POI_EDIT = 55;
	private static final int DOWNLOAD_MORE = 0;
	private SherpafyCustomization appCtx;
	private boolean lightContent;
	
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		appCtx = (SherpafyCustomization) getApp().getAppCustomization();
		lightContent = getApp().getSettings().isLightContent();
		setHasOptionsMenu(true);
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
		setListAdapter(new LocalAdapter(appCtx.getTourInformations()));
	}
	
	public OsmandApplication getApp(){
		return (OsmandApplication) getSherlockActivity().getApplication();
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		final OsmandApplication app = (OsmandApplication) getActivity().getApplication();
		com.actionbarsherlock.view.MenuItem menuItem = menu.add(0, DOWNLOAD_MORE, 0, R.string.download_more).setShowAsActionFlags(
				MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
//		boolean light = app.getSettings().isLightActionBar();
    	// menuItem = menuItem.setIcon(light ? R.drawable.ic_action_gdown_light : R.drawable.ic_action_gdown_dark);
		menuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(com.actionbarsherlock.view.MenuItem item) {
				Intent intent = new Intent(getActivity(), app.getAppCustomization().getDownloadIndexActivity());
				getActivity().startActivity(intent);
				return true;
			}
		});
	}

	@Override
	public void onListItemClick(ListView parent, View v, int position, long id) {
		final TourInformation tour = ((LocalAdapter) getListAdapter()).getItem(position);
		if(appCtx.getSelectedTour() != tour) {
			((TourCommonActivity) getActivity()).selectTour(tour);
		} else {
			((TourCommonActivity) getActivity()).selectTour(null);
		}
	}

	private class LocalAdapter extends ArrayAdapter<TourInformation> {
		LocalAdapter(List<TourInformation> list) {
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
			final TourInformation model = getItem(position);
			if (appCtx.getSelectedTour() == model) {
				check.setImageResource(!lightContent ? R.drawable.ic_action_ok_dark : R.drawable.ic_action_ok_light);
				check.setVisibility(View.VISIBLE);
			} else {
				check.setVisibility(View.INVISIBLE);
			}
			if(model.getShortDescription().length() > 0) {
				label.setText(model.getName() +"\n" + model.getShortDescription());
			} else {
				label.setText(model.getName());
			}
			return (row);
		}

	}

	@Override
	public void refreshTour() {
		setListAdapter(new LocalAdapter(appCtx.getTourInformations()));
		
	}

}
