package net.osmand.plus.wikivoyage;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings.WikivoyageShowImages;
import net.osmand.plus.R;
import net.osmand.plus.base.BottomSheetDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;

public class WikivoyageShowPicturesDialogFragment extends BottomSheetDialogFragment {

	public static final String TAG = WikivoyageShowPicturesDialogFragment.class.getSimpleName();

	public static final int SHOW_PICTURES_CHANGED_REQUEST_CODE = 1;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.fragment_wikivoyage_show_images_first_time, container, false);
		view.findViewById(R.id.button_no).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				OsmandApplication app = getMyApplication();
				if (app != null) {
					app.getSettings().WIKIVOYAGE_SHOW_IMAGES.set(WikivoyageShowImages.OFF);
				}
				sendResult();
				dismiss();
			}
		});
		TextView buttonDownload = view.findViewById(R.id.button_download);
		if (getMyApplication().getSettings().isWifiConnected()) {
			buttonDownload.setText(R.string.shared_string_only_with_wifi);
			buttonDownload.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					OsmandApplication app = getMyApplication();
					if (app != null) {
						app.getSettings().WIKIVOYAGE_SHOW_IMAGES.set(WikivoyageShowImages.WIFI);
					}
					sendResult();
					dismiss();
				}
			});
		} else {
			buttonDownload.setText(R.string.shared_string_do);
			buttonDownload.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					OsmandApplication app = getMyApplication();
					if (app != null) {
						app.getSettings().WIKIVOYAGE_SHOW_IMAGES.set(WikivoyageShowImages.ON);
					}
					sendResult();
					dismiss();
				}
			});
		}

		setupHeight(view);

		return view;
	}

	private void sendResult() {
		Fragment fragment = getTargetFragment();
		if (fragment != null) {
			fragment.onActivityResult(getTargetRequestCode(), SHOW_PICTURES_CHANGED_REQUEST_CODE, null);
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		if (!AndroidUiHelper.isOrientationPortrait(getActivity())) {
			final Activity activity = getActivity();
			final Window window = getDialog().getWindow();
			if (activity != null && window != null) {
				WindowManager.LayoutParams params = window.getAttributes();
				params.width = activity.getResources().getDimensionPixelSize(R.dimen.landscape_bottom_sheet_dialog_fragment_width);
				window.setAttributes(params);
			}
		}
	}

	protected void setupHeight(final View mainView) {
		final Activity activity = getActivity();
		if (activity != null) {
			final int screenHeight = AndroidUtils.getScreenHeight(activity);
			final int statusBarHeight = AndroidUtils.getStatusBarHeight(activity);
			final int contentHeight = screenHeight - statusBarHeight
					- AndroidUtils.getNavBarHeight(activity)
					- getResources().getDimensionPixelSize(R.dimen.bottom_sheet_descr_height);

			mainView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
				@Override
				public void onGlobalLayout() {
					final View contentView = mainView.findViewById(R.id.scroll_view);
					if (contentView.getHeight() > contentHeight) {
						contentView.getLayoutParams().height = contentHeight;
						contentView.requestLayout();
					}
					ViewTreeObserver obs = mainView.getViewTreeObserver();
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
						obs.removeOnGlobalLayoutListener(this);
					} else {
						obs.removeGlobalOnLayoutListener(this);
					}
				}
			});
		}
	}
}
