package net.osmand.plus.wikivoyage.explore.travelcards;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

public class StartEditingTravelCard extends BaseTravelCard {

	public StartEditingTravelCard(OsmandApplication app, boolean nightMode) {
		super(app, nightMode);
	}

	@Override
	public void bindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		View view = LayoutInflater.from(new ContextThemeWrapper(app, themeRes))
				.inflate(R.layout.wikivoyage_open_beta_card, null, false);
		ImageView imageView = (ImageView) view.findViewById(R.id.background_image);
		imageView.setImageResource(R.drawable.img_help_wikivoyage_articles);
		((TextView) view.findViewById(R.id.title)).setText(R.string.welcome_to_open_beta);
		((TextView) view.findViewById(R.id.description)).setText(R.string.welcome_to_open_beta_description);
		((TextView) view.findViewById(R.id.left_bottom_button_text)).setText(R.string.get_unlimited_access);
		view.findViewById(R.id.left_bottom_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder()
						.setToolbarColor(ContextCompat.getColor(app, nightMode ? R.color.actionbar_dark_color : R.color.actionbar_light_color))
						.build();
				String text = "https://" + app.getLanguage().toLowerCase() + ".m.wikivoyage.org";
				customTabsIntent.launchUrl(app, Uri.parse(text));
			}
		});
	}

	@Override
	public int getCardType() {
		return 1;
	}
}
