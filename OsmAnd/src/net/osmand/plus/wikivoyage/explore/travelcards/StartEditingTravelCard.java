package net.osmand.plus.wikivoyage.explore.travelcards;


import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.content.ContextCompat;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.OsmandActionBarActivity;

public class StartEditingTravelCard extends BaseTravelCard {

	public StartEditingTravelCard(OsmandActionBarActivity activity) {
		this.activity = activity;

	}

	public StartEditingTravelCard(OsmandActionBarActivity activity, int position) {
		this.activity = activity;
		this.position = position;
	}

	@Override
	public void inflate(OsmandApplication app, ViewGroup container, boolean nightMode) {
		View view = getView(app, container, isNightMode());
		ImageView imageView = (ImageView) view.findViewById(R.id.background_image);
		imageView.setImageDrawable(getBackgroundIcon(R.drawable.img_help_wikivoyage_contribute));
		((TextView) view.findViewById(R.id.title)).setText(getTitleId());
		((TextView) view.findViewById(R.id.description)).setText(getDescriptionId());

		((TextView) view.findViewById(R.id.left_bottom_button_text)).setText(getLeftButtonTextId());

		view.findViewById(R.id.left_bottom_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onLeftButtonClickAction();
			}
		});
		if (position != INVALID_POSITION) {
			container.addView(view, position);
		} else {
			container.addView(view);
		}
	}

	protected View getView(OsmandApplication app, ViewGroup parent, boolean nightMode) {
		if (view != null) {
			return view;
		}
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		return view = LayoutInflater.from(new ContextThemeWrapper(app, themeRes))
				.inflate(getLayoutId(), parent, false);
	}

	@Override
	protected int getTitleId() {
		return R.string.start_editing_card_image_text;
	}

	@Override
	protected int getDescriptionId() {
		return R.string.start_editing_card_description;
	}

	@Override
	protected Drawable getBackgroundIcon(int drawableRes) {
		return super.getBackgroundIcon(drawableRes);
	}

	@Override
	protected int getLayoutId() {
		return R.layout.wikivoyage_start_editing_card;
	}

	@Override
	protected int getLeftButtonTextId() {
		return R.string.start_editing;
	}

	@Override
	protected void onLeftButtonClickAction() {
		CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder()
				.setToolbarColor(ContextCompat.getColor(activity, isNightMode() ? R.color.actionbar_dark_color : R.color.actionbar_light_color))
				.build();
		String text = "https://" + activity.getMyApplication().getLanguage().toLowerCase() + ".m.wikivoyage.org";
		customTabsIntent.launchUrl(activity, Uri.parse(text));
	}
}
