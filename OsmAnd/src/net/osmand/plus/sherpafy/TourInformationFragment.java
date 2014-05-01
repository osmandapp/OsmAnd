/**
 * 
 */
package net.osmand.plus.sherpafy;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.sherpafy.TourCommonActivity.TourFragment;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;


public class TourInformationFragment extends SherlockFragment  implements TourFragment {
	
	private View view;
	private SherpafyCustomization appCtx;
	public View onCreateView(android.view.LayoutInflater inflater, android.view.ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.tour_info, container, false);
		setHasOptionsMenu(true);
		appCtx = (SherpafyCustomization) getApp().getAppCustomization();
		return view;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		updateView();
	}

	private void updateView() {
		ImageView img = (ImageView) view.findViewById(R.id.tour_image);
		TextView description = (TextView) view.findViewById(R.id.tour_description);
		TextView fullDescription = (TextView) view.findViewById(R.id.tour_fulldescription);
		TextView name = (TextView) view.findViewById(R.id.tour_name);
		if(appCtx.getSelectedTour() != null) {
			name.setText(appCtx.getSelectedTour().getName());
			description.setText(appCtx.getSelectedTour().getShortDescription());
			description.setVisibility(View.VISIBLE);
			fullDescription.setText(appCtx.getSelectedTour().getFulldescription());
			fullDescription.setVisibility(View.VISIBLE);
			final Bitmap imageBitmap = appCtx.getSelectedTour().getImageBitmap();
			if(imageBitmap != null) {
				img.setImageBitmap(imageBitmap);
				img.setAdjustViewBounds(true);
				img.setScaleType(ScaleType.CENTER_INSIDE);
				img.setCropToPadding(true);
				img.setVisibility(View.VISIBLE);
			} else {
				img.setVisibility(View.GONE);
			}
		} else {
			name.setText(R.string.no_tour_selected);
			img.setVisibility(View.GONE);
			description.setVisibility(View.GONE);
			fullDescription.setVisibility(View.GONE);
		}
	}
	
	
	public OsmandApplication getApp(){
		return (OsmandApplication) getSherlockActivity().getApplication();
	}

	@Override
	public void refreshTour() {
		updateView();
	}
	

}
