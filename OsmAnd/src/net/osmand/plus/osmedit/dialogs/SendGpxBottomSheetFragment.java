package net.osmand.plus.osmedit.dialogs;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputLayout;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.mapcontextmenu.other.HorizontalSelectionAdapter;
import net.osmand.plus.myplaces.AvailableGPXFragment;
import net.osmand.plus.osmedit.OsmEditingPlugin;
import net.osmand.plus.osmedit.UploadGPXFilesTask;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class SendGpxBottomSheetFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = "SendGpxBottomSheetFragment";

	protected OsmandSettings settings;

	private OsmEditingPlugin.UploadVisibility uploadVisibility;
	private List<AvailableGPXFragment.GpxInfo> info;
	private String selectedVisibilityType;

	protected OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

	private boolean isLoginOAuth() {
		return !Algorithms.isEmpty(getMyApplication().getSettings().USER_DISPLAY_NAME.get());
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		final View sendOsmPoiView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.send_gpx_fragment, null);
		final TextView accountName = sendOsmPoiView.findViewById(R.id.user_name);
		settings = getMyApplication().getSettings();
		String userNameOAuth = settings.USER_DISPLAY_NAME.get();
		String userNameOpenID = settings.USER_NAME.get();
		String userName = isLoginOAuth() ? userNameOAuth : userNameOpenID;
		accountName.setText(userName);
		final TextView visibilityName = sendOsmPoiView.findViewById(R.id.visibility_name);
		final TextView visibilityDescription = sendOsmPoiView.findViewById(R.id.visibility_description);
		HorizontalSelectionAdapter horizontalSelectionAdapter = new HorizontalSelectionAdapter(getMyApplication(), nightMode);
		List<HorizontalSelectionAdapter.HorizontalSelectionItem> itemsVisibility = new ArrayList<>();
		for (OsmEditingPlugin.UploadVisibility visibilityType : OsmEditingPlugin.UploadVisibility.values()) {
			String title = getMyApplication().getString(visibilityType.stringResource());
			HorizontalSelectionAdapter.HorizontalSelectionItem item = new HorizontalSelectionAdapter.HorizontalSelectionItem(title, visibilityType);
			itemsVisibility.add(item);
		}
		horizontalSelectionAdapter.setItems(itemsVisibility);
//		horizontalSelectionAdapter.setSelectedItemByTitle(selectedVisibilityType);
		horizontalSelectionAdapter.setListener(new HorizontalSelectionAdapter.HorizontalSelectionAdapterListener() {
			@Override
			public void onItemSelected(HorizontalSelectionAdapter.HorizontalSelectionItem item) {
				uploadVisibility = (OsmEditingPlugin.UploadVisibility) item.getObject();
				visibilityName.setText(uploadVisibility.stringResource());
				visibilityDescription.setText(uploadVisibility.getDescriptionId());
			}

		});
		RecyclerView iconCategoriesRecyclerView = sendOsmPoiView.findViewById(R.id.description_view);
		iconCategoriesRecyclerView.setAdapter(horizontalSelectionAdapter);
		iconCategoriesRecyclerView.setLayoutManager(new LinearLayoutManager(getMyApplication(), RecyclerView.HORIZONTAL, false));
		horizontalSelectionAdapter.notifyDataSetChanged();

		final SimpleBottomSheetItem titleItem = (SimpleBottomSheetItem) new SimpleBottomSheetItem.Builder()
				.setCustomView(sendOsmPoiView)
				.create();
		items.add(titleItem);
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, @Nullable Fragment targetFragment, final AvailableGPXFragment.GpxInfo... info) {
		if (!fragmentManager.isStateSaved()) {
			SendGpxBottomSheetFragment fragment = new SendGpxBottomSheetFragment();
			fragment.setTargetFragment(targetFragment, 0);
			fragment.show(fragmentManager, TAG);
		}
	}

	@Override
	protected UiUtilities.DialogButtonType getRightBottomButtonType() {
		return (UiUtilities.DialogButtonType.PRIMARY);
	}

	@Override
	protected void onRightBottomButtonClick() {
		final View sendOsmPoiView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.send_gpx_fragment, null);
		final TextInputLayout descr = sendOsmPoiView.findViewById(R.id.message_field);
		final TextInputLayout tags = sendOsmPoiView.findViewById(R.id.tags_field);

//		new UploadGPXFilesTask(getActivity(), descr.getEditText().toString(), tags.getEditText().toString(),
//				uploadVisibility
//		).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, info);
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_upload;
	}

}

