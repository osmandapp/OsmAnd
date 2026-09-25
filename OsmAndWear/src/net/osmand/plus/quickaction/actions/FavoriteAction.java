package net.osmand.plus.quickaction.actions;

import static net.osmand.plus.quickaction.QuickActionIds.FAVORITE_ACTION_ID;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.FragmentManager;

import net.osmand.data.LatLon;
import net.osmand.plus.GeocodingLookupService.AddressLookupRequest;
import net.osmand.plus.GeocodingLookupService.OnAddressLookupResult;
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
import net.osmand.plus.widgets.AutoCompleteTextViewEx;

public class FavoriteAction extends QuickAction {


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
	public void execute(@NonNull MapActivity mapActivity) {
		LatLon latLon = getMapLocation(mapActivity);
		String title = getParams().get(KEY_NAME);
		if (title == null || title.isEmpty()) {
			progressDialog = createProgressDialog(mapActivity, new DialogOnClickListener() {
				@Override
				public void skipOnClick() {
					onClick(mapActivity.getString(R.string.favorite), !Boolean.valueOf(getParams().get(KEY_DIALOG)));
				}

				@Override
				public void enterNameOnClick() {
					onClick("", false);
				}

				private void onClick(String title, boolean autoFill) {
					mapActivity.getMyApplication().getGeocodingLookupService().cancel(lookupRequest);
					dismissProgressDialog();
					addFavorite(mapActivity, latLon, title, autoFill);
				}
			});
			progressDialog.show();

			lookupRequest = new AddressLookupRequest(latLon, new OnAddressLookupResult() {
				@Override
				public void geocodingDone(String address) {
					dismissProgressDialog();
					addFavorite(mapActivity, latLon, address, !Boolean.valueOf(getParams().get(KEY_DIALOG)));
				}
			}, null);

			mapActivity.getMyApplication().getGeocodingLookupService().lookupAddress(lookupRequest);
		} else {
			addFavorite(mapActivity, latLon, title, !Boolean.valueOf(getParams().get(KEY_DIALOG)));
		}
	}

	private ProgressDialog createProgressDialog(Context context, @NonNull DialogOnClickListener listener) {
		ProgressDialog dialog = new ProgressDialog(context);
		dialog.setCancelable(false);
		dialog.setMessage(context.getString(R.string.search_address));
		dialog.setButton(Dialog.BUTTON_POSITIVE, context.getString(R.string.shared_string_skip),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						listener.skipOnClick();
					}
				});
		dialog.setButton(Dialog.BUTTON_NEGATIVE, context.getString(R.string.access_hint_enter_name),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						listener.enterNameOnClick();
					}
				});
		return dialog;
	}

	private void dismissProgressDialog() {
		if (progressDialog != null) {
			progressDialog.dismiss();
		}
	}

	private void addFavorite(MapActivity mapActivity, LatLon latLon, String title, boolean autoFill) {
		FavoritePointEditor favoritePointEditor = mapActivity.getContextMenu().getFavoritePointEditor();
		if (favoritePointEditor != null) {
			favoritePointEditor.add(latLon, title, getParams().get(KEY_CATEGORY_NAME),
					Integer.valueOf(getParams().get(KEY_CATEGORY_COLOR)), autoFill);
		}
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity) {
		FavouritesHelper helper = mapActivity.getMyApplication().getFavoritesHelper();
		View root = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_add_favorite, parent, false);

		parent.addView(root);

		AutoCompleteTextViewEx categoryEdit = root.findViewById(R.id.category_edit);
		SwitchCompat showDialog = root.findViewById(R.id.saveButton);
		ImageView categoryImage = root.findViewById(R.id.category_image);
		EditText name = root.findViewById(R.id.name_edit);

		if (!getParams().isEmpty()) {
			showDialog.setChecked(Boolean.valueOf(getParams().get(KEY_DIALOG)));
			categoryImage.setColorFilter(Integer.valueOf(getParams().get(KEY_CATEGORY_COLOR)));
			name.setText(getParams().get(KEY_NAME));
			categoryEdit.setText(getParams().get(KEY_CATEGORY_NAME));

			if (getParams().get(KEY_NAME).isEmpty() && Integer.valueOf(getParams().get(KEY_CATEGORY_COLOR)) == 0) {

				categoryEdit.setText(mapActivity.getString(R.string.shared_string_favorites));
				categoryImage.setColorFilter(mapActivity.getColor(R.color.color_favorite));
			}
		} else if (helper.getFavoriteGroups().size() > 0) {
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

		categoryEdit.setOnClickListener(view -> {
			FragmentManager manager = mapActivity.getSupportFragmentManager();
			CategorySelectionListener listener = (pointsGroup) -> fillGroupParams(root, pointsGroup.getName(), pointsGroup.getColor());
			SelectFavouriteGroupBottomSheet.showInstance(manager, "", listener);
		});

		SelectPointsCategoryBottomSheet dialogFragment = (SelectPointsCategoryBottomSheet)
				mapActivity.getSupportFragmentManager().findFragmentByTag(SelectPointsCategoryBottomSheet.TAG);
		if (dialogFragment != null) {
			dialogFragment.setListener((pointsGroup) -> fillGroupParams(root, pointsGroup.getName(), pointsGroup.getColor()));
		}
	}

	@Override
	public boolean fillParams(@NonNull View root, @NonNull MapActivity mapActivity) {
		getParams().put(KEY_NAME, ((EditText) root.findViewById(R.id.name_edit)).getText().toString());
		getParams().put(KEY_DIALOG, Boolean.toString(((SwitchCompat) root.findViewById(R.id.saveButton)).isChecked()));
		return true;
	}

	private void fillGroupParams(View root, String name, int color) {
		if (color == 0)
			color = root.getContext().getColor(R.color.color_favorite);

		((AutoCompleteTextViewEx) root.findViewById(R.id.category_edit)).setText(name);
		((ImageView) root.findViewById(R.id.category_image)).setColorFilter(color);

		getParams().put(KEY_CATEGORY_NAME, name);
		getParams().put(KEY_CATEGORY_COLOR, String.valueOf(color));
	}

	private interface DialogOnClickListener {

		void skipOnClick();

		void enterNameOnClick();
	}
}
