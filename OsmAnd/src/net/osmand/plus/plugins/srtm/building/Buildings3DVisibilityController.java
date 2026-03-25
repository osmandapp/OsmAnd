package net.osmand.plus.plugins.srtm.building;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.base.dialog.BaseDialogController;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.srtm.SRTMPlugin;

public class Buildings3DVisibilityController extends BaseDialogController {

	private static final String PROCESS_ID = "select_3d_buildings_visibility";

	private static final int MIN_VISIBILITY = 10;
	private static final int MAX_VISIBILITY = 100;

	private final SRTMPlugin plugin;
	private final int initialVisibility;
	private int visibility;
	private boolean applyChanges = false;

	public Buildings3DVisibilityController(@NonNull OsmandApplication app,
	                                       @NonNull SRTMPlugin plugin) {
		super(app);
		this.plugin = plugin;
		initialVisibility = (int) (plugin.BUILDINGS_3D_ALPHA.get() * 100);
		visibility = initialVisibility;
	}

	@NonNull
	@Override
	public String getProcessId() {
		return PROCESS_ID;
	}

	public void setVisibilityPercent(float percent) {
		visibility = (int) percent;
		float alpha = percent / 100;
		plugin.apply3DBuildingsAlpha(alpha);
		plugin.BUILDINGS_3D_ALPHA.set(alpha);
	}

	public void onApplyChanges() {
		applyChanges = true;
	}

	@Override
	public boolean finishProcessIfNeeded(@Nullable FragmentActivity activity) {
		if (super.finishProcessIfNeeded(activity)) {
			setVisibilityPercent(applyChanges ? visibility : initialVisibility);
			return true;
		}
		return false;
	}

	public float getMinVisibility() {
		return MIN_VISIBILITY;
	}

	public float getMaxVisibility() {
		return MAX_VISIBILITY;
	}

	public float getValidVisibility() {
		if (visibility < MIN_VISIBILITY) {
			return MIN_VISIBILITY;
		} else if (visibility > MAX_VISIBILITY) {
			return MAX_VISIBILITY;
		}
		return visibility;
	}

	protected boolean hasChanges() {
		return initialVisibility != visibility;
	}

	public static void showDialog(@NonNull OsmandApplication app,
	                              @NonNull FragmentManager fragmentManager) {
		SRTMPlugin srtmPlugin = PluginsHelper.getPlugin(SRTMPlugin.class);
		if (srtmPlugin != null) {
			Buildings3DVisibilityController controller =
					new Buildings3DVisibilityController(app, srtmPlugin);

			DialogManager dialogManager = app.getDialogManager();
			dialogManager.register(PROCESS_ID, controller);

			if (!Buildings3DVisibilityFragment.showInstance(fragmentManager)) {
				dialogManager.unregister(PROCESS_ID);
			}
		}
	}

	@Nullable
	public static Buildings3DVisibilityController getExistedInstance(@NonNull OsmandApplication app) {
		return (Buildings3DVisibilityController) app.getDialogManager().findController(PROCESS_ID);
	}
}
