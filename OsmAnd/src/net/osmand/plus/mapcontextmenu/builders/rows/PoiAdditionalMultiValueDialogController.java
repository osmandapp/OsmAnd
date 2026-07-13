package net.osmand.plus.mapcontextmenu.builders.rows;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.base.dialog.BaseDialogController;
import net.osmand.plus.base.dialog.DialogManager;

import java.util.ArrayList;
import java.util.List;

public class PoiAdditionalMultiValueDialogController extends BaseDialogController {

	private static final String PROCESS_ID = "poi_additional_multi_value_selector";

	private final String title;
	private final ArrayList<String> values;
	private OnItemClickListener itemClickListener;

	public PoiAdditionalMultiValueDialogController(@NonNull OsmandApplication app,
	                                               @Nullable String title,
	                                               @NonNull ArrayList<String> values) {
		super(app);
		this.title = title != null ? title : "";
		this.values = values;
	}

	@NonNull
	@Override
	public String getProcessId() {
		return PROCESS_ID;
	}

	@NonNull
	public String getTitle() {
		return title;
	}

	@NonNull
	public List<String> getValues() {
		return values;
	}

	public void setItemClickListener(@NonNull OnItemClickListener listener) {
		itemClickListener = listener;
	}

	public void onItemClick(@NonNull FragmentActivity activity, @NonNull String value) {
		if (itemClickListener != null) {
			itemClickListener.onItemClick(activity, value);
		}
	}

	@Nullable
	public static PoiAdditionalMultiValueDialogController getExistedInstance(@NonNull OsmandApplication app) {
		return (PoiAdditionalMultiValueDialogController) app.getDialogManager().findController(PROCESS_ID);
	}

	public static void showDialog(@NonNull FragmentActivity activity,
	                              @Nullable String title,
	                              @NonNull ArrayList<String> values,
	                              @NonNull OnItemClickListener listener) {
		if (values.isEmpty()) {
			return;
		}
		OsmandApplication app = (OsmandApplication) activity.getApplication();
		PoiAdditionalMultiValueDialogController controller =
				new PoiAdditionalMultiValueDialogController(app, title, values);
		controller.setItemClickListener(listener);

		DialogManager dialogManager = app.getDialogManager();
		dialogManager.register(controller.getProcessId(), controller);

		if (!PoiAdditionalActionsBottomSheet.showInstance(activity.getSupportFragmentManager())) {
			dialogManager.unregister(controller.getProcessId());
		}
	}

	public interface OnItemClickListener {
		void onItemClick(@NonNull FragmentActivity activity, @NonNull String value);
	}
}
