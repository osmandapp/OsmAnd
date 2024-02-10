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
		return getSelectedColoring().toHumanString(app);
	}

	@Override
	public boolean shouldShowMenuButton() {
		return getSupportedColorings().size() > 1;
	}

	@NonNull
	@Override
	public List<PopUpMenuItem> getMenuItems() {
		List<PopUpMenuItem> menuItems = new ArrayList<>();
		for (ColoringInfo coloringInfo : getSupportedColorings()) {
			menuItems.add(new PopUpMenuItem.Builder(app)
					.setTitle(coloringInfo.toHumanString(app))
					.showTopDivider(shouldShowTopDivider(coloringInfo))
					.setTag(coloringInfo)
					.create()
			);
		}
		return menuItems;
	}

	protected abstract boolean shouldShowTopDivider(@NonNull ColoringInfo coloringInfo);

	@NonNull
	protected abstract ColoringInfo getSelectedColoring();

	protected abstract void setSelectedColoring(@NonNull ColoringInfo coloringInfo);

	@NonNull
	protected abstract List<ColoringInfo> getSupportedColorings();

	@Override
	public boolean onMenuItemSelected(@NonNull PopUpMenuItem item) {
		ColoringInfo selectedColoring = getSelectedColoring();
		ColoringInfo newColoring = (ColoringInfo) item.getTag();
		if (!Objects.equals(selectedColoring, newColoring)) {
			setSelectedColoring(newColoring);
			return true;
		}
		return false;
	}
}
