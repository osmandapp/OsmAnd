package net.osmand.plus.backup.ui.status;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;

public class WarningViewHolder extends RecyclerView.ViewHolder {

	private final TextView title;
	private final ImageView icon;
	private final TextView description;
	private final View container;

	public WarningViewHolder(@NonNull View itemView) {
		super(itemView);
		title = itemView.findViewById(R.id.title);
		icon = itemView.findViewById(R.id.icon);
		description = itemView.findViewById(R.id.description);
		container = itemView.findViewById(R.id.warning_container);
	}

	public void bindView(@NonNull BackupStatus status, @Nullable String error, boolean hideBottomPadding) {
		if (status.warningTitleRes != -1) {
			title.setText(status.warningTitleRes);
			description.setText(status.warningDescriptionRes);
		} else {
			title.setText(R.string.subscribe_email_error);
			description.setText(error);
		}
		if (status != BackupStatus.SUBSCRIPTION_EXPIRED) {
			icon.setImageDrawable(getContentIcon(status.warningIconRes));
		} else {
			icon.setImageDrawable(getApplication().getUIUtilities().getIcon(status.warningIconRes));
		}
		setupWarningRoundedBg(hideBottomPadding);
	}

	private void setupWarningRoundedBg(boolean hideBottomPadding) {
		Context context = itemView.getContext();
		int activeColor = AndroidUtils.getColorFromAttr(context, R.attr.active_color_basic);
		int selectedColor = UiUtilities.getColorWithAlpha(activeColor, 0.3f);

		int backgroundColor = AndroidUtils.getColorFromAttr(context, R.attr.activity_background_color);
		Drawable bgDrawable = getPaintedIcon(R.drawable.rectangle_rounded, backgroundColor);
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
			Drawable selectable = getPaintedIcon(R.drawable.ripple_rectangle_rounded, selectedColor);
			Drawable[] layers = {bgDrawable, selectable};
			AndroidUtils.setBackground(container, new LayerDrawable(layers));
		} else {
			AndroidUtils.setBackground(container, bgDrawable);
		}
		int bottomMargin = hideBottomPadding ? 0 : context.getResources().getDimensionPixelSize(R.dimen.content_padding_half);
		LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) container.getLayoutParams();
		AndroidUtils.setMargins(params, params.leftMargin, params.topMargin, params.rightMargin, bottomMargin);
	}

	@NonNull
	private OsmandApplication getApplication() {
		return (OsmandApplication) itemView.getContext().getApplicationContext();
	}

	@Nullable
	private Drawable getContentIcon(@DrawableRes int icon) {
		OsmandApplication app = getApplication();
		return app.getUIUtilities().getIcon(icon, R.color.description_font_and_bottom_sheet_icons);
	}

	@Nullable
	private Drawable getPaintedIcon(@DrawableRes int id, @ColorInt int color) {
		OsmandApplication app = getApplication();
		return app.getUIUtilities().getPaintedIcon(id, color);
	}
}
