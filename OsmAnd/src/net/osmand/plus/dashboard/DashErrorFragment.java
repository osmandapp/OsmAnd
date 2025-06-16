package net.osmand.plus.dashboard;


import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

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

	@Override
	public View initView(ViewGroup container, Bundle savedState) {
		View view = inflate(R.layout.dash_error_fragment, container, false);
		Typeface typeface = FontCache.getMediumFont();

		String message = MessageFormat.format(getString(R.string.previous_run_crashed), FeedbackHelper.EXCEPTION_PATH);
		TextView tvMessage = view.findViewById(R.id.error_header);
		tvMessage.setTypeface(typeface);
		tvMessage.setText(message);

		ImageView ivErrorIcon = view.findViewById(R.id.error_icon);
		ivErrorIcon.setImageDrawable(uiUtilities.getThemedIcon(R.drawable.ic_crashlog));

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
		dismissListener = new ErrorDismissListener(getParentView(), dashboard, TAG, view);
		return view;
	}
}
