package net.osmand.plus.activities;

import net.osmand.osm.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.fragments.PlaceDetailsFragment;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;

public class PlacePickerActivity extends Activity {

    public static final int SELECT_PLACE_RESULT_OK = 1;
    
    public static final String PLACE_LATITUDE = "net.osmand.place_latitude"; //$NON-NLS-1$
    public static final String PLACE_LONGITUDE = "net.osmand.place_longitude"; //$NON-NLS-1$
    public static final String PLACE_NAME = "net.osmand.place_name"; //$NON-NLS-1$

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
       
        public void onPlaceTypeChanged(String placeTypeKey);
        
        public void onPlacePicked(LatLon location, String name);
    }

    private class MapPlacePickerListener implements PlacePickerListener {

        private PlacePickerActivity placePicker;
        
        @Override
        public void onPlacePickerCreated(PlacePickerActivity placePicker) {
            this.placePicker = placePicker;
        }

        @Override
        public void onPlaceTypeChanged(String placeTypeKey) {

            PlaceDetailsFragment details = (PlaceDetailsFragment)
                    placePicker.getFragmentManager().findFragmentById(R.id.placeDetails);
            if (details == null || details.getShownPlaceType() != placeTypeKey) {
                
                details = PlaceDetailsFragment.newInstance(placeTypeKey);
                // Execute a transaction, replacing any existing fragment
                // with this one inside the frame.
                FragmentTransaction ft = placePicker.getFragmentManager().beginTransaction();
                ft.replace(R.id.placeDetails, details);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                ft.commit();
            }
        }

        @Override
        public void onPlacePicked(LatLon location, String name) {
            Intent intent = placePicker.getIntent();
            intent.putExtra("PLACE_LATITUDE", location.getLatitude());
            intent.putExtra("PLACE_LONGITUDE", location.getLongitude());
            intent.putExtra("PLACE_NAME", name);
            placePicker.setResult(SELECT_PLACE_RESULT_OK, intent);
            placePicker.finish();
        }
       
    }

}
