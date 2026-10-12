package net.osmand.plus.card.icon;

import static net.osmand.plus.utils.ColorUtilities.getDefaultIconColor;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;

import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

public abstract class CircleIconPaletteElements<IconData> extends IconsPaletteElements<IconData> {

	public CircleIconPaletteElements(@NonNull Context context, boolean nightMode) {
		super(context, nightMode);
	}

	@Override
	protected int getLayoutId() {
		return R.layout.preference_circle_item;
	}

	@Override
	public void bindView(@NonNull View itemView, @NonNull IconData icon, int controlsColor, boolean isSelected) {
		View background = itemView.findViewById(R.id.background);
		int bgColor = getDefaultIconColor(app, nightMode);
		int bgColorWithAlpha = ColorUtilities.getColorWithAlpha(bgColor, 0.1f);
		Drawable bgDrawable = AppCompatResources.getDrawable(app, R.drawable.circle_background_light);
		AndroidUtils.setBackground(background, UiUtilities.tintDrawable(bgDrawable, bgColorWithAlpha));

		ImageView outlineCircle = itemView.findViewById(R.id.outline);
		if (isSelected) {
			GradientDrawable circleContourDrawable = (GradientDrawable)
					AppCompatResources.getDrawable(app, R.drawable.circle_contour_bg_light);
			if (circleContourDrawable != null) {
				int activeColor = ColorUtilities.getActiveColor(app, nightMode);
				circleContourDrawable.setStroke(AndroidUtils.dpToPx(app, 2), activeColor);
			}
			outlineCircle.setImageDrawable(circleContourDrawable);
			outlineCircle.setVisibility(View.VISIBLE);
		} else {
			outlineCircle.setVisibility(View.INVISIBLE);
		}

		ImageView checkMark = itemView.findViewById(R.id.checkMark);
		checkMark.setImageDrawable(getIconDrawable(icon, isSelected));
	}

	protected abstract Drawable getIconDrawable(@NonNull IconData icon, boolean isSelected);
}
