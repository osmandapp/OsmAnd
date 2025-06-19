package net.osmand.plus.base;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.view.ContextThemeWrapper;

import net.osmand.plus.R;

public class BaseAlertDialogFragment extends BaseOsmAndDialogFragment {

	// TODO: temporally solution of theme resolving, we should use a better way
	@NonNull
	protected AlertDialog.Builder createDialogBuilder() {
		return new Builder(getThemedContext());
	}

	@NonNull
	protected Context getThemedContext() {
		return new ContextThemeWrapper(requireActivity(), getDialogThemeId());
	}

	@StyleRes
	protected int getDialogThemeId() {
		return nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
	}
}
