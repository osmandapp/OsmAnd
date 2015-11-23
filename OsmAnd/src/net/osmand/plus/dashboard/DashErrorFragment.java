package net.osmand.plus.dashboard;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ViewCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.OsmandActionBarActivity;
import net.osmand.plus.dashboard.tools.DashFragmentData;
import net.osmand.plus.dialogs.ErrorBottomSheetDialog;
import net.osmand.plus.helpers.FontCache;

import java.io.File;
import java.text.MessageFormat;

public class DashErrorFragment extends DashBaseFragment {

	public static final String TAG = "DASH_ERROR_FRAGMENT";
	public static final DashFragmentData.ShouldShowFunction SHOULD_SHOW_FUNCTION =
			new DashFragmentData.ShouldShowFunction() {
				// If settings null. No changes in setting will be made.
				@Override
				public boolean shouldShow(OsmandSettings settings, MapActivity activity, String tag) {
					return ErrorBottomSheetDialog.shouldShow(settings, activity);
				}
			};
	private DismissListener dismissCallback;

	@Override
	public View initView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = getActivity().getLayoutInflater().inflate(R.layout.dash_error_fragment, container, false);
		String msg = MessageFormat.format(getString(R.string.previous_run_crashed), OsmandApplication.EXCEPTION_PATH);
		Typeface typeface = FontCache.getRobotoMedium(getActivity());
		ImageView iv = ((ImageView) view.findViewById(R.id.error_icon));
		iv.setImageDrawable(getMyApplication().getIconsCache().getContentIcon(R.drawable.ic_crashlog));
		TextView message = ((TextView) view.findViewById(R.id.error_header));
		message.setTypeface(typeface);
		message.setText(msg);
		Button errorBtn = ((Button) view.findViewById(R.id.error_btn));
		errorBtn.setTypeface(typeface);
		errorBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent intent = new Intent(Intent.ACTION_SEND);
				intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"crash@osmand.net"}); //$NON-NLS-1$
				File file = getMyApplication().getAppPath(OsmandApplication.EXCEPTION_PATH);
				intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
				intent.setType("vnd.android.cursor.dir/email"); //$NON-NLS-1$
				intent.putExtra(Intent.EXTRA_SUBJECT, "OsmAnd bug"); //$NON-NLS-1$
				StringBuilder text = new StringBuilder();
				text.append("\nDevice : ").append(Build.DEVICE); //$NON-NLS-1$
				text.append("\nBrand : ").append(Build.BRAND); //$NON-NLS-1$
				text.append("\nModel : ").append(Build.MODEL); //$NON-NLS-1$
				text.append("\nProduct : ").append(Build.PRODUCT); //$NON-NLS-1$
				text.append("\nBuild : ").append(Build.DISPLAY); //$NON-NLS-1$
				text.append("\nVersion : ").append(Build.VERSION.RELEASE); //$NON-NLS-1$
				text.append("\nApp Version : ").append(Version.getAppName(getMyApplication())); //$NON-NLS-1$
				try {
					PackageInfo info = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(),
							0);
					if (info != null) {
						text.append("\nApk Version : ").append(info.versionName).append(" ").append(info.versionCode); //$NON-NLS-1$ //$NON-NLS-2$
					}
				} catch (PackageManager.NameNotFoundException e) {
					PlatformUtil.getLog(DashErrorFragment.class).error("", e);
				}
				intent.putExtra(Intent.EXTRA_TEXT, text.toString());
				startActivity(Intent.createChooser(intent, getString(R.string.send_report)));
			}
		});

		Button cancelBtn = ((Button) view.findViewById(R.id.error_cancel));
		cancelBtn.setTypeface(typeface);
		cancelBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				OsmandActionBarActivity dashboardActivity = ((OsmandActionBarActivity) getActivity());
				if (dashboardActivity != null) {
					dashboardActivity.getSupportFragmentManager().beginTransaction().remove(DashErrorFragment.this)
							.commit();
				}
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
		private View parentView;
		private DashboardOnMap dashboardOnMap;
		private String fragmentTag;
		private View fragmentView;

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
			ViewCompat.setTranslationX(fragmentView, 0);
			ViewCompat.setAlpha(fragmentView, 1);
			Snackbar.make(parentView, dashboardOnMap.getMyApplication().getResources()
					.getString(R.string.shared_string_card_was_hidden), Snackbar.LENGTH_LONG)
					.setAction(R.string.shared_string_undo, new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							ErrorDismissListener.this.onUndo();
						}
					})
					.show();
		}

		public void onUndo() {
			dashboardOnMap.unhideFragmentByTag(fragmentTag);
			ViewCompat.setTranslationX(fragmentView, 0);
			ViewCompat.setAlpha(fragmentView, 1);
		}
	}
}
