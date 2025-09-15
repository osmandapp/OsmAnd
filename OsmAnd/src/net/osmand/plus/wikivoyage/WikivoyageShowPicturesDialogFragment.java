package net.osmand.plus.wikivoyage;

import android.app.Activity;
import android.app.Dialog;
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
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.wikipedia.WikiArticleShowImages;
import net.osmand.plus.R;
import net.osmand.plus.base.BottomSheetDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;

import java.util.List;

public class WikivoyageShowPicturesDialogFragment extends BottomSheetDialogFragment {

	public static final String TAG = WikivoyageShowPicturesDialogFragment.class.getSimpleName();
	public static final int SHOW_PICTURES_CHANGED_REQUEST_CODE = 1;

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		updateNightMode();
		View view = inflate(R.layout.fragment_wikivoyage_show_images_first_time, container, false);
		TextView buttonNo = view.findViewById(R.id.button_no);
		buttonNo.setText(R.string.shared_string_only_with_wifi);
		buttonNo.setOnClickListener(v -> {
			settings.WIKI_ARTICLE_SHOW_IMAGES.set(WikiArticleShowImages.WIFI);
			sendResult();
			dismiss();
		});
		TextView buttonDownload = view.findViewById(R.id.button_download);
		buttonDownload.setText(R.string.shared_string_always);
		buttonDownload.setOnClickListener(v -> {
			settings.WIKI_ARTICLE_SHOW_IMAGES.set(WikiArticleShowImages.ON);
			sendResult();
			dismiss();
		});
		setupHeightAndBackground(view);
		return view;
	}

	@Nullable
	@Override
	public List<Integer> getScrollableViewIds() {
		return null;
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
		callActivity(activity -> {
			if (!AndroidUiHelper.isOrientationPortrait(activity)) {
				Dialog dialog = getDialog();
				Window window = dialog != null ? dialog.getWindow() : null;
				if (window != null) {
					WindowManager.LayoutParams params = window.getAttributes();
					params.width = getDimensionPixelSize(R.dimen.landscape_bottom_sheet_dialog_fragment_width);
					window.setAttributes(params);
				}
			}
		});
	}

	protected void setupHeightAndBackground(View mainView) {
		Activity activity = getActivity();
		if (activity != null) {
			int screenHeight = AndroidUtils.getScreenHeight(activity);
			int statusBarHeight = AndroidUtils.getStatusBarHeight(activity);
			int contentHeight = screenHeight - statusBarHeight
					- AndroidUtils.getNavBarHeight(activity)
					- getDimensionPixelSize(R.dimen.wikivoyage_show_images_dialog_buttons_height);

			mainView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
				@Override
				public void onGlobalLayout() {
					View contentView = mainView.findViewById(R.id.scroll_view);
					if (contentView.getHeight() > contentHeight) {
						contentView.getLayoutParams().height = contentHeight;
						contentView.requestLayout();
					}

					// 8dp is the shadow height
					boolean showTopShadow = screenHeight - statusBarHeight - mainView.getHeight() >= dpToPx(8);
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

	public static void showInstance(@NonNull FragmentManager fragmentManager, @NonNull Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			WikivoyageShowPicturesDialogFragment fragment = new WikivoyageShowPicturesDialogFragment();
			fragment.setTargetFragment(target, SHOW_PICTURES_CHANGED_REQUEST_CODE);
			fragment.show(fragmentManager, TAG);
		}
	}
}
