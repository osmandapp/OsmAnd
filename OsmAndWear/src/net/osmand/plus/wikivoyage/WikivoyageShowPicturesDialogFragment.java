package net.osmand.plus.wikivoyage;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.wikipedia.WikiArticleShowImages;
import net.osmand.plus.R;
import net.osmand.plus.base.BottomSheetDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;

public class WikivoyageShowPicturesDialogFragment extends BottomSheetDialogFragment {

	public static final String TAG = WikivoyageShowPicturesDialogFragment.class.getSimpleName();

	public static final int SHOW_PICTURES_CHANGED_REQUEST_CODE = 1;

	protected boolean nightMode;

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		nightMode = !getMyApplication().getSettings().isLightContent();
		View view = inflater.inflate(R.layout.fragment_wikivoyage_show_images_first_time, container, false);
		TextView buttonNo = view.findViewById(R.id.button_no);
		buttonNo.setText(R.string.shared_string_only_with_wifi);
		buttonNo.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				OsmandApplication app = getMyApplication();
				if (app != null) {
					app.getSettings().WIKI_ARTICLE_SHOW_IMAGES.set(WikiArticleShowImages.WIFI);
				}
				sendResult();
				dismiss();
			}
		});
		TextView buttonDownload = view.findViewById(R.id.button_download);
		buttonDownload.setText(R.string.shared_string_always);
		buttonDownload.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				OsmandApplication app = getMyApplication();
				if (app != null) {
					app.getSettings().WIKI_ARTICLE_SHOW_IMAGES.set(WikiArticleShowImages.ON);
				}
				sendResult();
				dismiss();
			}
		});
		setupHeightAndBackground(view);
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
			Activity activity = getActivity();
			Window window = getDialog().getWindow();
			if (activity != null && window != null) {
				WindowManager.LayoutParams params = window.getAttributes();
				params.width = activity.getResources().getDimensionPixelSize(R.dimen.landscape_bottom_sheet_dialog_fragment_width);
				window.setAttributes(params);
			}
		}
	}

	protected void setupHeightAndBackground(View mainView) {
		Activity activity = getActivity();
		if (activity != null) {
			int screenHeight = AndroidUtils.getScreenHeight(activity);
			int statusBarHeight = AndroidUtils.getStatusBarHeight(activity);
			int contentHeight = screenHeight - statusBarHeight
					- AndroidUtils.getNavBarHeight(activity)
					- getResources().getDimensionPixelSize(R.dimen.wikivoyage_show_images_dialog_buttons_height);

			mainView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
				@Override
				public void onGlobalLayout() {
					View contentView = mainView.findViewById(R.id.scroll_view);
					if (contentView.getHeight() > contentHeight) {
						contentView.getLayoutParams().height = contentHeight;
						contentView.requestLayout();
					}

					// 8dp is the shadow height
					boolean showTopShadow = screenHeight - statusBarHeight - mainView.getHeight() >= AndroidUtils.dpToPx(activity, 8);
					if (AndroidUiHelper.isOrientationPortrait(activity)) {
						mainView.setBackgroundResource(showTopShadow ? getPortraitBgResId() : getBgColorId());
					} else {
						mainView.setBackgroundResource(showTopShadow ? getLandscapeTopsidesBgResId() : getLandscapeSidesBgResId());
					}

					ViewTreeObserver obs = mainView.getViewTreeObserver();
					obs.removeOnGlobalLayoutListener(this);
				}
			});
		}
	}

	@DrawableRes
	protected int getPortraitBgResId() {
		return nightMode ? R.drawable.bg_bottom_menu_dark : R.drawable.bg_bottom_menu_light;
	}

	@DrawableRes
	protected int getLandscapeTopsidesBgResId() {
		return nightMode ? R.drawable.bg_bottom_sheet_topsides_landscape_dark : R.drawable.bg_bottom_sheet_topsides_landscape_light;
	}

	@DrawableRes
	protected int getLandscapeSidesBgResId() {
		return nightMode ? R.drawable.bg_bottom_sheet_sides_landscape_dark : R.drawable.bg_bottom_sheet_sides_landscape_light;
	}

	@ColorRes
	protected int getBgColorId() {
		return ColorUtilities.getListBgColorId(nightMode);
	}
}
