package net.osmand.plus.plugins.osmedit.helpers;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.measurementtool.LoginBottomSheetFragment;
import net.osmand.plus.plugins.osmedit.OsmEditsUploadListener;
import net.osmand.plus.plugins.osmedit.data.OsmPoint;
import net.osmand.plus.plugins.osmedit.dialogs.UploadingErrorDialogFragment;
import net.osmand.plus.plugins.osmedit.dialogs.UploadingMultipleErrorDialogFragment;

import org.apache.commons.logging.Log;

import java.text.MessageFormat;
import java.util.Map;
import java.util.Objects;

public class OsmEditsUploadListenerHelper implements OsmEditsUploadListener {
	private static final Log LOG = PlatformUtil.getLog(OsmEditsUploadListenerHelper.class);

	private final OsmandApplication app;
	private final FragmentActivity activity;
	private final String numberFormat;

	public OsmEditsUploadListenerHelper(FragmentActivity activity, String numberFormat) {
		this.activity = activity;
		this.app = (OsmandApplication) activity.getApplicationContext();
		this.numberFormat = numberFormat;
	}

	@Override
	public void uploadUpdated(@NonNull OsmPoint point) {
	}

	@MainThread
	@Override
	public void uploadEnded(@NonNull Map<OsmPoint, String> loadErrorsMap) {
		if (activity.getSupportFragmentManager().isStateSaved()) {
			return;
		}
		int uploaded = 0;
		int pointsNum = loadErrorsMap.keySet().size();
		for (String s : loadErrorsMap.values()) {
			if (s == null) {
				uploaded++;
			}
		}
		if (uploaded == pointsNum) {
			app.showToastMessage(MessageFormat.format(numberFormat, uploaded));
		} else if (pointsNum == 1) {
			LOG.debug("in if1");
			OsmPoint point = loadErrorsMap.keySet().iterator().next();
			String message = loadErrorsMap.get(point);
			if (Objects.equals(message, activity.getString(R.string.auth_failed))) {
				LoginBottomSheetFragment.showInstance(activity.getSupportFragmentManager(), null);
			} else {
				UploadingErrorDialogFragment.showInstance(activity.getSupportFragmentManager(), message, point);
			}
		} else {
			UploadingMultipleErrorDialogFragment.showInstance(activity.getSupportFragmentManager(), loadErrorsMap);
		}
	}
}
