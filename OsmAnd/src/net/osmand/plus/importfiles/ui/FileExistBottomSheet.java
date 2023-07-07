package net.osmand.plus.importfiles.ui;

import static net.osmand.plus.utils.UiUtilities.getInflater;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.utils.AndroidUtils;

public class FileExistBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = FileExistBottomSheet.class.getSimpleName();

	private static final String FILE_NAME_KEY = "file_name";

	private OsmandApplication app;
	private String fileName;
	private SaveExistingFileListener listener;

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
		items.add(new BaseBottomSheetItem.Builder().setCustomView(createView()).create());
	}

	@NonNull
	private View createView() {
		LayoutInflater inflater = getInflater(app, nightMode);
		View view = inflater.inflate(R.layout.fragment_file_exists_bottom_sheet, null);

		setupReplaceButton(view.findViewById(R.id.replace_button));
		setupDuplicateButton(view.findViewById(R.id.duplicate_button));

		TextView description = view.findViewById(R.id.description);
		description.setText(getString(R.string.file_already_exists_description, fileName));
		return view;
	}

	private void setupReplaceButton(@NonNull View view) {
		view.setOnClickListener(v -> {
			if (listener != null) {
				listener.saveExistingFile(true);
			}
			dismiss();
		});
	}

	private void setupDuplicateButton(@NonNull View view) {
		view.setOnClickListener(v -> {
			if (listener != null) {
				listener.saveExistingFile(false);
			}
			dismiss();
		});
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(FILE_NAME_KEY, fileName);
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull String fileName, @Nullable SaveExistingFileListener listener) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			Bundle args = new Bundle();
			args.putString(FILE_NAME_KEY, fileName);

			FileExistBottomSheet fragment = new FileExistBottomSheet();
			fragment.listener = listener;
			fragment.setArguments(args);
			fragment.setRetainInstance(true);
			fragment.show(manager, TAG);
		}
	}

	public interface SaveExistingFileListener {
		void saveExistingFile(boolean overwrite);
	}
}
