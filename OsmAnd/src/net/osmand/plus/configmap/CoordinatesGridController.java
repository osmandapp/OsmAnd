package net.osmand.plus.configmap;

import static net.osmand.plus.utils.ColorUtilities.getPrimaryTextColor;

import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.OnResultCallback;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.containers.Limits;
import net.osmand.plus.base.dialog.BaseDialogController;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.utils.InsetsUtils.InsetSide;
import net.osmand.plus.views.layers.CoordinatesGridSettings;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.enums.EnumWithTitleId;
import net.osmand.plus.settings.enums.GridFormat;
import net.osmand.plus.settings.enums.GridLabelsPosition;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.plus.widgets.popup.PopUpMenuWidthMode;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Set;
import java.util.List;

public class CoordinatesGridController extends BaseDialogController {

	private static final String PROCESS_ID = "configure_coordinates_grid";

	private final CoordinatesGridSettings gridSettings;
	private ICoordinatesGridScreen screen;

	public CoordinatesGridController(@NonNull OsmandApplication app) {
		super(app);
		gridSettings = new CoordinatesGridSettings(app);
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
		return getString(gridFormat.getTitleId());
	}

	public void onFormatSelectorClicked(@NonNull View anchorView, @ColorInt int color, boolean nightMode) {
		showPopUpMenu(anchorView, GridFormat.values(), getGridFormat(), this::onSelectFormat, color, nightMode);
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
		GridZoomLevelsController.showDialog(activity, gridSettings);
	}

	@DrawableRes
	public int getSelectedLabelsPositionIcon() {
		GridLabelsPosition position = getLabelsPosition();
		return position.getIconId();
	}

	@NonNull
	public String getSelectedLabelsPositionName() {
		GridLabelsPosition position = getLabelsPosition();
		return app.getString(position.getTitleId());
	}

	public void onLabelsPositionSelectorClicked(@NonNull View anchorView, @ColorInt int color, boolean nightMode) {
		showPopUpMenu(anchorView, GridLabelsPosition.values(), getLabelsPosition(), this::onSelectLabelsPosition, color, nightMode);
	}

	private void onSelectLabelsPosition(@NonNull GridLabelsPosition position) {
		setLabelsPosition(position);
		if (screen != null) {
			screen.updateLabelsPositionButton();
		}
	}

	public void onSelectGridColorClicked(@NonNull MapActivity mapActivity) {
		GridColorController.showDialog(mapActivity, gridSettings);
	}

	private <T extends Enum<T> & EnumWithTitleId> void showPopUpMenu(
			@NonNull View anchorView, @NonNull T[] values, @NonNull T selectedValue,
			@NonNull OnResultCallback<T> callback, @ColorInt int controlsColor, boolean nightMode
	) {
		List<PopUpMenuItem> items = new ArrayList<>();
		for (T value : values) {
			items.add(new PopUpMenuItem.Builder(app)
					.setTitle(app.getString(value.getTitleId()))
					.setTitleColor(getPrimaryTextColor(app, nightMode))
					.setSelected(value == selectedValue)
					.showCompoundBtn(controlsColor)
					.setTag(value)
					.create()
			);
		}
		PopUpMenuDisplayData data = new PopUpMenuDisplayData();
		data.widthMode = PopUpMenuWidthMode.STANDARD;
		data.anchorView = anchorView;
		data.menuItems = items;
		data.nightMode = nightMode;
		data.onItemClickListener = item -> {
			@SuppressWarnings("unchecked")
			T selected = (T) item.getTag();
			callback.onResult(selected);
		};
		PopUpMenu.show(data);
	}

	public boolean isEnabled() {
		return gridSettings.isEnabled(getSelectedAppMode());
	}

	public void setEnabled(boolean enabled) {
		gridSettings.setEnabled(getSelectedAppMode(), enabled);
	}

	@NonNull
	public GridFormat getGridFormat() {
		return gridSettings.getGridFormat(getSelectedAppMode());
	}

	public void setGridFormat(@NonNull GridFormat format) {
		gridSettings.setGridFormat(getSelectedAppMode(), format);
	}

	@NonNull
	public Limits<Integer> getZoomLevels() {
		return gridSettings.getZoomLevelsWithRestrictions(getSelectedAppMode());
	}

	@NonNull
	public GridLabelsPosition getLabelsPosition() {
		return gridSettings.getGridLabelsPosition(getSelectedAppMode());
	}

	public void setLabelsPosition(@NonNull GridLabelsPosition position) {
		gridSettings.setGridLabelsPosition(getSelectedAppMode(), position);
	}

	@ColorInt
	public int getGridColor() {
		return gridSettings.getGridColor(getSelectedAppMode(), isNightMode());
	}

	@NonNull
	private ApplicationMode getSelectedAppMode() {
		return app.getSettings().getApplicationMode();
	}

	public boolean isNightMode() {
		return app.getDaynightHelper().isNightMode(ThemeUsageContext.MAP);
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

	@Nullable
	protected Set<InsetSide> getSideInsets() {
		return null;
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
