package net.osmand.plus.quickaction.actions;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import net.osmand.GPXUtilities.PointsCategory;
import net.osmand.data.FavouritePoint.BackgroundType;
import net.osmand.data.LatLon;
import net.osmand.plus.GeocodingLookupService.AddressLookupRequest;
import net.osmand.plus.GeocodingLookupService.OnAddressLookupResult;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.editors.FavoritePointEditor;
import net.osmand.plus.mapcontextmenu.editors.SelectPointsCategoryBottomSheet;
import net.osmand.plus.mapcontextmenu.editors.SelectPointsCategoryBottomSheet.CategorySelectionListener;
import net.osmand.plus.myplaces.FavouritesDbHelper;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.widgets.AutoCompleteTextViewEx;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import static net.osmand.GPXUtilities.DEFAULT_ICON_NAME;

public class FavoriteAction extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(3,
			"fav.add", FavoriteAction.class).
			nameRes(R.string.quick_action_add_favorite).iconRes(R.drawable.ic_action_favorite).
			category(QuickActionType.CREATE_CATEGORY);

	public static final String KEY_NAME = "name";
	public static final String KEY_DIALOG = "dialog";

	public static final String KEY_CATEGORY_NAME = "category_name";
	public static final String KEY_CATEGORY_COLOR = "category_color";
	public static final String KEY_CATEGORY_ICON_NAME = "category_icon_name";
	public static final String KEY_CATEGORY_BACKGROUND_TYPE = "category_background_type";

	private transient AddressLookupRequest lookupRequest;
	private transient ProgressDialog progressDialog;

	public FavoriteAction() {
		super(TYPE);
	}

	public FavoriteAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull final MapActivity mapActivity) {
		final LatLon latLon = mapActivity.getMapView().getCurrentRotatedTileBox().getCenterLatLon();
		final String title = getParams().get(KEY_NAME);

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

	private ProgressDialog createProgressDialog(Context context, @NonNull final DialogOnClickListener listener) {
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

	private void addFavorite(@NonNull MapActivity mapActivity, @NonNull LatLon latLon, @NonNull String title, boolean autoFill) {
		FavoritePointEditor favoritePointEditor = mapActivity.getContextMenu().getFavoritePointEditor();
		if (favoritePointEditor != null) {
			PointsCategory category = new PointsCategory(
					getParams().get(KEY_CATEGORY_NAME),
					Integer.valueOf(getParams().get(KEY_CATEGORY_COLOR)),
					getParams().get(KEY_CATEGORY_ICON_NAME),
					getParams().get(KEY_CATEGORY_BACKGROUND_TYPE)
			);
			favoritePointEditor.add(latLon, title, category, autoFill);
		}
	}

	@Override
	public void drawUI(@NonNull final ViewGroup parent, @NonNull final MapActivity mapActivity) {

		FavouritesDbHelper helper = mapActivity.getMyApplication().getFavorites();

		final View root = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_add_favorite, parent, false);

		parent.addView(root);

		AutoCompleteTextViewEx categoryEdit = (AutoCompleteTextViewEx) root.findViewById(R.id.category_edit);
		SwitchCompat showDialog = (SwitchCompat) root.findViewById(R.id.saveButton);
		ImageView categoryImage = (ImageView) root.findViewById(R.id.category_image);
		EditText name = (EditText) root.findViewById(R.id.name_edit);

		if (!getParams().isEmpty()) {

			showDialog.setChecked(Boolean.valueOf(getParams().get(KEY_DIALOG)));
			categoryImage.setColorFilter(Integer.valueOf(getParams().get(KEY_CATEGORY_COLOR)));
			name.setText(getParams().get(KEY_NAME));
			categoryEdit.setText(getParams().get(KEY_CATEGORY_NAME));

			if (getParams().get(KEY_NAME).isEmpty() && Integer.valueOf(getParams().get(KEY_CATEGORY_COLOR)) == 0) {

				categoryEdit.setText(mapActivity.getString(R.string.shared_string_favorites));
				categoryImage.setColorFilter(mapActivity.getResources().getColor(R.color.color_favorite));
			}

		} else if (helper.getFavoriteGroups().size() > 0) {

			FavouritesDbHelper.FavoriteGroup group = helper.getFavoriteGroups().get(0);

			int color = group.getColor() == 0 ? mapActivity.getResources().getColor(R.color.color_favorite) : group.getColor();
			categoryEdit.setText(group.getDisplayName(mapActivity));
			categoryImage.setColorFilter(color);

			getParams().put(KEY_CATEGORY_NAME, group.getName());
			getParams().put(KEY_CATEGORY_COLOR, String.valueOf(group.getColor()));
			getParams().put(KEY_CATEGORY_ICON_NAME, group.getIconName(null));
			BackgroundType backgroundType = group.getShape(null);
			getParams().put(KEY_CATEGORY_BACKGROUND_TYPE, backgroundType == null ? null : backgroundType.getTypeName());

		} else {

			categoryEdit.setText(mapActivity.getString(R.string.shared_string_favorites));
			categoryImage.setColorFilter(mapActivity.getResources().getColor(R.color.color_favorite));

			getParams().put(KEY_CATEGORY_NAME, "");
			getParams().put(KEY_CATEGORY_COLOR, "0");
			getParams().put(KEY_CATEGORY_ICON_NAME, null);
			getParams().put(KEY_CATEGORY_BACKGROUND_TYPE, null);
		}

		categoryEdit.setOnClickListener(view -> {
			FragmentManager fragmentManager = mapActivity.getSupportFragmentManager();
			CategorySelectionListener listener = (category) -> fillGroupParams(root, category);
			SelectPointsCategoryBottomSheet.showSelectFavoriteCategoryFragment(fragmentManager, listener, "");
		});

		SelectPointsCategoryBottomSheet dialogFragment = (SelectPointsCategoryBottomSheet)
				mapActivity.getSupportFragmentManager().findFragmentByTag(SelectPointsCategoryBottomSheet.TAG);
		if (dialogFragment != null) {
			dialogFragment.setSelectionListener((category) -> fillGroupParams(root, category));
		}
	}

	@Override
	public boolean fillParams(@NonNull View root, @NonNull MapActivity mapActivity) {

		getParams().put(KEY_NAME, ((EditText) root.findViewById(R.id.name_edit)).getText().toString());
		getParams().put(KEY_DIALOG, Boolean.toString(((SwitchCompat) root.findViewById(R.id.saveButton)).isChecked()));

		return true;
	}

	private void fillGroupParams(@NonNull View root, @NonNull PointsCategory category) {

		int color = category.getColor() == 0 ?
				ContextCompat.getColor(root.getContext(), R.color.color_favorite)
				: category.getColor();
		String iconName = RenderingIcons.containsBigIcon(category.getIconName())
				? category.getIconName()
				: DEFAULT_ICON_NAME;
		BackgroundType backgroundType = BackgroundType.getByTypeName(category.getBackgroundType(), BackgroundType.CIRCLE);

		((AutoCompleteTextViewEx) root.findViewById(R.id.category_edit)).setText(category.getName());
		((ImageView) root.findViewById(R.id.category_image)).setColorFilter(color);

		getParams().put(KEY_CATEGORY_NAME, category.getName());
		getParams().put(KEY_CATEGORY_COLOR, String.valueOf(color));
		getParams().put(KEY_CATEGORY_ICON_NAME, iconName);
		getParams().put(KEY_CATEGORY_BACKGROUND_TYPE, backgroundType.getTypeName());
	}

	private interface DialogOnClickListener {

		void skipOnClick();

		void enterNameOnClick();
	}
}
