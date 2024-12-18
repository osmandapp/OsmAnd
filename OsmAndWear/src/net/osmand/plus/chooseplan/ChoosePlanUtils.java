package net.osmand.plus.chooseplan;

import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.utils.AndroidUtils;

public class ChoosePlanUtils {

	public static void onGetPlugin(@Nullable FragmentActivity activity, @NonNull OsmandPlugin plugin) {
		if (activity == null) return;

		OsmAndFeature feature = plugin.getOsmAndFeature();
		if (feature != null) {
			ChoosePlanFragment.showInstance(activity, feature);
		} else {
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(plugin.getInstallURL()));
			AndroidUtils.startActivityIfSafe(activity, intent);
		}
	}
}
