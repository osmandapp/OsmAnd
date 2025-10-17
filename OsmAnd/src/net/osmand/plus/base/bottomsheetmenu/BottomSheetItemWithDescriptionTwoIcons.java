package net.osmand.plus.base.bottomsheetmenu;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorRes;
import androidx.annotation.LayoutRes;
import androidx.appcompat.widget.AppCompatImageView;

import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class BottomSheetItemWithDescriptionTwoIcons extends BottomSheetItemWithDescription {

	private boolean isDownloading;
	private MaterialProgressBar progressBar;
	Drawable rightIcon;

	public BottomSheetItemWithDescriptionTwoIcons(View customView,
	                                              @LayoutRes int layoutId,
	                                              Object tag,
	                                              boolean disabled,
	                                              View.OnClickListener onClickListener,
	                                              int position,
	                                              Drawable icon,
	                                              Drawable background,
	                                              CharSequence title,
	                                              @ColorRes int titleColorId,
	                                              boolean iconHidden,
	                                              CharSequence description,
	                                              @ColorRes int descriptionColorId,
	                                              int descriptionMaxLines,
	                                              boolean descriptionLinksClickable,
	                                              Drawable rightIcon) {
		super(customView, layoutId, tag, disabled, onClickListener, position, icon, background, title, titleColorId, iconHidden, description, descriptionColorId, descriptionMaxLines, descriptionLinksClickable);
		this.rightIcon = rightIcon;
	}

	@Override
	public void inflate(Context context, ViewGroup container, boolean nightMode) {
		super.inflate(context, container, nightMode);
		AppCompatImageView rightIconView = view.findViewById(R.id.icon_right);
		if (rightIconView != null) {
			rightIconView.setImageDrawable(rightIcon);
		}
		progressBar = view.findViewById(R.id.progress_bar);
		updateDownloadingState();
	}

	public void setIsDownloading(boolean isDownloading) {
		this.isDownloading = isDownloading;
		updateDownloadingState();
	}

	private void updateDownloadingState() {
		AndroidUiHelper.updateVisibility(progressBar, isDownloading);
		AndroidUiHelper.updateVisibility(descriptionTv, !isDownloading);
		if (view != null) {
			view.invalidate();
		}
	}

	public static class Builder extends BottomSheetItemWithDescription.Builder {

		protected Drawable rightIcon;

		public BottomSheetItemWithDescription.Builder setRightIcon(Drawable icon) {
			this.rightIcon = icon;
			return this;
		}

		@Override
		public BottomSheetItemWithDescriptionTwoIcons create() {
			return new BottomSheetItemWithDescriptionTwoIcons(customView,
					layoutId,
					tag,
					disabled,
					onClickListener,
					position,
					icon,
					background,
					title,
					titleColorId,
					iconHidden,
					description,
					descriptionColorId,
					descriptionMaxLines,
					descriptionLinksClickable,
					rightIcon);
		}
	}
}