package net.osmand.plus.widgets.alert;

import static android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE;
import static net.osmand.plus.widgets.alert.AlertDialogData.INVALID_ID;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.OsmandTextFieldBoxes;

import studio.carbonylgroup.textfieldboxes.ExtendedEditText;

public class CustomAlert {

	public static void showSimpleMessage(@NonNull AlertDialogData data, @StringRes int messageId) {
		showSimpleMessage(data, data.getContext().getString(messageId));
	}

	public static void showSimpleMessage(@NonNull AlertDialogData data, @NonNull CharSequence message) {
		AlertDialog.Builder builder = createAlertDialogBuilder(data);
		builder.setMessage(message);

		AlertDialog dialog = builder.show();
		applyAdditionalParameters(dialog, data);
	}

	public static void showInput(@NonNull AlertDialogData data, @NonNull FragmentActivity activity,
	                             @Nullable String initialText, @Nullable String caption) {
		OsmandApplication app = (OsmandApplication) activity.getApplicationContext();
		LayoutInflater inflater = LayoutInflater.from(data.getContext());
		UiUtilities iconsCache = app.getUIUtilities();

		AlertDialog.Builder builder = createAlertDialogBuilder(data);
		View view = inflater.inflate(R.layout.alert_dialog_input, null, false);
		OsmandTextFieldBoxes textBox = view.findViewById(R.id.text_box);
		ExtendedEditText editText = view.findViewById(R.id.edit_text);
		data.putExtra(AlertDialogExtra.EDIT_TEXT, editText);
		builder.setView(view);

		Integer controlsColor = data.getControlsColor();
		if (controlsColor != null) {
			textBox.setPrimaryColor(controlsColor);
		}
		if (caption != null) {
			textBox.setLabelText(caption);
		}
		Drawable iconActionRemove = iconsCache.getIcon(
				R.drawable.ic_action_remove_circle,
				ColorUtilities.getDefaultIconColorId(data.isNightMode())
		);
		textBox.setClearButton(iconActionRemove);
		if (initialText != null) {
			editText.setText(initialText);
		}
		editText.requestFocus();
		AndroidUtils.softKeyboardDelayed(activity, editText);

		AlertDialog dialog = builder.create();
		dialog.getWindow().setSoftInputMode(SOFT_INPUT_STATE_VISIBLE);
		applyAdditionalParameters(dialog, data);
		dialog.show();
	}

	public static void showSingleSelection(@NonNull AlertDialogData data, @NonNull CharSequence[] items,
	                                       int selectedEntryIndex, @Nullable View.OnClickListener itemClickListener) {
		AlertDialog.Builder builder = createAlertDialogBuilder(data);
		SelectionDialogAdapter adapter = new SelectionDialogAdapter(
				data.getContext(), items, selectedEntryIndex, null,
				data.getControlsColor(), data.isNightMode(), itemClickListener, false
		);
		builder.setAdapter(adapter, null);

		AlertDialog dialog = builder.show();
		applyAdditionalParameters(dialog, data);
		adapter.setDialog(dialog);
	}

	public static void showMultiSelection(@NonNull AlertDialogData data, @NonNull CharSequence[] items,
	                                      @Nullable boolean[] checkedItems, @Nullable View.OnClickListener itemClickListener) {
		AlertDialog.Builder builder = createAlertDialogBuilder(data);
		SelectionDialogAdapter adapter = new SelectionDialogAdapter(
				data.getContext(), items, INVALID_ID, checkedItems,
				data.getControlsColor(), data.isNightMode(), itemClickListener, true
		);
		builder.setAdapter(adapter, null);

		AlertDialog dialog = builder.show();
		applyAdditionalParameters(dialog, data);
		adapter.setDialog(dialog);
	}

	private static AlertDialog.Builder createAlertDialogBuilder(@NonNull AlertDialogData data) {
		Context ctx = data.getContext();
		AlertDialog.Builder builder = new Builder(ctx);

		if (data.getTitle() != null) {
			builder.setTitle(data.getTitle());
		} else if (data.getTitleId() != null) {
			builder.setTitle(data.getTitleId());
		}

		if (data.getPositiveButtonTitle() != null) {
			builder.setPositiveButton(data.getPositiveButtonTitle(), data.getPositiveButtonListener());
		} else if (data.getPositiveButtonTitleId() != null) {
			builder.setPositiveButton(data.getPositiveButtonTitleId(), data.getPositiveButtonListener());
		}

		if (data.getNegativeButtonTitle() != null) {
			builder.setNegativeButton(data.getNegativeButtonTitle(), data.getNegativeButtonListener());
		} else if (data.getNegativeButtonTitleId() != null) {
			builder.setNegativeButton(data.getNegativeButtonTitleId(), data.getNegativeButtonListener());
		}

		if (data.getNeutralButtonTitle() != null) {
			builder.setNeutralButton(data.getNeutralButtonTitle(), data.getNeutralButtonListener());
		} else if (data.getNeutralButtonTitleId() != null) {
			builder.setNeutralButton(data.getNeutralButtonTitleId(), data.getNeutralButtonListener());
		}

		if (data.getOnDismissListener() != null) {
			builder.setOnDismissListener(data.getOnDismissListener());
		}
		return builder;
	}

	private static void applyAdditionalParameters(@NonNull AlertDialog dialog, @NonNull AlertDialogData data) {
		if (data.getPositiveButtonTextColor() != null) {
			Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
			button.setTextColor(data.getPositiveButtonTextColor());
		}
	}
}