package net.osmand.plus.osmedit.dialogs;

import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.osmedit.OpenstreetmapPoint;
import net.osmand.plus.osmedit.OsmPoint;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.util.Algorithms;

public class SendOsmNoteBottomSheetFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = "SendPoiBottomSheetFragment";
	public static final String OPENSTREETMAP_POINT = "openstreetmap_point";
	public static final String POI_UPLOADER_TYPE = "poi_uploader_type";
	private OsmPoint[] poi;

	protected OsmandSettings settings;

	public enum PoiUploaderType {
		SIMPLE,
		FRAGMENT
	}

	protected OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

	private boolean isLoginOAuth() {
		return !Algorithms.isEmpty(getMyApplication().getSettings().USER_DISPLAY_NAME.get());
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		final boolean isNightMode = !getMyApplication().getSettings().isLightContent();
		final View sendOsmNoteView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.send_osm_note_fragment, null);
		final LinearLayout accountBlockView = sendOsmNoteView.findViewById(R.id.account_block);
		final SwitchCompat uploadAnonymously = sendOsmNoteView.findViewById(R.id.upload_anonymously_switch);
		final TextView accountName = sendOsmNoteView.findViewById(R.id.user_name);
		settings = getMyApplication().getSettings();
		String userNameOAuth = settings.USER_DISPLAY_NAME.get();
		String userNameOpenID = settings.USER_NAME.get();
		String userName = isLoginOAuth() ? userNameOAuth : userNameOpenID;
		accountName.setText(userName);
		accountBlockView.setVisibility(View.VISIBLE);
		uploadAnonymously.setBackgroundResource(isNightMode ? R.drawable.layout_bg_dark : R.drawable.layout_bg);
		uploadAnonymously.setPadding(30, 0, 0, 0);
		uploadAnonymously.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				accountBlockView.setVisibility(isChecked ? View.GONE : View.VISIBLE);
				if (isNightMode) {
					uploadAnonymously.setBackgroundResource(isChecked ? R.drawable.layout_bg_dark_solid : R.drawable.layout_bg_dark);
				} else {
					uploadAnonymously.setBackgroundResource(isChecked ? R.drawable.layout_bg_solid : R.drawable.layout_bg);
				}
				uploadAnonymously.setPadding(30, 0, 0, 0);
			}
		});
		final SimpleBottomSheetItem titleItem = (SimpleBottomSheetItem) new SimpleBottomSheetItem.Builder()
				.setCustomView(sendOsmNoteView)
				.create();
		items.add(titleItem);
	}

	public static SendOsmNoteBottomSheetFragment showInstance(@NonNull OsmPoint[] points, @NonNull PoiUploaderType uploaderType) {
		SendOsmNoteBottomSheetFragment fragment = new SendOsmNoteBottomSheetFragment();
		Bundle bundle = new Bundle();
		bundle.putSerializable(OPENSTREETMAP_POINT, points);
		bundle.putString(POI_UPLOADER_TYPE, uploaderType.name());
		fragment.setArguments(bundle);
		return fragment;
	}

	@Override
	protected UiUtilities.DialogButtonType getRightBottomButtonType() {
		return (UiUtilities.DialogButtonType.PRIMARY);
	}

	@Override
	protected void onRightBottomButtonClick() {
		View view = getView();
		poi = (OsmPoint[]) getArguments().getSerializable(OPENSTREETMAP_POINT);
		boolean hasPoiGroup = false;
		assert poi != null;
		for (OsmPoint p : poi) {
			if (p.getGroup() == OsmPoint.Group.POI) {
				hasPoiGroup = true;
				break;
			}
		}
		final boolean hasPOI = hasPoiGroup;
		final SwitchCompat uploadAnonymously = (SwitchCompat) view.findViewById(R.id.upload_anonymously_switch);
		final EditText messageEditText = (EditText) view.findViewById(R.id.message_field);
		final SendPoiDialogFragment.PoiUploaderType poiUploaderType = SendPoiDialogFragment.PoiUploaderType.valueOf(getArguments().getString(POI_UPLOADER_TYPE, SendPoiDialogFragment.PoiUploaderType.SIMPLE.name()));
		final SendPoiDialogFragment.ProgressDialogPoiUploader progressDialogPoiUploader;
		if (poiUploaderType == SendPoiDialogFragment.PoiUploaderType.SIMPLE && getActivity() instanceof MapActivity) {
			progressDialogPoiUploader =
					new SendPoiDialogFragment.SimpleProgressDialogPoiUploader((MapActivity) getActivity());
		} else {
			progressDialogPoiUploader = (SendPoiDialogFragment.ProgressDialogPoiUploader) getParentFragment();
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
			progressDialogPoiUploader.showProgressDialog(poi,
					false,
					!hasPOI && uploadAnonymously.isChecked());
		}
		dismiss();
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_upload;
	}
}
