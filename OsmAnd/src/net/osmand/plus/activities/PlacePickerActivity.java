package net.osmand.plus.activities;

import net.osmand.plus.R;
import net.osmand.plus.fragments.PlaceDetailsFragment;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;

public class PlacePickerActivity extends Activity {

    public static final int SELECT_PLACE_RESULT_OK = 1;
    
    public static final String SEARCH_LAT = "net.osmand.search_lat"; //$NON-NLS-1$
    public static final String SEARCH_LON = "net.osmand.search_lon"; //$NON-NLS-1$

    private PlacePickerListener placePickerListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getPlacePickerListener().onPlacePickerCreated(this);

        requestWindowFeature(Window.FEATURE_NO_TITLE); 
        setContentView(R.layout.place_picker);
        
        View closeButton = findViewById(R.id.place_picker_close);
        closeButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    PlacePickerActivity.this.finish();
                }
        });
        
        // TODO(natashaj): how to cancel when touched outside?
    }
    
    public PlacePickerListener getPlacePickerListener() {
        if (placePickerListener == null) {
            placePickerListener = new MapPlacePickerListener();
        }
        return placePickerListener;
    }
    
    public interface PlacePickerListener {
        public void onPlacePickerCreated(PlacePickerActivity placePicker);
       
        public void onPlaceTypeChanged(int placeTypeIndex);
    }

    private class MapPlacePickerListener implements PlacePickerListener {

        private PlacePickerActivity placePicker;
        
        @Override
        public void onPlacePickerCreated(PlacePickerActivity placePicker) {
            this.placePicker = placePicker;
        }

        @Override
        public void onPlaceTypeChanged(int placeTypeIndex) {

            PlaceDetailsFragment details = (PlaceDetailsFragment)
                    placePicker.getFragmentManager().findFragmentById(R.id.placeDetails);
            if (details == null || details.getShownIndex() != placeTypeIndex) {
                
                details = PlaceDetailsFragment.newInstance(placeTypeIndex);
                
                // Execute a transaction, replacing any existing fragment
                // with this one inside the frame.
                FragmentTransaction ft = placePicker.getFragmentManager().beginTransaction();
                ft.replace(R.id.placeDetails, details);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                ft.commit();
            }
        }
        
    }

}
