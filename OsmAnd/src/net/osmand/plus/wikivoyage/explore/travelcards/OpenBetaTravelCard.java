package net.osmand.plus.wikivoyage.explore.travelcards;

import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.RecyclerView;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.dialogs.ChoosePlanDialogFragment;

public class OpenBetaTravelCard extends BaseTravelCard {

	private FragmentManager fragmentManager;

	public OpenBetaTravelCard(OsmandApplication app, boolean nightMode, FragmentManager fragmentManager) {
		super(app, nightMode);
		this.fragmentManager = fragmentManager;
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
				ChoosePlanDialogFragment.showFreeVersionInstance(fragmentManager);
			}
		});
	}

	@Override
	public int getCardType() {
		return 0;
	}
}
