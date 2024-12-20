package net.osmand.plus.quickaction;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import com.google.android.material.slider.Slider;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.card.base.multistate.CardState;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;

import java.util.ArrayList;
import java.util.List;

public abstract class SliderButtonsCard extends MapBaseCard {

	protected Slider slider;
	protected TextView title;
	protected TextView valueTv;
	protected TextView description;
	protected ImageButton increaseButton;
	protected ImageButton decreaseButton;

	protected boolean showOriginal;

	@Override
	public int getCardLayoutId() {
		return R.layout.slider_with_buttons;
	}

	public SliderButtonsCard(@NonNull MapActivity activity, boolean showOriginal) {
		super(activity, false);
		this.showOriginal = showOriginal;
	}

	@Override
	protected void updateContent() {
		setupHeader(view);
		setupSlider(view);
		setupDescription(view);
		setupButtons(view);
	}

	protected void setupHeader(@NonNull View view) {
		View container = view.findViewById(R.id.header_container);
		title = container.findViewById(R.id.card_title);
		valueTv = container.findViewById(R.id.title);

		View selector = view.findViewById(R.id.card_selector);
		if (showOriginal) {
			selector.setOnClickListener(v -> showMenu(selector));
		} else {
			container.findViewById(R.id.header).getLayoutParams().height = WRAP_CONTENT;
		}
		AndroidUiHelper.updateVisibility(selector.findViewById(R.id.drop_down_icon), showOriginal);
	}

	protected void setupDescription(@NonNull View view) {
		View container = view.findViewById(R.id.description_container);
		description = container.findViewById(R.id.summary);
		AndroidUiHelper.updateVisibility(container, showOriginal && isOriginalValue());
	}

	protected void setupSlider(@NonNull View view) {
		slider = view.findViewById(R.id.slider);
		slider.addOnChangeListener((s, value, fromUser) -> {
			if (fromUser) {
				onValueSelected(value);
			}
		});
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.slider_container), !showOriginal || !isOriginalValue());
	}

	protected void setupButtons(@NonNull View view) {
		increaseButton = view.findViewById(R.id.increase_button);
		increaseButton.setImageDrawable(getPersistentPrefIcon(R.drawable.ic_zoom_in));
		increaseButton.setOnClickListener(v -> {
			int value = (int) (slider.getValue() + slider.getStepSize());
			if (value <= slider.getValueTo()) {
				slider.setValue(value);
				onValueSelected(value);
			}
		});
		decreaseButton = view.findViewById(R.id.decrease_button);
		decreaseButton.setImageDrawable(getPersistentPrefIcon(R.drawable.ic_zoom_out));
		decreaseButton.setOnClickListener(v -> {
			int value = (int) (slider.getValue() - slider.getStepSize());
			if (value >= slider.getValueFrom()) {
				slider.setValue(value);
				onValueSelected(value);
			}
		});
	}

	protected void onValueSelected(float value) {
		increaseButton.setEnabled(value < slider.getValueTo());
		decreaseButton.setEnabled(value > slider.getValueFrom());
	}

	public void showMenu(@NonNull View view) {
		List<PopUpMenuItem> items = new ArrayList<>();
		for (CardState state : getCardStates()) {
			items.add(new PopUpMenuItem.Builder(app)
					.setTitle(state.toHumanString(app))
					.showTopDivider(state.isShowTopDivider())
					.setTitleColor(ColorUtilities.getPrimaryTextColor(app, nightMode))
					.setTag(state)
					.create()
			);
		}
		PopUpMenuDisplayData data = new PopUpMenuDisplayData();
		data.anchorView = view;
		data.menuItems = items;
		data.nightMode = nightMode;
		data.onItemClickListener = item -> setSelectedState((CardState) item.getTag());
		PopUpMenu.show(data);
	}


	protected abstract boolean isOriginalValue();

	@NonNull
	protected abstract String getFormattedValue(float value);

	@NonNull
	protected abstract List<CardState> getCardStates();

	protected abstract void setSelectedState(@NonNull CardState cardState);

	@NonNull
	protected Drawable getPersistentPrefIcon(@DrawableRes int iconId) {
		Drawable enabled = UiUtilities.createTintedDrawable(app, iconId, ColorUtilities.getActiveColor(app, nightMode));
		Drawable disabled = UiUtilities.createTintedDrawable(app, iconId, ColorUtilities.getSecondaryIconColor(app, nightMode));

		return AndroidUtils.createEnabledStateListDrawable(disabled, enabled);
	}
}
