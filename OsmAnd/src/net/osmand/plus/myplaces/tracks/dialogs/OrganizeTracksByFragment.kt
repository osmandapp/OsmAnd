package net.osmand.plus.myplaces.tracks.dialogs

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
import com.google.android.material.transition.Hold
import net.osmand.plus.R
import net.osmand.plus.base.BaseFullScreenFragment
import net.osmand.plus.base.dialog.interfaces.dialog.IAskRefreshDialogCompletely
import net.osmand.plus.myplaces.tracks.dialogs.OrganizeTracksByController.Companion.PROCESS_ID
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.widgets.dialogbutton.DialogButton

class OrganizeTracksByFragment : BaseFullScreenFragment(), IAskRefreshDialogCompletely {

    private var adapter: OrganizeTracksByAdapter? = null
    private var controller: OrganizeTracksByController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controller = app.dialogManager.findController(PROCESS_ID) as? OrganizeTracksByController
        if (controller != null) {
            controller!!.registerDialog(this)
        } else {
            dismiss()
        }
        exitTransition = Hold()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        updateNightMode()
        val view = inflate(R.layout.fragment_organize_tracks_by, container, false)
        AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view)

        setupToolbar(view)
        setupRecycler(view)
        setupSaveButton(view)

        updateScreen()
        return view
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

        ViewCompat.setElevation(view.findViewById(R.id.appbar), 5.0f)
    }

    private fun setupRecycler(view: View) {
        val currentController = controller ?: return

        adapter = OrganizeTracksByAdapter(app, appMode, currentController)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = adapter
    }

    private fun setupSaveButton(view: View) {
        view.findViewById<DialogButton>(R.id.save_button).setOnClickListener {
            controller?.askSaveChanges()
            dismiss()
        }
    }

    override fun onAskRefreshDialogCompletely(processId: String) {
        updateScreen()
    }

    private fun updateScreen() {
        val currentController = controller ?: return
        adapter?.setScreenItems(currentController.populateScreenItems())
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

    private fun dismiss() {
        activity?.onBackPressed()
    }

    override fun getStatusBarColorId(): Int {
        return ColorUtilities.getStatusBarSecondaryColorId(nightMode)
    }

    override fun getContentStatusBarNightMode() = nightMode

    companion object {

        const val TAG = "OrganizeTracksByFragment"

        fun showInstance(
            manager: androidx.fragment.app.FragmentManager,
            appMode: ApplicationMode
        ): Boolean {
            if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
                val fragment = OrganizeTracksByFragment()
                val arguments = Bundle()
                arguments.putString(APP_MODE_KEY, appMode.stringKey)
                fragment.arguments = arguments
                manager.beginTransaction()
                    .replace(R.id.fragmentContainer, fragment, TAG)
                    .addToBackStack(TAG)
                    .commitAllowingStateLoss()
                return true
            }
            return false
        }
    }
}