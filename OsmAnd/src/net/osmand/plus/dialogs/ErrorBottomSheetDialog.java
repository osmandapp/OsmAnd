package net.osmand.plus.dialogs;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.OsmandActionBarActivity;
import net.osmand.plus.base.BottomSheetDialogFragment;
import net.osmand.plus.helpers.FontCache;

import java.text.MessageFormat;

public class ErrorBottomSheetDialog extends BottomSheetDialogFragment {
	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		FragmentActivity activity = requireActivity();
		OsmandApplication app = (OsmandApplication) activity.getApplication();
		View view = activity.getLayoutInflater().inflate(R.layout.dash_error_fragment, container, false);
		String msg = MessageFormat.format(getString(R.string.previous_run_crashed), OsmandApplication.EXCEPTION_PATH);
		Typeface typeface = FontCache.getRobotoMedium(activity);
		ImageView iv = ((ImageView) view.findViewById(R.id.error_icon));
		iv.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_crashlog));
		TextView message = ((TextView) view.findViewById(R.id.error_header));
		message.setTypeface(typeface);
		message.setText(msg);
		Button errorBtn = ((Button) view.findViewById(R.id.error_btn));
		errorBtn.setTypeface(typeface);
		errorBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				OsmandApplication app = getMyApplication();
				if (app != null) {
					app.sendCrashLog();
				}
				dismiss();
			}
		});

		Button cancelBtn = ((Button) view.findViewById(R.id.error_cancel));
		cancelBtn.setTypeface(typeface);
		cancelBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				OsmandActionBarActivity dashboardActivity = ((OsmandActionBarActivity) getActivity());
				if (dashboardActivity != null) {
					dismiss();
				}
			}
		});
		return view;
	}

	public static boolean shouldShow(OsmandSettings settings, MapActivity activity) {
		return activity.getMyApplication().getAppInitializer()
				.checkPreviousRunsForExceptions(activity, settings != null);
	}
}
