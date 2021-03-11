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

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;

import net.osmand.data.LatLon;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.GeocodingLookupService.AddressLookupRequest;
import net.osmand.plus.GeocodingLookupService.OnAddressLookupResult;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.editors.EditCategoryDialogFragment;
import net.osmand.plus.mapcontextmenu.editors.FavoritePointEditor;
import net.osmand.plus.mapcontextmenu.editors.SelectCategoryDialogFragment;
import net.osmand.plus.mapcontextmenu.editors.SelectFavoriteCategoryBottomSheet;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.widgets.AutoCompleteTextViewEx;

public class FavoriteAction extends QuickAction {


	public static final QuickActionType TYPE = new QuickActionType(3,
			"fav.add", FavoriteAction.class).
			nameRes(R.string.quick_action_add_favorite).iconRes(R.drawable.ic_action_favorite).
			category(QuickActionType.CREATE_CATEGORY);
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
	public void execute(final MapActivity activity) {
		final LatLon latLon = activity.getMapView().getCurrentRotatedTileBox().getCenterLatLon();
		final String title = getParams().get(KEY_NAME);

		if (title == null || title.isEmpty()) {
			progressDialog = createProgressDialog(activity, new DialogOnClickListener() {
				@Override
				public void skipOnClick() {
					onClick(activity.getString(R.string.favorite), !Boolean.valueOf(getParams().get(KEY_DIALOG)));
				}

				@Override
				public void enterNameOnClick() {
					onClick("", false);
				}

				private void onClick(String title, boolean autoFill) {
					activity.getMyApplication().getGeocodingLookupService().cancel(lookupRequest);
					dismissProgressDialog();
					addFavorite(activity, latLon, title, autoFill);
				}
			});
			progressDialog.show();

			lookupRequest = new AddressLookupRequest(latLon, new OnAddressLookupResult() {
				@Override
				public void geocodingDone(String address) {
					dismissProgressDialog();
					addFavorite(activity, latLon, address, !Boolean.valueOf(getParams().get(KEY_DIALOG)));
				}
			}, null);

			activity.getMyApplication().getGeocodingLookupService().lookupAddress(lookupRequest);
		} else {
			addFavorite(activity, latLon, title, !Boolean.valueOf(getParams().get(KEY_DIALOG)));
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

	private void addFavorite(MapActivity mapActivity, LatLon latLon, String title, boolean autoFill) {
		FavoritePointEditor favoritePointEditor = mapActivity.getContextMenu().getFavoritePointEditor();
		if (favoritePointEditor != null) {
			favoritePointEditor.add(latLon, title, "", getParams().get(KEY_CATEGORY_NAME),
					Integer.valueOf(getParams().get(KEY_CATEGORY_COLOR)), autoFill);
		}
	}

	@Override
	public void drawUI(final ViewGroup parent, final MapActivity activity) {

		FavouritesDbHelper helper = activity.getMyApplication().getFavorites();

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

				categoryEdit.setText(activity.getString(R.string.shared_string_favorites));
				categoryImage.setColorFilter(activity.getResources().getColor(R.color.color_favorite));
			}

		} else if (helper.getFavoriteGroups().size() > 0) {

			FavouritesDbHelper.FavoriteGroup group = helper.getFavoriteGroups().get(0);

			int color = group.getColor() == 0 ? activity.getResources().getColor(R.color.color_favorite) : group.getColor();
			categoryEdit.setText(group.getDisplayName(activity));
			categoryImage.setColorFilter(color);

			getParams().put(KEY_CATEGORY_NAME, group.getName());
			getParams().put(KEY_CATEGORY_COLOR, String.valueOf(group.getColor()));

		} else {

			categoryEdit.setText(activity.getString(R.string.shared_string_favorites));
			categoryImage.setColorFilter(activity.getResources().getColor(R.color.color_favorite));

			getParams().put(KEY_CATEGORY_NAME, "");
			getParams().put(KEY_CATEGORY_COLOR, "0");
		}

		categoryEdit.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View view) {

				SelectFavoriteCategoryBottomSheet dialogFragment = SelectFavoriteCategoryBottomSheet.createInstance("", "");

				dialogFragment.show(
						activity.getSupportFragmentManager(),
						SelectCategoryDialogFragment.TAG);

				dialogFragment.setSelectionListener(new SelectFavoriteCategoryBottomSheet.CategorySelectionListener() {
					@Override
					public void onCategorySelected(String category, int color) {

						fillGroupParams(root, category, color);
					}
				});
			}
		});

		SelectFavoriteCategoryBottomSheet dialogFragment = (SelectFavoriteCategoryBottomSheet)
				activity.getSupportFragmentManager().findFragmentByTag(SelectCategoryDialogFragment.TAG);

		if (dialogFragment != null) {

			dialogFragment.setSelectionListener(new SelectFavoriteCategoryBottomSheet.CategorySelectionListener() {
				@Override
				public void onCategorySelected(String category, int color) {

					fillGroupParams(root, category, color);
				}
			});

		} else {

			EditCategoryDialogFragment dialog = (EditCategoryDialogFragment)
					activity.getSupportFragmentManager().findFragmentByTag(EditCategoryDialogFragment.TAG);

			if (dialog != null) {

				dialogFragment.setSelectionListener(new SelectFavoriteCategoryBottomSheet.CategorySelectionListener() {
					@Override
					public void onCategorySelected(String category, int color) {

						fillGroupParams(root, category, color);
					}
				});
			}
		}
	}

	@Override
	public boolean fillParams(View root, MapActivity activity) {

		getParams().put(KEY_NAME, ((EditText) root.findViewById(R.id.name_edit)).getText().toString());
		getParams().put(KEY_DIALOG, Boolean.toString(((SwitchCompat) root.findViewById(R.id.saveButton)).isChecked()));

		return true;
	}

	private void fillGroupParams(View root, String name, int color) {

		if (color == 0)
			color = root.getContext().getResources().getColor(R.color.color_favorite);

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
