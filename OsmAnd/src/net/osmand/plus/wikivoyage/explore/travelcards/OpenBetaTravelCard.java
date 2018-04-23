package net.osmand.plus.wikivoyage.explore.travelcards;

import android.graphics.drawable.Drawable;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.OsmandActionBarActivity;
import net.osmand.plus.dialogs.ChoosePlanDialogFragment;

public class OpenBetaTravelCard extends BaseTravelCard {


	public OpenBetaTravelCard(OsmandActionBarActivity activity) {
		this.activity = activity;
	}

	public OpenBetaTravelCard(OsmandActionBarActivity activity, int position) {
		this.activity = activity;
		this.position = position;
	}

	@Override
	public void inflate(OsmandApplication app, ViewGroup container, boolean nightMode) {
		View view = getView(app, container, isNightMode());
		ImageView imageView = (ImageView) view.findViewById(R.id.background_image);
		imageView.setImageDrawable(getBackgroundIcon(R.drawable.img_help_wikivoyage_articles));
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
		return R.string.welcome_to_open_beta;
	}

	@Override
	protected int getDescriptionId() {
		return R.string.welcome_to_open_beta_description;
	}

	@Override
	protected Drawable getBackgroundIcon(int drawableRes) {
		return super.getBackgroundIcon(drawableRes);
	}

	@Override
	protected int getLayoutId() {
		return R.layout.wikivoyage_open_beta_card;
	}

	@Override
	protected int getLeftButtonTextId() {
		return R.string.get_unlimited_access;
	}

	@Override
	protected void onLeftButtonClickAction() {
		ChoosePlanDialogFragment.showFreeVersionInstance(activity.getSupportFragmentManager());

	}
}
