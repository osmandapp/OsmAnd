package net.osmand.plus.configmap;

import static net.osmand.plus.utils.ColorUtilities.getPrimaryTextColor;

import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.containers.Limits;
import net.osmand.plus.base.dialog.BaseDialogController;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.GridFormat;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.plus.widgets.popup.PopUpMenuWidthMode;

import java.util.ArrayList;
import java.util.List;

public class CoordinatesGridController extends BaseDialogController {

	private static final String PROCESS_ID = "configure_coordinates_grid";

	private final OsmandSettings settings;
	private ICoordinatesGridScreen screen;

	/**
	 * If true, the controller remains active when the zoom levels screen opens
	 */
	private boolean keepActive;

	public void bindScreen(@NonNull ICoordinatesGridScreen screen) {
		this.screen = screen;
		keepActive = false;
	}

	public CoordinatesGridController(@NonNull OsmandApplication app) {
		super(app);
		settings = app.getSettings();
	}

	@NonNull
	@Override
	public String getProcessId() {
		return PROCESS_ID;
	}

	@NonNull
	public String getSelectedFormatName() {
		GridFormat gridFormat = getGridFormat();
		return gridFormat.getTitle(app);
	}

	public void onFormatSelectorClicked(@NonNull View anchorView,
	                                    @ColorInt int activeColor, boolean nightMode) {
		GridFormat gridFormat = getGridFormat();
		List<PopUpMenuItem> items = new ArrayList<>();
		for (GridFormat format : GridFormat.values()) {
			items.add(new PopUpMenuItem.Builder(app)
					.setTitle(format.getTitle(app))
					.showTopDivider(format == GridFormat.UTM)
					.setTitleColor(getPrimaryTextColor(app, nightMode))
					.setSelected(gridFormat == format)
					.showCompoundBtn(activeColor)
					.setTag(format)
					.create()
			);
		}
		PopUpMenuDisplayData data = new PopUpMenuDisplayData();
		data.widthMode = PopUpMenuWidthMode.STANDARD;
		data.anchorView = anchorView;
		data.menuItems = items;
		data.nightMode = nightMode;
		data.onItemClickListener = item -> onSelectFormat((GridFormat) item.getTag());
		PopUpMenu.show(data);
	}

	private void onSelectFormat(@NonNull GridFormat format) {
		setGridFormat(format);
		if (screen != null) {
			screen.updateFormatButton();
		}
	}

	@NonNull
	public String getFormattedZoomLevels() {
		Limits<Integer> zoomLevels = getZoomLevels();
		String min = String.valueOf(zoomLevels.min());
		String max = String.valueOf(zoomLevels.max());
		return getString(R.string.ltr_or_rtl_combine_via_dash, min, max);
	}

	public void onZoomLevelsClicked(@NonNull MapActivity activity) {
		keepActive = true;
		GridZoomLevelsController.showDialog(activity);
	}

	public boolean isEnabled() {
		return settings.SHOW_COORDINATES_GRID.get();
	}

	public void setEnabled(boolean enabled) {
		settings.SHOW_COORDINATES_GRID.set(enabled);
	}

	@NonNull
	public GridFormat getGridFormat() {
		return settings.COORDINATE_GRID_FORMAT.get();
	}

	public void setGridFormat(@Nullable GridFormat format) {
		settings.COORDINATE_GRID_FORMAT.set(format);
	}

	@NonNull
	public Limits<Integer> getZoomLevels() {
		int min = settings.COORDINATE_GRID_MIN_ZOOM.get();
		int max = settings.COORDINATE_GRID_MAX_ZOOM.get();
		return new Limits<>(min, max);
	}

	@Override
	public void finishProcessIfNeeded(@Nullable FragmentActivity activity) {
		if (!keepActive) {
			super.finishProcessIfNeeded(activity);
		}
		screen = null;
	}

	@DrawableRes
	public static int getStateIcon(boolean selected) {
		return selected ?
				R.drawable.ic_action_world_globe :
				R.drawable.ic_action_coordinates_grid_disabled;
	}

	@NonNull
	public static CoordinatesGridController getOrCreateInstance(@NonNull OsmandApplication app) {
		CoordinatesGridController controller = getExistedInstance(app);
		return controller != null ? controller : new CoordinatesGridController(app);
	}

	@Nullable
	public static CoordinatesGridController getExistedInstance(@NonNull OsmandApplication app) {
		DialogManager dialogManager = app.getDialogManager();
		return (CoordinatesGridController) dialogManager.findController(PROCESS_ID);
	}

	public static void showDialog(@NonNull FragmentActivity activity) {
		OsmandApplication app = (OsmandApplication) activity.getApplicationContext();
		CoordinatesGridController controller = getOrCreateInstance(app);

		DialogManager dialogManager = app.getDialogManager();
		dialogManager.register(controller.getProcessId(), controller);

		FragmentManager manager = activity.getSupportFragmentManager();
		CoordinatesGridFragment.showInstance(manager);
	}
}
