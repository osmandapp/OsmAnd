package net.osmand.aidl;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.widgets.ui.SegmentedList;
import net.osmand.plus.widgets.ui.SettingRow;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A connected app (e.g. the AI assistant plugin) asks for AIDL permission groups:
 * startActivityForResult(new Intent(ACTION_REQUEST_PERMISSIONS).setPackage("net.osmand.plus")
 * .putExtra(EXTRA_GROUPS, new String[] {"map", "navigation"}), ...).
 * The caller is known from startActivityForResult only; the result carries the granted groups.
 */
public class AidlPermissionRequestActivity extends AppCompatActivity {

	private static final Log LOG = PlatformUtil.getLog(AidlPermissionRequestActivity.class);

	public static final String ACTION_REQUEST_PERMISSIONS = "net.osmand.aidl.REQUEST_PERMISSIONS";
	public static final String EXTRA_GROUPS = "groups";
	public static final String EXTRA_GRANTED_GROUPS = "granted_groups";

	@Nullable
	private AlertDialog dialog;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		OsmandApplication app = (OsmandApplication) getApplication();
		String pack = getCallingPackage();
		List<AidlPermissionGroup> requested = getRequestedGroups(getIntent());
		if (Algorithms.isEmpty(pack) || pack.equals(getPackageName()) || requested.isEmpty()) {
			LOG.warn("Permission request without a calling app or groups: " + pack);
			setResult(RESULT_CANCELED);
			finish();
			return;
		}
		// also after a recreation (rotation, theme): the old dialog is gone with the old window
		showDialog(app, pack, requested);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (dialog != null) {
			dialog.dismiss();
			dialog = null;
		}
	}

	private void showDialog(OsmandApplication app, String pack, List<AidlPermissionGroup> requested) {
		boolean nightMode = app.getDaynightHelper().isNightMode(ThemeUsageContext.APP);
		Context context = new ContextThemeWrapper(this,
				nightMode ? R.style.OsmandMaterialDarkTheme : R.style.OsmandMaterialLightTheme);
		PackageManager pm = getPackageManager();
		CharSequence appName = pack;
		Drawable icon = null;
		try {
			ApplicationInfo info = pm.getApplicationInfo(pack, 0);
			appName = pm.getApplicationLabel(info);
			icon = pm.getApplicationIcon(info);
		} catch (PackageManager.NameNotFoundException e) {
			LOG.error(e);
		}
		ConnectedApp connectedApp = app.getAidlApi().getOrCreateConnectedApp(pack);
		Set<AidlPermissionGroup> granted = new LinkedHashSet<>();
		for (AidlPermissionGroup group : requested) {
			// only safe groups are preselected, the user turns on the others
			if (group.isSafe() || connectedApp.isEnabled() && connectedApp.isGroupGranted(group)) {
				granted.add(group);
			}
		}
		View view = LayoutInflater.from(context).inflate(R.layout.dialog_aidl_permission_request, null);
		// no grants through an overlay drawn over the dialog (tapjacking)
		view.setFilterTouchesWhenObscured(true);
		TextView description = view.findViewById(R.id.description);
		description.setText(getString(R.string.aidl_permissions_request_descr, pack));
		ViewGroup groupsView = view.findViewById(R.id.groups);
		int iconColor = AndroidUtils.getColorFromAttr(context, R.attr.colorOnSurfaceVariant);
		for (AidlPermissionGroup group : requested) {
			View rowView = LayoutInflater.from(context).inflate(R.layout.item_ui_setting_row, groupsView, false);
			SettingRow row = new SettingRow(rowView);
			row.setIcon(group.getIconId(), iconColor);
			row.setTitle(group.getTitleId());
			// one line rows: the dialog is narrow, the details are on the permissions screen in Plugins
			row.setSubtitle(null);
			row.setChecked(granted.contains(group));
			row.setOnClickListener(v -> {
				if (!granted.remove(group)) {
					granted.add(group);
				}
				row.setChecked(granted.contains(group));
			});
			groupsView.addView(rowView);
		}
		SegmentedList.apply(groupsView);

		dialog = new MaterialAlertDialogBuilder(context)
				.setIcon(icon)
				.setTitle(getString(R.string.aidl_permissions_request_title, appName))
				.setView(view)
				.setPositiveButton(R.string.shared_string_allow, (d, which) -> {
					app.getAidlApi().applyRequestedGroups(this, pack, new LinkedHashSet<>(requested), granted);
					finishWithResult(app, pack);
				})
				.setNegativeButton(R.string.aidl_permissions_deny, (d, which) -> finishWithResult(app, pack))
				.setOnCancelListener(d -> finishWithResult(app, pack))
				.create();
		dialog.show();
		// the buttons are outside the content view
		dialog.getButton(AlertDialog.BUTTON_POSITIVE).setFilterTouchesWhenObscured(true);
	}

	private void finishWithResult(OsmandApplication app, String pack) {
		ConnectedApp connectedApp = app.getAidlApi().getOrCreateConnectedApp(pack);
		ArrayList<String> granted = new ArrayList<>();
		if (connectedApp.isEnabled()) {
			for (AidlPermissionGroup group : connectedApp.getGroups()) {
				granted.add(group.getId());
			}
		}
		setResult(granted.isEmpty() ? RESULT_CANCELED : RESULT_OK,
				new Intent().putExtra(EXTRA_GRANTED_GROUPS, granted.toArray(new String[0])));
		finish();
	}

	private static List<AidlPermissionGroup> getRequestedGroups(@Nullable Intent intent) {
		List<AidlPermissionGroup> groups = new ArrayList<>();
		String[] ids = intent != null ? intent.getStringArrayExtra(EXTRA_GROUPS) : null;
		if (ids != null) {
			for (String id : ids) {
				AidlPermissionGroup group = AidlPermissionGroup.getById(id);
				if (group != null && !groups.contains(group)) {
					groups.add(group);
				}
			}
		}
		return groups;
	}
}
