package net.osmand.plus.mapcontextmenu.other;

import androidx.annotation.Nullable;

import net.osmand.NativeLibrary;
import net.osmand.OnCompleteCallback;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.mapcontextmenu.MenuController.MenuType;
import net.osmand.plus.mapcontextmenu.MenuTitleController;

public class MenuObject extends MenuTitleController {

	private final LatLon latLon;
	private final PointDescription pointDescription;
	private final Object object;
	private int order;

	@Nullable
	private MapActivity mapActivity;
	@Nullable
	private MenuController controller;
	@Nullable
	private OnCompleteCallback onSearchAddressDone;

	MenuObject(LatLon latLon, PointDescription pointDescription, Object object, @Nullable MapActivity mapActivity) {
		this.latLon = latLon;
		this.pointDescription = pointDescription;
		this.object = object;
		this.mapActivity = mapActivity;
		if (object instanceof NativeLibrary.RenderedObject) {
			this.order = ((NativeLibrary.RenderedObject) object).getOrder();
		}
		init();
	}

	protected void init() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			controller = MenuController.getMenuController(mapActivity, latLon, pointDescription, object, MenuType.MULTI_LINE);
			controller.setActive(true);
			initTitle();
		}
	}

	protected void deinit() {
		controller = null;
	}

	public void setOnSearchAddressDoneCallback(@Nullable OnCompleteCallback onSearchAddressDone) {
		this.onSearchAddressDone = onSearchAddressDone;
	}

	@Override
	public LatLon getLatLon() {
		return latLon;
	}

	@Override
	public PointDescription getPointDescription() {
		return pointDescription;
	}

	@Override
	public Object getObject() {
		return object;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Nullable
	@Override
	public MapActivity getMapActivity() {
		return mapActivity;
	}

	public void setMapActivity(@Nullable MapActivity mapActivity) {
		this.mapActivity = mapActivity;
	}

	@Override
	public MenuController getMenuController() {
		return controller;
	}

	@Override
	protected boolean needStreetName() {
		return controller != null && controller.needStreetName();
	}

	@Override
	protected void onSearchAddressDone() {
		if (onSearchAddressDone != null) {
			onSearchAddressDone.onComplete();
		}
	}
}
