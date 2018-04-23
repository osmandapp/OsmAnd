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

public class StartEditingTravelCard extends BaseTravelCard {

	public StartEditingTravelCard(OsmandApplication app, boolean nightMode) {
		this.app = app;
		this.nightMode = nightMode;
	}

	public StartEditingTravelCard(OsmandApplication app, int position, boolean nightMode) {
		this.app = app;
		this.position = position;
		this.nightMode = nightMode;
	}

	@Override
	public void inflate(OsmandApplication app, ViewGroup container, boolean nightMode) {
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		View view = LayoutInflater.from(new ContextThemeWrapper(app, themeRes))
				.inflate(R.layout.wikivoyage_start_editing_card, container, false);
		ImageView imageView = (ImageView) view.findViewById(R.id.background_image);
		imageView.setImageDrawable(getIcon(R.drawable.img_help_wikivoyage_contribute));
		((TextView) view.findViewById(R.id.title)).setText(R.string.start_editing_card_image_text);
		((TextView) view.findViewById(R.id.description)).setText(R.string.start_editing_card_description);
		((TextView) view.findViewById(R.id.left_bottom_button_text)).setText(R.string.start_editing);
		view.findViewById(R.id.left_bottom_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onLeftButtonClickAction();
			}
		});
		if (getRightButtonTextId() == DEFAULT_VALUE) {
			view.findViewById(R.id.right_bottom_button).setVisibility(View.GONE);
			view.findViewById(R.id.bottom_buttons_divider).setVisibility(View.GONE);
		}
		if (position != INVALID_POSITION) {
			container.addView(view, position);
		} else {
			container.addView(view);
		}
	}

	@Override
	protected int getLeftButtonTextId() {
		return R.string.start_editing;
	}

	@Override
	protected void onLeftButtonClickAction() {
		CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder()
				.setToolbarColor(ContextCompat.getColor(app, nightMode ? R.color.actionbar_dark_color : R.color.actionbar_light_color))
				.build();
		String text = "https://" + app.getLanguage().toLowerCase() + ".m.wikivoyage.org";
		customTabsIntent.launchUrl(app, Uri.parse(text));
	}
}
