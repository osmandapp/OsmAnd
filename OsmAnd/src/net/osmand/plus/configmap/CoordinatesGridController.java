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
import net.osmand.plus.helpers.CoordinatesGridHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.enums.GridFormat;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.plus.widgets.popup.PopUpMenuWidthMode;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class CoordinatesGridController extends BaseDialogController {

	private static final String PROCESS_ID = "configure_coordinates_grid";

	private final CoordinatesGridHelper gridHelper;
	private ICoordinatesGridScreen screen;

	public CoordinatesGridController(@NonNull OsmandApplication app) {
		super(app);
		gridHelper = app.getOsmandMap().getMapView().getGridHelper();
	}

	public void bindScreen(@NonNull ICoordinatesGridScreen screen) {
		this.screen = screen;
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
			screen.updateZoomLevelsButton();
		}
	}

	@NonNull
	public String getFormattedZoomLevels() {
		Limits<Integer> zoomLevels = getZoomLevels();
		NumberFormat numberFormat = OsmAndFormatter.getNumberFormat(app);
		String min = numberFormat.format(zoomLevels.min());
		String max = numberFormat.format(zoomLevels.max());
		return getString(R.string.ltr_or_rtl_combine_via_dash, min, max);
	}

	public void onZoomLevelsClicked(@NonNull MapActivity activity) {
		GridZoomLevelsController.showDialog(activity);
	}

	public boolean isEnabled() {
		return gridHelper.isEnabled(getSelectedAppMode());
	}

	public void setEnabled(boolean enabled) {
		gridHelper.setEnabled(getSelectedAppMode(), enabled);
	}

	@NonNull
	public GridFormat getGridFormat() {
		return gridHelper.getGridFormat(getSelectedAppMode());
	}

	public void setGridFormat(@NonNull GridFormat format) {
		gridHelper.setGridFormat(getSelectedAppMode(), format);
	}

	@NonNull
	public Limits<Integer> getZoomLevels() {
		return gridHelper.getZoomLevelsWithRestrictions(getSelectedAppMode());
	}

	@NonNull
	private ApplicationMode getSelectedAppMode() {
		return app.getSettings().getApplicationMode();
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
