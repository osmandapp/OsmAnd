package net.osmand.plus.dashboard;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;

import java.util.Calendar;

/**
 * Created by Denis on
 * 26.03.2015.
 */
public class DashRateUsFragment extends DashBaseFragment {
    public static final String TAG = "DASH_RATE_US_FRAGMENT";

    // Imported in shouldShow method
    private static OsmandSettings settings;
    private FragmentState state = FragmentState.INITIAL_STATE;

    @Override
    public void onOpenDash() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = getActivity().getLayoutInflater().inflate(R.layout.dash_rate_us_fragment, container, false);
        TextView header = (TextView) view.findViewById(R.id.header);
        TextView subheader = (TextView) view.findViewById(R.id.subheader);
        Button positiveButton = (Button) view.findViewById(R.id.positive_button);
        Button negativeButton = (Button) view.findViewById(R.id.negative_button);
        positiveButton.setOnClickListener(
                new PositiveButtonListener(header, subheader, positiveButton, negativeButton));
        negativeButton.setOnClickListener(
                new NegativeButtonListener(header, subheader, positiveButton, negativeButton));
        return view;
    }

    public static boolean shouldShow(OsmandSettings settings) {
        if(!settings.LAST_DISPLAY_TIME.isSet()) {
            settings.LAST_DISPLAY_TIME.set(System.currentTimeMillis());
        }
        DashRateUsFragment.settings = settings;
        long lastDisplayTimeInMillis = settings.LAST_DISPLAY_TIME.get();
        int numberOfApplicationRuns = settings.NUMBER_OF_APPLICATION_STARTS.get();
        RateUsState state = settings.RATE_US_STATE.get();

        Calendar modifiedTime = Calendar.getInstance();
        Calendar lastDisplayTime = Calendar.getInstance();
        lastDisplayTime.setTimeInMillis(lastDisplayTimeInMillis);

        int bannerFreeRuns = 0;

        Log.v(TAG, "state=" + state + "; lastDisplayTimeInMillis=" + lastDisplayTimeInMillis
                + "; numberOfApplicationRuns=" + numberOfApplicationRuns);

        switch (state) {
            case LIKED:
                return false;
            case INITIAL_STATE:
                break;
            case IGNORED:
                modifiedTime.add(Calendar.WEEK_OF_YEAR, -1);
                bannerFreeRuns = 5;
                break;
            case DISLIKED_WITH_MESSAGE:
                modifiedTime.add(Calendar.MONTH, -3);
                bannerFreeRuns = 3;
                break;
            case DISLIKED_WITHOUT_MESSAGE:
                modifiedTime.add(Calendar.MONTH, -2);
                break;
            default:
                throw new IllegalStateException("Unexpected state:" + state);
        }

        if (state != RateUsState.INITIAL_STATE) {
            if (modifiedTime.after(lastDisplayTime) && numberOfApplicationRuns >= bannerFreeRuns) {
                settings.RATE_US_STATE.set(RateUsState.INITIAL_STATE);
                modifiedTime = Calendar.getInstance();
            } else {
                return false;
            }
        }
        // Initial state now
        modifiedTime.add(Calendar.HOUR, -72);
        bannerFreeRuns = 3;
        if (modifiedTime.after(lastDisplayTime) && numberOfApplicationRuns >= bannerFreeRuns) {
            return true;
        }
        return false;
    }

    public class PositiveButtonListener implements View.OnClickListener {
        private TextView header;
        private TextView subheader;
        private Button positiveButton;
        private Button negativeButton;

        public PositiveButtonListener(TextView header, TextView subheader, Button positiveButton,
                                      Button negativeButton) {
            this.header = header;
            this.subheader = subheader;
            this.positiveButton = positiveButton;
            this.negativeButton = negativeButton;
        }

        @Override
        public void onClick(View v) {
            Log.v(TAG, "onClick(" + "v=" + v + ")");
            Log.v(TAG, this.getClass().getName());
            Log.v(TAG, "state=" + state);
            switch (state) {
                case INITIAL_STATE:
                    state = FragmentState.USER_LIKES_APP;

                    header.setText(getResources().getString(R.string.rate_this_app));
                    subheader.setText(getResources().getString(R.string.rate_this_app_long));
                    positiveButton.setText(getResources().getString(R.string.shared_string_ok));
                    negativeButton.setText(getResources().getString(R.string.shared_string_no_thanks));
                    Log.v(TAG, "state2=" + state);
                    return;
                case USER_LIKES_APP:
                    settings.RATE_US_STATE.set(RateUsState.LIKED);
                    // Assuming GooglePlay
                    Uri uri = Uri.parse("market://details?id=" + getActivity().getPackageName());
                    Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                    try {
                        startActivity(goToMarket);
                    } catch (ActivityNotFoundException e) {
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse("http://play.google.com/store/apps/details?id="
                                        + getActivity().getPackageName())));
                    }
                    dashboard.refreshDashboardFragments();
                    Log.v(TAG, "state2=" + state);
                    return;
                case USER_HATES_APP:
                    settings.RATE_US_STATE.set(RateUsState.DISLIKED_WITH_MESSAGE);
                    settings.NUMBER_OF_APPLICATION_STARTS.set(0);
                    settings.LAST_DISPLAY_TIME.set(System.currentTimeMillis());
                    dashboard.refreshDashboardFragments();
                    Intent sendEmail = new Intent(Intent.ACTION_SENDTO);
                    sendEmail.setType("text/plain");
                    // TODO replace email address with constant
                    sendEmail.putExtra(Intent.EXTRA_EMAIL, "support@osmand.net");
                    startActivity(sendEmail);
                    Log.v(TAG, "state2=" + state);
                    break;
            }
        }
    }


    public class NegativeButtonListener implements View.OnClickListener {
        private TextView header;
        private TextView subheader;
        private Button positiveButton;
        private Button negativeButton;

        public NegativeButtonListener(TextView header, TextView subheader, Button positiveButton,
                                      Button negativeButton) {
            this.header = header;
            this.subheader = subheader;
            this.positiveButton = positiveButton;
            this.negativeButton = negativeButton;
        }

        @Override
        public void onClick(View v) {
            Log.v(TAG, this.getClass().getName());
            Log.v(TAG, "state=" + state);
            switch (state) {
                case INITIAL_STATE:
                    state = FragmentState.USER_HATES_APP;

                    header.setText(getResources().getString(R.string.user_hates_app_get_feedback));
                    subheader.setText(getResources().getString(R.string.user_hates_app_get_feedback_long));
                    positiveButton.setText(getResources().getString(R.string.shared_string_ok));
                    negativeButton.setText(getResources().getString(R.string.shared_string_no_thanks));
                    Log.v(TAG, "state2=" + state);
                    return;
                case USER_LIKES_APP:
                    settings.RATE_US_STATE.set(RateUsState.IGNORED);
                    break;
                case USER_HATES_APP:
                    settings.RATE_US_STATE.set(RateUsState.DISLIKED_WITHOUT_MESSAGE);
                    break;
            }
            settings.NUMBER_OF_APPLICATION_STARTS.set(0);
            settings.LAST_DISPLAY_TIME.set(System.currentTimeMillis());
            dashboard.refreshDashboardFragments();
            Log.v(TAG, "state2=" + state);
        }
    }

    private enum FragmentState {
        INITIAL_STATE,
        USER_LIKES_APP,
        USER_HATES_APP
    }

    public enum RateUsState {
        INITIAL_STATE,
        IGNORED,
        LIKED,
        DISLIKED_WITH_MESSAGE,
        DISLIKED_WITHOUT_MESSAGE
    }
}
