package net.osmand;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

public class SecondSplashScreenFragment extends Fragment {
    public static final String TAG = "SecondSplashScreenFragment";
    public static boolean SHOW = true;
    private static final int SECOND_SPLASH_TIME_OUT = 2000;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.second_splash_screen_layout, container, false);

        ((MapActivity) getActivity()).hideStatusBar();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                ((MapActivity) getActivity()).showStatusBar();
                getActivity().getSupportFragmentManager().beginTransaction().remove(SecondSplashScreenFragment.this).commitAllowingStateLoss();
            }
        }, SECOND_SPLASH_TIME_OUT);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        ((MapActivity)getActivity()).disableDrawer();
    }

    @Override
    public void onPause() {
        super.onPause();
        ((MapActivity)getActivity()).enableDrawer();
    }
}
