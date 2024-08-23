package net.osmand.plus.quickaction;

import static net.osmand.plus.quickaction.ButtonAppearanceParams.DEFAULT_ICON_ID;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.card.icon.CircleIconPaletteElements;
import net.osmand.plus.card.icon.IconsPaletteCard;
import net.osmand.plus.card.icon.IconsPaletteController;
import net.osmand.plus.card.icon.IconsPaletteElements;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.mapwidgets.configure.buttons.QuickActionButtonState;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ButtonIconsCard extends MapBaseCard {

	private final QuickActionButtonState buttonState;
	private final ButtonAppearanceParams appearanceParams;
	private IconsPaletteElements<String> paletteElements;
	private IconsPaletteController<String> paletteController;

	@Override
	public int getCardLayoutId() {
		return R.layout.card_headed_content;
	}

	public ButtonIconsCard(@NonNull MapActivity activity,
	                       @NonNull QuickActionButtonState buttonState,
	                       @NonNull ButtonAppearanceParams appearanceParams) {
		super(activity);

		this.buttonState = buttonState;
		this.appearanceParams = appearanceParams;

		initPaletteController();
	}

	@Override
	protected void updateContent() {
		TextView title = view.findViewById(R.id.card_title);
		title.setText(R.string.shared_string_icon);

		ViewGroup container = view.findViewById(R.id.content_container);
		container.removeAllViews();

		paletteController.setIcons(getIconsNames());
		String iconName = appearanceParams.getIconName();
		if (!Algorithms.isEmpty(iconName)) {
			paletteController.setSelectedIcon(iconName);
		}
		container.addView(new IconsPaletteCard<>(activity, paletteController) {
			@Override
			public int getCardLayoutId() {
				return R.layout.icons_list;
			}
		}.build());

		AndroidUiHelper.updateVisibility(view.findViewById(R.id.card_summary), false);
		AndroidUtils.setPadding(container, 0, 0, 0, getDimen(R.dimen.content_padding));
		AndroidUtils.setBackground(view, new ColorDrawable(ColorUtilities.getCardAndListBackgroundColor(app, nightMode)));
	}

	@NonNull
	private List<String> getIconsNames() {
		Set<String> iconNames = new LinkedHashSet<>();
		iconNames.add(DEFAULT_ICON_ID);

		for (QuickAction action : buttonState.getQuickActions()) {
			int iconId = action.getIconRes();
			if (iconId > 0) {
				iconNames.add(app.getResources().getResourceEntryName(iconId));
			}
		}
		return new ArrayList<>(iconNames);
	}

	private void initPaletteController() {
		paletteController = new IconsPaletteController<>(app) {
			@NonNull
			public IconsPaletteElements<String> getPaletteElements(@NonNull Context context, boolean nightMode) {
				if (paletteElements == null || paletteElements.isNightMode() != nightMode) {
					paletteElements = new CircleIconPaletteElements<>(context, nightMode) {
						@Override
						protected Drawable getIconDrawable(@NonNull String iconName, boolean isSelected) {
							int iconId = AndroidUtils.getDrawableId(app, iconName);
							if (iconId <= 0) {
								iconId = RenderingIcons.getBigIconResourceId(iconName);
							}
							if (iconId <= 0) {
								iconId = R.drawable.ic_quick_action;
							}
							return getIcon(iconId, R.color.icon_color_default_light);
						}
					};
				}
				return paletteElements;
			}

			@Override
			public String getPaletteTitle() {
				return getString(R.string.shared_string_icon);
			}
		};
		paletteController.setPaletteListener(icon -> {
			appearanceParams.setIconName(icon);
			notifyCardPressed();
		});
	}
}