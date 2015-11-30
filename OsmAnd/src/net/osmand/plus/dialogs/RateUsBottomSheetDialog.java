package net.osmand.plus.dialogs;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.base.BottomSheetDialogFragment;

import java.util.Calendar;

public class RateUsBottomSheetDialog extends BottomSheetDialogFragment {
	private RateUsBottomSheetDialog.FragmentState state = RateUsBottomSheetDialog.FragmentState.INITIAL_STATE;
	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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

	public static boolean shouldShow(OsmandApplication application) {
		if (Version.isMarketEnabled(application)) {
			return false;
		}
		OsmandSettings settings = application.getSettings();
		if(!settings.LAST_DISPLAY_TIME.isSet()) {
			settings.LAST_DISPLAY_TIME.set(System.currentTimeMillis());
		}
		long lastDisplayTimeInMillis = settings.LAST_DISPLAY_TIME.get();
		int numberOfApplicationRuns = settings.NUMBER_OF_APPLICATION_STARTS.get();
		RateUsState state = settings.RATE_US_STATE.get();

		Calendar modifiedTime = Calendar.getInstance();
		Calendar lastDisplayTime = Calendar.getInstance();
		lastDisplayTime.setTimeInMillis(lastDisplayTimeInMillis);

		int bannerFreeRuns = 0;

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
		return modifiedTime.after(lastDisplayTime) && numberOfApplicationRuns >= bannerFreeRuns;
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
			dismiss();
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
			dismiss();
		}
	}

	public enum FragmentState {
		INITIAL_STATE,
		USER_LIKES_APP,
		USER_DISLIKES_APP
	}

	public enum RateUsState {
		INITIAL_STATE,
		IGNORED,
		LIKED,
		DISLIKED_WITH_MESSAGE,
		DISLIKED_WITHOUT_MESSAGE
	}
}
