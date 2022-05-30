package net.osmand.plus.plugins.osmedit.dialogs;

import static net.osmand.plus.settings.fragments.BaseSettingsFragment.SettingsScreenType.OPEN_STREET_MAP_EDITING;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.textfield.TextInputEditText;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.myplaces.ui.AvailableGPXFragment.GpxInfo;
import net.osmand.plus.myplaces.ui.FavoritesActivity;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin.UploadVisibility;
import net.osmand.plus.plugins.osmedit.asynctasks.UploadGPXFilesTask;
import net.osmand.plus.plugins.osmedit.asynctasks.UploadGPXFilesTask.UploadGpxListener;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UiUtilities.DialogButtonType;
import net.osmand.plus.widgets.chips.ChipItem;
import net.osmand.plus.widgets.chips.HorizontalChipsView;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class SendGpxBottomSheetFragment extends MenuBottomSheetDialogFragment implements UploadGpxListener {

	public static final String TAG = SendGpxBottomSheetFragment.class.getSimpleName();

	private GpxInfo[] gpxInfos;
	private UploadVisibility selectedUploadVisibility;
	private OsmEditingPlugin plugin;

	private TextInputEditText tagsField;
	private TextInputEditText messageField;

	public void setGpxInfos(GpxInfo[] gpxInfos) {
		this.gpxInfos = gpxInfos;
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		OsmandApplication app = requiredMyApplication();
		plugin = OsmandPlugin.getPlugin(OsmEditingPlugin.class);

		LayoutInflater themedInflater = UiUtilities.getInflater(app, nightMode);
		View sendGpxView = themedInflater.inflate(R.layout.send_gpx_fragment, null);
		sendGpxView.getViewTreeObserver().addOnGlobalLayoutListener(getShadowLayoutListener());

		if (selectedUploadVisibility == null) {
			selectedUploadVisibility = plugin.OSM_UPLOAD_VISIBILITY.get();
		}
		tagsField = sendGpxView.findViewById(R.id.tags_field);
		messageField = sendGpxView.findViewById(R.id.message_field);

		TextView accountName = sendGpxView.findViewById(R.id.user_name);
		if (!Algorithms.isEmpty(plugin.OSM_USER_DISPLAY_NAME.get())) {
			accountName.setText(plugin.OSM_USER_DISPLAY_NAME.get());
		} else {
			accountName.setText(plugin.OSM_USER_NAME_OR_EMAIL.get());
		}

		final TextView visibilityName = sendGpxView.findViewById(R.id.visibility_name);
		final TextView visibilityDescription = sendGpxView.findViewById(R.id.visibility_description);
		visibilityName.setText(selectedUploadVisibility.getTitleId());
		visibilityDescription.setText(selectedUploadVisibility.getDescriptionId());

		List<ChipItem> itemsVisibility = new ArrayList<>();
		for (UploadVisibility visibilityType : UploadVisibility.values()) {
			String title = getString(visibilityType.getTitleId());
			ChipItem item = new ChipItem(title);
			item.title = title;
			item.tag = visibilityType;
			itemsVisibility.add(item);
		}

		HorizontalChipsView chipsView = sendGpxView.findViewById(R.id.selector_view);
		chipsView.setItems(itemsVisibility);

		ChipItem selected = chipsView.getChipById(getString(selectedUploadVisibility.getTitleId()));
		chipsView.setSelected(selected);

		chipsView.setOnSelectChipListener(chip -> {
			selectedUploadVisibility = (UploadVisibility) chip.tag;
			plugin.OSM_UPLOAD_VISIBILITY.set(selectedUploadVisibility);
			visibilityName.setText(selectedUploadVisibility.getTitleId());
			visibilityDescription.setText(selectedUploadVisibility.getDescriptionId());
			chipsView.smoothScrollTo(chip);
			return true;
		});
		chipsView.notifyDataSetChanged();

		LinearLayout account = sendGpxView.findViewById(R.id.account_container);
		account.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					showOpenStreetMapScreen(activity);
				}
				dismiss();
			}
		});

		SimpleBottomSheetItem titleItem = (SimpleBottomSheetItem) new SimpleBottomSheetItem.Builder()
				.setCustomView(sendGpxView)
				.create();
		items.add(titleItem);
	}

	protected static void showOpenStreetMapScreen(@NonNull FragmentActivity activity) {
		if (activity instanceof MapActivity) {
			BaseSettingsFragment.showInstance(activity, OPEN_STREET_MAP_EDITING);
		} else {
			Bundle prevIntentParams = null;
			if (activity instanceof FavoritesActivity) {
				prevIntentParams = ((FavoritesActivity) activity).storeCurrentState();
			} else if (activity.getIntent() != null) {
				prevIntentParams = activity.getIntent().getExtras();
			}
			Bundle params = new Bundle();
			params.putString(BaseSettingsFragment.OPEN_SETTINGS, OPEN_STREET_MAP_EDITING.name());

			MapActivity.launchMapActivityMoveToTop(activity, prevIntentParams, null, params);
		}
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
			Editable descrText = messageField.getText();
			Editable tagsText = tagsField.getText();
			String commonDescription = descrText != null ? descrText.toString() : "";
			String tags = tagsText != null ? tagsText.toString() : "";

			UploadGPXFilesTask uploadGPXFilesTask = new UploadGPXFilesTask(activity, commonDescription,
					tags, selectedUploadVisibility, this);
			uploadGPXFilesTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, gpxInfos);
		}
		dismiss();
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull GpxInfo[] info, @Nullable Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			SendGpxBottomSheetFragment fragment = new SendGpxBottomSheetFragment();
			fragment.setTargetFragment(target, 0);
			fragment.setGpxInfos(info);
			fragment.setRetainInstance(true);
			fragment.show(manager, TAG);
		}
	}

	@Override
	public void onGpxUploaded(String result) {
		Fragment target = getTargetFragment();
		if (target instanceof UploadGpxListener) {
			((UploadGpxListener) target).onGpxUploaded(result);
		}
	}
}