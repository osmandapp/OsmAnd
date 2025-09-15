package net.osmand.plus.quickaction.actions;

import static net.osmand.plus.quickaction.QuickActionIds.FAVORITE_ACTION_ID;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.FragmentManager;

import net.osmand.data.LatLon;
import net.osmand.plus.GeocodingLookupService.AddressLookupRequest;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.editors.FavoritePointEditor;
import net.osmand.plus.mapcontextmenu.editors.SelectFavouriteGroupBottomSheet;
import net.osmand.plus.mapcontextmenu.editors.SelectPointsCategoryBottomSheet;
import net.osmand.plus.mapcontextmenu.editors.SelectPointsCategoryBottomSheet.CategorySelectionListener;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.myplaces.favorites.FavouritesHelper;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.layers.FavouritesLayer;
import net.osmand.plus.widgets.AutoCompleteTextViewEx;

import java.util.Objects;

public class FavoriteAction extends SelectMapLocationAction {

	public static final QuickActionType TYPE = new QuickActionType(FAVORITE_ACTION_ID,
			"fav.add", FavoriteAction.class).
			nameRes(R.string.shared_string_favorite).iconRes(R.drawable.ic_action_favorite).
			category(QuickActionType.MY_PLACES).nameActionRes(R.string.shared_string_add).
			forceUseExtendedName();

	public static final String KEY_NAME = "name";
	public static final String KEY_DIALOG = "dialog";
	public static final String KEY_CATEGORY_NAME = "category_name";
	public static final String KEY_CATEGORY_COLOR = "category_color";

	private transient AddressLookupRequest lookupRequest;
	private transient ProgressDialog progressDialog;

	public FavoriteAction() {
		super(TYPE);
	}

	public FavoriteAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	protected void onLocationSelected(@NonNull MapActivity mapActivity, @NonNull LatLon latLon, @Nullable Bundle params) {
		String title = getParams().get(KEY_NAME);
		boolean showDialog = Boolean.parseBoolean(getParams().get(KEY_DIALOG));

		if (title == null || title.isEmpty()) {
			showAddressLookupDialog(mapActivity, latLon, showDialog);
		} else {
			addFavorite(mapActivity, latLon, title, !showDialog);
		}
	}

	@Override
	@Nullable
	protected Object getLocationIcon(@NonNull MapActivity mapActivity) {
		FavouritesLayer layer = mapActivity.getMapLayers().getFavouritesLayer();
		return layer.createDefaultFavoriteIcon(getColor(mapActivity));
	}

	private void addFavorite(MapActivity mapActivity, LatLon latLon, String title, boolean autoFill) {
		FavoritePointEditor editor = mapActivity.getContextMenu().getFavoritePointEditor();
		if (editor != null) {
			editor.add(latLon, title, getCategoryName(), getCategoryColor(), autoFill);
		}
	}

	private void showAddressLookupDialog(@NonNull MapActivity mapActivity, @NonNull LatLon latLon, boolean showDialog) {
		progressDialog = createProgressDialog(mapActivity, new DialogOnClickListener() {
			@Override
			public void skipOnClick() {
				onClick(mapActivity.getString(R.string.favorite), !showDialog);
			}

			@Override
			public void enterNameOnClick() {
				onClick("", false);
			}

			private void onClick(String title, boolean autoFill) {
				mapActivity.getApp().getGeocodingLookupService().cancel(lookupRequest);
				dismissProgressDialog();
				addFavorite(mapActivity, latLon, title, autoFill);
			}
		});
		progressDialog.show();

		lookupRequest = new AddressLookupRequest(latLon, address -> {
			dismissProgressDialog();
			addFavorite(mapActivity, latLon, address, !showDialog);
		}, null);
		mapActivity.getApp().getGeocodingLookupService().lookupAddress(lookupRequest);
	}

	@NonNull
	private ProgressDialog createProgressDialog(Context context, @NonNull DialogOnClickListener listener) {
		ProgressDialog dialog = new ProgressDialog(context);
		dialog.setCancelable(false);
		dialog.setMessage(context.getString(R.string.search_address));
		dialog.setButton(Dialog.BUTTON_POSITIVE, context.getString(R.string.shared_string_skip),
				(d, which) -> listener.skipOnClick());
		dialog.setButton(Dialog.BUTTON_NEGATIVE, context.getString(R.string.access_hint_enter_name),
				(d, which) -> listener.enterNameOnClick());
		return dialog;
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity, boolean nightMode) {
		View root = UiUtilities.inflate(parent.getContext(), nightMode, R.layout.quick_action_add_favorite, parent, false);
		setupPointLocationView(root.findViewById(R.id.point_location_container), mapActivity);
		parent.addView(root);

		AutoCompleteTextViewEx categoryEdit = root.findViewById(R.id.category_edit);

		if (!getParams().isEmpty()) {
			setupUIFromParams(root, mapActivity);
		} else {
			setupUIDefaults(root, mapActivity);
		}

		categoryEdit.setOnClickListener(view -> {
			FragmentManager manager = mapActivity.getSupportFragmentManager();
			CategorySelectionListener listener = (pointsGroup) -> fillGroupParams(root, pointsGroup.getName(), pointsGroup.getColor());
			SelectFavouriteGroupBottomSheet.showInstance(manager, "", listener);
		});

		SelectPointsCategoryBottomSheet fragment = (SelectPointsCategoryBottomSheet)
				mapActivity.getSupportFragmentManager().findFragmentByTag(SelectPointsCategoryBottomSheet.TAG);
		if (fragment != null) {
			fragment.setListener((pointsGroup) -> fillGroupParams(root, pointsGroup.getName(), pointsGroup.getColor()));
		}
	}

	private void setupUIFromParams(@NonNull View root, @NonNull MapActivity mapActivity) {
		AutoCompleteTextViewEx categoryEdit = root.findViewById(R.id.category_edit);
		SwitchCompat showDialog = root.findViewById(R.id.saveButton);
		ImageView categoryImage = root.findViewById(R.id.category_image);
		EditText name = root.findViewById(R.id.name_edit);

		showDialog.setChecked(Boolean.parseBoolean(getParams().get(KEY_DIALOG)));
		categoryImage.setColorFilter(getCategoryColor());
		name.setText(getParams().get(KEY_NAME));
		categoryEdit.setText(getCategoryName());

		if (Objects.requireNonNull(getParams().get(KEY_NAME)).isEmpty() && getCategoryColor() == 0) {
			categoryEdit.setText(mapActivity.getString(R.string.shared_string_favorites));
			categoryImage.setColorFilter(mapActivity.getColor(R.color.color_favorite));
		}
	}

	private void setupUIDefaults(@NonNull View root, @NonNull MapActivity mapActivity) {
		FavouritesHelper helper = mapActivity.getApp().getFavoritesHelper();
		AutoCompleteTextViewEx categoryEdit = root.findViewById(R.id.category_edit);
		ImageView categoryImage = root.findViewById(R.id.category_image);

		if (!helper.getFavoriteGroups().isEmpty()) {
			FavoriteGroup group = helper.getFavoriteGroups().get(0);
			int color = group.getColor() == 0 ? mapActivity.getColor(R.color.color_favorite) : group.getColor();
			categoryEdit.setText(group.getDisplayName(mapActivity));
			categoryImage.setColorFilter(color);

			getParams().put(KEY_CATEGORY_NAME, group.getName());
			getParams().put(KEY_CATEGORY_COLOR, String.valueOf(group.getColor()));
		} else {
			categoryEdit.setText(mapActivity.getString(R.string.shared_string_favorites));
			categoryImage.setColorFilter(mapActivity.getColor(R.color.color_favorite));

			getParams().put(KEY_CATEGORY_NAME, "");
			getParams().put(KEY_CATEGORY_COLOR, "0");
		}
	}

	@Override
	public boolean fillParams(@NonNull View root, @NonNull MapActivity mapActivity) {
		setParameter(KEY_NAME, ((EditText) root.findViewById(R.id.name_edit)).getText().toString());
		setParameter(KEY_DIALOG, Boolean.toString(((SwitchCompat) root.findViewById(R.id.saveButton)).isChecked()));
		return super.fillParams(root, mapActivity);
	}

	private void fillGroupParams(View root, String name, int color) {
		if (color == 0) {
			color = root.getContext().getColor(R.color.color_favorite);
		}

		((AutoCompleteTextViewEx) root.findViewById(R.id.category_edit)).setText(name);
		((ImageView) root.findViewById(R.id.category_image)).setColorFilter(color);

		getParams().put(KEY_CATEGORY_NAME, name);
		getParams().put(KEY_CATEGORY_COLOR, String.valueOf(color));
	}

	private void dismissProgressDialog() {
		if (progressDialog != null) {
			progressDialog.dismiss();
		}
	}

	@ColorInt
	private int getColor(@NonNull Context context) {
		int categoryColor = getCategoryColor();
		int defaultColor = ColorUtilities.getColor(context, R.color.color_favorite);
		return categoryColor != 0 ? categoryColor : defaultColor;
	}

	@Nullable
	private String getCategoryName() {
		return getParams().get(KEY_CATEGORY_NAME);
	}

	@ColorInt
	private int getCategoryColor() {
		return Integer.parseInt(getParams().getOrDefault(KEY_CATEGORY_COLOR, "0"));
	}

	private interface DialogOnClickListener {
		void skipOnClick();
		void enterNameOnClick();
	}
}
