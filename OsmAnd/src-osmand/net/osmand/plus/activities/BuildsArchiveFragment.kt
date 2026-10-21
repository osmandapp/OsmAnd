package net.osmand.plus.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import net.osmand.plus.R
import net.osmand.plus.activities.BuildsManager.ListState
import net.osmand.plus.base.BaseMaterialFragment
import net.osmand.plus.utils.InsetTarget
import net.osmand.plus.utils.InsetTargetsCollection
import net.osmand.plus.widgets.ui.SegmentedList
import net.osmand.plus.widgets.ui.SettingRow
import java.util.Date

/** Night builds that are no longer the latest ones, newest first. */
class BuildsArchiveFragment : BaseMaterialFragment(), BuildsManager.Listener {

	private lateinit var manager: BuildsManager
	private val adapter = BuildsAdapter()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		manager = BuildsManager.getInstance(osmandApp)
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		val view = inflater.inflate(R.layout.fragment_builds_archive, container, false)
		val toolbar: MaterialToolbar = view.findViewById(R.id.toolbar)
		toolbar.setTitle(R.string.shared_string_archive)
		toolbar.setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

		val recyclerView: RecyclerView = view.findViewById(R.id.scroll_view)
		recyclerView.layoutManager = LinearLayoutManager(view.context)
		recyclerView.adapter = adapter
		return view
	}

	override fun onResume() {
		super.onResume()
		manager.addListener(this)
		// after the process was killed the list is not there yet
		manager.loadBuilds(false)
		manager.listState?.let { onListStateChanged(it) }
	}

	override fun onPause() {
		super.onPause()
		manager.removeListener(this)
	}

	override fun onListStateChanged(state: ListState) {
		if (state is ListState.Loaded) {
			adapter.setBuilds(state.builds.filter {
				!it.path.startsWith(BuildsManager.LATEST_BUILDS_FOLDER)
			}, manager.getInstalledDate())
		}
	}

	override fun getStatusBarColorId(): Int =
		if (nightMode) R.color.surface_dark else R.color.surface_light

	override fun getInsetTargets(): InsetTargetsCollection {
		val collection = InsetTargetsCollection()
		collection.add(InsetTarget.createRootInset())
		collection.add(InsetTarget.createScrollable(R.id.scroll_view))
		return collection
	}

	private inner class BuildsAdapter : RecyclerView.Adapter<RowHolder>() {

		private var builds: List<OsmAndBuild> = emptyList()
		private var installedDate: Date? = null

		fun setBuilds(builds: List<OsmAndBuild>, installedDate: Date?) {
			this.builds = builds
			this.installedDate = installedDate
			notifyDataSetChanged()
		}

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowHolder =
			RowHolder(LayoutInflater.from(parent.context)
				.inflate(R.layout.item_ui_setting_row, parent, false))

		override fun onBindViewHolder(holder: RowHolder, position: Int) {
			val view = holder.row.view
			// the rows of one group: shape by position, a small gap between them
			view.setBackgroundResource(SegmentedList.backgroundFor(position, builds.size))
			val params = view.layoutParams as ViewGroup.MarginLayoutParams
			params.topMargin = if (position == 0) 0 else view.resources.getDimensionPixelSize(R.dimen.ui_segment_gap)
			view.layoutParams = params
			BuildsFragment.bindBuildRow(holder.row, builds[position], installedDate) {
				BuildDownloadDialog.show(parentFragmentManager, it)
			}
		}

		override fun getItemCount(): Int = builds.size
	}

	private class RowHolder(view: View) : RecyclerView.ViewHolder(view) {
		val row = SettingRow(view)
	}
}
