package net.osmand.plus.osmedit.dialogs;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.UiUtilities.DialogButtonType;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.mapcontextmenu.other.HorizontalSelectionAdapter;
import net.osmand.plus.mapcontextmenu.other.HorizontalSelectionAdapter.HorizontalSelectionAdapterListener;
import net.osmand.plus.mapcontextmenu.other.HorizontalSelectionAdapter.HorizontalSelectionItem;
import net.osmand.plus.myplaces.AvailableGPXFragment.GpxInfo;
import net.osmand.plus.osmedit.OsmEditingPlugin;
import net.osmand.plus.osmedit.OsmEditingPlugin.UploadVisibility;
import net.osmand.plus.osmedit.UploadGPXFilesTask;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class SendGpxBottomSheetFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = SendGpxBottomSheetFragment.class.getSimpleName();

	private GpxInfo[] gpxInfos;
	private UploadVisibility selectedUploadVisibility = UploadVisibility.PUBLIC;

	private TextInputEditText tagsField;
	private TextInputEditText messageField;

	public void setGpxInfos(GpxInfo[] gpxInfos) {
		this.gpxInfos = gpxInfos;
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		OsmandApplication app = requiredMyApplication();
		OsmandSettings settings = app.getSettings();

		LayoutInflater themedInflater = UiUtilities.getInflater(app, nightMode);
		View sendOsmPoiView = themedInflater.inflate(R.layout.send_gpx_fragment, null);

		tagsField = sendOsmPoiView.findViewById(R.id.tags_field);
		messageField = sendOsmPoiView.findViewById(R.id.message_field);

		TextView accountName = sendOsmPoiView.findViewById(R.id.user_name);
		if (!Algorithms.isEmpty(settings.USER_DISPLAY_NAME.get())) {
			accountName.setText(settings.USER_DISPLAY_NAME.get());
		} else {
			accountName.setText(settings.USER_NAME.get());
		}

		String fileName = gpxInfos[0].getFileName();
		messageField.setText(Algorithms.getFileNameWithoutExtension(fileName));

		final TextView visibilityName = sendOsmPoiView.findViewById(R.id.visibility_name);
		final TextView visibilityDescription = sendOsmPoiView.findViewById(R.id.visibility_description);
		visibilityName.setText(selectedUploadVisibility.getTitleId());
		visibilityDescription.setText(selectedUploadVisibility.getDescriptionId());

		List<HorizontalSelectionItem> itemsVisibility = new ArrayList<>();
		for (UploadVisibility visibilityType : UploadVisibility.values()) {
			String title = getString(visibilityType.getTitleId());
			HorizontalSelectionItem item = new HorizontalSelectionAdapter.HorizontalSelectionItem(title, visibilityType);
			itemsVisibility.add(item);
		}

		final HorizontalSelectionAdapter horizontalSelectionAdapter = new HorizontalSelectionAdapter(app, nightMode);
		horizontalSelectionAdapter.setItems(itemsVisibility);
		horizontalSelectionAdapter.setSelectedItemByTitle(getString(selectedUploadVisibility.getTitleId()));
		horizontalSelectionAdapter.setListener(new HorizontalSelectionAdapterListener() {
			@Override
			public void onItemSelected(HorizontalSelectionAdapter.HorizontalSelectionItem item) {
				selectedUploadVisibility = (OsmEditingPlugin.UploadVisibility) item.getObject();
				visibilityName.setText(selectedUploadVisibility.getTitleId());
				visibilityDescription.setText(selectedUploadVisibility.getDescriptionId());
				horizontalSelectionAdapter.notifyDataSetChanged();
			}

		});

		RecyclerView iconCategoriesRecyclerView = sendOsmPoiView.findViewById(R.id.description_view);
		iconCategoriesRecyclerView.setAdapter(horizontalSelectionAdapter);
		iconCategoriesRecyclerView.setLayoutManager(new LinearLayoutManager(app, RecyclerView.HORIZONTAL, false));
		horizontalSelectionAdapter.notifyDataSetChanged();

		SimpleBottomSheetItem titleItem = (SimpleBottomSheetItem) new SimpleBottomSheetItem.Builder()
				.setCustomView(sendOsmPoiView)
				.create();
		items.add(titleItem);
	}

	@Override
	protected DialogButtonType getRightBottomButtonType() {
		return DialogButtonType.PRIMARY;
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_upload;
	}

	@Override
	protected void onRightBottomButtonClick() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			Editable tagsText = tagsField.getText();
			Editable descrText = messageField.getText();
			String tags = tagsText != null ? tagsText.toString() : "";
			String descr = descrText != null ? descrText.toString() : "";

			UploadGPXFilesTask uploadGPXFilesTask = new UploadGPXFilesTask(activity, descr, tags, selectedUploadVisibility);
			uploadGPXFilesTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, gpxInfos);
		}
		dismiss();
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, @Nullable Fragment targetFragment, GpxInfo[] info) {
		if (!fragmentManager.isStateSaved()) {
			SendGpxBottomSheetFragment fragment = new SendGpxBottomSheetFragment();
			fragment.setTargetFragment(targetFragment, 0);
			fragment.setGpxInfos(info);
			fragment.setRetainInstance(true);
			fragment.show(fragmentManager, TAG);
		}
	}
}