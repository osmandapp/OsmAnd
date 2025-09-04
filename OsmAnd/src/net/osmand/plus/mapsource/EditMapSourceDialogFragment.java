package net.osmand.plus.mapsource;


import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.map.TileSourceManager;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseFullScreenDialogFragment;
import net.osmand.plus.mapsource.ExpireTimeBottomSheet.OnExpireValueSetListener;
import net.osmand.plus.mapsource.InputZoomLevelsBottomSheet.OnZoomSetListener;
import net.osmand.plus.mapsource.MercatorProjectionBottomSheet.OnMercatorSelectedListener;
import net.osmand.plus.mapsource.TileStorageFormatBottomSheet.OnTileStorageFormatSelectedListener;
import net.osmand.plus.resources.SQLiteTileSource;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FileUtils;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.tools.SimpleTextWatcher;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.List;

public class EditMapSourceDialogFragment extends BaseFullScreenDialogFragment
		implements OnZoomSetListener, OnExpireValueSetListener, OnMercatorSelectedListener,
		OnTileStorageFormatSelectedListener {

	public static final String TAG = EditMapSourceDialogFragment.class.getName();
	private static final Log LOG = PlatformUtil.getLog(EditMapSourceDialogFragment.class);
	static final int EXPIRE_TIME_NEVER = -1;
	private static final String PNG_EXT = "png";
	private static final int MAX_ZOOM = 17;
	private static final int MIN_ZOOM = 5;
	private static final int TILE_SIZE = 256;
	private static final int BIT_DENSITY = 16;
	private static final int AVG_SIZE = 32000;
	private static final String EDIT_LAYER_NAME_KEY = "edit_layer_name_key";
	private static final String MIN_ZOOM_KEY = "min_zoom_key";
	private static final String MAX_ZOOM_KEY = "max_zoom_key";
	private static final String EXPIRE_TIME_KEY = "expire_time_key";
	private static final String ELLIPTIC_KEY = "elliptic_key";
	private static final String SQLITE_DB_KEY = "sqlite_db_key";
	private static final String FROM_TEMPLATE_KEY = "from_template_key";
	private TextInputLayout nameInputLayout;
	private TextInputLayout urlInputLayout;
	private TextInputEditText nameEditText;
	private TextInputEditText urlEditText;
	private LinearLayout contentContainer;
	private FrameLayout saveBtn;
	private TextView saveBtnTitle;
	private TileSourceTemplate template;
	@Nullable
	private String editedLayerName;
	private String urlToLoad = "";
	private int minZoom = MIN_ZOOM;
	private int maxZoom = MAX_ZOOM;
	private int expireTimeMinutes = EXPIRE_TIME_NEVER;
	private boolean elliptic;
	private boolean sqliteDB;
	private boolean fromTemplate;
	private boolean wasChanged;

	public static void showInstance(@NonNull FragmentActivity activity,
	                                @Nullable Fragment targetFragment,
	                                @Nullable String editedFileName) {
		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			EditMapSourceDialogFragment fragment = new EditMapSourceDialogFragment();
			fragment.setTargetFragment(targetFragment, 0);
			fragment.setEditedLayerName(editedFileName);
			fragment.show(fragmentManager, TAG);
		}
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager,
	                                @NonNull TileSourceTemplate template) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			EditMapSourceDialogFragment fragment = new EditMapSourceDialogFragment();
			fragment.setTemplate(template);
			fragment.fromTemplate = true;
			fragmentManager.beginTransaction()
					.add(fragment, TAG)
					.commitAllowingStateLoss();
		}
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		if (savedInstanceState != null) {
			editedLayerName = savedInstanceState.getString(EDIT_LAYER_NAME_KEY);
			minZoom = savedInstanceState.getInt(MIN_ZOOM_KEY);
			maxZoom = savedInstanceState.getInt(MAX_ZOOM_KEY);
			expireTimeMinutes = savedInstanceState.getInt(EXPIRE_TIME_KEY);
			elliptic = savedInstanceState.getBoolean(ELLIPTIC_KEY);
			sqliteDB = savedInstanceState.getBoolean(SQLITE_DB_KEY);
			fromTemplate = savedInstanceState.getBoolean(FROM_TEMPLATE_KEY);
		}
		View root = inflate(R.layout.fragment_edit_map_source, container, false);
		Toolbar toolbar = root.findViewById(R.id.toolbar);
		toolbar.setBackgroundColor(ColorUtilities.getAppBarColor(app, nightMode));
		toolbar.setTitleTextColor(ColorUtilities.getActiveButtonsAndLinksTextColor(app, nightMode));
		toolbar.setTitle(editedLayerName == null ? R.string.add_online_source : R.string.edit_online_source);
		ImageButton iconHelp = root.findViewById(R.id.toolbar_action);
		int activeButtonsColorId = ColorUtilities.getActiveButtonsAndLinksTextColorId(nightMode);
		Drawable closeDrawable = app.getUIUtilities().getIcon(
				AndroidUtils.getNavigationIconResId(app), activeButtonsColorId);
		Drawable helpDrawable = app.getUIUtilities().getIcon(R.drawable.ic_action_help_online, activeButtonsColorId);
		iconHelp.setImageDrawable(helpDrawable);
		iconHelp.setOnClickListener(view -> onHelpClick());
		toolbar.setNavigationIcon(closeDrawable);
		toolbar.setNavigationContentDescription(R.string.shared_string_close);
		toolbar.setNavigationOnClickListener(v -> {
			if (wasChanged || fromTemplate) {
				showExitDialog();
			} else {
				dismiss();
			}
		});
		int boxStrokeColor = ContextCompat.getColor(app, nightMode ? R.color.icon_color_osmand_dark : R.color.icon_color_osmand_light);
		int btnBgColorRes = ColorUtilities.getListBgColorId(nightMode);
		nameInputLayout = root.findViewById(R.id.name_input_layout);
		nameInputLayout.setBoxStrokeColor(boxStrokeColor);
		nameEditText = root.findViewById(R.id.name_edit_text);
		nameEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
		nameEditText.setRawInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
		nameEditText.setOnFocusChangeListener((v, hasFocus) -> {
			if (hasFocus) {
				nameEditText.setSelection(nameEditText.getText().length());
			}
		});
		urlInputLayout = root.findViewById(R.id.url_input_layout);
		urlInputLayout.setBoxStrokeColor(boxStrokeColor);
		urlEditText = root.findViewById(R.id.url_edit_text);
		urlEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
		urlEditText.setRawInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
		urlEditText.setOnFocusChangeListener((v, hasFocus) -> {
			if (hasFocus) {
				urlEditText.setSelection(urlEditText.getText().length());
			}
		});
		contentContainer = root.findViewById(R.id.content_container);
		saveBtn = root.findViewById(R.id.save_button);
		saveBtn.setBackgroundResource(nightMode ? R.drawable.dlg_btn_primary_dark : R.drawable.dlg_btn_primary_light);
		FrameLayout saveBtnBg = root.findViewById(R.id.save_button_bg);
		saveBtnBg.setBackgroundColor(ContextCompat.getColor(app, btnBgColorRes));
		saveBtnTitle = root.findViewById(R.id.save_button_title);
		saveBtnTitle.setTypeface(FontCache.getMediumFont());
		saveBtnTitle.setTextColor(ContextCompat.getColorStateList(app,
				nightMode ? R.color.dlg_btn_primary_text_dark : R.color.dlg_btn_primary_text_light));
		saveBtn.setOnClickListener(view -> {
			saveTemplate();
			dismiss();
		});
		ScrollView scrollView = root.findViewById(R.id.scroll_view);
		scrollView.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
			int pastY;

			@Override
			public void onScrollChanged() {
				int y = scrollView.getScrollY();
				if (pastY != y) {
					pastY = y;
					View view = getDialog().getCurrentFocus();
					AndroidUtils.hideSoftKeyboard(requireActivity(), view);
				}
			}
		});
		if (template == null) {
			template = new TileSourceTemplate("", "", PNG_EXT, MAX_ZOOM, MIN_ZOOM, TILE_SIZE, BIT_DENSITY, AVG_SIZE);
		}
		if (editedLayerName != null && !fromTemplate) {
			if (!editedLayerName.endsWith(IndexConstants.SQLITE_EXT)) {
				File f = app.getAppPath(IndexConstants.TILES_INDEX_DIR + editedLayerName);
				template = TileSourceManager.createTileSourceTemplate(f);
				sqliteDB = false;
			} else {
				List<TileSourceTemplate> knownTemplates = TileSourceManager.getKnownSourceTemplates();
				File tPath = app.getAppPath(IndexConstants.TILES_INDEX_DIR);
				File dir = new File(tPath, editedLayerName);
				SQLiteTileSource sqLiteTileSource = new SQLiteTileSource(app, dir, knownTemplates);
				sqLiteTileSource.couldBeDownloadedFromInternet();
				template = new TileSourceTemplate(sqLiteTileSource.getName(),
						sqLiteTileSource.getUrlTemplate(), PNG_EXT, sqLiteTileSource.getMaximumZoomSupported(),
						sqLiteTileSource.getMinimumZoomSupported(), sqLiteTileSource.getTileSize(),
						sqLiteTileSource.getBitDensity(), AVG_SIZE);
				template.setExpirationTimeMinutes(sqLiteTileSource.getExpirationTimeMinutes());
				template.setEllipticYTile(sqLiteTileSource.isEllipticYTile());
				sqliteDB = true;
			}
		}
		if (savedInstanceState == null) {
			if (fromTemplate) {
				editedLayerName = template.getName();
			}
			urlToLoad = template.getUrlTemplate();
			expireTimeMinutes = template.getExpirationTimeMinutes();
			minZoom = template.getMinimumZoomSupported();
			maxZoom = template.getMaximumZoomSupported();
			elliptic = template.isEllipticYTile();
		}
		updateUi();
		return root;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		outState.putString(EDIT_LAYER_NAME_KEY, editedLayerName);
		outState.putInt(MIN_ZOOM_KEY, minZoom);
		outState.putInt(MAX_ZOOM_KEY, maxZoom);
		outState.putInt(EXPIRE_TIME_KEY, expireTimeMinutes);
		outState.putBoolean(ELLIPTIC_KEY, elliptic);
		outState.putBoolean(SQLITE_DB_KEY, sqliteDB);
		outState.putBoolean(FROM_TEMPLATE_KEY, fromTemplate);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onResume() {
		super.onResume();
		Dialog dialog = getDialog();
		if (dialog != null) {
			dialog.setOnKeyListener((_dialog, keyCode, event) -> {
				if (keyCode == KeyEvent.KEYCODE_BACK) {
					if (event.getAction() == KeyEvent.ACTION_DOWN) {
						return true;
					} else if (wasChanged || fromTemplate) {
						showExitDialog();
					} else {
						dismiss();
					}
					return true;
				}
				return false;
			});
		}
	}

	@Override
	public void onZoomSet(int min, int max) {
		if (isAdded()) {
			minZoom = min;
			maxZoom = max;
			updateDescription(ConfigurationItem.ZOOM_LEVELS);
			wasChanged = true;
		}
	}

	@Override
	public void onExpireValueSet(int expireValue) {
		if (isAdded()) {
			expireTimeMinutes = expireValue;
			updateDescription(ConfigurationItem.EXPIRE_TIME);
			wasChanged = true;
		}
	}

	@Override
	public void onMercatorSelected(boolean elliptic) {
		if (isAdded()) {
			this.elliptic = elliptic;
			updateDescription(ConfigurationItem.MERCATOR_PROJECTION);
			wasChanged = true;
		}
	}

	@Override
	public void onStorageFormatSelected(boolean sqliteDb) {
		if (isAdded()) {
			this.sqliteDB = sqliteDb;
			updateDescription(ConfigurationItem.STORAGE_FORMAT);
			wasChanged = true;
		}
	}

	private void updateSaveButton() {
		boolean enabled = !nameEditText.getText().toString().isEmpty()
				&& !urlEditText.getText().toString().isEmpty();
		saveBtn.setEnabled(enabled);
		saveBtnTitle.setEnabled(enabled);
	}

	private void checkWasChanged() {
		if (!Algorithms.objectEquals(editedLayerName, nameEditText.getText().toString())
				|| !Algorithms.objectEquals(urlToLoad, urlEditText.getText().toString())) {
			wasChanged = true;
		}
	}

	private void saveTemplate() {
		try {
			String newName = nameEditText.getText().toString();
			String urlToLoad = urlEditText.getText().toString();
			template.setName(newName);
			template.setUrlToLoad(urlToLoad.isEmpty() ? null : urlToLoad.replace("{$x}", "{1}").replace("{$y}", "{2}").replace("{$z}", "{0}"));
			template.setMinZoom(minZoom);
			template.setMaxZoom(maxZoom);
			template.setEllipticYTile(elliptic);
			template.setExpirationTimeMinutes(expireTimeMinutes);
			File f = app.getAppPath(IndexConstants.TILES_INDEX_DIR + editedLayerName);
			String ext = null;
			boolean storageChanged = false;
			if (f.exists()) {
				int extIndex = f.getName().lastIndexOf('.');
				ext = extIndex == -1 ? "" : f.getName().substring(extIndex);
				String originalName = extIndex == -1 ? f.getName() : f.getName().substring(0, extIndex);
				if (!Algorithms.objectEquals(newName, originalName)) {
					if (IndexConstants.SQLITE_EXT.equals(ext)) {
						f = FileUtils.renameSQLiteFile(app, f, newName + ext, null);
					} else {
						f.renameTo(app.getAppPath(IndexConstants.TILES_INDEX_DIR + newName));
						f = app.getAppPath(IndexConstants.TILES_INDEX_DIR + newName);
					}
				}
			}
			if (sqliteDB) {
				if (IndexConstants.SQLITE_EXT.equals(ext)) {
					List<TileSourceTemplate> knownTemplates = TileSourceManager.getKnownSourceTemplates();
					SQLiteTileSource sqLiteTileSource = new SQLiteTileSource(app, f, knownTemplates);
					sqLiteTileSource.couldBeDownloadedFromInternet();
					sqLiteTileSource.updateFromTileSourceTemplate(template);
				} else {
					String rule = "";
					String refer = "";
					String randoms = "";
					String userAgent = "";
					boolean invertedY = false;
					boolean inversiveZoom = false;
					boolean timeSupported = expireTimeMinutes > 0;
					long expirationTimeMillis = expireTimeMinutes * 60 * 1000L;

					SQLiteTileSource sqLiteTileSource = new SQLiteTileSource(
							app, newName, minZoom, maxZoom, urlToLoad, randoms,
							elliptic, invertedY, refer, userAgent, timeSupported,
							expirationTimeMillis, inversiveZoom, rule
					);
					sqLiteTileSource.createDataBase();
					storageChanged = f.exists();
				}
			} else {
				settings.installTileSource(template);
				storageChanged = f != null && f.exists() && IndexConstants.SQLITE_EXT.equals(ext);
			}
			if (storageChanged) {
				OsmAndTaskManager.executeTask(new DeleteTilesTask(app), f);
			}
			Fragment fragment = getTargetFragment();
			if (fragment instanceof OnMapSourceUpdateListener) {
				((OnMapSourceUpdateListener) fragment).onMapSourceUpdated();
			}
		} catch (RuntimeException e) {
			LOG.error("Error on saving template " + e);
		}
	}

	private void updateUi() {
		nameEditText.setText(getTitle());
		urlEditText.setText(urlToLoad);
		nameEditText.addTextChangedListener(new MapSourceTextWatcher(nameInputLayout));
		urlEditText.addTextChangedListener(new MapSourceTextWatcher(urlInputLayout));
		updateSaveButton();
		addConfigurationItems(ConfigurationItem.values());
	}

	private void onHelpClick() {
		AndroidUtils.openUrl(requireContext(), R.string.docs_add_online_maps, nightMode);
	}

	private void showExitDialog() {
		Context themedContext = UiUtilities.getThemedContext(getActivity(), nightMode);
		AlertDialog.Builder dismissDialog = new AlertDialog.Builder(themedContext);
		dismissDialog.setTitle(getString(R.string.shared_string_dismiss));
		dismissDialog.setMessage(getString(R.string.exit_without_saving));
		dismissDialog.setNegativeButton(R.string.shared_string_cancel, null);
		dismissDialog.setPositiveButton(R.string.shared_string_exit, (dialog, which) -> dismiss());
		dismissDialog.show();
	}

	@NonNull
	private String getTitle() {
		return editedLayerName != null ? settings.getTileSourceTitle(editedLayerName) : "";
	}

	private String getDescription(ConfigurationItem item) {
		switch (item) {
			case ZOOM_LEVELS:
				String min = getString(R.string.ltr_or_rtl_combine_via_space, getString(R.string.shared_string_min), String.valueOf(minZoom));
				String max = getString(R.string.ltr_or_rtl_combine_via_space, getString(R.string.shared_string_max), String.valueOf(maxZoom));
				return getString(R.string.ltr_or_rtl_combine_via_bold_point, min, max);
			case EXPIRE_TIME:
				return expireTimeMinutes == EXPIRE_TIME_NEVER
						? getString(R.string.shared_string_never)
						: getString(R.string.ltr_or_rtl_combine_via_space, String.valueOf(expireTimeMinutes), getString(R.string.shared_string_minute_lowercase));
			case MERCATOR_PROJECTION:
				return elliptic ? getString(R.string.edit_tilesource_elliptic_tile) : getString(R.string.pseudo_mercator_projection);
			case STORAGE_FORMAT:
				return sqliteDB ? getString(R.string.sqlite_db_file) : getString(R.string.one_image_per_tile);
			default:
				return "";
		}
	}

	private OnClickListener getClickListener(ConfigurationItem item) {
		return view -> {
			FragmentManager fm = getFragmentManager();
			boolean newMapSource = Algorithms.isEmpty(editedLayerName) || fromTemplate;
			if (fm != null && !fm.isStateSaved()) {
				switch (item) {
					case ZOOM_LEVELS:
						InputZoomLevelsBottomSheet.showInstance(
								fm, EditMapSourceDialogFragment.this,
								R.string.map_source_zoom_levels, R.string.map_source_zoom_levels_descr,
								minZoom, maxZoom, newMapSource
						);
						break;
					case EXPIRE_TIME:
						ExpireTimeBottomSheet.showInstance(fm, EditMapSourceDialogFragment.this, expireTimeMinutes);
						break;
					case MERCATOR_PROJECTION:
						MercatorProjectionBottomSheet.showInstance(fm, EditMapSourceDialogFragment.this, elliptic);
						break;
					case STORAGE_FORMAT:
						TileStorageFormatBottomSheet.showInstance(fm, EditMapSourceDialogFragment.this, sqliteDB, newMapSource);
						break;
				}
			}
		};
	}

	private void addConfigurationItems(ConfigurationItem... items) {
		LayoutInflater inflater = UiUtilities.getInflater(requireContext(), nightMode);
		for (ConfigurationItem item : items) {
			View view = inflater.inflate(R.layout.list_item_ui_customization, null);
			((ImageView) view.findViewById(R.id.icon)).setImageDrawable(app.getUIUtilities().getIcon(item.iconRes, nightMode));
			((TextView) view.findViewById(R.id.title)).setText(item.titleRes);
			((TextView) view.findViewById(R.id.sub_title)).setText(getDescription(item));
			view.setOnClickListener(getClickListener(item));
			contentContainer.addView(view);
		}
	}

	private void updateDescription(ConfigurationItem item) {
		View view = contentContainer.getChildAt(item.ordinal());
		((TextView) view.findViewById(R.id.sub_title)).setText(getDescription(item));
	}

	private enum ConfigurationItem {
		ZOOM_LEVELS(R.drawable.ic_action_layers, R.string.shared_string_zoom_levels),
		EXPIRE_TIME(R.drawable.ic_action_time_span, R.string.expire_time),
		MERCATOR_PROJECTION(R.drawable.ic_world_globe_dark, R.string.mercator_projection),
		STORAGE_FORMAT(R.drawable.ic_sdcard, R.string.storage_format);

		@DrawableRes
		public int iconRes;
		@StringRes
		public int titleRes;

		ConfigurationItem(int iconRes, int titleRes) {
			this.titleRes = titleRes;
			this.iconRes = iconRes;
		}
	}

	private void setEditedLayerName(@Nullable String editedLayerName) {
		this.editedLayerName = editedLayerName;
	}

	public void setTemplate(TileSourceTemplate template) {
		this.template = template;
	}

	class MapSourceTextWatcher extends SimpleTextWatcher {
		private final TextInputLayout relatedInputLayout;

		public MapSourceTextWatcher(TextInputLayout textInputLayout) {
			this.relatedInputLayout = textInputLayout;
		}

		@Override
		public void afterTextChanged(Editable editable) {
			if (editable.toString().isEmpty()) {
				relatedInputLayout.setError(relatedInputLayout.getHelperText());
			} else {
				relatedInputLayout.setError(null);
			}
			updateSaveButton();
			checkWasChanged();
		}
	}

	public interface OnMapSourceUpdateListener {
		void onMapSourceUpdated();
	}

	public static class DeleteTilesTask extends AsyncTask<File, Void, Void> {

		private final OsmandApplication app;

		public DeleteTilesTask(OsmandApplication app) {
			this.app = app;
		}

		@Override
		protected Void doInBackground(File... files) {
			for (File file : files) {
				if (Algorithms.removeAllFiles(file)) {
					app.getResourceManager().closeFile(file.getName());
					File tShm = new File(file.getParentFile(), file.getName() + "-shm");
					File tWal = new File(file.getParentFile(), file.getName() + "-wal");
					if (tShm.exists()) {
						Algorithms.removeAllFiles(tShm);
					}
					if (tWal.exists()) {
						Algorithms.removeAllFiles(tWal);
					}
				}
			}
			return null;
		}
	}
}
