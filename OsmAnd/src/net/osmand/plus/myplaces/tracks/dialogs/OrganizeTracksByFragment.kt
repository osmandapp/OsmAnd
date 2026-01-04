package net.osmand.plus.myplaces.tracks.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.R
import net.osmand.plus.base.BaseFullScreenDialogFragment
import net.osmand.plus.base.dialog.interfaces.dialog.IAskRefreshDialogCompletely
import net.osmand.plus.base.dialog.interfaces.dialog.IDialogNightModeInfoProvider
import net.osmand.plus.myplaces.tracks.controller.OrganizeTracksByController
import net.osmand.plus.myplaces.tracks.controller.OrganizeTracksByController.Companion.PROCESS_ID
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.InsetTarget
import net.osmand.plus.utils.InsetTargetsCollection
import net.osmand.plus.widgets.dialogbutton.DialogButton

class OrganizeTracksByFragment : BaseFullScreenDialogFragment(), IAskRefreshDialogCompletely,
    IDialogNightModeInfoProvider {

    companion object {

        private val TAG = OrganizeTracksByFragment::class.java.simpleName

        private const val RECYCLER_STATE_KEY = "recycler_state_key"

        fun showInstance(
            manager: androidx.fragment.app.FragmentManager,
            appMode: ApplicationMode
        ): Boolean {
            if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
                val fragment = OrganizeTracksByFragment()
                val arguments = Bundle()
                arguments.putString(APP_MODE_KEY, appMode.stringKey)
                fragment.arguments = arguments
                fragment.show(manager, TAG)
                return true
            }
            return false
        }
    }

    private var adapter: OrganizeTracksByAdapter? = null
    private var controller: OrganizeTracksByController? = null

    /**
     * Stores the exact scroll position (state) of the LayoutManager
     * to restore it after a configuration change (e.g., screen rotation).
     */
    private var recyclerState: android.os.Parcelable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controller = app.dialogManager.findController(PROCESS_ID) as? OrganizeTracksByController
        if (controller != null) {
            controller!!.registerDialog(this)
        } else {
            dismiss()
        }

        if (savedInstanceState != null) {
            recyclerState = savedInstanceState.getParcelable(RECYCLER_STATE_KEY)
        }
    }

    override fun getThemeId(): Int {
        return if (nightMode) R.style.OsmandDarkTheme else R.style.OsmandLightTheme_LightStatusBar
    }

    override fun getStatusBarColorId(): Int {
        return ColorUtilities.getStatusBarSecondaryColorId(nightMode)
    }

    override fun createDialog(savedInstanceState: Bundle?): Dialog {
        return object : Dialog(requireContext(), themeId) {
            override fun onBackPressed() {
                dismiss()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        updateNightMode()
        val view = inflate(R.layout.fragment_organize_tracks_by, container, false)
        view.setBackgroundColor(ColorUtilities.getActivityListBgColor(app, nightMode))
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar(view)
        setupRecycler(view)
        setupApplyButton(view)
        updateScreen()
    }

    override fun getInsetTargets(): InsetTargetsCollection {
        val collection = super.getInsetTargets()
        // Define targets for edge-to-edge display
        collection.replace(InsetTarget.createBottomContainer(R.id.buttons_container))
        collection.replace(InsetTarget.createScrollable(R.id.recycler_view))
        return collection
    }

    private fun setupToolbar(view: View) {
        val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
        toolbar.findViewById<View>(R.id.toolbar_subtitle).visibility = View.GONE
        toolbar.findViewById<View>(R.id.action_button).visibility = View.GONE

        val closeButton = toolbar.findViewById<ImageView>(R.id.close_button)
        closeButton.setImageResource(AndroidUtils.getNavigationIconResId(app))
        closeButton.setOnClickListener { dismiss() }

        val title = toolbar.findViewById<TextView>(R.id.toolbar_title)
        title.setText(R.string.organize_by)

        val appbar = view.findViewById<View>(R.id.appbar)
        if (appbar != null) {
            ViewCompat.setElevation(appbar, 5.0f)
        }
    }

    private fun setupRecycler(view: View) {
        val currentController = controller ?: return

        adapter = OrganizeTracksByAdapter(app, appMode, currentController)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = adapter
        recyclerView.layoutManager?.isItemPrefetchEnabled = false
    }

    private fun setupApplyButton(view: View) {
        view.findViewById<DialogButton>(R.id.save_button).setOnClickListener {
            controller?.askSaveChanges(activity)
            dismiss()
        }
    }

    override fun onAskRefreshDialogCompletely(processId: String) {
        updateScreen()
    }

    private fun updateScreen() {
        val currentController = controller ?: return
        adapter?.setScreenItems(currentController.populateScreenItems())

        // Restore exact scroll position if we have it (after rotation)
        if (recyclerState != null) {
            val recyclerView = view?.findViewById<RecyclerView>(R.id.recycler_view)
            recyclerView?.layoutManager?.onRestoreInstanceState(recyclerState)
            recyclerState = null
        }
    }

    override fun onResume() {
        super.onResume()
        callMapActivity {
            it.disableDrawer()
            controller?.fragmentActivity = it
        }
    }

    override fun onPause() {
        super.onPause()
        callMapActivity {
            it.enableDrawer()
        }
        controller?.fragmentActivity = null
    }

    override fun onDestroy() {
        super.onDestroy()
        controller?.finishProcessIfNeeded(activity)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save the current scroll position before destruction
        val recyclerView = view?.findViewById<RecyclerView>(R.id.recycler_view)
        val state = recyclerView?.layoutManager?.onSaveInstanceState()
        outState.putParcelable(RECYCLER_STATE_KEY, state)
    }
}