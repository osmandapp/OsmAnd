package net.osmand.plus.download.ui;

import static net.osmand.plus.download.ui.DownloadItemFragment.updateActionButtons;
import static net.osmand.plus.download.ui.DownloadItemFragment.updateDescription;
import static net.osmand.plus.download.ui.DownloadItemFragment.updateImagesPager;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
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
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import net.osmand.map.WorldRegion;
import net.osmand.plus.LockableViewPager;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.plugins.custom.CustomRegion;
import net.osmand.plus.plugins.custom.CustomIndexItem;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.download.DownloadItem;
import net.osmand.plus.download.DownloadResourceGroup;
import net.osmand.plus.download.DownloadResourceGroupType;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseListener;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseTaskType;
import net.osmand.plus.plugins.weather.listener.RemoveLocalForecastListener;
import net.osmand.plus.utils.AndroidNetworkUtils;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class DownloadResourceGroupFragment extends BaseOsmAndDialogFragment implements DownloadEvents,
		InAppPurchaseListener, RemoveLocalForecastListener, OnChildClickListener {
	public static final int RELOAD_ID = 0;
	public static final int SEARCH_ID = 1;
	public static final String REGION_ID_DLG_KEY = "world_region_dialog_key";

	private DownloadIndexesThread downloadThread;
	private InAppPurchaseHelper purchaseHelper;

	private String groupId;
	private DownloadResourceGroup group;

	private View view;
	private BannerAndDownloadFreeVersion banner;
	private ExpandableListView listView;
	private DownloadResourceGroupAdapter listAdapter;
	private DownloadActivity activity;
	private Toolbar toolbar;
	private View searchView;
	private View restorePurchasesView;
	private View subscribeEmailView;
	private View freeMapsView;
	private View descriptionView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		downloadThread = app.getDownloadThread();
		purchaseHelper = getDownloadActivity().getPurchaseHelper();
		setStyle(STYLE_NO_FRAME, nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme);
		setHasOptionsMenu(true);
	}

	public boolean openAsDialog() {
		return !Algorithms.isEmpty(groupId);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		updateNightMode();
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

		toolbar = view.findViewById(R.id.toolbar);
		toolbar.setNavigationIcon(getIcon(AndroidUtils.getNavigationIconResId(activity)));
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(v -> dismiss());
		if (!openAsDialog()) {
			toolbar.setVisibility(View.GONE);
		}

		setHasOptionsMenu(true);

		if (openAsDialog()) {
			banner = new BannerAndDownloadFreeVersion(view, (DownloadActivity) getActivity(), false);
		} else {
			banner = null;
			view.findViewById(R.id.freeVersionBanner).setVisibility(View.GONE);
		}
		listView = view.findViewById(android.R.id.list);
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
		if (DownloadActivity.shouldShowFreeVersionBanner(app) && !settings.EMAIL_SUBSCRIBED.get()) {
			subscribeEmailView = activity.getLayoutInflater().inflate(R.layout.subscribe_email_header, null, false);
			subscribeEmailView.findViewById(R.id.subscribe_btn).setOnClickListener(v -> subscribe());
			listView.addHeaderView(subscribeEmailView);
			IndexItem worldBaseMapItem = downloadThread.getIndexes().getWorldBaseMapItem();
			if (worldBaseMapItem == null || !worldBaseMapItem.isDownloaded()
					|| DownloadActivity.isDownloadingPermitted(settings)) {
				subscribeEmailView.findViewById(R.id.container).setVisibility(View.GONE);
			}
		}
	}

	private void addRestorePurchasesRow() {
		if (!openAsDialog() && purchaseHelper != null && !purchaseHelper.hasInventory()) {
			restorePurchasesView = activity.getLayoutInflater().inflate(R.layout.restore_purchases_list_footer, null);
			((ImageView) restorePurchasesView.findViewById(R.id.icon)).setImageDrawable(
					getContentIcon(R.drawable.ic_action_reset_to_default_dark));
			restorePurchasesView.findViewById(R.id.button).setOnClickListener(v -> {
				restorePurchasesView.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
				purchaseHelper.requestInventory(true);
			});
			listView.addFooterView(restorePurchasesView);
			listView.setFooterDividersEnabled(false);
			IndexItem worldBaseMapItem = downloadThread.getIndexes().getWorldBaseMapItem();
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
		if (!openAsDialog()) {
			searchView = activity.getLayoutInflater().inflate(R.layout.simple_list_menu_item, null);
			searchView.setBackgroundResource(android.R.drawable.list_selector_background);
			TextView title = searchView.findViewById(R.id.title);
			title.setCompoundDrawablesWithIntrinsicBounds(getContentIcon(R.drawable.ic_action_search_dark), null, null, null);
			title.setHint(R.string.search_map_hint);
			searchView.setOnClickListener(v -> getDownloadActivity().showDialog(getActivity(), SearchDialogFragment.createInstance("")));
			listView.addHeaderView(searchView);
			listView.setHeaderDividersEnabled(true);
			IndexItem worldBaseMapItem = downloadThread.getIndexes().getWorldBaseMapItem();
			if (worldBaseMapItem == null || !worldBaseMapItem.isDownloaded()) {
				searchView.findViewById(R.id.title).setVisibility(View.GONE);
				listView.setHeaderDividersEnabled(false);
			}
		}
	}

	private void updateSearchView() {
		IndexItem worldBaseMapItem = null;
		if (searchView != null && searchView.findViewById(R.id.title).getVisibility() == View.GONE) {
			worldBaseMapItem = downloadThread.getIndexes().getWorldBaseMapItem();
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
				&& !DownloadActivity.isDownloadingPermitted(settings)
				&& !settings.EMAIL_SUBSCRIBED.get()) {
			IndexItem worldBaseMapItem = downloadThread.getIndexes().getWorldBaseMapItem();
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

	private void updateFreeMapsView() {
		if (shouldDisplayFreeMapsMessage()) {
			if (freeMapsView == null) {
				freeMapsView = activity.getLayoutInflater().inflate(R.layout.free_maps_header, null, false);
				listView.addHeaderView(freeMapsView);
			}
			TextView description = freeMapsView.findViewById(R.id.description);
			description.setText(getFreeMapsMessage());
		} else if (freeMapsView != null && freeMapsView.findViewById(R.id.container).getVisibility() == View.VISIBLE) {
			freeMapsView.findViewById(R.id.container).setVisibility(View.GONE);
		}
	}

	private boolean shouldDisplayFreeMapsMessage() {
		if (group != null) {
			WorldRegion region = group.getRegion();
			if (region != null) {
				WorldRegion worldRegion = app.getRegions().getWorldRegion();
				return !Algorithms.objectEquals(region, worldRegion) && !Algorithms.objectEquals(region.getSuperregion(), worldRegion) && hasFreeMaps();
			}
		}
		return false;
	}

	private boolean hasFreeMaps() {
		if (group != null) {
			for (DownloadItem item : group.getAllDownloadItems()) {
				if (item.isFree()) {
					return true;
				}
			}
		}
		return false;
	}

	@Nullable
	private String getFreeMapsMessage() {
		if (group != null) {
			for (DownloadItem item : group.getAllDownloadItems()) {
				String message = item.getFreeMessage();
				if (!Algorithms.isEmpty(message)) {
					return message;
				}
			}
		}
		return null;
	}

	private void hideSubscribeEmailView() {
		if (subscribeEmailView != null && subscribeEmailView.findViewById(R.id.container).getVisibility() == View.VISIBLE) {
			subscribeEmailView.findViewById(R.id.container).setVisibility(View.GONE);
		}
	}

	private void subscribe() {
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setTitle(R.string.shared_string_email_address);
		int hPadding = AndroidUtils.dpToPx(activity, 24f);
		int vPadding = AndroidUtils.dpToPx(activity, 4f);
		FrameLayout container = new FrameLayout(activity);
		container.setPadding(hPadding, vPadding, hPadding, vPadding);
		EditText editText = new EditText(activity);
		container.addView(editText);
		builder.setView(container);
		builder.setPositiveButton(R.string.shared_string_ok, null);
		builder.setNegativeButton(R.string.shared_string_cancel, null);
		AlertDialog alertDialog = builder.create();
		alertDialog.setOnShowListener(dialog -> alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(
				v -> {
					String email = editText.getText().toString().trim();
					if (Algorithms.isEmpty(email) || !AndroidUtils.isValidEmail(email)) {
						app.showToastMessage(getString(R.string.osm_live_enter_email));
						return;
					}
					doSubscribe(email);
					alertDialog.dismiss();
				}));
		alertDialog.show();
	}

	@SuppressLint("StaticFieldLeak")
	private void doSubscribe(String email) {
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
					if (app.isUserAndroidIdAllowed()) {
						parameters.put("aid", app.getUserAndroidId());
					}
					parameters.put("email", email);

					return AndroidNetworkUtils.sendRequest(app,
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
				if (response == null) {
					app.showShortToastMessage(activity.getString(R.string.shared_string_unexpected_error));
				} else {
					try {
						JSONObject obj = new JSONObject(response);
						String responseEmail = obj.getString("email");
						if (!email.equalsIgnoreCase(responseEmail)) {
							app.showShortToastMessage(activity.getString(R.string.shared_string_unexpected_error));
						} else {
							int newDownloads = settings.NUMBER_OF_FREE_DOWNLOADS.get().intValue() - 3;
							if (newDownloads < 0) {
								newDownloads = 0;
							} else if (newDownloads > DownloadValidationManager.MAXIMUM_AVAILABLE_FREE_DOWNLOADS - 3) {
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
	public void onGetItems() {
		if (restorePurchasesView != null && restorePurchasesView.findViewById(R.id.container).getVisibility() == View.VISIBLE) {
			restorePurchasesView.findViewById(R.id.container).setVisibility(View.GONE);
		}
	}

	@Override
	public void onItemPurchased(String sku, boolean active) {
		downloadThread.runReloadIndexFilesSilent();
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

		DownloadActivity activity = getDownloadActivity();
		app.getOfflineForecastHelper().registerRemoveLocalForecastListener(this);
		String filter = activity.getFilterAndClear();
		String filterCat = activity.getFilterCatAndClear();
		String filterGroup = activity.getFilterGroupAndClear();
		if (filter != null && filterCat != null
				&& filterCat.equals(DownloadActivityType.WIKIPEDIA_FILE.getTag())) {
			activity.showDialog(getActivity(),
					SearchDialogFragment.createInstance(filter, false,
							DownloadActivityType.WIKIPEDIA_FILE));
		} else if (filter != null) {
			activity.showDialog(getActivity(), SearchDialogFragment.createInstance(filter));
		} else if (filterCat != null) {
			if (filterCat.equals(DownloadActivityType.VOICE_FILE.getTag())) {
				String uniqueId = DownloadResourceGroupType.getVoiceTTSId();
				DownloadResourceGroupFragment regionDialogFragment = createInstance(uniqueId);
				((DownloadActivity) getActivity()).showDialog(getActivity(), regionDialogFragment);
			}
		} else if (filterGroup != null) {
			DownloadResourceGroupFragment regionDialogFragment = createInstance(filterGroup);
			((DownloadActivity) getActivity()).showDialog(getActivity(), regionDialogFragment);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		app.getOfflineForecastHelper().unregisterRemoveLocalForecastListener(this);
	}

	private void reloadData() {
		DownloadResources indexes = downloadThread.getIndexes();
		group = indexes.getGroupById(groupId);

		if (!openAsDialog()) {
			updateSearchView();
		}
		updateSubscribeEmailView();
		updateDescriptionView();
		updateFreeMapsView();

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
		listView.setBackgroundColor(ColorUtilities.getListBgColor(app, nightMode));
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
		if (subscribeEmailView != null && !DownloadActivity.isDownloadingPermitted(settings) && !settings.EMAIL_SUBSCRIBED.get()) {
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
			DownloadResourceGroupFragment regionDialogFragment = createInstance(uniqueId);
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
	public void onRemoveLocalForecastEvent() {
		listAdapter.notifyDataSetChanged();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putString(REGION_ID_DLG_KEY, groupId);
		super.onSaveInstanceState(outState);
	}

	private DownloadActivity getDownloadActivity() {
		return (DownloadActivity) getActivity();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		if (!openAsDialog()) {
			int colorResId = ColorUtilities.getActiveButtonsAndLinksTextColorId(nightMode);

			MenuItem itemReload = menu.add(0, RELOAD_ID, 0, R.string.shared_string_refresh);
			itemReload.setIcon(getIcon(R.drawable.ic_action_refresh_dark, colorResId));
			itemReload.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

			MenuItem itemSearch = menu.add(0, SEARCH_ID, 1, R.string.shared_string_search);
			itemSearch.setIcon(getIcon(R.drawable.ic_action_search_dark, colorResId));
			itemSearch.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case RELOAD_ID:
				// re-create the thread
				downloadThread.runReloadIndexFiles();
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