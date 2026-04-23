package net.osmand.plus.plugins.astronomy.views.contextmenu

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.osmand.plus.R
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.astronomy.AstroArticle
import net.osmand.plus.plugins.astronomy.AstronomyPlugin
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.InsetTarget
import net.osmand.plus.utils.InsetTargetsCollection
import net.osmand.plus.utils.InsetsUtils.InsetSide
import net.osmand.plus.wikipedia.WikiArticleBaseDialogFragment

class AstroArticleDialogFragment : WikiArticleBaseDialogFragment() {

	companion object {
		val TAG: String = AstroArticleDialogFragment::class.java.simpleName

		private const val ARG_WIKIDATA_ID = "wikidataId"
		private const val ARG_LANG = "lang"
		private val BODY_CONTENT_REGEX = Regex(
			"<body[^>]*>([\\s\\S]*?)</body>",
			RegexOption.IGNORE_CASE
		)

		fun showInstance(fragmentManager: FragmentManager, wikidataId: String, lang: String): Boolean {
			if (!AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG, true)) {
				return false
			}
			val fragment = AstroArticleDialogFragment().apply {
				arguments = Bundle().apply {
					putString(ARG_WIKIDATA_ID, wikidataId)
					putString(ARG_LANG, lang)
				}
			}
			fragment.show(fragmentManager, TAG)
			return true
		}
	}

	private var article: AstroArticle? = null
	private var articleHtml: String? = null
	private var htmlLoadJob: Job? = null

	private lateinit var readFullArticleButton: TextView

	@SuppressLint("SetJavaScriptEnabled")
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		updateNightMode()
		val mainView = inflate(R.layout.wikipedia_dialog_fragment, container, false)

		setupToolbar(mainView.findViewById(R.id.toolbar))

		articleToolbarText = mainView.findViewById(R.id.title_text_view)
		selectedLangTv = mainView.findViewById(R.id.select_language_text_view)
		selectedLangTv.isVisible = false
		mainView.findViewById<ImageView>(R.id.options_button).isVisible = false

		readFullArticleButton = mainView.findViewById(R.id.read_full_article)
		readFullArticleButton.setBackgroundResource(
			if (nightMode) R.drawable.bt_round_long_night else R.drawable.bt_round_long_day
		)
		readFullArticleButton.setTextColor(
			AndroidUtils.createPressedColorStateList(
				context,
				nightMode,
				R.color.ctx_menu_controller_button_text_color_light_n,
				R.color.ctx_menu_controller_button_text_color_light_p,
				R.color.ctx_menu_controller_button_text_color_dark_n,
				R.color.ctx_menu_controller_button_text_color_dark_p
			)
		)
		readFullArticleButton.setCompoundDrawablesWithIntrinsicBounds(
			getContentIcon(R.drawable.ic_world_globe_dark),
			null,
			null,
			null
		)
		readFullArticleButton.compoundDrawablePadding =
			resources.getDimensionPixelSize(R.dimen.content_padding_small)
		readFullArticleButton.setPadding(
			resources.getDimensionPixelSize(R.dimen.wikipedia_button_left_padding),
			0,
			resources.getDimensionPixelSize(R.dimen.dialog_content_margin),
			0
		)

		contentWebView = mainView.findViewById(R.id.content_web_view)
		contentWebView.setBackgroundColor(
			ContextCompat.getColor(
				app,
				if (nightMode) R.color.list_background_color_dark else R.color.list_background_color_light
			)
		)
		val webSettings = contentWebView.settings
		webSettings.javaScriptEnabled = true
		webSettings.domStorageEnabled = true
		webSettings.cacheMode = WebSettings.LOAD_DEFAULT
		webSettings.textZoom = (resources.configuration.fontScale * 100f).toInt()
		updateWebSettings()

		return mainView
	}

	override fun getInsetTargets(): InsetTargetsCollection {
		val collection = super.getInsetTargets()
		collection.add(
			InsetTarget.createCustomBuilder(R.id.read_full_article)
				.portraitSides(InsetSide.BOTTOM)
				.preferMargin(true)
				.build()
		)
		return collection
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		populateArticle()
	}

	override fun onDestroyView() {
		htmlLoadJob?.cancel()
		super.onDestroyView()
	}

	override fun populateArticle() {
		val wikidataId = arguments?.getString(ARG_WIKIDATA_ID) ?: return
		val lang = arguments?.getString(ARG_LANG)
		article = PluginsHelper.requirePlugin(AstronomyPlugin::class.java)
			.dataProvider
			.getAstroArticle(app, wikidataId, lang)
		val currentArticle = article ?: return

		articleToolbarText.text = currentArticle.title
		val onlineArticleUrl = currentArticle.getOnlineArticleUrl()
		contentWebView.webViewClient = AstroArticleWebViewClient(
			requireActivity(),
			nightMode,
			onlineArticleUrl
		)

		readFullArticleButton.isVisible =
			!onlineArticleUrl.isNullOrBlank() && app.settings.isInternetConnectionAvailable(true)
		readFullArticleButton.setOnClickListener {
			if (!onlineArticleUrl.isNullOrBlank()) {
				AndroidUtils.openUrl(requireContext(), onlineArticleUrl, nightMode)
			}
		}

		htmlLoadJob?.cancel()
		htmlLoadJob = viewLifecycleOwner.lifecycleScope.launch {
			articleHtml = withContext(Dispatchers.Default) {
				currentArticle.getMobileHtmlString()
			}
			if (!isAdded || articleHtml.isNullOrBlank()) {
				return@launch
			}
			contentWebView.loadDataWithBaseURL(
				getBaseUrl(),
				createHtmlContent(),
				"text/html",
				"UTF-8",
				null
			)
		}
	}

	override fun showPopupLangMenu(view: View, langSelected: String) = Unit

	override fun createHtmlContent(): String {
		val currentArticle = article
		val lang = currentArticle?.lang
		val bodyTag = if (rtlLanguages.contains(lang)) "<body dir=\"rtl\">\n" else "<body>\n"
		val nightModeClass = if (nightMode) " nightmode" else ""
		val bodyContent = extractBodyContent(articleHtml.orEmpty())
		return buildString {
			append(HEADER_INNER)
			append(bodyTag)
			append("<div class=\"main")
			append(nightModeClass)
			append("\">\n")
			append(bodyContent)
			append(FOOTER_INNER)
		}
	}

	override fun getBaseUrl(): String {
		return super.getBaseUrl()
	}

	private fun extractBodyContent(html: String): String {
		return BODY_CONTENT_REGEX.find(html)?.groups?.get(1)?.value ?: html
	}
}
