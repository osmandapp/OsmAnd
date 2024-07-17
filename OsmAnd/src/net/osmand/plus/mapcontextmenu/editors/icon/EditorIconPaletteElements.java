package net.osmand.plus.mapcontextmenu.editors.icon;

import static net.osmand.plus.utils.ColorUtilities.getDefaultIconColor;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;

import net.osmand.plus.R;
import net.osmand.plus.card.icon.IconsPaletteElements;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

public class EditorIconPaletteElements extends IconsPaletteElements<String> {

	public EditorIconPaletteElements(@NonNull Context context, boolean nightMode) {
		super(context, nightMode);
	}

	@Override
	protected int getLayoutId() {
		return R.layout.preference_circle_item;
	}

	@Override
	public void bindView(@NonNull View itemView, @NonNull String icon, int controlsColor, boolean isSelected) {
		int iconId = getIconId(icon);
		View background = itemView.findViewById(R.id.background);
		int bgColor = isSelected ? controlsColor : getDefaultIconColor(app, nightMode);
		int bgColorWithAlpha = ColorUtilities.getColorWithAlpha(bgColor, 0.1f);
		Drawable bgDrawable = AppCompatResources.getDrawable(app, R.drawable.circle_background_light);
		AndroidUtils.setBackground(background, UiUtilities.tintDrawable(bgDrawable, bgColorWithAlpha));

		ImageView outlineCircle = itemView.findViewById(R.id.outline);
		if (isSelected) {
			GradientDrawable circleContourDrawable = (GradientDrawable)
					AppCompatResources.getDrawable(app, R.drawable.circle_contour_bg_light);
			if (circleContourDrawable != null) {
				circleContourDrawable.setStroke(AndroidUtils.dpToPx(app, 2), controlsColor);
			}
			outlineCircle.setImageDrawable(circleContourDrawable);
			outlineCircle.setVisibility(View.VISIBLE);
		} else {
			outlineCircle.setVisibility(View.GONE);
		}

		ImageView checkMark = itemView.findViewById(R.id.checkMark);
		if (isSelected) {
			checkMark.setImageDrawable(getPaintedIcon(iconId, controlsColor));
		} else {
			checkMark.setImageDrawable(getIcon(iconId, R.color.icon_color_default_light));
		}
	}

	@DrawableRes
	private int getIconId(@NonNull String iconKey) {
		return RenderingIcons.getBigIconResourceId(iconKey);
	}
}
