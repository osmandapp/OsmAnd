package net.osmand.plus.fragments;

import net.osmand.plus.R;
import net.osmand.plus.fragments.MapFragment;
import android.app.FragmentTransaction;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

/**
 * Top-level menu fragment, showing the menu options that the user can
 * pick. Upon picking an item, it takes care of displaying the
 * appropriate data to the user.
 */
public class MainMenuFragment extends Fragment {
	static final int MAP_TAB_INDEX = 0;
	static final int MUSIC_TAB_INDEX = 1;
	static final int PHONE_TAB_INDEX = 2;
	static final int CAR_TAB_INDEX = 3;
	private static int DEFAULT_TAB_INDEX = 0;
	
	int mCurrentSelectedTab = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        if (savedInstanceState != null) {
        	mCurrentSelectedTab = getSelectedTab(savedInstanceState);
        }

        View view = inflater.inflate(R.layout.main_menu_fragment, container, false);
        wireTabButtons(view);
    	showDetails(mCurrentSelectedTab);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("selectedTab", mCurrentSelectedTab);
    }
    
    int getSelectedTab(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
        	return DEFAULT_TAB_INDEX;
        } else {
        	return savedInstanceState.getInt("selectedTab", 0);
        }
    }
    
    void wireTabButtons(View view) {
    	ImageButton button = (ImageButton) view.findViewById(R.id.mapButton);
        setOnClickListener(button, MAP_TAB_INDEX);
        
        button = (ImageButton) view.findViewById(R.id.musicButton);
        setOnClickListener(button, MUSIC_TAB_INDEX);
        
        button = (ImageButton) view.findViewById(R.id.phoneButton);
        setOnClickListener(button, PHONE_TAB_INDEX);
        
        button = (ImageButton) view.findViewById(R.id.carButton);
        setOnClickListener(button, CAR_TAB_INDEX);
    }

    void setOnClickListener(ImageButton button, final int index) {
        button.setOnClickListener(new View.OnClickListener() {
        	@Override
            public void onClick(View view) {
            	showDetails(index);
            }
        });

    }
    
    /**
     * Helper function to show the details of a selected item, by
     * displaying a fragment in-place in the current UI.
     */
    void showDetails(int tabIndex) {
        // Check what fragment is currently shown, replace if needed.
        TabFragment tab = (TabFragment)
                getFragmentManager().findFragmentById(R.id.Details);
        if (tab == null || tab.getShownIndex() != tabIndex) {
        	switch (tabIndex) {
        	case MAP_TAB_INDEX:
                tab = new MapFragment();
        		break;
        	case MUSIC_TAB_INDEX:
                tab = new MusicFragment();
        		break;
        	case PHONE_TAB_INDEX:
                tab = new PhoneFragment();
                break;
        	case CAR_TAB_INDEX:
                tab = new CarFragment();
        		break;
        	}
            // Execute a transaction, replacing any existing fragment
            // with this one inside the frame.
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.Details, tab);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.commit();
        }
    }
    
}

