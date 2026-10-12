package net.osmand.plus.plugins.osmedit.dialogs;

import static net.osmand.plus.plugins.osmedit.dialogs.SendGpxBottomSheetFragment.showOpenStreetMapScreen;

import android.app.Activity;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.osm.PoiType;
import net.osmand.osm.edit.Entity;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.plugins.osmedit.data.OpenstreetmapPoint;
import net.osmand.plus.plugins.osmedit.data.OsmPoint;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.HashMap;
import java.util.Map;

public class SendPoiBottomSheetFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = SendPoiBottomSheetFragment.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(SendPoiBottomSheetFragment.class);
	public static final String OPENSTREETMAP_POINT = "openstreetmap_point";

	private SwitchCompat closeChangeSet;
	private EditText messageEditText;

	private OsmEditingPlugin plugin;
	private OsmPoint[] poi;

	private boolean isLoginOAuth() {
		return !Algorithms.isEmpty(plugin.OSM_USER_DISPLAY_NAME.get());
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		OsmandApplication app = getMyApplication();
		plugin = PluginsHelper.getPlugin(OsmEditingPlugin.class);
		if (app == null || plugin == null) return;

		poi = AndroidUtils.getSerializable(getArguments(), OPENSTREETMAP_POINT, OsmPoint[].class);
		boolean isNightMode = app.getDaynightHelper().isNightModeForMapControls();
		View sendOsmPoiView = View.inflate(new ContextThemeWrapper(getContext(), themeRes),
				R.layout.send_poi_fragment, null);
		sendOsmPoiView.getViewTreeObserver().addOnGlobalLayoutListener(getShadowLayoutListener());
		closeChangeSet = sendOsmPoiView.findViewById(R.id.close_change_set_checkbox);
		messageEditText = sendOsmPoiView.findViewById(R.id.message_field);
		String defaultChangeSet = createDefaultChangeSet(app);
		messageEditText.setText(defaultChangeSet);
		messageEditText.setSelection(messageEditText.getText().length());
		TextView accountName = sendOsmPoiView.findViewById(R.id.user_name);

		String userNameOAuth = plugin.OSM_USER_DISPLAY_NAME.get();
		String userNameOpenID = plugin.OSM_USER_NAME_OR_EMAIL.get();
		String userName = isLoginOAuth() ? userNameOAuth : userNameOpenID;
		accountName.setText(userName);
		int paddingSmall = app.getResources().getDimensionPixelSize(R.dimen.content_padding_small);
		closeChangeSet.setChecked(true);
		setCloseChangeSet(isNightMode, paddingSmall);
		closeChangeSet.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				setCloseChangeSet(isNightMode, paddingSmall);
			}
		});
		LinearLayout account = sendOsmPoiView.findViewById(R.id.account_container);
		account.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				showOpenStreetMapScreen(activity);
			}
			dismiss();
		});
		SimpleBottomSheetItem titleItem = (SimpleBottomSheetItem) new SimpleBottomSheetItem.Builder()
				.setCustomView(sendOsmPoiView)
				.create();
		items.add(titleItem);
	}

	public static void showInstance(@NonNull FragmentManager fm, @NonNull OsmPoint[] points) {
		try {
			if (!fm.isStateSaved()) {
				SendPoiBottomSheetFragment fragment = new SendPoiBottomSheetFragment();
				Bundle bundle = new Bundle();
				bundle.putSerializable(OPENSTREETMAP_POINT, points);
				fragment.setArguments(bundle);
				fragment.show(fm, TAG);
			}
		} catch (RuntimeException e) {
			LOG.error("showInstance", e);
		}
	}

	@Override
	protected DialogButtonType getRightBottomButtonType() {
		return (DialogButtonType.PRIMARY);
	}

	@Override
	protected void onRightBottomButtonClick() {
		ProgressDialogPoiUploader progressDialogPoiUploader = null;
		Activity activity = getActivity();
		if (activity instanceof MapActivity) {
			progressDialogPoiUploader = new SimpleProgressDialogPoiUploader((MapActivity) activity);
		} else if (getParentFragment() instanceof ProgressDialogPoiUploader) {
			progressDialogPoiUploader = (ProgressDialogPoiUploader) getParentFragment();
		}
		if (progressDialogPoiUploader != null) {
			String comment = messageEditText.getText().toString();
			if (comment.length() > 0) {
				for (OsmPoint osmPoint : poi) {
					if (osmPoint.getGroup() == OsmPoint.Group.POI) {
						((OpenstreetmapPoint) osmPoint).setComment(comment);
						break;
					}
				}
			}
			progressDialogPoiUploader.showProgressDialog(poi, closeChangeSet.isChecked(), false);
		}
		dismiss();
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_upload;
	}

	private String createDefaultChangeSet(OsmandApplication app) {
		Map<String, PoiType> allTranslatedSubTypes = app.getPoiTypes().getAllTranslatedNames(true);
		if (allTranslatedSubTypes == null) {
			return "";
		}
		Map<String, Integer> addGroup = new HashMap<>();
		Map<String, Integer> editGroup = new HashMap<>();
		Map<String, Integer> deleteGroup = new HashMap<>();
		Map<String, Integer> reopenGroup = new HashMap<>();
		String comment = "";
		for (OsmPoint p : poi) {
			if (p.getGroup() == OsmPoint.Group.POI) {
				OsmPoint.Action action = p.getAction();
				String type = ((OpenstreetmapPoint) p).getEntity().getTag(Entity.POI_TYPE_TAG);
				if (type == null) {
					continue;
				}
				PoiType localizedPoiType = allTranslatedSubTypes.get(type.toLowerCase().trim());
				if (localizedPoiType != null) {
					type = Algorithms.capitalizeFirstLetter(localizedPoiType.getKeyName().replace('_', ' '));
				}
				if (action == OsmPoint.Action.CREATE) {
					if (!addGroup.containsKey(type)) {
						addGroup.put(type, 1);
					} else {
						addGroup.put(type, addGroup.get(type) + 1);
					}
				} else if (action == OsmPoint.Action.MODIFY) {
					if (!editGroup.containsKey(type)) {
						editGroup.put(type, 1);
					} else {
						editGroup.put(type, editGroup.get(type) + 1);
					}
				} else if (action == OsmPoint.Action.DELETE) {
					if (!deleteGroup.containsKey(type)) {
						deleteGroup.put(type, 1);
					} else {
						deleteGroup.put(type, deleteGroup.get(type) + 1);
					}
				} else if (action == OsmPoint.Action.REOPEN) {
					if (!reopenGroup.containsKey(type)) {
						reopenGroup.put(type, 1);
					} else {
						reopenGroup.put(type, reopenGroup.get(type) + 1);
					}
				}
			}
		}
		int modifiedItemsOutOfLimit = 0;
		for (int i = 0; i < 4; i++) {
			String action;
			Map<String, Integer> group;
			switch (i) {
				case 0:
					action = getString(R.string.default_changeset_add);
					group = addGroup;
					break;
				case 1:
					action = getString(R.string.default_changeset_edit);
					group = editGroup;
					break;
				case 2:
					action = getString(R.string.default_changeset_delete);
					group = deleteGroup;
					break;
				case 3:
					action = getString(R.string.default_changeset_reopen);
					group = reopenGroup;
					break;
				default:
					action = "";
					group = new HashMap<>();
			}

			if (!group.isEmpty()) {
				int pos = 0;
				for (Map.Entry<String, Integer> entry : group.entrySet()) {
					String type = entry.getKey();
					int quantity = entry.getValue();
					if (comment.length() > 200) {
						modifiedItemsOutOfLimit += quantity;
					} else {
						if (pos == 0) {
							comment = comment.concat(comment.length() == 0 ? "" : "; ").concat(action).concat(" ")
									.concat(quantity == 1 ? "" : quantity + " ").concat(type);
						} else {
							comment = comment.concat(", ").concat(quantity == 1 ? "" : quantity + " ").concat(type);
						}
					}
					pos++;
				}
			}
		}
		if (modifiedItemsOutOfLimit != 0) {
			comment = comment.concat("; ").concat(modifiedItemsOutOfLimit + " ")
					.concat(getString(R.string.items_modified)).concat(".");
		} else if (!comment.isEmpty()) {
			comment = comment.concat(".");
		}
		return comment;
	}

	private void setCloseChangeSet(boolean isNightMode, int paddingSmall) {
		if (isNightMode) {
			closeChangeSet.setBackgroundResource(
					closeChangeSet.isChecked() ? R.drawable.layout_bg_dark_solid : R.drawable.layout_bg_dark);
		} else {
			closeChangeSet.setBackgroundResource(
					closeChangeSet.isChecked() ? R.drawable.layout_bg_solid : R.drawable.layout_bg);
		}
		closeChangeSet.setPadding(paddingSmall, 0, paddingSmall, 0);
	}
}

