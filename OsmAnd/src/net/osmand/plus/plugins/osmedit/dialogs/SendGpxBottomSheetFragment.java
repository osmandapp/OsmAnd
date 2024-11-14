package net.osmand.plus.plugins.osmedit.dialogs;

import static net.osmand.plus.plugins.osmedit.OsmEditingPlugin.OSMAND_TAG;
import static net.osmand.plus.plugins.osmedit.asynctasks.UploadGPXFilesTask.DEFAULT_ACTIVITY_TAG;
import static net.osmand.plus.settings.fragments.BaseSettingsFragment.OPEN_SETTINGS;
import static net.osmand.plus.settings.fragments.SettingsScreenType.OPEN_STREET_MAP_EDITING;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.textfield.TextInputEditText;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.myplaces.MyPlacesActivity;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.plugins.osmedit.UploadVisibility;
import net.osmand.plus.plugins.osmedit.asynctasks.UploadGPXFilesTask;
import net.osmand.plus.plugins.osmedit.asynctasks.UploadGPXFilesTask.UploadGpxListener;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.chips.ChipItem;
import net.osmand.plus.widgets.chips.HorizontalChipsView;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SendGpxBottomSheetFragment extends MenuBottomSheetDialogFragment implements UploadGpxListener {

	public static final String TAG = SendGpxBottomSheetFragment.class.getSimpleName();

	private final OsmEditingPlugin plugin = PluginsHelper.requirePlugin(OsmEditingPlugin.class);


	private File[] files;
	private UploadVisibility uploadVisibility;

	private TextInputEditText tagsField;
	private TextInputEditText messageField;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (uploadVisibility == null) {
			uploadVisibility = plugin.OSM_UPLOAD_VISIBILITY.get();
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		LayoutInflater themedInflater = UiUtilities.getInflater(requireContext(), nightMode);
		View view = themedInflater.inflate(R.layout.send_gpx_fragment, null);
		view.getViewTreeObserver().addOnGlobalLayoutListener(getShadowLayoutListener());

		setupDescriptionRow(view);
		setupTagsRow(view);
		setupVisibilityRow(view);
		setupAccountRow(view);

		items.add(new SimpleBottomSheetItem.Builder().setCustomView(view).create());
	}

	private void setupDescriptionRow(@NonNull View view) {
		messageField = view.findViewById(R.id.message_field);
	}

	private void setupTagsRow(@NonNull View view) {
		tagsField = view.findViewById(R.id.tags_field);

		Editable tagsText = tagsField.getText();
		String text = tagsText != null ? tagsText.toString() : "";

		if (Algorithms.isEmpty(text)) {
			tagsField.setText(getDefaultTags());
		}
	}

	@NonNull
	private String getDefaultTags() {
		return OSMAND_TAG + ", " + DEFAULT_ACTIVITY_TAG;
	}

	private void setupVisibilityRow(@NonNull View view) {
		TextView visibilityName = view.findViewById(R.id.visibility_name);
		TextView visibilityDescription = view.findViewById(R.id.visibility_description);
		visibilityName.setText(uploadVisibility.getTitleId());
		visibilityDescription.setText(uploadVisibility.getDescriptionId());

		List<ChipItem> itemsVisibility = new ArrayList<>();
		for (UploadVisibility visibilityType : UploadVisibility.values()) {
			String title = getString(visibilityType.getTitleId());
			ChipItem item = new ChipItem(title);
			item.title = title;
			item.contentDescription = title;
			item.tag = visibilityType;
			itemsVisibility.add(item);
		}

		HorizontalChipsView chipsView = view.findViewById(R.id.selector_view);
		chipsView.setItems(itemsVisibility);

		ChipItem selected = chipsView.getChipById(getString(uploadVisibility.getTitleId()));
		chipsView.setSelected(selected);

		chipsView.setOnSelectChipListener(chip -> {
			uploadVisibility = (UploadVisibility) chip.tag;
			plugin.OSM_UPLOAD_VISIBILITY.set(uploadVisibility);
			visibilityName.setText(uploadVisibility.getTitleId());
			visibilityDescription.setText(uploadVisibility.getDescriptionId());
			chipsView.smoothScrollTo(chip);
			return true;
		});
		chipsView.notifyDataSetChanged();
	}

	private void setupAccountRow(@NonNull View view) {
		ViewGroup container = view.findViewById(R.id.account_container);

		String name = plugin.OSM_USER_DISPLAY_NAME.get();
		TextView textView = container.findViewById(R.id.user_name);
		textView.setText(Algorithms.isEmpty(name) ? plugin.OSM_USER_NAME_OR_EMAIL.get() : name);

		container.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				showOpenStreetMapScreen(activity);
			}
			dismiss();
		});
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
			String description = descrText != null ? descrText.toString() : "";
			String tags = tagsText != null ? tagsText.toString() : "";

			UploadGPXFilesTask task = new UploadGPXFilesTask(activity, description, tags, uploadVisibility, this);
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, files);
		}
		dismiss();
	}

	@Override
	public void onGpxUploaded(String result) {
		Fragment target = getTargetFragment();
		if (target instanceof UploadGpxListener) {
			((UploadGpxListener) target).onGpxUploaded(result);
		}
	}

	protected static void showOpenStreetMapScreen(@NonNull FragmentActivity activity) {
		if (activity instanceof MapActivity) {
			BaseSettingsFragment.showInstance(activity, OPEN_STREET_MAP_EDITING);
		} else {
			Bundle prevIntentParams = null;
			if (activity instanceof MyPlacesActivity) {
				prevIntentParams = ((MyPlacesActivity) activity).storeCurrentState();
			} else if (activity.getIntent() != null) {
				prevIntentParams = activity.getIntent().getExtras();
			}
			Bundle params = new Bundle();
			params.putString(OPEN_SETTINGS, OPEN_STREET_MAP_EDITING.name());

			MapActivity.launchMapActivityMoveToTop(activity, prevIntentParams, null, params);
		}
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull File[] files, @Nullable Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			SendGpxBottomSheetFragment fragment = new SendGpxBottomSheetFragment();
			fragment.files = files;
			fragment.setTargetFragment(target, 0);
			fragment.setRetainInstance(true);
			fragment.show(manager, TAG);
		}
	}
}