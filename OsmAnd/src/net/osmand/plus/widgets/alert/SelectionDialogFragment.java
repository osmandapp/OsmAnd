package net.osmand.plus.widgets.alert;

import static net.osmand.plus.configmap.ConfigureMapDialogs.MapLanguageDialog.getViewByPosition;

import android.app.Dialog;
import android.view.View;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.configmap.ViewOfSettingHighlighter;

import org.threeten.bp.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.KnollFrank.lib.settingssearch.results.Setting;
import de.KnollFrank.lib.settingssearch.results.SettingHighlighter;

class SelectionDialogFragment {

	private final DialogFragment dialogFragment;
	private final AlertDialog alertDialog;
	private final AlertDialogData alertDialogData;
	public final Map<String, CharSequence> itemByKey;
	private final SelectionDialogAdapter adapter;

	public SelectionDialogFragment(final DialogFragment dialogFragment,
								   final AlertDialog alertDialog,
								   final AlertDialogData alertDialogData,
								   final Map<String, CharSequence> itemByKey,
								   final SelectionDialogAdapter adapter) {
		this.dialogFragment = dialogFragment;
		this.alertDialog = alertDialog;
		this.alertDialogData = alertDialogData;
		this.itemByKey = itemByKey;
		this.adapter = adapter;
	}

	public void show(final FragmentManager fragmentManager) {
		dialogFragment.show(fragmentManager, null);
		CustomAlert.applyAdditionalParameters(alertDialog, alertDialogData);
	}

	public void showNow(final FragmentManager fragmentManager) {
		dialogFragment.showNow(fragmentManager, null);
		CustomAlert.applyAdditionalParameters(alertDialog, alertDialogData);
	}

	public void setSelectedIndex(final int selectedIndex) {
		adapter.setSelectedIndex(selectedIndex);
	}

	public Dialog onCreateDialog() {
		return alertDialog;
	}

	public SettingHighlighter getSettingHighlighter() {
		return new ViewOfSettingHighlighter(
				this::getView,
				Duration.ofSeconds(1));
	}

	public ListView getListView() {
		return ((AlertDialog) dialogFragment.getDialog()).getListView();
	}

	public int getIndexedOf(final Setting setting) {
		return getKeys().indexOf(setting.getKey());
	}

	private View getView(final Setting setting) {
		return getViewByPosition(getListView(), getIndexedOf(setting));
	}

	private List<String> getKeys() {
		return new ArrayList<>(itemByKey.keySet());
	}
}
