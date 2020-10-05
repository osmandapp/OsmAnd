package net.osmand.telegram.ui

import android.os.AsyncTask
import android.os.Bundle
import android.view.*
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.osmand.PlatformUtil
import net.osmand.telegram.R
import net.osmand.telegram.TelegramApplication
import java.io.*
import java.lang.ref.WeakReference
import java.util.*

class TrackerLogcatActivity : AppCompatActivity() {
    private var logcatAsyncTask: LogcatAsyncTask? = null
    private val logs: MutableList<String> = ArrayList()
    private var adapter: LogcatAdapter? = null
    private val LEVELS = arrayOf("D", "I", "W", "E")
    private var filterLevel = 1
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        val app: TelegramApplication = getApplication() as TelegramApplication
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tracker_logcat)

        val toolbar = findViewById<Toolbar>(R.id.toolbar).apply {
            navigationIcon = app.uiUtils.getThemedIcon(R.drawable.ic_arrow_back)
            setNavigationOnClickListener { onBackPressed() }
        }
        setSupportActionBar(toolbar)
        setupIntermediateProgressBar()

        adapter = LogcatAdapter()
        recyclerView = findViewById<View>(R.id.recycler_view) as RecyclerView
        recyclerView!!.layoutManager = LinearLayoutManager(this)
        recyclerView!!.adapter = adapter
    }

    protected fun setupIntermediateProgressBar() {
        val progressBar = ProgressBar(this)
        progressBar.visibility = View.GONE
        progressBar.isIndeterminate = true
        val supportActionBar = supportActionBar
        if (supportActionBar != null) {
            supportActionBar.setDisplayShowCustomEnabled(true)
            supportActionBar.customView = progressBar
            setSupportProgressBarIndeterminateVisibility(false)
        }
    }

    override fun setSupportProgressBarIndeterminateVisibility(visible: Boolean) {
        val supportActionBar = supportActionBar
        if (supportActionBar != null) {
            supportActionBar.customView.visibility = if (visible) View.VISIBLE else View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        startLogcatAsyncTask()
    }

    override fun onPause() {
        super.onPause()
        stopLogcatAsyncTask()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val app: TelegramApplication = applicationContext as TelegramApplication
        val share: MenuItem = menu.add(0, SHARE_ID, 0, R.string.shared_string_export)
        share.icon = app.uiUtils.getThemedIcon(R.drawable.ic_action_share)
        share.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        val level = menu.add(0, LEVEL_ID, 0, "")
        level.title = getFilterLevel()
        level.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return super.onCreateOptionsMenu(menu)
    }

    private fun getFilterLevel(): String {
        return "*:" + LEVELS[filterLevel]
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        when (itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            LEVEL_ID -> {
                filterLevel++
                if (filterLevel >= LEVELS.size) {
                    filterLevel = 0
                }
                item.title = getFilterLevel()
                stopLogcatAsyncTask()
                logs.clear()
                adapter!!.notifyDataSetChanged()
                startLogcatAsyncTask()
                return true
            }
            SHARE_ID -> {
                startSaveLogsAsyncTask()
                return true
            }
        }
        return false
    }

    private fun startSaveLogsAsyncTask() {
        val saveLogsAsyncTask = SaveLogsAsyncTask(this, logs)
        saveLogsAsyncTask.execute()
    }

    private fun startLogcatAsyncTask() {
        logcatAsyncTask = LogcatAsyncTask(this, getFilterLevel())
        logcatAsyncTask!!.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    private fun stopLogcatAsyncTask() {
        if (logcatAsyncTask != null && logcatAsyncTask!!.status == AsyncTask.Status.RUNNING) {
            logcatAsyncTask!!.cancel(false)
            logcatAsyncTask!!.stopLogging()
        }
    }

    private inner class LogcatAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(viewGroup.context)
            val itemView = inflater.inflate(R.layout.item_description_long, viewGroup, false) as TextView
            itemView.gravity = Gravity.CENTER_VERTICAL
            return LogViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is LogViewHolder) {
                val log = getLog(position)
                holder.logTextView.text = log
            }
        }

        override fun getItemCount(): Int {
            return logs.size
        }

        private fun getLog(position: Int): String {
            return logs[position]
        }

        private inner class LogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val logTextView: TextView = itemView.findViewById(R.id.description)
        }
    }

    class SaveLogsAsyncTask internal constructor(logcatActivity: TrackerLogcatActivity, logs: Collection<String>) : AsyncTask<Void?, String?, File?>() {
        private val logcatActivity: WeakReference<TrackerLogcatActivity>
        private val logs: Collection<String>

        override fun onPreExecute() {
            val activity = logcatActivity.get()
            activity?.setSupportProgressBarIndeterminateVisibility(true)
        }

        override fun doInBackground(vararg voids: Void?): File {
            val app: TelegramApplication = logcatActivity.get()?.applicationContext as TelegramApplication
            val file = File(app.getExternalFilesDir(null), LOGCAT_PATH)
            try {
                if (file.exists()) {
                    file.delete()
                }
                val stringBuilder = StringBuilder()
                for (log in logs) {
                    stringBuilder.append(log)
                    stringBuilder.append("\n")
                }
                if (file.parentFile.canWrite()) {
                    val writer = BufferedWriter(FileWriter(file, true))
                    writer.write(stringBuilder.toString())
                    writer.close()
                }
            } catch (e: Exception) {
                log.error(e)
            }
            return file
        }

        override fun onPostExecute(file: File?) {
            val activity = logcatActivity.get()
            if (activity != null && file != null) {
                val app: TelegramApplication = activity.applicationContext as TelegramApplication
                activity.setSupportProgressBarIndeterminateVisibility(false)
                app.sendCrashLog(file)
            }
        }

        init {
            this.logcatActivity = WeakReference(logcatActivity)
            this.logs = logs
        }
    }

    class LogcatAsyncTask internal constructor(logcatActivity: TrackerLogcatActivity?, filterLevel: String) : AsyncTask<Void?, String?, Void?>() {
        private var processLogcat: Process? = null
        private val logcatActivity: WeakReference<TrackerLogcatActivity?>
        private val filterLevel: String

        override fun doInBackground(vararg voids: Void?): Void? {
            try {
                val filter = android.os.Process.myPid().toString()
                val command = arrayOf("logcat", filterLevel, "--pid=$filter", "-T", MAX_BUFFER_LOG.toString())
                processLogcat = Runtime.getRuntime().exec(command)
                val bufferedReader = BufferedReader(InputStreamReader(processLogcat?.inputStream))
                var line: String?
                while (bufferedReader.readLine().also { line = it } != null && logcatActivity.get() != null) {
                    if (isCancelled) {
                        break
                    }
                    publishProgress(line)
                }
                stopLogging()
            } catch (e: IOException) { // ignore
            } catch (e: Exception) {
                log.error(e)
            }
            return null
        }

        override fun onProgressUpdate(vararg values: String?) {
            if (values.size > 0 && !isCancelled) {
                val activity = logcatActivity.get()
                if (activity != null) {
                    val autoscroll = !activity.recyclerView!!.canScrollVertically(1)
                    for (s in values) {
                        if (s != null) {
                            activity.logs.add(s)
                        }
                    }
                    activity.adapter!!.notifyDataSetChanged()
                    if (autoscroll) {
                        activity.recyclerView!!.scrollToPosition(activity.logs.size - 1)
                    }
                }
            }
        }

        fun stopLogging() {
            if (processLogcat != null) {
                processLogcat!!.destroy()
            }
        }

        init {
            this.logcatActivity = WeakReference(logcatActivity)
            this.filterLevel = filterLevel
        }
    }

    companion object {
        private const val LOGCAT_PATH = "logcat.log"
        private const val MAX_BUFFER_LOG = 10000
        private const val SHARE_ID = 0
        private const val LEVEL_ID = 1
        private val log = PlatformUtil.getLog(TrackerLogcatActivity::class.java)
    }
}
