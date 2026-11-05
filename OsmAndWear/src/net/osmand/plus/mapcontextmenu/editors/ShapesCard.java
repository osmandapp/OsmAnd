package net.osmand.plus.mapcontextmenu.editors;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import net.osmand.data.BackgroundType;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.FlowLayout;

public class ShapesCard extends MapBaseCard {

	@NonNull
	private BackgroundType selectedShape;
	@ColorInt
	private int selectedColor;

	public ShapesCard(@NonNull MapActivity mapActivity, @NonNull BackgroundType shape, @ColorInt int color) {
		super(mapActivity);
		this.selectedShape = shape;
		this.selectedColor = color;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.shapes_card;
	}

	@NonNull
	public BackgroundType getSelectedShape() {
		return selectedShape;
	}

	public void updateSelectedShape(@ColorInt int color, @NonNull BackgroundType shape) {
		selectedColor = color;
		reselectShape(shape, false);
	}

	@Override
	protected void updateContent() {
		FlowLayout flowLayout = ((FlowLayout) view);
		flowLayout.setHorizontalAutoSpacing(true);

		int width = getDimen(R.dimen.favorites_select_icon_button_right_padding);
		for (BackgroundType shape : BackgroundType.values()) {
			if (shape.isSelected()) {
				FlowLayout.LayoutParams layoutParams = new FlowLayout.LayoutParams(width, 0);
				flowLayout.addView(createShapeItemView(shape), layoutParams);
			}
		}
	}

	@NonNull
	private View createShapeItemView(@NonNull BackgroundType shape) {
		View shapeItemView = themedInflater.inflate(R.layout.point_editor_button, ((ViewGroup) view), false);
		shapeItemView.setTag(shape);

		ImageView background = shapeItemView.findViewById(R.id.background);
		setUnselectedBackground(shape, background);
		background.setOnClickListener(v -> reselectShape(shape, true));

		ImageView outline = shapeItemView.findViewById(R.id.outline);
		outline.setImageDrawable(getOutlineDrawable(shape.getIconId()));

		return shapeItemView;
	}

	private Drawable getOutlineDrawable(@DrawableRes int shapeIconId) {
		Resources resources = app.getResources();
		String shapeIconName = resources.getResourceName(shapeIconId);
		String shapeBackgroundIconName = shapeIconName + "_contour";
		int iconRes = resources.getIdentifier(shapeBackgroundIconName, "drawable", app.getPackageName());
		return getColoredIcon(iconRes, ColorUtilities.getStrokedButtonsOutlineColorId(nightMode));
	}

	private void reselectShape(@NonNull BackgroundType shape, boolean notifyListener) {
		unselectOldShape(selectedShape);
		selectNewShape(shape);
		selectedShape = shape;
		if (notifyListener) {
			notifyCardPressed();
		}
	}

	private void unselectOldShape(@NonNull BackgroundType oldShape) {
		View oldShapeView = view.findViewWithTag(oldShape);
		if (oldShapeView != null) {
			oldShapeView.findViewById(R.id.outline).setVisibility(View.INVISIBLE);
			ImageView background = oldShapeView.findViewById(R.id.background);
			setUnselectedBackground(selectedShape, background);
		}
	}

	private void selectNewShape(@NonNull BackgroundType newShape) {
		View newShapeView = view.findViewWithTag(newShape);
		if (newShapeView != null) {
			newShapeView.findViewById(R.id.outline).setVisibility(View.VISIBLE);
			ImageView background = newShapeView.findViewById(R.id.background);
			background.setImageDrawable(getPaintedIcon(newShape.getIconId(), selectedColor));
		}
	}

	private void setUnselectedBackground(@NonNull BackgroundType backgroundType, @NonNull ImageView background) {
		int inactiveColorId = ColorUtilities.getInactiveButtonsAndLinksColorId(nightMode);
		Drawable inactiveIcon = getColoredIcon(backgroundType.getIconId(), inactiveColorId);
		background.setImageDrawable(inactiveIcon);
	}
}