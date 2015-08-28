package net.osmand.plus.osmedit.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import net.osmand.access.AccessibleToast;
import net.osmand.data.Amenity;
import net.osmand.osm.edit.Node;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.osmedit.EditPoiFragment;
import net.osmand.plus.osmedit.OpenstreetmapLocalUtil;
import net.osmand.plus.osmedit.OpenstreetmapRemoteUtil;
import net.osmand.plus.osmedit.OpenstreetmapUtil;
import net.osmand.plus.osmedit.OsmEditingPlugin;
import net.osmand.plus.osmedit.OsmPoint;

/**
 * Created by GaidamakUA on 8/28/15.
 */
public class DeletePoiDialogFragment extends DialogFragment {
	private static final String KEY_AMENITY_NODE = "amenity_node";

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final Activity activity = getActivity();

		OsmandSettings settings = ((OsmandApplication) activity.getApplication()).getSettings();
		OsmEditingPlugin plugin = OsmandPlugin.getPlugin(OsmEditingPlugin.class);
		final OpenstreetmapUtil mOpenstreetmapUtil;
		if (settings.OFFLINE_EDITION.get() || !settings.isInternetConnectionAvailable(true)) {
			mOpenstreetmapUtil = new OpenstreetmapLocalUtil(plugin, activity);
		} else if (!settings.isInternetConnectionAvailable(true)) {
			mOpenstreetmapUtil = new OpenstreetmapLocalUtil(plugin, activity);
		} else {
			mOpenstreetmapUtil = new OpenstreetmapRemoteUtil(activity);
		}

		final Bundle args = getArguments();
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setTitle(R.string.poi_remove_title);
		LinearLayout ll = new LinearLayout(activity);
		ll.setPadding(4, 2, 4, 0);
		ll.setOrientation(LinearLayout.VERTICAL);
		final EditText comment = new EditText(activity);
		comment.setText(R.string.poi_remove_title);
		ll.addView(comment);
		final CheckBox closeChangeset;
		closeChangeset = new CheckBox(activity);
		closeChangeset.setText(R.string.close_changeset);
		ll.addView(closeChangeset);
		builder.setView(ll);
		builder.setNegativeButton(R.string.shared_string_cancel, null);
		builder.setPositiveButton(R.string.shared_string_delete, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Node n = (Node) args.getSerializable(KEY_AMENITY_NODE);
				String c = comment.getText().toString();
				EditPoiFragment.commitNode(OsmPoint.Action.DELETE, n, mOpenstreetmapUtil.getEntityInfo(), c,
						closeChangeset == null ? false : closeChangeset.isSelected(), new Runnable() {
							@Override
							public void run() {
								AccessibleToast.makeText(activity, R.string.poi_remove_success, Toast.LENGTH_LONG).show();
								if (activity instanceof MapActivity) {
									((MapActivity) activity).getMapView().refreshMap(true);
								}
							}
						}, getActivity(), mOpenstreetmapUtil);
			}
		});
		return builder.create();
	}

	public static DeletePoiDialogFragment createInstance(Node amenityNode) {
		DeletePoiDialogFragment fragment = new DeletePoiDialogFragment();
		Bundle bundle = new Bundle();
		bundle.putSerializable(KEY_AMENITY_NODE, amenityNode);
		fragment.setArguments(bundle);
		return fragment;
	}
}
