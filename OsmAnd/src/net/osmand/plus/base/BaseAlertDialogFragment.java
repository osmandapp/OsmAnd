package net.osmand.plus.base;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;

public class BaseAlertDialogFragment extends BaseOsmAndDialogFragment {

	// TODO: temporally solution of theme resolving, we should use a better way
	@NonNull
	protected AlertDialog.Builder createDialogBuilder() {
		return new Builder(getThemedContext());
	}
}
