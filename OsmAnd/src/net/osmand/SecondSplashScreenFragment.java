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

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
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

        boolean isSmartphone = getResources().getConfiguration().smallestScreenWidthDp < 600;
        if (isSmartphone && Configuration.ORIENTATION_LANDSCAPE == orientation)
            return 0;

        int id = getResources().getIdentifier(orientation == Configuration.ORIENTATION_PORTRAIT ? "navigation_bar_height" : "navigation_bar_height_landscape", "dimen", "android");
        if (id > 0)
            return getResources().getDimensionPixelSize(id);

        return 0;
    }

    public boolean isSmartphone() {
        return getResources().getConfiguration().smallestScreenWidthDp < 600;
    }

    public int getNavigationBarWidth ()
    {
        if (!hasNavBar())
            return 0;

        int orientation = getResources().getConfiguration().orientation;

        boolean isSmartphone = getResources().getConfiguration().smallestScreenWidthDp < 600;

        if (orientation == Configuration.ORIENTATION_LANDSCAPE && isSmartphone) {
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
                    int logoPaddingLeft = getNavigationBarWidth();
                    logo.setPadding(logoPaddingLeft, 0, 0, 0);
                    logoLayoutParams.setMargins(0, defaultLogoMarginTop - getStatusBarHeight(), 0, 0);
                } else if (AndroidUiHelper.getScreenOrientation(getActivity()) == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
                    int defaultLogoMarginTop = AndroidUtils.dpToPx(getActivity(), 24);
                    int logoPaddingRight = getNavigationBarWidth();
                    logo.setPadding(0, 0, logoPaddingRight, 0);
                    logoLayoutParams.setMargins(0, defaultLogoMarginTop - getStatusBarHeight(), 0, 0);
                }
            } else {
                int defaultLogoMarginTop = AndroidUtils.dpToPx(getActivity(), 24);
                int logoPaddingLeft = getNavigationBarWidth();
                logo.setPadding(logoPaddingLeft, 0, 0, 0);
                logoLayoutParams.setMargins(0, defaultLogoMarginTop - getStatusBarHeight(), 0, 0);
            }
        }
        logo.setLayoutParams(logoLayoutParams);
        view.addView(logo);

        ImageView text = new ImageView(getActivity());
        if (Version.isFreeVersion(((MapActivity) getActivity()).getMyApplication())) {
            text.setImageDrawable(getResources().getDrawable(R.drawable.image_text_osmand));
        } else if ((Version.isPaidVersion(((MapActivity) getActivity()).getMyApplication())) ||
                (Version.isDeveloperVersion(((MapActivity) getActivity()).getMyApplication()))) {
            text.setImageDrawable(getResources().getDrawable(R.drawable.image_text_osmand_plus));
        }
        RelativeLayout.LayoutParams textLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        textLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        textLayoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        if (AndroidUiHelper.getScreenOrientation(getActivity()) == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT ||
                AndroidUiHelper.getScreenOrientation(getActivity()) == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT) {
            int defaultTextMarginBottom = AndroidUtils.dpToPx(getActivity(), 128);
            Log.d("PORTRAIT", "defaultTextMarginBottom: " + defaultTextMarginBottom);
            Log.d("PORTRAIT", "getNavigationBarHeight: " + getNavigationBarHeight());
            textLayoutParams.setMargins(0, 0, 0, defaultTextMarginBottom - getNavigationBarHeight());
        } else if (AndroidUiHelper.getScreenOrientation(getActivity()) == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE ||
                AndroidUiHelper.getScreenOrientation(getActivity()) == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
                if (AndroidUiHelper.getScreenOrientation(getActivity()) == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                    int defaultTextMarginBottom = AndroidUtils.dpToPx(getActivity(), 48);
                    int textPaddingLeft = getNavigationBarWidth();
                    text.setPadding(textPaddingLeft, 0, 0, 0);
                    textLayoutParams.setMargins(0, 0, 0, defaultTextMarginBottom);
                } else if (AndroidUiHelper.getScreenOrientation(getActivity()) == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
                    int defaultTextMarginBottom = AndroidUtils.dpToPx(getActivity(), 48);
                    int textPaddingRight = getNavigationBarWidth();
                    text.setPadding(0, 0, textPaddingRight, 0);
                    textLayoutParams.setMargins(0, 0, 0, defaultTextMarginBottom);
                }
            } else {
                int defaultTextMarginBottom = AndroidUtils.dpToPx(getActivity(), 48);
                int textPaddingLeft = getNavigationBarWidth();
                text.setPadding(textPaddingLeft, 0, 0, 0);
                textLayoutParams.setMargins(0, 0, 0, defaultTextMarginBottom);
            }
        }
        text.setLayoutParams(textLayoutParams);
        view.addView(text);

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
//                getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
//                if (((MapActivity)getActivity()).getMyApplication().getSettings().MAP_SCREEN_ORIENTATION.get() != getActivity().getRequestedOrientation()) {
//                    getActivity().setRequestedOrientation(settings.MAP_SCREEN_ORIENTATION.get());
//                    // can't return from this method we are not sure if activity will be recreated or not
//                }
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
