package net.osmand.plus.dashboard;


import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.OsmandActionBarActivity;
import net.osmand.plus.dashboard.tools.DashFragmentData.ShouldShowFunction;
import net.osmand.plus.feedback.CrashBottomSheetDialogFragment;
import net.osmand.plus.feedback.FeedbackHelper;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.FontCache;

import java.text.MessageFormat;

public class DashErrorFragment extends DashBaseFragment {

	public static final String TAG = "DASH_ERROR_FRAGMENT";
	public static final ShouldShowFunction SHOULD_SHOW_FUNCTION = new ShouldShowFunction() {
		// If settings null. No changes in setting will be made.
		@Override
		public boolean shouldShow(OsmandSettings settings, MapActivity activity, String tag) {
			return CrashBottomSheetDialogFragment.shouldShow(settings, activity);
		}
	};
	private DismissListener dismissCallback;

	@Override
	public View initView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		OsmandApplication app = requireMyApplication();
		View view = inflater.inflate(R.layout.dash_error_fragment, container, false);
		String msg = MessageFormat.format(getString(R.string.previous_run_crashed), FeedbackHelper.EXCEPTION_PATH);
		Typeface typeface = FontCache.getMediumFont();
		ImageView iv = view.findViewById(R.id.error_icon);
		iv.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_crashlog));
		TextView message = view.findViewById(R.id.error_header);
		message.setTypeface(typeface);
		message.setText(msg);
		Button errorBtn = view.findViewById(R.id.error_btn);
		errorBtn.setTypeface(typeface);
		errorBtn.setOnClickListener(v -> app.getFeedbackHelper().sendCrashLog());

		Button cancelBtn = view.findViewById(R.id.error_cancel);
		cancelBtn.setTypeface(typeface);
		cancelBtn.setOnClickListener(v -> {
			OsmandActionBarActivity dashboardActivity = ((OsmandActionBarActivity) getActivity());
			if (dashboardActivity != null) {
				dashboardActivity.getSupportFragmentManager()
						.beginTransaction()
						.remove(this)
						.commitAllowingStateLoss();
			}
		});
		dismissCallback = new ErrorDismissListener(getParentView(), dashboard, TAG, view);
		return view;
	}

	@Override
	public void onOpenDash() {
	}

	@Override
	public DismissListener getDismissCallback() {
		return dismissCallback;
	}

	private static class ErrorDismissListener implements DismissListener {

		private final View parentView;
		private final DashboardOnMap dashboardOnMap;
		private final String fragmentTag;
		private final View fragmentView;

		public ErrorDismissListener(View parentView, DashboardOnMap dashboardOnMap,
		                            String fragmentTag, View fragmentView) {
			this.parentView = parentView;
			this.dashboardOnMap = dashboardOnMap;
			this.fragmentTag = fragmentTag;
			this.fragmentView = fragmentView;
		}

		@Override
		public void onDismiss() {
			dashboardOnMap.hideFragmentByTag(fragmentTag);
			fragmentView.setTranslationX(0);
			fragmentView.setAlpha(1);
			Snackbar.make(parentView, dashboardOnMap.getMyApplication().getResources()
							.getString(R.string.shared_string_card_was_hidden), Snackbar.LENGTH_LONG)
					.setAction(R.string.shared_string_undo, view -> ErrorDismissListener.this.onUndo())
					.show();
		}

		public void onUndo() {
			dashboardOnMap.unhideFragmentByTag(fragmentTag);
			fragmentView.setTranslationX(0);
			fragmentView.setAlpha(1);
		}
	}
}
