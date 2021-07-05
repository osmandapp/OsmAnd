package net.osmand.plus.download.ui;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;

import net.osmand.AndroidNetworkUtils;
import net.osmand.AndroidUtils;
import net.osmand.map.WorldRegion;
import net.osmand.plus.CustomRegion;
import net.osmand.plus.LockableViewPager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.CustomIndexItem;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadActivity.BannerAndDownloadFreeVersion;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.download.DownloadItem;
import net.osmand.plus.download.DownloadResourceGroup;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseListener;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseTaskType;
import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import static net.osmand.plus.download.ui.DownloadItemFragment.updateActionButtons;
import static net.osmand.plus.download.ui.DownloadItemFragment.updateDescription;
import static net.osmand.plus.download.ui.DownloadItemFragment.updateImagesPager;

public class DownloadResourceGroupFragment extends DialogFragment implements DownloadEvents,
		InAppPurchaseListener, OnChildClickListener {
	public static final int RELOAD_ID = 0;
	public static final int SEARCH_ID = 1;

	public static final String TAG = "RegionDialogFragment";
	public static final String REGION_ID_DLG_KEY = "world_region_dialog_key";

	private String groupId;
	private View view;
	private BannerAndDownloadFreeVersion banner;
	protected ExpandableListView listView;
	protected DownloadResourceGroupAdapter listAdapter;
	private DownloadResourceGroup group;
	private DownloadActivity activity;
	private InAppPurchaseHelper purchaseHelper;
	private Toolbar toolbar;
	private View searchView;
	private View restorePurchasesView;
	private View subscribeEmailView;
	private View descriptionView;
	private boolean nightMode;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		purchaseHelper = getDownloadActivity().getPurchaseHelper();
		nightMode = !getMyApplication().getSettings().isLightContent();
		int themeId = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		setStyle(STYLE_NO_FRAME, themeId);
		setHasOptionsMenu(true);
	}

	public boolean openAsDialog() {
		return !Algorithms.isEmpty(groupId);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.maps_in_category_fragment, container, false);
		if (savedInstanceState != null) {
			groupId = savedInstanceState.getString(REGION_ID_DLG_KEY);
		}
		if (groupId == null && getArguments() != null) {
			groupId = getArguments().getString(REGION_ID_DLG_KEY);
		}
		if (groupId == null) {
			groupId = "";
		}
		activity = (DownloadActivity) getActivity();
		activity.getAccessibilityAssistant().registerPage(view, DownloadActivity.DOWNLOAD_TAB_NUMBER);

		toolbar = (Toolbar) view.findViewById(R.id.toolbar);
		Drawable icBack = getMyApplication().getUIUtilities().getIcon(AndroidUtils.getNavigationIconResId(activity));
		toolbar.setNavigationIcon(icBack);
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
		if (!openAsDialog()) {
			toolbar.setVisibility(View.GONE);
		}

		setHasOptionsMenu(true);

		if(openAsDialog()) {
			banner = new BannerAndDownloadFreeVersion(view, (DownloadActivity) getActivity(), false);
		} else {
			banner = null;
			view.findViewById(R.id.freeVersionBanner).setVisibility(View.GONE);
		}
		listView = (ExpandableListView) view.findViewById(android.R.id.list);
		addSubscribeEmailRow();
		addSearchRow();
		addRestorePurchasesRow();
		addDescriptionRow();
		listView.setOnChildClickListener(this);
		listAdapter = new DownloadResourceGroupAdapter(activity);
		listView.setAdapter(listAdapter);

		return view;
	}

	private void addSubscribeEmailRow() {
		if (DownloadActivity.shouldShowFreeVersionBanner(activity.getMyApplication())
				&& !getMyApplication().getSettings().EMAIL_SUBSCRIBED.get()) {
			subscribeEmailView = activity.getLayoutInflater().inflate(R.layout.subscribe_email_header, null, false);
			subscribeEmailView.findViewById(R.id.subscribe_btn).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					subscribe();
				}
			});
			listView.addHeaderView(subscribeEmailView);
			IndexItem worldBaseMapItem = activity.getDownloadThread().getIndexes().getWorldBaseMapItem();
			if (worldBaseMapItem == null || !worldBaseMapItem.isDownloaded()
					|| DownloadActivity.isDownlodingPermitted(activity.getMyApplication().getSettings())) {
				subscribeEmailView.findViewById(R.id.container).setVisibility(View.GONE);
			}
		}
	}

	private void addRestorePurchasesRow() {
		if (!openAsDialog() && purchaseHelper != null && !purchaseHelper.hasInventory()) {
			restorePurchasesView = activity.getLayoutInflater().inflate(R.layout.restore_purchases_list_footer, null);
			((ImageView) restorePurchasesView.findViewById(R.id.icon)).setImageDrawable(
					getMyApplication().getUIUtilities().getThemedIcon(R.drawable.ic_action_reset_to_default_dark));
			restorePurchasesView.findViewById(R.id.button).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					restorePurchasesView.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
					purchaseHelper.requestInventory();
				}
			});
			listView.addFooterView(restorePurchasesView);
			listView.setFooterDividersEnabled(false);
			IndexItem worldBaseMapItem = activity.getDownloadThread().getIndexes().getWorldBaseMapItem();
			if (worldBaseMapItem == null || !worldBaseMapItem.isDownloaded()) {
				restorePurchasesView.findViewById(R.id.container).setVisibility(View.GONE);
			}
		}
	}

	private void addDescriptionRow() {
		descriptionView = activity.getLayoutInflater().inflate(R.layout.group_description_item, listView, false);
		listView.addHeaderView(descriptionView);
	}

	private void addSearchRow() {
		if (!openAsDialog() ) {
			searchView = activity.getLayoutInflater().inflate(R.layout.simple_list_menu_item, null);
			searchView.setBackgroundResource(android.R.drawable.list_selector_background);
			TextView title = (TextView) searchView.findViewById(R.id.title);
			title.setCompoundDrawablesWithIntrinsicBounds(getMyApplication().getUIUtilities().getThemedIcon(R.drawable.ic_action_search_dark), null, null, null);
			title.setHint(R.string.search_map_hint);
			searchView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					getDownloadActivity().showDialog(getActivity(), SearchDialogFragment.createInstance(""));
				}
			});
			listView.addHeaderView(searchView);
			listView.setHeaderDividersEnabled(true);
			IndexItem worldBaseMapItem = activity.getDownloadThread().getIndexes().getWorldBaseMapItem();
			if (worldBaseMapItem == null || !worldBaseMapItem.isDownloaded()) {
				searchView.findViewById(R.id.title).setVisibility(View.GONE);
				listView.setHeaderDividersEnabled(false);
			}
		}
	}

	private void updateSearchView() {
		IndexItem worldBaseMapItem = null;
		if (searchView != null && searchView.findViewById(R.id.title).getVisibility() == View.GONE) {
			worldBaseMapItem = activity.getDownloadThread().getIndexes().getWorldBaseMapItem();
			if (worldBaseMapItem != null && worldBaseMapItem.isDownloaded()) {
				searchView.findViewById(R.id.title).setVisibility(View.VISIBLE);
				listView.setHeaderDividersEnabled(true);
			}
		}
		if (restorePurchasesView != null && restorePurchasesView.findViewById(R.id.container).getVisibility() == View.GONE
				&& purchaseHelper != null && !purchaseHelper.hasInventory()) {
			if (worldBaseMapItem != null && worldBaseMapItem.isDownloaded()) {
				restorePurchasesView.findViewById(R.id.container).setVisibility(View.VISIBLE);
			}
		}
	}

	private void updateSubscribeEmailView() {
		if (subscribeEmailView != null && subscribeEmailView.findViewById(R.id.container).getVisibility() == View.GONE
				&& !DownloadActivity.isDownlodingPermitted(getMyApplication().getSettings())
				&& !getMyApplication().getSettings().EMAIL_SUBSCRIBED.get()) {
			IndexItem worldBaseMapItem = activity.getDownloadThread().getIndexes().getWorldBaseMapItem();
			if (worldBaseMapItem != null && worldBaseMapItem.isDownloaded()) {
				subscribeEmailView.findViewById(R.id.container).setVisibility(View.VISIBLE);
			}
		}
	}

	private void updateDescriptionView() {
		if (descriptionView != null) {
			if (group != null && group.getRegion() instanceof CustomRegion) {
				CustomRegion customRegion = (CustomRegion) group.getRegion();
				DownloadDescriptionInfo descriptionInfo = customRegion.getDescriptionInfo();
				if (descriptionInfo != null) {
					OsmandApplication app = activity.getMyApplication();
					TextView description = descriptionView.findViewById(R.id.description);
					updateDescription(app, descriptionInfo, description);

					ViewGroup buttonsContainer = descriptionView.findViewById(R.id.buttons_container);
					updateActionButtons(activity, descriptionInfo, null, buttonsContainer, R.layout.download_description_button, nightMode);

					LockableViewPager viewPager = descriptionView.findViewById(R.id.images_pager);
					updateImagesPager(app, descriptionInfo, viewPager);

					descriptionView.findViewById(R.id.container).setVisibility(View.VISIBLE);
					return;
				}
			}
			descriptionView.findViewById(R.id.container).setVisibility(View.GONE);
		}
	}

	private void hideSubscribeEmailView() {
		if (subscribeEmailView != null && subscribeEmailView.findViewById(R.id.container).getVisibility() == View.VISIBLE) {
			subscribeEmailView.findViewById(R.id.container).setVisibility(View.GONE);
		}
	}

	private void subscribe() {
		AlertDialog.Builder b = new AlertDialog.Builder(activity);
		b.setTitle(R.string.shared_string_email_address);
		final EditText editText = new EditText(activity);
		int leftPadding = AndroidUtils.dpToPx(activity, 24f);
		int topPadding = AndroidUtils.dpToPx(activity, 4f);
		b.setView(editText, leftPadding, topPadding, leftPadding, topPadding);
		b.setPositiveButton(R.string.shared_string_ok, null);
		b.setNegativeButton(R.string.shared_string_cancel, null);
		final AlertDialog alertDialog = b.create();
		alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			public void onShow(DialogInterface dialog) {
				alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(
						new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								String email = editText.getText().toString();
								if (Algorithms.isEmpty(email) || !AndroidUtils.isValidEmail(email)) {
									getMyApplication().showToastMessage(getString(R.string.osm_live_enter_email));
									return;
								}
								doSubscribe(email);
								alertDialog.dismiss();
							}
						});
			}
		});
		alertDialog.show();
	}

	@SuppressLint("StaticFieldLeak")
	private void doSubscribe(final String email) {
		new AsyncTask<Void, Void, String>() {

			ProgressDialog dlg;

			@Override
			protected void onPreExecute() {
				dlg = new ProgressDialog(getActivity());
				dlg.setTitle("");
				dlg.setMessage(getString(R.string.wait_current_task_finished));
				dlg.setCancelable(false);
				dlg.show();
			}

			@Override
			protected String doInBackground(Void... params) {
				try {
					Map<String, String> parameters = new HashMap<>();
					parameters.put("aid", getMyApplication().getUserAndroidId());
					parameters.put("email", email);

					return AndroidNetworkUtils.sendRequest(getMyApplication(),
							"https://osmand.net/subscription/register_email",
							parameters, "Subscribing email...", true, true);

				} catch (Exception e) {
					return null;
				}
			}

			@Override
			protected void onPostExecute(String response) {
				if (dlg != null) {
					dlg.dismiss();
					dlg = null;
				}
				OsmandApplication app = getMyApplication();
				if (response == null) {
					app.showShortToastMessage(activity.getString(R.string.shared_string_unexpected_error));
				} else {
					try {
						JSONObject obj = new JSONObject(response);
						String responseEmail = obj.getString("email");
						if (!email.equalsIgnoreCase(responseEmail)) {
							app.showShortToastMessage(activity.getString(R.string.shared_string_unexpected_error));
						} else {
							int newDownloads = app.getSettings().NUMBER_OF_FREE_DOWNLOADS.get().intValue() - 3;
							if(newDownloads < 0) {
								newDownloads = 0;
							} else if(newDownloads > DownloadValidationManager.MAXIMUM_AVAILABLE_FREE_DOWNLOADS - 3) {
								newDownloads = DownloadValidationManager.MAXIMUM_AVAILABLE_FREE_DOWNLOADS - 3;
							}
							app.getSettings().NUMBER_OF_FREE_DOWNLOADS.set(newDownloads);
							app.getSettings().EMAIL_SUBSCRIBED.set(true);
							hideSubscribeEmailView();
							activity.updateBanner();
						}
					} catch (JSONException e) {
						String message = "JSON parsing error: "
								+ (e.getMessage() == null ? "unknown" : e.getMessage());
						app.showShortToastMessage(MessageFormat.format(
								activity.getString(R.string.error_message_pattern), message));
					}
				}
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
	}

	@Override
	public void onError(InAppPurchaseTaskType taskType, String error) {
	}

	@Override
	public void onGetItems() {
		if (restorePurchasesView != null && restorePurchasesView.findViewById(R.id.container).getVisibility() == View.VISIBLE) {
			restorePurchasesView.findViewById(R.id.container).setVisibility(View.GONE);
		}
	}

	@Override
	public void onItemPurchased(String sku, boolean active) {
		getMyApplication().getDownloadThread().runReloadIndexFilesSilent();
	}

	@Override
	public void showProgress(InAppPurchaseTaskType taskType) {
	}

	@Override
	public void dismissProgress(InAppPurchaseTaskType taskType) {
		if (restorePurchasesView != null && restorePurchasesView.findViewById(R.id.container).getVisibility() == View.VISIBLE) {
			restorePurchasesView.findViewById(R.id.progressBar).setVisibility(View.GONE);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		reloadData();
		String filter = getDownloadActivity().getFilterAndClear();
		String filterCat = getDownloadActivity().getFilterCatAndClear();
		String filterGroup = getDownloadActivity().getFilterGroupAndClear();
		if (filter != null && filterCat != null
				&& filterCat.equals(DownloadActivityType.WIKIPEDIA_FILE.getTag())) {
			getDownloadActivity().showDialog(getActivity(),
					SearchDialogFragment.createInstance(filter, false,
							DownloadActivityType.WIKIPEDIA_FILE));
		} else if (filter != null) {
			getDownloadActivity().showDialog(getActivity(),
					SearchDialogFragment.createInstance(filter));
		} else if (filterCat != null) {
			if (filterCat.equals(DownloadActivityType.VOICE_FILE.getTag())) {
				String uniqueId = DownloadResourceGroup.DownloadResourceGroupType.getVoiceTTSId();
				final DownloadResourceGroupFragment regionDialogFragment = DownloadResourceGroupFragment
						.createInstance(uniqueId);
				((DownloadActivity) getActivity()).showDialog(getActivity(), regionDialogFragment);
			}
		} else if (filterGroup != null) {
			final DownloadResourceGroupFragment regionDialogFragment = DownloadResourceGroupFragment
					.createInstance(filterGroup);
			((DownloadActivity) getActivity()).showDialog(getActivity(), regionDialogFragment);
		}
	}

	private void reloadData() {
		DownloadResources indexes = activity.getDownloadThread().getIndexes();
		group = indexes.getGroupById(groupId);

		if (!openAsDialog()) {
			updateSearchView();
		}
		updateSubscribeEmailView();
		updateDescriptionView();

		if (group != null) {
			listAdapter.update(group);
			toolbar.setTitle(group.getName(activity));
			WorldRegion region = group.getRegion();
			if (region instanceof CustomRegion) {
				int headerColor = ((CustomRegion) region).getHeaderColor();
				if (headerColor != CustomRegion.INVALID_ID) {
					toolbar.setBackgroundColor(headerColor);
				}
			}
		}
		expandAllGroups();
	}

	private void expandAllGroups() {
		for (int i = 0; i < listAdapter.getGroupCount(); i++) {
			listView.expandGroup(i);
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setShowsDialog(openAsDialog());
		listView.setBackgroundColor(getResources().getColor(
				getMyApplication().getSettings().isLightContent() ? R.color.list_background_color_light : R.color.list_background_color_dark));
	}

	@Override
	public void onUpdatedIndexesList() {
		if (banner != null) {
			banner.updateBannerInProgress();
		}
		reloadData();
	}

	@Override
	public void downloadHasFinished() {
		if (banner != null) {
			banner.updateBannerInProgress();
		}
		if (subscribeEmailView != null
				&& !DownloadActivity.isDownlodingPermitted(activity.getMyApplication().getSettings())
				&& !getMyApplication().getSettings().EMAIL_SUBSCRIBED.get()) {
			subscribeEmailView.findViewById(R.id.container).setVisibility(View.VISIBLE);
		}
		listAdapter.notifyDataSetChanged();
	}

	@Override
	public void downloadInProgress() {
		if (banner != null) {
			banner.updateBannerInProgress();
		}
		listAdapter.notifyDataSetChanged();
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		Object child = listAdapter.getChild(groupPosition, childPosition);
		if (child instanceof DownloadResourceGroup) {
			String uniqueId = ((DownloadResourceGroup) child).getUniqueId();
			final DownloadResourceGroupFragment regionDialogFragment = DownloadResourceGroupFragment
					.createInstance(uniqueId);
			((DownloadActivity) getActivity()).showDialog(getActivity(), regionDialogFragment);
			return true;
		} else if (child instanceof CustomIndexItem) {
			String regionId = group.getGroupByIndex(groupPosition).getUniqueId();

			DownloadItemFragment downloadItemFragment = DownloadItemFragment.createInstance(regionId, childPosition);
			((DownloadActivity) getActivity()).showDialog(getActivity(), downloadItemFragment);
		} else if (child instanceof DownloadItem) {
			DownloadItem downloadItem = (DownloadItem) child;
			ItemViewHolder vh = (ItemViewHolder) v.getTag();
			OnClickListener ls = vh.getRightButtonAction(downloadItem, vh.getClickAction(downloadItem));
			ls.onClick(v);
			return true;
		}
		return false;
	}

	

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putString(REGION_ID_DLG_KEY, groupId);
		super.onSaveInstanceState(outState);
	}

	private OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

	private DownloadActivity getDownloadActivity() {
		return (DownloadActivity) getActivity();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		if (!openAsDialog()) {
			OsmandApplication app = getMyApplication();
			int colorResId = app.getSettings().isLightContent() ? R.color.active_buttons_and_links_text_light : R.color.active_buttons_and_links_text_dark;
			
			MenuItem itemReload = menu.add(0, RELOAD_ID, 0, R.string.shared_string_refresh);
			Drawable icReload = app.getUIUtilities().getIcon(R.drawable.ic_action_refresh_dark, colorResId);
			itemReload.setIcon(icReload);
			itemReload.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

			MenuItem itemSearch = menu.add(0, SEARCH_ID, 1, R.string.shared_string_search);
			Drawable icSearch = app.getUIUtilities().getIcon(R.drawable.ic_action_search_dark, colorResId);
			itemSearch.setIcon(icSearch);
			itemSearch.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case RELOAD_ID:
			// re-create the thread
			getDownloadActivity().getDownloadThread().runReloadIndexFiles();
			return true;
		case SEARCH_ID:
			getDownloadActivity().showDialog(getActivity(), SearchDialogFragment.createInstance(""));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public static DownloadResourceGroupFragment createInstance(String regionId) {
		Bundle bundle = new Bundle();
		bundle.putString(REGION_ID_DLG_KEY, regionId);
		DownloadResourceGroupFragment fragment = new DownloadResourceGroupFragment();
		fragment.setArguments(bundle);
		return fragment;
	}
}