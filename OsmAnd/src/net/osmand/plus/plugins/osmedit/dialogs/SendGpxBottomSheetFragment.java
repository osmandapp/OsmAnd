package net.osmand.plus.plugins.osmedit.dialogs;

import static net.osmand.plus.plugins.osmedit.OsmEditingPlugin.OSMAND_TAG;
import static net.osmand.plus.settings.fragments.BaseSettingsFragment.OPEN_SETTINGS;
import static net.osmand.plus.settings.fragments.SettingsScreenType.OPEN_STREET_MAP_EDITING;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.textfield.TextInputEditText;

import net.osmand.plus.OsmAndTaskManager;
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
import net.osmand.plus.widgets.chips.ChipItem;
import net.osmand.plus.widgets.chips.HorizontalChipsView;
import net.osmand.shared.gpx.GpxDataItem;
import net.osmand.shared.gpx.GpxDbHelper;
import net.osmand.shared.gpx.GpxParameter;
import net.osmand.shared.io.KFile;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SendGpxBottomSheetFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = SendGpxBottomSheetFragment.class.getSimpleName();

	private final OsmEditingPlugin plugin = PluginsHelper.requirePlugin(OsmEditingPlugin.class);


	private GpxDbHelper gpxDbHelper;
	private File[] files;
	private UploadVisibility uploadVisibility;

	private TextInputEditText tagsField;
	private TextInputEditText messageField;
	private String defaultActivity;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		gpxDbHelper = app.getGpxDbHelper();
		if (uploadVisibility == null) {
			uploadVisibility = plugin.OSM_UPLOAD_VISIBILITY.get();
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		View view = inflate(R.layout.send_gpx_fragment);
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

	@Nullable
	private String getDefaultActivity() {
		for (File file : files) {
			GpxDataItem item = gpxDbHelper.getItem(new KFile(file.getPath()));
			String activity = item != null ? item.getParameter(GpxParameter.ACTIVITY_TYPE) : null;

			if (!Algorithms.isEmpty(activity)) {
				return activity;
			}
		}
		return null;
	}

	@NonNull
	private String getDefaultTags() {
		String defaultTags = OSMAND_TAG;
		defaultActivity = getDefaultActivity();
		if (!Algorithms.isEmpty(defaultActivity)) {
			return defaultTags + ", " + defaultActivity;
		}
		return defaultTags;
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
			callActivity(SendGpxBottomSheetFragment::showOpenStreetMapScreen);
			dismiss();
		});
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_upload;
	}

	@Override
	protected void onRightBottomButtonClick() {
		callActivity(activity -> {
			Editable descrText = messageField.getText();
			Editable tagsText = tagsField.getText();
			String description = descrText != null ? descrText.toString() : "";
			Set<String> tags = tagsText != null ? parseTags(tagsText.toString()) : Collections.emptySet();

			UploadGpxListener listener = getUploadListener(activity);
			UploadGPXFilesTask task = new UploadGPXFilesTask(app, tags, description, defaultActivity, uploadVisibility, listener);
			OsmAndTaskManager.executeTask(task, files);
		});
		dismiss();
	}

	@NonNull
	private Set<String> parseTags(@NonNull String tags) {
		return Arrays.stream(tags.split(","))
				.map(String::trim)
				.filter(tag -> !Algorithms.isEmpty(tag))
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	@NonNull
	private UploadGpxListener getUploadListener(@NonNull FragmentActivity activity) {
		return new UploadGpxListener() {
			@Override
			public void onGpxUploadStarted() {
				updateProgressVisibility(true);
			}

			public void onGpxUploadFinished(String result) {
				updateProgressVisibility(false);

				if (getTargetFragment() instanceof UploadGpxListener listener) {
					listener.onGpxUploadFinished(result);
				}
			}

			private void updateProgressVisibility(boolean visible) {
				if (AndroidUtils.isActivityNotDestroyed(activity)) {
					activity.setProgressBarIndeterminateVisibility(visible);
				}
			}
		};
	}

	protected static void showOpenStreetMapScreen(@NonNull FragmentActivity activity) {
		if (activity instanceof MapActivity mapActivity) {
			BaseSettingsFragment.showInstance(mapActivity, OPEN_STREET_MAP_EDITING);
		} else {
			Bundle prevIntentParams = null;
			if (activity instanceof MyPlacesActivity myPlacesActivity) {
				prevIntentParams = myPlacesActivity.storeCurrentState();
			} else if (activity.getIntent() != null) {
				prevIntentParams = activity.getIntent().getExtras();
			}
			Bundle params = new Bundle();
			params.putString(OPEN_SETTINGS, OPEN_STREET_MAP_EDITING.name());

			MapActivity.launchMapActivityMoveToTop(activity, prevIntentParams, null, params);
		}
	}

	public static void showInstance(@NonNull FragmentManager manager,
	                                @NonNull File[] files, @Nullable Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			SendGpxBottomSheetFragment fragment = new SendGpxBottomSheetFragment();
			fragment.files = files;
			fragment.setTargetFragment(target, 0);
			fragment.setRetainInstance(true);
			fragment.show(manager, TAG);
		}
	}
}