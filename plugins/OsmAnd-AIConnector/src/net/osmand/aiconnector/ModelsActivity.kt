package net.osmand.aiconnector

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.mlkit.genai.common.FeatureStatus
import net.osmand.aiconnector.assistant.GeminiNanoModel
import net.osmand.aiconnector.assistant.LocalModels
import net.osmand.aiconnector.assistant.ModelEngine
import net.osmand.aiconnector.assistant.ModelInfo
import net.osmand.aiconnector.assistant.ModelState
import java.util.concurrent.Executors

/** Which model answers in the chat: download, choose or delete the models that run on the phone. */
class ModelsActivity : AppCompatActivity() {

	private lateinit var models: LocalModels
	private val cards = mutableMapOf<String, View>()
	private val handler = Handler(Looper.getMainLooper())
	private val worker = Executors.newSingleThreadExecutor()
	private val poll = object : Runnable {
		override fun run() {
			refresh()
			handler.postDelayed(this, 1000)
		}
	}

	/** AICore status of Gemini Nano, checked off the main thread; null until known. */
	private var nanoStatus: Int? = null
	private var nanoMessage: String? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_models)
		models = (application as AssistantApplication).assistant.models
		findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }
		val container: LinearLayout = findViewById(R.id.models)
		for (m in models.all) {
			val card = LayoutInflater.from(this).inflate(R.layout.item_model, container, false)
			card.findViewById<TextView>(R.id.title).text = m.title
			card.findViewById<View>(R.id.badge).visibility = if (m.recommended) View.VISIBLE else View.GONE
			card.findViewById<TextView>(R.id.summary).setText(m.summary)
			card.findViewById<Button>(R.id.use).setOnClickListener {
				models.selected = m
				finish()
			}
			card.findViewById<Button>(R.id.download).setOnClickListener {
				if (m.engine == ModelEngine.GEMINI_NANO) downloadNano() else models.download(m)
				refresh()
			}
			card.findViewById<Button>(R.id.delete).setOnClickListener {
				models.delete(m)
				refresh()
			}
			container.addView(card)
			cards[m.id] = card
		}
		checkNano()
	}

	override fun onResume() {
		super.onResume()
		handler.post(poll)
	}

	override fun onDestroy() {
		super.onDestroy()
		worker.shutdown()
	}

	override fun onPause() {
		super.onPause()
		handler.removeCallbacks(poll)
	}

	private fun checkNano() = worker.execute {
		val (status, message) = try {
			GeminiNanoModel().status() to null
		} catch (e: Throwable) {
			FeatureStatus.UNAVAILABLE to (e.message ?: e.toString())
		}
		handler.post {
			nanoStatus = status
			nanoMessage = message
			refresh()
		}
	}

	private fun downloadNano() = worker.execute {
		// the Download button goes away at once, a second tap would start a second download
		handler.post {
			nanoStatus = FeatureStatus.DOWNLOADING
			refresh()
		}
		try {
			GeminiNanoModel().download({ done ->
				handler.post { nanoMessage = getString(R.string.downloading_bytes, Formatter.formatShortFileSize(this, done)) }
			}) { error ->
				handler.post { nanoMessage = error }
				checkNano()
			}
		} catch (e: Throwable) {
			handler.post { nanoMessage = e.message ?: e.toString() }
		}
	}

	private fun refresh() {
		val selected = models.selected
		for (m in models.all) {
			val card = cards[m.id] ?: continue
			val stateView = card.findViewById<TextView>(R.id.state)
			val bar = card.findViewById<LinearProgressIndicator>(R.id.download_progress)
			val download = card.findViewById<Button>(R.id.download)
			val delete = card.findViewById<Button>(R.id.delete)
			val use = card.findViewById<Button>(R.id.use)
			bar.visibility = View.GONE
			val size = Formatter.formatShortFileSize(this, m.sizeBytes)
			if (m.engine == ModelEngine.GEMINI_NANO) {
				bindNano(stateView, download, delete, use)
			} else when (val s = models.state(m)) {
				is ModelState.NotDownloaded -> {
					stateView.text = getString(R.string.state_not_downloaded, size)
					show(download, true, R.string.download); show(delete, false); show(use, false)
				}
				is ModelState.Failed -> {
					stateView.text = getString(R.string.state_failed, s.reason)
					show(download, true, R.string.download); show(delete, false); show(use, false)
				}
				is ModelState.Downloading -> {
					stateView.text = getString(R.string.state_downloading,
						Formatter.formatShortFileSize(this, s.done), Formatter.formatShortFileSize(this, s.total))
					bar.visibility = View.VISIBLE
					bar.isIndeterminate = s.total <= 0
					if (s.total > 0) bar.setProgressCompat((s.done * 100 / s.total).toInt(), true)
					show(download, false); show(delete, true, R.string.cancel); show(use, false)
				}
				is ModelState.Ready -> {
					stateView.text = getString(R.string.state_ready, size)
					show(download, false); show(delete, true, R.string.delete); show(use, true)
				}
			}
			(card as MaterialCardView).strokeColor = MaterialColors.getColor(card,
				if (m.id == selected?.id) androidx.appcompat.R.attr.colorPrimary
				else com.google.android.material.R.attr.colorOutlineVariant)
			if (m.id == selected?.id) use.setText(R.string.model_in_use)
			use.isEnabled = m.id != selected?.id
		}
	}

	private fun bindNano(state: TextView, download: Button, delete: Button, use: Button) {
		show(delete, false)
		when (nanoStatus) {
			null -> {
				state.setText(R.string.nano_checking); show(download, false); show(use, false)
			}
			FeatureStatus.AVAILABLE -> {
				state.setText(R.string.nano_ready); show(download, false); show(use, true)
			}
			FeatureStatus.DOWNLOADABLE -> {
				state.text = nanoMessage ?: getString(R.string.nano_downloadable)
				show(download, true, R.string.download); show(use, false)
			}
			FeatureStatus.DOWNLOADING -> {
				state.text = nanoMessage ?: getString(R.string.nano_downloading); show(download, false); show(use, false)
			}
			else -> {
				state.text = getString(R.string.nano_unavailable) + (nanoMessage?.let { "\n$it" } ?: "")
				show(download, false); show(use, false)
			}
		}
	}

	private fun show(button: Button, visible: Boolean, text: Int = 0) {
		button.visibility = if (visible) View.VISIBLE else View.GONE
		if (text != 0) button.setText(text)
	}
}
