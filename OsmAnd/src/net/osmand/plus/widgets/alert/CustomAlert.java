package net.osmand.plus.widgets.alert;

import static android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE;
import static net.osmand.plus.configmap.ConfigureMapDialogs.MapLanguageDialog.getViewByPosition;
import static net.osmand.plus.widgets.alert.AlertDialogData.INVALID_ID;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.configmap.ViewOfSettingHighlighter;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.OsmandTextFieldBoxes;

import org.threeten.bp.Duration;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import de.KnollFrank.lib.settingssearch.client.searchDatabaseConfig.InitializePreferenceFragmentWithFragmentBeforeOnCreate;
import de.KnollFrank.lib.settingssearch.results.Setting;
import de.KnollFrank.lib.settingssearch.results.SettingHighlighter;
import de.KnollFrank.lib.settingssearch.results.SettingHighlighterProvider;
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

	public static SingleSelectionDialogFragment createSingleSelectionDialogFragment(
			final @NonNull AlertDialogData data,
			final Map<String, CharSequence> itemByKey,
			final int selectedEntryIndex,
			final View.OnClickListener itemClickListener) {
		final SelectionDialogAdapter adapter =
				new SelectionDialogAdapter(
						data.getContext(),
						itemByKey.values().toArray(new CharSequence[0]),
						selectedEntryIndex,
						null,
						data.getControlsColor(),
						data.isNightMode(),
						itemClickListener,
						false);
		final AlertDialog alertDialog =
				CustomAlert
						.createAlertDialogBuilder(data)
						.setAdapter(adapter, null)
						.create();
		adapter.setDialog(alertDialog);
		return new SingleSelectionDialogFragment(alertDialog, data, itemByKey, adapter);
	}

	public static class SingleSelectionDialogFragment extends DialogFragment implements SettingHighlighterProvider {

		private final AlertDialog alertDialog;
		private final AlertDialogData alertDialogData;
		private final Map<String, CharSequence> itemByKey;
		private final SelectionDialogAdapter adapter;

		public SingleSelectionDialogFragment(final AlertDialog alertDialog,
											 final AlertDialogData alertDialogData,
											 final Map<String, CharSequence> itemByKey,
											 final SelectionDialogAdapter adapter) {
			this.alertDialog = alertDialog;
			this.alertDialogData = alertDialogData;
			this.itemByKey = itemByKey;
			this.adapter = adapter;
		}

		public void show(final FragmentManager fragmentManager) {
			show(fragmentManager, null);
			applyAdditionalParameters(alertDialog, alertDialogData);
		}

		public void showNow(final FragmentManager fragmentManager) {
			showNow(fragmentManager, null);
			applyAdditionalParameters(alertDialog, alertDialogData);
		}

		public void setSelectedIndex(final int selectedIndex) {
			adapter.setSelectedIndex(selectedIndex);
		}

		@NonNull
		@Override
		public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
			return alertDialog;
		}

		@Override
		public SettingHighlighter getSettingHighlighter() {
			return new ViewOfSettingHighlighter(
					this::getView,
					Duration.ofSeconds(1));
		}

		private View getView(final Setting setting) {
			return getViewByPosition(getListView(), getIndexedOf(setting));
		}

		public ListView getListView() {
			return ((AlertDialog) getDialog()).getListView();
		}

		public int getIndexedOf(final Setting setting) {
			return getKeys().indexOf(setting.getKey());
		}

		private List<String> getKeys() {
			return new ArrayList<>(itemByKey.keySet());
		}

		public static class PreferenceFragment extends PreferenceFragmentCompat implements InitializePreferenceFragmentWithFragmentBeforeOnCreate<SingleSelectionDialogFragment> {

			private SingleSelectionDialogFragment singleSelectionDialogFragment;

			@Override
			public void initializePreferenceFragmentWithFragmentBeforeOnCreate(final SingleSelectionDialogFragment singleSelectionDialogFragment) {
				this.singleSelectionDialogFragment = singleSelectionDialogFragment;
			}

			public SingleSelectionDialogFragment getPrincipal() {
				return singleSelectionDialogFragment;
			}

			@Override
			public void onCreatePreferences(@Nullable final Bundle savedInstanceState, @Nullable final String rootKey) {
				setPreferenceScreen(asPreferenceScreen(asPreferences(singleSelectionDialogFragment.itemByKey)));
			}

			private Collection<Preference> asPreferences(final Map<String, CharSequence> itemByKey) {
				return new TitleByKey2PreferencesConverter(getContext()).asPreferences(itemByKey);
			}

			private PreferenceScreen asPreferenceScreen(final Collection<Preference> preferences) {
				return new PreferenceScreenFactory(this).asPreferenceScreen(preferences);
			}
		}
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

	public static void showMultiSelection(final @NonNull AlertDialogData data,
										  final @NonNull CharSequence[] items,
										  final @Nullable boolean[] checkedItems,
										  final @Nullable View.OnClickListener itemClickListener,
										  final FragmentManager fragmentManager) {
		AlertDialog.Builder builder = createAlertDialogBuilder(data);
		SelectionDialogAdapter adapter = new SelectionDialogAdapter(
				data.getContext(), items, INVALID_ID, checkedItems,
				data.getControlsColor(), data.isNightMode(), itemClickListener, true
		);
		builder.setAdapter(adapter, null);

		final AlertDialog dialog = builder.create();
		adapter.setDialog(dialog);
		final MultiSelectionDialogFragment multiSelectionDialogFragment =
				new MultiSelectionDialogFragment(
						dialog,
						data,
						Arrays
								.stream(items)
								.collect(
										Collectors.toMap(
												CharSequence::toString,
												Function.identity())),
						adapter);
		multiSelectionDialogFragment.show(fragmentManager);
	}

	// FK-TODO: DRY with SingleSelectionDialogFragment
	public static class MultiSelectionDialogFragment extends DialogFragment implements SettingHighlighterProvider {

		private final AlertDialog alertDialog;
		private final AlertDialogData alertDialogData;
		private final Map<String, CharSequence> itemByKey;
		private final SelectionDialogAdapter adapter;

		public MultiSelectionDialogFragment(final AlertDialog alertDialog,
											final AlertDialogData alertDialogData,
											final Map<String, CharSequence> itemByKey,
											final SelectionDialogAdapter adapter) {
			this.alertDialog = alertDialog;
			this.alertDialogData = alertDialogData;
			this.itemByKey = itemByKey;
			this.adapter = adapter;
		}

		public void show(final FragmentManager fragmentManager) {
			show(fragmentManager, null);
			applyAdditionalParameters(alertDialog, alertDialogData);
		}

		public void showNow(final FragmentManager fragmentManager) {
			showNow(fragmentManager, null);
			applyAdditionalParameters(alertDialog, alertDialogData);
		}

		public void setSelectedIndex(final int selectedIndex) {
			adapter.setSelectedIndex(selectedIndex);
		}

		@NonNull
		@Override
		public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
			return alertDialog;
		}

		@Override
		public SettingHighlighter getSettingHighlighter() {
			return new ViewOfSettingHighlighter(
					this::getView,
					Duration.ofSeconds(1));
		}

		private View getView(final Setting setting) {
			return getViewByPosition(getListView(), getIndexedOf(setting));
		}

		public ListView getListView() {
			return ((AlertDialog) getDialog()).getListView();
		}

		public int getIndexedOf(final Setting setting) {
			return getKeys().indexOf(setting.getKey());
		}

		private List<String> getKeys() {
			return new ArrayList<>(itemByKey.keySet());
		}

		public static class PreferenceFragment extends PreferenceFragmentCompat implements InitializePreferenceFragmentWithFragmentBeforeOnCreate<MultiSelectionDialogFragment> {

			private MultiSelectionDialogFragment multiSelectionDialogFragment;

			@Override
			public void initializePreferenceFragmentWithFragmentBeforeOnCreate(final MultiSelectionDialogFragment multiSelectionDialogFragment) {
				this.multiSelectionDialogFragment = multiSelectionDialogFragment;
			}

			public MultiSelectionDialogFragment getPrincipal() {
				return multiSelectionDialogFragment;
			}

			@Override
			public void onCreatePreferences(@Nullable final Bundle savedInstanceState, @Nullable final String rootKey) {
				setPreferenceScreen(asPreferenceScreen(asPreferences(multiSelectionDialogFragment.itemByKey)));
			}

			private Collection<Preference> asPreferences(final Map<String, CharSequence> itemByKey) {
				return new TitleByKey2PreferencesConverter(getContext()).asPreferences(itemByKey);
			}

			private PreferenceScreen asPreferenceScreen(final Collection<Preference> preferences) {
				return new PreferenceScreenFactory(this).asPreferenceScreen(preferences);
			}
		}
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