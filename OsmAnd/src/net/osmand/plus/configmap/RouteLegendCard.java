package net.osmand.plus.configmap;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.track.fragments.TrackAppearanceFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;

import java.util.List;

public class RouteLegendCard extends BaseCard {
	private final LayoutInflater themedInflater;

	private final List<DataClass> items;
	private final String cardTitle;

	public RouteLegendCard(@NonNull FragmentActivity activity, @NonNull List<DataClass> items, @NonNull String cardTitle) {
		super(activity, true);
		this.items = items;
		this.cardTitle = cardTitle;
		themedInflater = UiUtilities.getInflater(activity, nightMode);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.route_legend_card;
	}

	@Override
	protected void updateContent() {
		setupCardTitle();
		setupLegendItems();
	}

	private void setupLegendItems() {
		ViewGroup mainContainer = view.findViewById(R.id.main_container);
		mainContainer.removeAllViews();
		for (int i = 0; i < items.size(); i++) {
			mainContainer.addView(createView(i));
		}
	}

	@NonNull
	private View createView(int position) {
		DataClass dataClass = items.get(position);
		View itemView = themedInflater.inflate(R.layout.route_legend_item, null, false);
		CompoundButton compoundButton = itemView.findViewById(R.id.compound_button);
		TextView title = itemView.findViewById(R.id.title);
		TextView description = itemView.findViewById(R.id.description);
		AndroidUiHelper.updateVisibility(description, false);
		View divider = itemView.findViewById(R.id.divider_bottom);
		ImageView icon = itemView.findViewById(R.id.icon);

		Drawable iconDrawable = TrackAppearanceFragment.getTrackIcon(app, null, false, dataClass.color);
		icon.setImageDrawable(iconDrawable);

		title.setText(dataClass.title());

		compoundButton.setChecked(isClassEnabled(dataClass));

		AndroidUiHelper.updateVisibility(divider, position != items.size() - 1);
		itemView.setOnClickListener(view -> {
			compoundButton.performClick();
			onClassSelected(dataClass, compoundButton.isChecked());
		});

		compoundButton.setFocusable(false);
		compoundButton.setClickable(false);

		OsmandSettings settings = app.getSettings();
		Drawable background = UiUtilities.getColoredSelectableDrawable(app, settings.getApplicationMode().getProfileColor(nightMode), 0.3f);
		AndroidUtils.setBackground(itemView, background);

		return itemView;
	}

	private void setupCardTitle() {
		TextView title = view.findViewById(R.id.card_title);
		title.setText(cardTitle);
	}

	public boolean isClassEnabled(@NonNull DataClass dataClass) {
		return false;
	}

	public void onClassSelected(@NonNull DataClass dataClass, boolean checked) {

	}

	public record DataClass(String title, int color) {
	}
}
