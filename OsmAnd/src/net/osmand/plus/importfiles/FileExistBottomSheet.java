package net.osmand.plus.importfiles;

import static net.osmand.plus.utils.UiUtilities.*;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.mapwidgets.configure.CompassVisibilityBottomSheetDialogFragment;

public class FileExistBottomSheet extends MenuBottomSheetDialogFragment {
	public static final String TAG = CompassVisibilityBottomSheetDialogFragment.class.getSimpleName();

	private static final String FILE_NAME_KEY = "file_name";

	private OsmandApplication app;
	private String fileName;
	private FileExistsBottomSheetListener listener;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requiredMyApplication();

		Bundle args = getArguments();
		if (savedInstanceState != null) {
			fileName = savedInstanceState.getString(FILE_NAME_KEY);
		} else if (args != null) {
			fileName = args.getString(FILE_NAME_KEY);
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(createView())
				.create());
	}

	@NonNull
	private View createView() {
		LayoutInflater inflater = getInflater(app, nightMode);
		View view = inflater.inflate(R.layout.fragment_file_exists_bottom_sheet, null);

		View replaceButton = view.findViewById(R.id.replace_button);
		setupReplaceButton(replaceButton);
		View duplicateButton = view.findViewById(R.id.duplicate_button);
		setupDuplicateButton(duplicateButton);

		TextView description = view.findViewById(R.id.description);
		description.setText(getString(R.string.file_already_exists_description, fileName));
		return view;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(FILE_NAME_KEY, fileName);
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, String fileName, FileExistsBottomSheetListener listener) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			FileExistBottomSheet fragment = new FileExistBottomSheet();
			fragment.listener = listener;
			fragment.setRetainInstance(true);
			Bundle args = new Bundle();
			args.putString(FILE_NAME_KEY, fileName);
			fragment.setArguments(args);
			fragment.show(fragmentManager, TAG);
		}
	}

	private void setupReplaceButton(View replaceButton) {
		setupDialogButton(nightMode, replaceButton, DialogButtonType.SECONDARY, R.string.update_existing);
		replaceButton.setOnClickListener(v -> {
			dismiss();
			if (listener != null) {
				listener.onActionSelected(true);
			}
		});
	}

	private void setupDuplicateButton(View replaceButton) {
		setupDialogButton(nightMode, replaceButton, DialogButtonType.PRIMARY, R.string.keep_both);
		replaceButton.setOnClickListener(v -> {
			dismiss();
			if (listener != null) {
				listener.onActionSelected(false);
			}
		});
	}

	public interface FileExistsBottomSheetListener {
		void onActionSelected(boolean overwrite);
	}
}
