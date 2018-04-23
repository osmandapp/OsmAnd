package net.osmand.plus.wikivoyage.explore.travelcards;

import android.graphics.drawable.Drawable;
import android.support.v4.app.FragmentManager;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.dialogs.ChoosePlanDialogFragment;

public class OpenBetaTravelCard extends BaseTravelCard {

	private FragmentManager fm;

	public OpenBetaTravelCard(OsmandApplication app, FragmentManager fm, boolean nightMode) {
		this.app = app;
		this.fm = fm;
		this.nightMode = nightMode;
	}

	public OpenBetaTravelCard(OsmandApplication app, FragmentManager fm, int position, boolean nightMode) {
		this.app = app;
		this.fm = fm;
		this.position = position;
		this.nightMode = nightMode;
	}

	@Override
	public void inflate(OsmandApplication app, ViewGroup container, boolean nightMode) {
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		view = LayoutInflater.from(new ContextThemeWrapper(app, themeRes))
				.inflate(R.layout.wikivoyage_open_beta_card, container, false);
		ImageView imageView = (ImageView) view.findViewById(R.id.background_image);
		imageView.setImageDrawable(getIcon(R.drawable.img_help_wikivoyage_articles));
		((TextView) view.findViewById(R.id.title)).setText(R.string.welcome_to_open_beta);
		((TextView) view.findViewById(R.id.description)).setText(R.string.welcome_to_open_beta_description);
		((TextView) view.findViewById(R.id.left_bottom_button_text)).setText(R.string.get_unlimited_access);
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
	protected Drawable getIcon(int drawableRes) {
		return super.getIcon(drawableRes);
	}

	@Override
	protected int getLeftButtonTextId() {
		return R.string.get_unlimited_access;
	}

	@Override
	protected void onLeftButtonClickAction() {
		ChoosePlanDialogFragment.showFreeVersionInstance(fm);
	}
}
