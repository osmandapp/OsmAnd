package net.osmand.plus.configmap;

import static net.osmand.render.RenderingRuleStorageProperties.ATTR_COLOR_VALUE;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.track.fragments.TrackAppearanceFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.render.RenderingClass;
import net.osmand.render.RenderingRule;
import net.osmand.render.RenderingRulesStorage;

import java.util.List;

public class RouteLegendCard extends BaseCard {


	private final LayoutInflater themedInflater;

	private final List<RenderingClass> items;
	private final String cardTitle;

	public RouteLegendCard(@NonNull FragmentActivity activity, @NonNull List<RenderingClass> items,
			@NonNull String cardTitle) {
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
		RenderingClass dataClass = items.get(position);
		View itemView = themedInflater.inflate(R.layout.route_legend_item, null, false);
		CompoundButton compoundButton = itemView.findViewById(R.id.compound_button);
		TextView title = itemView.findViewById(R.id.title);
		TextView description = itemView.findViewById(R.id.description);
		AndroidUiHelper.updateVisibility(description, false);
		View divider = itemView.findViewById(R.id.divider_bottom);
		ImageView icon = itemView.findViewById(R.id.icon);

		String colorName = dataClass.getColorName();
		Integer color = parseColor(app.getRendererRegistry().getCurrentSelectedRenderer(), colorName);
		if (color != null) {
			Drawable iconDrawable = TrackAppearanceFragment.getTrackIcon(app, null, false, color);
			icon.setImageDrawable(iconDrawable);
		}

		title.setText(dataClass.getTitle());

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

	public boolean isClassEnabled(@NonNull RenderingClass dataClass) {
		return false;
	}

	public void onClassSelected(@NonNull RenderingClass dataClass, boolean checked) {
		settings.getСustomBooleanRenderClassProperty(dataClass.getName(), dataClass.isEnable()).set(checked);
		refreshMap();
	}

	private void refreshMap() {
		if (activity instanceof MapActivity mapActivity) {
			mapActivity.refreshMapComplete();
			mapActivity.updateLayers();
		}
	}

	public static Integer parseColor(@NonNull RenderingRulesStorage routeRender, @Nullable String colorName) {
		if (colorName == null) {
			return null;
		}
		Integer color = null;

		RenderingRule colorRule = routeRender.getRenderingAttributeRule(colorName);
		List<RenderingRule> rules = colorRule.getIfElseChildren();
		for (RenderingRule rule : rules) {
			int colorValue = rule.getIntPropertyValue(ATTR_COLOR_VALUE);
			if (colorValue != -1) {
				color = colorValue;
			}
		}
		return color;
	}
}