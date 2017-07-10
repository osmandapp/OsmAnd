package net.osmand;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;

public class SecondSplashScreenFragment extends Fragment {
    public static final String TAG = "SecondSplashScreenFragment";
    public static boolean SHOW = true;
    private static final int SECOND_SPLASH_TIME_OUT = 2000;

    public boolean hasNavBar () {
        int id = getResources().getIdentifier("config_showNavigationBar", "bool", "android");
        if (id > 0)
            return getResources().getBoolean(id);
        else
            return false;
    }

    public int getStatusBarHeight() {
        int statusBarHeight = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }
        return statusBarHeight;
    }

    public int getNavigationBarHeight () {
        if (!hasNavBar())
            return 0;

        int orientation = getResources().getConfiguration().orientation;

        //Only phone between 0-599 has navigationbar can move
        boolean isSmartphone = getResources().getConfiguration().smallestScreenWidthDp < 600;
        if (isSmartphone && Configuration.ORIENTATION_LANDSCAPE == orientation)
            return 0;

        int id = getResources().getIdentifier(orientation == Configuration.ORIENTATION_PORTRAIT ? "navigation_bar_height" : "navigation_bar_height_landscape", "dimen", "android");
        if (id > 0)
            return getResources().getDimensionPixelSize(id);

        return 0;
    }

    public int getNavigationBarWidth ()
    {
        if (!hasNavBar())
            return 0;

        int orientation = getResources().getConfiguration().orientation;

        //Only phone between 0-599 has navigationbar can move
        boolean isSmartphone = getResources().getConfiguration().smallestScreenWidthDp < 600;

        if (orientation == Configuration.ORIENTATION_LANDSCAPE && isSmartphone)
        {
            int id = getResources().getIdentifier("navigation_bar_width", "dimen", "android");
            if (id > 0)
                return getResources().getDimensionPixelSize(id);
        }

        return 0;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        RelativeLayout view = new RelativeLayout(getActivity());
        view.setBackgroundColor(getResources().getColor(R.color.map_background_color_light));

        ImageView logo = new ImageView(getContext());
        logo.setImageDrawable(getResources().getDrawable(R.drawable.ic_logo_splash_osmand_plus));
        RelativeLayout.LayoutParams logoLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        logoLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        logoLayoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        if (AndroidUiHelper.getScreenOrientation(getActivity()) == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT ||
                AndroidUiHelper.getScreenOrientation(getActivity()) == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT) {
            int defaultLogoMarginTop = AndroidUtils.dpToPx(getActivity(), 150);
            logoLayoutParams.setMargins(0, defaultLogoMarginTop - getStatusBarHeight(), 0, 0);
        } else if (AndroidUiHelper.getScreenOrientation(getActivity()) == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE ||
                AndroidUiHelper.getScreenOrientation(getActivity()) == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
                if (AndroidUiHelper.getScreenOrientation(getActivity()) == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                    int defaultLogoMarginTop = AndroidUtils.dpToPx(getActivity(), 24);
                    int logoMarginLeft = getNavigationBarWidth();
                    logoLayoutParams.setMargins(logoMarginLeft, defaultLogoMarginTop - getStatusBarHeight(), 0, 0);
                } else if (AndroidUiHelper.getScreenOrientation(getActivity()) == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
                    int defaultLogoMarginTop = AndroidUtils.dpToPx(getActivity(), 24);
                    int logoMarginRight = getNavigationBarWidth();
                    logoLayoutParams.setMargins(0, defaultLogoMarginTop - getStatusBarHeight(), logoMarginRight, 0);
                }
            } else {
                int defaultLogoMarginTop = AndroidUtils.dpToPx(getActivity(), 24);
                int logoMarginLeft = getNavigationBarWidth();
                logoLayoutParams.setMargins(logoMarginLeft, defaultLogoMarginTop - getStatusBarHeight(), 0, 0);
            }
        }
        logo.setLayoutParams(logoLayoutParams);
        view.addView(logo);

        ImageView text = new ImageView(getActivity());
        text.setImageDrawable(getResources().getDrawable(R.drawable.image_text_osmand_plus));
        RelativeLayout.LayoutParams textLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        textLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        textLayoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        if (AndroidUiHelper.getScreenOrientation(getActivity()) == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT ||
                AndroidUiHelper.getScreenOrientation(getActivity()) == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT) {
            int defaultTextMarginBottom = AndroidUtils.dpToPx(getActivity(), 128);
            textLayoutParams.setMargins(0, 0, 0, defaultTextMarginBottom - getNavigationBarHeight());
        } else if (AndroidUiHelper.getScreenOrientation(getActivity()) == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE ||
                AndroidUiHelper.getScreenOrientation(getActivity()) == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
                if (AndroidUiHelper.getScreenOrientation(getActivity()) == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                    int defaultTextMarginBottom = AndroidUtils.dpToPx(getActivity(), 48);
                    int textMarginLeft = getNavigationBarWidth();
                    Log.d("LANDSCAPE", "width: " + getNavigationBarWidth());
                    textLayoutParams.setMargins(textMarginLeft, 0, 0, defaultTextMarginBottom);
                } else if (AndroidUiHelper.getScreenOrientation(getActivity()) == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
                    int defaultTextMarginBottom = AndroidUtils.dpToPx(getActivity(), 48);
                    int textMarginRight = getNavigationBarWidth();
                    Log.d("REVERSE_LANDSCAPE", "width: " + getNavigationBarWidth());
                    textLayoutParams.setMargins(0, 0, textMarginRight, defaultTextMarginBottom);
                }
            } else {
                int defaultTextMarginBottom = AndroidUtils.dpToPx(getActivity(), 48);
                textLayoutParams.setMargins(0, 0, 0, defaultTextMarginBottom);
            }
        }
        text.setLayoutParams(textLayoutParams);
        view.addView(text);

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
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
