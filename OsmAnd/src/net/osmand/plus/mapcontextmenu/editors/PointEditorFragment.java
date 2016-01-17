package net.osmand.plus.mapcontextmenu.editors;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.data.LatLon;
import net.osmand.data.QuadPoint;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.mapcontextmenu.MapContextMenuFragment;
import net.osmand.plus.views.AnimateDraggingMapThread;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.widgets.AutoCompleteTextViewEx;
import net.osmand.util.Algorithms;

public abstract class PointEditorFragment extends Fragment {

	private View view;
	private int mainViewHeight;
	private EditText nameEdit;

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

		getActivity().findViewById(R.id.MapHudButtonsOverlay).setVisibility(View.INVISIBLE);

		view = inflater.inflate(R.layout.point_editor_fragment, container, false);

		getEditor().updateLandscapePortrait();
		getEditor().updateNightMode();

		if (getEditor().isLandscapeLayout()) {
			AndroidUtils.setBackground(view.getContext(), view, !getEditor().isLight(),
					R.drawable.bg_left_menu_light, R.drawable.bg_left_menu_dark);
		} else {
			AndroidUtils.setBackground(view.getContext(), view.findViewById(R.id.title_view), !getEditor().isLight(),
					R.drawable.bg_point_editor_view_light, R.drawable.bg_point_editor_view_dark);
		}

		View editorScrollView = view.findViewById(R.id.editor_scroll_view);
		if (editorScrollView != null && getEditor().isLandscapeLayout()) {
			if (getEditor().isLight()) {
				editorScrollView.setBackgroundColor(getResources().getColor(R.color.ctx_menu_info_view_bg_light));
			} else {
				editorScrollView.setBackgroundColor(getResources().getColor(R.color.ctx_menu_info_view_bg_dark));
			}
		}
		View descriptionInfoView = view.findViewById(R.id.description_info_view);
		if (descriptionInfoView != null) {
			if (getEditor().isLight()) {
				descriptionInfoView.setBackgroundColor(getResources().getColor(R.color.ctx_menu_info_view_bg_light));
			} else {
				descriptionInfoView.setBackgroundColor(getResources().getColor(R.color.ctx_menu_info_view_bg_dark));
			}
		}

		Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
		toolbar.setTitle(getToolbarTitle());
		toolbar.setNavigationIcon(getMyApplication().getIconsCache().getIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha));
		toolbar.setTitleTextColor(getResources().getColor(getResIdFromAttribute(getMapActivity(), R.attr.pstsTextColor)));
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		View scrollViewHeader = view.findViewById(R.id.editor_scroll_view_header);
		if (scrollViewHeader != null) {
			scrollViewHeader.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					dismiss();
				}
			});
		}

		Button saveButton = (Button) toolbar.findViewById(R.id.save_button);
		saveButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				savePressed();
			}
		});

		ImageButton okButton = (ImageButton) toolbar.findViewById(R.id.ok_button);
		okButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				savePressed();
			}
		});

		ImageButton deleteButton = (ImageButton) toolbar.findViewById(R.id.delete_button);
		deleteButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				deletePressed();
			}
		});

		if (getEditor().isNew()) {
			okButton.setVisibility(View.GONE);
			deleteButton.setVisibility(View.GONE);
		} else {
			saveButton.setVisibility(View.GONE);
		}

		TextView headerCaption = (TextView) view.findViewById(R.id.header_caption);
		AndroidUtils.setTextPrimaryColor(view.getContext(), headerCaption, !getEditor().isLight());
		headerCaption.setText(getHeaderCaption());
		TextView nameCaption = (TextView) view.findViewById(R.id.name_caption);
		AndroidUtils.setTextSecondaryColor(view.getContext(), nameCaption, !getEditor().isLight());
		nameCaption.setText(getNameCaption());
		TextView categoryCaption = (TextView) view.findViewById(R.id.category_caption);
		AndroidUtils.setTextSecondaryColor(view.getContext(), categoryCaption, !getEditor().isLight());
		categoryCaption.setText(getCategoryCaption());

		nameEdit = (EditText) view.findViewById(R.id.name_edit);
		AndroidUtils.setTextPrimaryColor(view.getContext(), nameEdit, !getEditor().isLight());
		AndroidUtils.setHintTextSecondaryColor(view.getContext(), nameEdit, !getEditor().isLight());
		nameEdit.setText(getNameInitValue());
		AutoCompleteTextViewEx categoryEdit = (AutoCompleteTextViewEx) view.findViewById(R.id.category_edit);
		AndroidUtils.setTextPrimaryColor(view.getContext(), categoryEdit, !getEditor().isLight());
		categoryEdit.setText(getCategoryInitValue());
		categoryEdit.setFocusable(false);
		categoryEdit.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(final View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_UP) {
					DialogFragment dialogFragment =
							SelectCategoryDialogFragment.createInstance(getEditor().getFragmentTag());
					dialogFragment.show(getChildFragmentManager(), SelectCategoryDialogFragment.TAG);
					return true;
				}
				return false;
			}
		});

		EditText descriptionEdit = (EditText) view.findViewById(R.id.description_edit);
		AndroidUtils.setTextPrimaryColor(view.getContext(), descriptionEdit, !getEditor().isLight());
		AndroidUtils.setHintTextSecondaryColor(view.getContext(), descriptionEdit, !getEditor().isLight());
		if (getDescriptionInitValue() != null) {
			descriptionEdit.setText(getDescriptionInitValue());
		}

		ImageView nameImage = (ImageView) view.findViewById(R.id.name_image);
		nameImage.setImageDrawable(getNameIcon());
		ImageView categoryImage = (ImageView) view.findViewById(R.id.category_image);
		categoryImage.setImageDrawable(getCategoryIcon());

		ImageView descriptionImage = (ImageView) view.findViewById(R.id.description_image);
		descriptionImage.setImageDrawable(getRowIcon(R.drawable.ic_action_note_dark));

		runLayoutListener();

		return view;
	}

	public Drawable getRowIcon(int iconId) {
		IconsCache iconsCache = getMyApplication().getIconsCache();
		return iconsCache.getIcon(iconId,
				getEditor().isLight() ? R.color.icon_color : R.color.icon_color_light);
	}

	@Override
	public void onStart() {
		super.onStart();
		getMapActivity().getContextMenu().setBaseFragmentVisibility(false);
	}

	@Override
	public void onResume() {
		super.onResume();
		if (getEditor().isNew()) {
			nameEdit.selectAll();
			nameEdit.requestFocus();
			AndroidUtils.softKeyboardDelayed(nameEdit);
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		hideKeyboard();
		getMapActivity().getContextMenu().setBaseFragmentVisibility(true);
	}

	@Override
	public void onDestroyView() {
		if (!wasSaved() && !getEditor().isNew()) {
			save(false);
		}
		super.onDestroyView();
		getActivity().findViewById(R.id.MapHudButtonsOverlay).setVisibility(View.VISIBLE);
	}

	private void adjustMapPosition(boolean animated) {
		OsmandMapTileView map = getMapActivity().getMapView();
		MapContextMenu menu = getMapActivity().getContextMenu();
		double markerLat = menu.getLatLon().getLatitude();
		double markerLon = menu.getLatLon().getLongitude();

		RotatedTileBox box = map.getCurrentRotatedTileBox();
		int origMarkerY = (int)box.getPixYFromLatLon(markerLat, markerLon);

		int markerPaddingPx = AndroidUtils.dpToPx(getMapActivity(), MapContextMenuFragment.MARKER_PADDING_DP);

		int y = view.getHeight() - mainViewHeight;

		if (!menu.isLandscapeLayout()) {
			int markerY = (int)box.getPixYFromLatLon(markerLat, markerLon);
			if (markerY + markerPaddingPx > y || markerY < origMarkerY) {
				int dy = markerY - (y - markerPaddingPx);
				if (markerY - dy <= origMarkerY) {
					QuadPoint cp = box.getCenterPixelPoint();
					LatLon latlon = box.getLatLonFromPixel(cp.x + 0, cp.y + dy);
					double destLat = latlon.getLatitude();
					double destLon = latlon.getLongitude();
					if (animated) {
						AnimateDraggingMapThread thread = map.getAnimatedDraggingThread();
						int fZoom = map.getZoom();
						double flat = destLat;
						double flon = destLon;

						RotatedTileBox cbox = map.getCurrentRotatedTileBox().copy();
						cbox.setCenterLocation(0.5f, 0.5f);
						cbox.setLatLonCenter(flat, flon);
						flat = cbox.getLatFromPixel(cbox.getPixWidth() / 2, cbox.getPixHeight() / 2);
						flon = cbox.getLonFromPixel(cbox.getPixWidth() / 2, cbox.getPixHeight() / 2);

						thread.startMoving(flat, flon, fZoom, true);
					} else {
						map.setLatLon(destLat, destLon);
					}
				}
			}
		}
	}

	private void runLayoutListener() {
		ViewTreeObserver vto = view.getViewTreeObserver();
		vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

			@Override
			public void onGlobalLayout() {
				mainViewHeight = view.findViewById(R.id.main_view).getHeight();

				ViewTreeObserver obs = view.getViewTreeObserver();
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
					obs.removeOnGlobalLayoutListener(this);
				} else {
					obs.removeGlobalOnLayoutListener(this);
				}
				updateScrollHeaderHeight();
				adjustMapPosition(true);
			}

		});
	}

	private void updateScrollHeaderHeight() {
		View scrollViewHeader = view.findViewById(R.id.editor_scroll_view_header);
		if (scrollViewHeader != null) {
			View scrollView = view.findViewById(R.id.editor_scroll_view);
			int headerHeight = scrollView.getHeight() - mainViewHeight;
			ViewGroup.LayoutParams p = scrollViewHeader.getLayoutParams();
			p.height = headerHeight;
			scrollViewHeader.setLayoutParams(p);
		}
	}

	private void hideKeyboard() {
		InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
		if (inputMethodManager != null) {
			View currentFocus = getActivity().getCurrentFocus();
			if (currentFocus != null) {
				IBinder windowToken = currentFocus.getWindowToken();
				if (windowToken != null) {
					inputMethodManager.hideSoftInputFromWindow(windowToken, 0);
				}
			}
		}
	}

	protected void savePressed() {
		save(true);
	}

	protected void deletePressed() {
		delete(true);
	}

	protected abstract boolean wasSaved();
	protected abstract void save(boolean needDismiss);
	protected abstract void delete(boolean needDismiss);

	static int getResIdFromAttribute(final Context ctx, final int attr) {
		if (attr == 0)
			return 0;
		final TypedValue typedvalueattr = new TypedValue();
		ctx.getTheme().resolveAttribute(attr, typedvalueattr, true);
		return typedvalueattr.resourceId;
	}

	public abstract PointEditor getEditor();
	public abstract String getToolbarTitle();

	public void setCategory(String name) {
		AutoCompleteTextViewEx categoryEdit = (AutoCompleteTextViewEx) view.findViewById(R.id.category_edit);
		String n = name.length() == 0 ? getDefaultCategoryName() : name;
		categoryEdit.setText(n);
		ImageView categoryImage = (ImageView) view.findViewById(R.id.category_image);
		categoryImage.setImageDrawable(getCategoryIcon());
		ImageView nameImage = (ImageView) view.findViewById(R.id.name_image);
		nameImage.setImageDrawable(getNameIcon());
	}

	protected String getDefaultCategoryName() {
		return getString(R.string.shared_string_none);
	}

	protected MapActivity getMapActivity() {
		return (MapActivity)getActivity();
	}

	protected OsmandApplication getMyApplication() {
		if (getActivity() == null) {
			return null;
		}
		return (OsmandApplication) getActivity().getApplication();
	}

	public void dismiss() {
		dismiss(false);
	}

	public void dismiss(boolean includingMenu) {
		if (includingMenu) {
			getMapActivity().getSupportFragmentManager().popBackStack();
			getMapActivity().getContextMenu().close();
		} else {
			getMapActivity().getSupportFragmentManager().popBackStack();
		}
	}

	public abstract String getHeaderCaption();

	public String getNameCaption() {
		return getMapActivity().getResources().getString(R.string.favourites_edit_dialog_name);
	}
	public String getCategoryCaption() {
		return getMapActivity().getResources().getString(R.string.favourites_edit_dialog_category);
	}

	public abstract String getNameInitValue();
	public abstract String getCategoryInitValue();
	public abstract String getDescriptionInitValue();

	public abstract Drawable getNameIcon();
	public abstract Drawable getCategoryIcon();

	public String getNameTextValue() {
		EditText nameEdit = (EditText) view.findViewById(R.id.name_edit);
		return nameEdit.getText().toString().trim();
	}

	public String getCategoryTextValue() {
		AutoCompleteTextViewEx categoryEdit = (AutoCompleteTextViewEx) view.findViewById(R.id.category_edit);
		String name = categoryEdit.getText().toString().trim();
		return name.equals(getDefaultCategoryName()) ? "" : name;
	}

	public String getDescriptionTextValue() {
		EditText descriptionEdit = (EditText) view.findViewById(R.id.description_edit);
		String res = descriptionEdit.getText().toString().trim();
		return Algorithms.isEmpty(res) ? null : res;
	}

	protected Drawable getPaintedIcon(int iconId, int color) {
		IconsCache iconsCache = getMapActivity().getMyApplication().getIconsCache();
		return iconsCache.getPaintedContentIcon(iconId, color);
	}
}
