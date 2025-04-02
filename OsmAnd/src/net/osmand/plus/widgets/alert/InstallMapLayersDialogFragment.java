package net.osmand.plus.widgets.alert;

import androidx.appcompat.app.AlertDialog;

import java.util.Map;

public class InstallMapLayersDialogFragment extends SelectionDialogFragment {

	public InstallMapLayersDialogFragment(final AlertDialog alertDialog,
										  final AlertDialogData alertDialogData,
										  final Map<String, CharSequence> itemByKey,
										  final SelectionDialogAdapter adapter) {
		super(alertDialog, alertDialogData, itemByKey, adapter);
	}

	public static class InstallMapLayersDialogFragmentProxy extends SelectionDialogFragmentProxy<InstallMapLayersDialogFragment> {
	}
}
