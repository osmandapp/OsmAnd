package net.osmand.plus.download.ui;

import static net.osmand.map.WorldRegion.WORLD_BASEMAP;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.BottomSheetDialogFragment;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.utils.AndroidUtils;

import java.util.ArrayList;
import java.util.List;

public class AskMapDownloadFragment extends BottomSheetDialogFragment {

	public static final String TAG = AskMapDownloadFragment.class.getSimpleName();

	private static final String ITEM_FILENAME_KEY = "item_filename_key";

	private IndexItem indexItem;

	public void setIndexItem(IndexItem indexItem) {
		this.indexItem = indexItem;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			String itemFileName = savedInstanceState.getString(ITEM_FILENAME_KEY);
			if (itemFileName != null) {
				indexItem = app.getDownloadThread().getIndexes().getIndexItem(itemFileName);
			}
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		updateNightMode();
		View view = inflate(R.layout.ask_map_download_fragment, container, false);

		ImageView icon = view.findViewById(R.id.titleIconImageView);
		icon.setImageDrawable(getIcon(R.drawable.ic_map, R.color.osmand_orange));

		Button actionButtonOk = view.findViewById(R.id.actionButtonOk);
		if (indexItem != null) {
			if (indexItem.getBasename().equalsIgnoreCase(WORLD_BASEMAP)) {
				((TextView) view.findViewById(R.id.titleTextView)).setText(R.string.index_item_world_basemap);
				((TextView) view.findViewById(R.id.descriptionTextView)).setText(R.string.world_map_download_descr);
			}
			actionButtonOk.setText(getString(R.string.shared_string_download) + " (" + indexItem.getSizeDescription(view.getContext()) + ")");
		}

		ImageButton closeButton = view.findViewById(R.id.closeImageButton);
		closeButton.setImageDrawable(getContentIcon(R.drawable.ic_action_remove_dark));
		closeButton.setOnClickListener(v -> dismiss());

		actionButtonOk.setOnClickListener(v -> {
			DownloadActivity activity = (DownloadActivity) getActivity();
			if (indexItem != null && activity != null) {
				activity.startDownload(indexItem);
				dismiss();
			}
		});
		view.findViewById(R.id.actionButtonCancel).setOnClickListener(v -> dismiss());

		return view;
	}

	@Nullable
	@Override
	public List<Integer> getBottomContainersIds() {
		List<Integer> ids = new ArrayList<>();
		ids.add(R.id.root_view);
		return ids;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		if (indexItem != null) {
			outState.putString(ITEM_FILENAME_KEY, indexItem.getFileName());
		}
	}

	public static void showInstance(@NonNull FragmentActivity activity, @NonNull IndexItem item) {
		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			AskMapDownloadFragment fragment = new AskMapDownloadFragment();
			fragment.setIndexItem(item);
			fragment.show(fragmentManager, AskMapDownloadFragment.TAG);
		}
	}
}