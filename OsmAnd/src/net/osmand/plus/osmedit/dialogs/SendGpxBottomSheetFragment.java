package net.osmand.plus.osmedit.dialogs;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
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

import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
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
	private UploadVisibility selectedUploadVisibility = UploadVisibility.Public;

	private TextInputEditText tagsField;
	private TextInputEditText messageField;

	public void setGpxInfos(GpxInfo[] gpxInfos) {
		this.gpxInfos = gpxInfos;
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		View sendOsmPoiView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.send_gpx_fragment, null);

		messageField = sendOsmPoiView.findViewById(R.id.message_field);
		tagsField = sendOsmPoiView.findViewById(R.id.tags_field);

		OsmandSettings settings = requiredMyApplication().getSettings();

		TextView accountName = sendOsmPoiView.findViewById(R.id.user_name);
		if (!Algorithms.isEmpty(settings.USER_DISPLAY_NAME.get())) {
			accountName.setText(settings.USER_DISPLAY_NAME.get());
		} else {
			accountName.setText(settings.USER_NAME.get());
		}

		if (gpxInfos.length > 0 && gpxInfos[0].getFileName() != null) {
			int dt = gpxInfos[0].getFileName().indexOf('.');
			messageField.setText(gpxInfos[0].getFileName().substring(0, dt));
		}
		tagsField.setText(R.string.app_name_osmand);

		final TextView visibilityName = sendOsmPoiView.findViewById(R.id.visibility_name);
		final TextView visibilityDescription = sendOsmPoiView.findViewById(R.id.visibility_description);
		visibilityName.setText(selectedUploadVisibility.stringResource());
		visibilityDescription.setText(selectedUploadVisibility.getDescriptionId());

		List<HorizontalSelectionItem> itemsVisibility = new ArrayList<>();
		for (UploadVisibility visibilityType : UploadVisibility.values()) {
			String title = getString(visibilityType.stringResource());
			HorizontalSelectionItem item = new HorizontalSelectionAdapter.HorizontalSelectionItem(title, visibilityType);
			itemsVisibility.add(item);
		}

		final HorizontalSelectionAdapter horizontalSelectionAdapter = new HorizontalSelectionAdapter(getMyApplication(), nightMode);
		horizontalSelectionAdapter.setItems(itemsVisibility);
		horizontalSelectionAdapter.setSelectedItemByTitle(getString(selectedUploadVisibility.stringResource()));
		horizontalSelectionAdapter.setListener(new HorizontalSelectionAdapterListener() {
			@Override
			public void onItemSelected(HorizontalSelectionAdapter.HorizontalSelectionItem item) {
				selectedUploadVisibility = (OsmEditingPlugin.UploadVisibility) item.getObject();
				visibilityName.setText(selectedUploadVisibility.stringResource());
				visibilityDescription.setText(selectedUploadVisibility.getDescriptionId());
				horizontalSelectionAdapter.notifyDataSetChanged();
			}

		});

		RecyclerView iconCategoriesRecyclerView = sendOsmPoiView.findViewById(R.id.description_view);
		iconCategoriesRecyclerView.setAdapter(horizontalSelectionAdapter);
		iconCategoriesRecyclerView.setLayoutManager(new LinearLayoutManager(getMyApplication(), RecyclerView.HORIZONTAL, false));
		horizontalSelectionAdapter.notifyDataSetChanged();

		SimpleBottomSheetItem titleItem = (SimpleBottomSheetItem) new SimpleBottomSheetItem.Builder()
				.setCustomView(sendOsmPoiView)
				.create();
		items.add(titleItem);
	}

	private boolean isLoginOAuth() {
		return !Algorithms.isEmpty(getMyApplication().getSettings().USER_DISPLAY_NAME.get());
	}

	@Override
	protected UiUtilities.DialogButtonType getRightBottomButtonType() {
		return (UiUtilities.DialogButtonType.PRIMARY);
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_upload;
	}

	@Override
	protected void onRightBottomButtonClick() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			String tags = tagsField.getText().toString();
			String descr = messageField.getText().toString();

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
			fragment.show(fragmentManager, TAG);
		}
	}
}