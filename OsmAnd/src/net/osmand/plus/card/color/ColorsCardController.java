package net.osmand.plus.card.color;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.card.base.multistate.IMultiStateCardController;
import net.osmand.plus.widgets.popup.PopUpMenuItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class ColorsCardController implements IMultiStateCardController {

	protected final OsmandApplication app;

	public ColorsCardController(@NonNull OsmandApplication app) {
		this.app = app;
	}

	@NonNull
	@Override
	public String getCardTitle() {
		return app.getString(R.string.shared_string_color);
	}

	@NonNull
	@Override
	public String getMenuButtonTitle() {
		return getSelectedColoringType().toHumanString(app);
	}

	@Override
	public boolean shouldShowMenuButton() {
		return getSupportedColoringTypes().size() > 1;
	}

	@NonNull
	@Override
	public List<PopUpMenuItem> getMenuItems() {
		List<PopUpMenuItem> menuItems = new ArrayList<>();
		for (ColoringTypeWrapper coloringType : getSupportedColoringTypes()) {
			menuItems.add(new PopUpMenuItem.Builder(app)
					.setTitle(coloringType.toHumanString(app))
					.showTopDivider(shouldShowTopDivider(coloringType))
					.setTag(coloringType)
					.create()
			);
		}
		return menuItems;
	}

	protected abstract boolean shouldShowTopDivider(@NonNull ColoringTypeWrapper coloringType);

	@NonNull
	protected abstract ColoringTypeWrapper getSelectedColoringType();

	protected abstract void setSelectedColoringType(@NonNull ColoringTypeWrapper coloringType);

	@NonNull
	protected abstract List<ColoringTypeWrapper> getSupportedColoringTypes();

	@Override
	public boolean onMenuItemSelected(@NonNull PopUpMenuItem item) {
		ColoringTypeWrapper selectedColoringType = getSelectedColoringType();
		ColoringTypeWrapper coloringType = (ColoringTypeWrapper) item.getTag();
		if (!Objects.equals(selectedColoringType, coloringType)) {
			setSelectedColoringType(coloringType);
			return true;
		}
		return false;
	}
}
