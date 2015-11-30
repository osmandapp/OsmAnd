package net.osmand.plus.dashboard;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.tools.DashFragmentData;
import net.osmand.plus.dialogs.RateUsBottomSheetDialog;

public class DashRateUsFragment extends DashBaseFragment {
	public static final String TAG = "DASH_RATE_US_FRAGMENT";

	public static final DashFragmentData.ShouldShowFunction SHOULD_SHOW_FUNCTION =
			new DashboardOnMap.DefaultShouldShow() {
				@Override
				public boolean shouldShow(OsmandSettings settings, MapActivity activity, String tag) {
					return RateUsBottomSheetDialog.shouldShow(activity.getMyApplication())
							&& super.shouldShow(settings, activity, tag);
				}
			};

	private RateUsBottomSheetDialog.FragmentState state = RateUsBottomSheetDialog.FragmentState.INITIAL_STATE;
	private RateUsDismissListener mRateUsDismissListener;

	@Override
	public void onOpenDash() {

	}

	@Override
	public View initView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = getActivity().getLayoutInflater().inflate(R.layout.dash_rate_us_fragment, container, false);
		TextView header = (TextView) view.findViewById(R.id.header);
		TextView subheader = (TextView) view.findViewById(R.id.subheader);
		Button positiveButton = (Button) view.findViewById(R.id.positive_button);
		Button negativeButton = (Button) view.findViewById(R.id.negative_button);
		positiveButton.setOnClickListener(
				new PositiveButtonListener(header, subheader, positiveButton, negativeButton));
		negativeButton.setOnClickListener(
				new NegativeButtonListener(header, subheader, positiveButton, negativeButton));
		OsmandSettings settings = getMyApplication().getSettings();
		mRateUsDismissListener = new RateUsDismissListener(dashboard, settings);
		return view;
	}

	@Override
	public DismissListener getDismissCallback() {
		return mRateUsDismissListener;
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
			final OsmandSettings settings = getMyApplication().getSettings();
			switch (state) {
				case INITIAL_STATE:
					state = RateUsBottomSheetDialog.FragmentState.USER_LIKES_APP;

					header.setText(getResources().getString(R.string.rate_this_app));
					subheader.setText(getResources().getString(R.string.rate_this_app_long));
					positiveButton.setText(getResources().getString(R.string.shared_string_ok));
					negativeButton.setText(getResources().getString(R.string.shared_string_no_thanks));
					return;
				case USER_LIKES_APP:
					settings.RATE_US_STATE.set(RateUsBottomSheetDialog.RateUsState.LIKED);
					Uri uri = Uri.parse(Version.marketPrefix(getMyApplication()) + getActivity().getPackageName());
					Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
					startActivity(goToMarket);
					break;
				case USER_DISLIKES_APP:
					String email = getString(R.string.support_email);
					settings.RATE_US_STATE.set(RateUsBottomSheetDialog.RateUsState.DISLIKED_WITH_MESSAGE);
					settings.NUMBER_OF_APPLICATION_STARTS.set(0);
					settings.LAST_DISPLAY_TIME.set(System.currentTimeMillis());
					Intent sendEmail = new Intent(Intent.ACTION_SENDTO);
					sendEmail.setType("text/plain");
					sendEmail.setData(Uri.parse("mailto:" + email));
					sendEmail.putExtra(Intent.EXTRA_EMAIL, email);
					startActivity(sendEmail);
					break;
			}
			dashboard.refreshDashboardFragments();
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
			final OsmandSettings settings = getMyApplication().getSettings();
			switch (state) {
				case INITIAL_STATE:
					state = RateUsBottomSheetDialog.FragmentState.USER_DISLIKES_APP;

					header.setText(getResources().getString(R.string.user_hates_app_get_feedback));
					subheader.setText(getResources().getString(R.string.user_hates_app_get_feedback_long));
					positiveButton.setText(getResources().getString(R.string.shared_string_ok));
					negativeButton.setText(getResources().getString(R.string.shared_string_no_thanks));
					return;
				case USER_LIKES_APP:
					settings.RATE_US_STATE.set(RateUsBottomSheetDialog.RateUsState.IGNORED);
					break;
				case USER_DISLIKES_APP:
					settings.RATE_US_STATE.set(RateUsBottomSheetDialog.RateUsState.DISLIKED_WITHOUT_MESSAGE);
					break;
			}
			settings.NUMBER_OF_APPLICATION_STARTS.set(0);
			settings.LAST_DISPLAY_TIME.set(System.currentTimeMillis());
			dashboard.refreshDashboardFragments();
		}
	}

	private static class RateUsDismissListener implements DismissListener {
		private DashboardOnMap dashboardOnMap;
		private OsmandSettings settings;

		public RateUsDismissListener(DashboardOnMap dashboardOnMap, OsmandSettings settings) {
			this.dashboardOnMap = dashboardOnMap;
			this.settings = settings;
		}

		@Override
		public void onDismiss() {
			settings.RATE_US_STATE.set(RateUsBottomSheetDialog.RateUsState.IGNORED);
			settings.NUMBER_OF_APPLICATION_STARTS.set(0);
			settings.LAST_DISPLAY_TIME.set(System.currentTimeMillis());
			dashboardOnMap.refreshDashboardFragments();
		}
	}
}
