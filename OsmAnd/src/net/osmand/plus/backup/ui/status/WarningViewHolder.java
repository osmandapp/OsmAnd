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

	public WarningViewHolder(@NonNull View itemView) {
		super(itemView);
		title = itemView.findViewById(R.id.title);
		icon = itemView.findViewById(R.id.icon);
		description = itemView.findViewById(R.id.description);
	}

	public void bindView(@NonNull BackupStatus status, @Nullable String error) {
		if (status.warningTitleRes != -1) {
			title.setText(status.warningTitleRes);
			description.setText(status.warningDescriptionRes);
		} else {
			title.setText(R.string.subscribe_email_error);
			description.setText(error);
		}
		icon.setImageDrawable(getContentIcon(status.warningIconRes));
		setupWarningRoundedBg(itemView.findViewById(R.id.warning_container));
	}

	private void setupWarningRoundedBg(@NonNull View view) {
		Context context = itemView.getContext();
		int activeColor = AndroidUtils.getColorFromAttr(context, R.attr.active_color_basic);
		int selectedColor = UiUtilities.getColorWithAlpha(activeColor, 0.3f);

		int backgroundColor = AndroidUtils.getColorFromAttr(context, R.attr.activity_background_color);
		Drawable bgDrawable = getPaintedIcon(R.drawable.rectangle_rounded, backgroundColor);
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
			Drawable selectable = getPaintedIcon(R.drawable.ripple_rectangle_rounded, selectedColor);
			Drawable[] layers = {bgDrawable, selectable};
			AndroidUtils.setBackground(view, new LayerDrawable(layers));
		} else {
			AndroidUtils.setBackground(view, bgDrawable);
		}
		LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.getLayoutParams();
		params.setMargins(params.leftMargin, AndroidUtils.dpToPx(context, 6), params.rightMargin, params.bottomMargin);
	}

	@Nullable
	private Drawable getContentIcon(@DrawableRes int icon) {
		OsmandApplication app = (OsmandApplication) itemView.getContext().getApplicationContext();
		return app.getUIUtilities().getIcon(icon, R.color.description_font_and_bottom_sheet_icons);
	}

	@Nullable
	private Drawable getPaintedIcon(@DrawableRes int id, @ColorInt int color) {
		OsmandApplication app = (OsmandApplication) itemView.getContext().getApplicationContext();
		return app.getUIUtilities().getPaintedIcon(id, color);
	}
}
