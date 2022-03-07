package net.osmand.plus.views.mapwidgets.widgets;

import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.mapwidgets.RouteInfoWidgetsFactory;
import net.osmand.plus.views.mapwidgets.TurnDrawable;
import net.osmand.router.TurnType;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class NextTurnWidget extends TextInfoWidget {

	protected boolean horizontalMini;

	protected int deviatedPath = 0;
	protected int nextTurnDistance = 0;

	private final ImageView topImageView;
	private final TextView topTextView;
	private final ViewGroup bottomLayout;

	private final TurnDrawable turnDrawable;

	public NextTurnWidget(@NonNull MapActivity mapActivity, boolean horizontalMini) {
		super(mapActivity);
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
		if (RouteInfoWidgetsFactory.distChanged(deviatePath, this.deviatedPath)) {
			this.deviatedPath = deviatePath;
			updateDistance();
		}
	}

	public void setTurnDistance(int nextTurnDistance) {
		if (RouteInfoWidgetsFactory.distChanged(nextTurnDistance, this.nextTurnDistance)) {
			this.nextTurnDistance = nextTurnDistance;
			updateDistance();
		}
	}

	private void updateDistance() {
		int deviatePath = turnDrawable.isDeviatedFromRoute() ? deviatedPath : nextTurnDistance;
		String ds = OsmAndFormatter.getFormattedDistance(deviatePath, app);

		TurnType turnType = getTurnType();
		RoutingHelper routingHelper = app.getRoutingHelper();
		if ((turnType != null) && (routingHelper != null)) {
			setContentDescription(ds + " " + RouteCalculationResult.toString(turnType, app, false));
		} else {
			setContentDescription(ds);
		}

		int ls = ds.lastIndexOf(' ');
		if (ls == -1) {
			setTextNoUpdateVisibility(ds, null);
		} else {
			setTextNoUpdateVisibility(ds.substring(0, ls), ds.substring(ls + 1));
		}
	}

	@Override
	public void updateTextColor(int textColor, int textShadowColor, boolean bold, int rad) {
		super.updateTextColor(textColor, textShadowColor, bold, rad);
		updateTextColor(topTextView, null, textColor, textShadowColor, bold, rad);
	}
}