package net.osmand.plus.mapcontextmenu.editors;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.data.BackgroundType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.card.base.multistate.BaseMultiStateCardController;
import net.osmand.plus.card.base.multistate.CardState;
import net.osmand.plus.card.base.simple.DescriptionCard;
import net.osmand.plus.configmap.tracks.appearance.favorite.FavoriteAppearanceController;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.List;

public class FavoriteShapesCardController extends BaseMultiStateCardController {

	private final FavoriteAppearanceController centralController;
	private ShapesCard shapesCard;

	public FavoriteShapesCardController(@NonNull OsmandApplication app, @NonNull FavoriteAppearanceController controller, @Nullable BackgroundType preselectedShape) {
		super(app);
		this.centralController = controller;
		this.selectedState = findCardState(preselectedShape);
	}

	@NonNull
	@Override
	protected List<CardState> collectSupportedCardStates() {
		List<CardState> list = new ArrayList<>();

		list.add(new CardState(R.string.shared_string_original));

		for (BackgroundType shape : BackgroundType.values()) {
			if (shape.isSelected()) {
				CardState state = new CardState(shape.getNameId());
				state.setTag(shape);
				list.add(state);
			}
		}

		return list;
	}

	@Override
	protected void onSelectCardState(@NonNull CardState cardState) {
		Object tag = cardState.getTag();
		BackgroundType shape = null;
		if (cardState.isOriginal()) {
			shapesCard = null;
		} else {
			shape = (BackgroundType) tag;
		}
		selectedState = cardState;
		centralController.setShape(shape);
		card.updateSelectedCardState();
	}

	@NonNull
	@Override
	public String getCardTitle() {
		return app.getString(R.string.shared_string_shape);
	}

	@NonNull
	@Override
	public String getCardStateSelectorTitle() {
		return selectedState.toHumanString(app);
	}

	@Override
	public void onBindCardContent(@NonNull FragmentActivity activity, @NonNull ViewGroup container, boolean nightMode, boolean usedOnMap) {
		container.removeAllViews();
		LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) container.getLayoutParams();
		if (selectedState.isOriginal()) {
			shapesCard = null;
			LayoutInflater inflater = UiUtilities.getInflater(activity, nightMode);
			inflater.inflate(R.layout.list_item_divider_with_padding_basic, container, true);

			params.setMargins(0, 0, 0, 0);
			container.setLayoutParams(params);
			container.addView(new DescriptionCard(activity, R.string.original_shape_description).build());
		} else {
			BackgroundType type = (BackgroundType) selectedState.getTag();
			if (type != null) {
				shapesCard = new ShapesCard(activity, type, centralController.requireColor()){

					@Override
					protected Drawable getOutlineDrawable(@DrawableRes int shapeIconId) {
						Resources resources = app.getResources();
						String shapeIconName = resources.getResourceName(shapeIconId);
						String shapeBackgroundIconName = shapeIconName + "_contour";
						int iconRes = resources.getIdentifier(shapeBackgroundIconName, "drawable", app.getPackageName());
						return getColoredIcon(iconRes, ColorUtilities.getActiveColorId(nightMode));
					}

					@Override
					public void setUnselectedBackground(@NonNull BackgroundType backgroundType, @NonNull ImageView background) {
						Drawable inactiveIcon = getPaintedIcon(backgroundType.getIconId(), selectedColor);
						background.setImageDrawable(inactiveIcon);
					}

				};
				shapesCard.setListener(centralController);

				Resources resources = app.getResources();
				int horizontalMargin = resources.getDimensionPixelSize(R.dimen.content_padding);
				int verticalMargin = resources.getDimensionPixelSize(R.dimen.content_padding_small);
				params.setMargins(horizontalMargin, 0, horizontalMargin, verticalMargin);
				container.setLayoutParams(params);

				container.addView(shapesCard.build());
				shapesCard.updateSelectedShape(centralController.requireColor(), centralController.requireShape());
			}
		}
	}

	@Nullable
	public BackgroundType getSelectedShape() {
		if (shapesCard != null) {
			return shapesCard.getSelectedShape();
		}

		return null;
	}

	public void updateContent() {
		if (shapesCard != null) {
			BackgroundType selectedShape = centralController.requireShape();
			selectedState = findCardState(selectedShape);
			shapesCard.updateSelectedShape(centralController.requireColor(), selectedShape);
			card.updateSelectedCardState();
		}
	}
}
