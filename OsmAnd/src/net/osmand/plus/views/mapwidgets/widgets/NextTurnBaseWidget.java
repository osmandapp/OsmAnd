package net.osmand.plus.views.mapwidgets.widgets;

import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.layers.MapInfoLayer.TextState;
import net.osmand.plus.views.mapwidgets.TurnDrawable;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.router.TurnType;

public class NextTurnBaseWidget extends TextInfoWidget {

	private static final int DISTANCE_CHANGE_THRESHOLD = 10;

	protected boolean horizontalMini;

	protected int deviatedPath;
	protected int nextTurnDistance;

	private final ImageView topImageView;
	private final TextView topTextView;
	private final ViewGroup bottomLayout;

	private final TurnDrawable turnDrawable;

	public NextTurnBaseWidget(@NonNull MapActivity mapActivity, @Nullable WidgetType widgetType, boolean horizontalMini) {
		super(mapActivity, widgetType);
		this.horizontalMini = horizontalMini;

		topImageView = view.findViewById(R.id.widget_top_icon);
		topTextView = view.findViewById(R.id.widget_top_icon_text);
		bottomLayout = view.findViewById(R.id.widget_bottom_layout);

		turnDrawable = new TurnDrawable(mapActivity, horizontalMini);
		if (horizontalMini) {
			setImageDrawable(turnDrawable, false);
			setTopImageDrawable(null, null);
		} else {
			setImageDrawable(null, true);
			setTopImageDrawable(turnDrawable, "");
		}

		updateVisibility(false);
	}

	public TurnType getTurnType() {
		return turnDrawable.getTurnType();
	}

	public void setTurnType(TurnType turnType) {
		boolean vis = updateVisibility(turnType != null);
		if (turnDrawable.setTurnType(turnType) || vis) {
			turnDrawable.setTextPaint(topTextView.getPaint());
			if (horizontalMini) {
				setImageDrawable(turnDrawable, false);
			} else {
				setTopImageDrawable(turnDrawable, "");
			}
		}
	}

	public void setTopImageDrawable(@Nullable Drawable imageDrawable, @Nullable String topText) {
		boolean hasImage = imageDrawable != null;
		if (hasImage) {
			topImageView.setImageDrawable(imageDrawable);
			topTextView.setText(topText == null ? "" : topText);

			LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) bottomLayout.getLayoutParams();
			lp.gravity = Gravity.CENTER_HORIZONTAL;
			bottomLayout.setLayoutParams(lp);
			bottomLayout.invalidate();
		} else {
			LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) bottomLayout.getLayoutParams();
			lp.gravity = Gravity.NO_GRAVITY;
			bottomLayout.setLayoutParams(lp);
		}

		AndroidUiHelper.updateVisibility(topImageView, hasImage);
		AndroidUiHelper.updateVisibility(topTextView, hasImage);

		topTextView.invalidate();
		topImageView.invalidate();
	}

	public void setTurnImminent(int turnImminent, boolean deviatedFromRoute) {
		if (turnDrawable.getTurnImminent() != turnImminent || turnDrawable.isDeviatedFromRoute() != deviatedFromRoute) {
			turnDrawable.setTurnImminent(turnImminent, deviatedFromRoute);
		}
	}

	public void setDeviatePath(int deviatePath) {
		if (isDistanceChanged(this.deviatedPath, deviatePath)) {
			this.deviatedPath = deviatePath;
			updateDistance();
		}
	}

	public void setTurnDistance(int nextTurnDistance) {
		if (isDistanceChanged(this.nextTurnDistance, nextTurnDistance)) {
			this.nextTurnDistance = nextTurnDistance;
			updateDistance();
		}
	}

	private boolean isDistanceChanged(int oldDistance, int distance) {
		return oldDistance == 0 || Math.abs(oldDistance - distance) >= DISTANCE_CHANGE_THRESHOLD;
	}

	private void updateDistance() {
		int deviatePath = turnDrawable.isDeviatedFromRoute() ? deviatedPath : nextTurnDistance;
		String distance = OsmAndFormatter.getFormattedDistance(deviatePath, app, OsmAndFormatter.OsmAndFormatterParams.USE_LOWER_BOUNDS);

		TurnType turnType = getTurnType();
		if (turnType != null) {
			setContentDescription(distance + " " + RouteCalculationResult.toString(turnType, app, false));
		} else {
			setContentDescription(distance);
		}

		int ls = distance.lastIndexOf(' ');
		if (ls == -1) {
			setTextNoUpdateVisibility(distance, null);
		} else {
			setTextNoUpdateVisibility(distance.substring(0, ls), distance.substring(ls + 1));
		}
	}

	@Override
	public void updateColors(@NonNull TextState textState) {
		super.updateColors(textState);
		updateTextColor(topTextView, null, textState.textColor, textState.textShadowColor, textState.textBold,
				textState.textShadowRadius);
	}
}