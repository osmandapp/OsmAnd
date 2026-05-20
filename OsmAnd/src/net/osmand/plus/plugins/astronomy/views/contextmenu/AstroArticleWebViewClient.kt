package net.osmand.plus.plugins.astronomy.views.contextmenu

import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.FragmentActivity
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.wikipedia.WikiArticleHelper

class AstroArticleWebViewClient(
	private val activity: FragmentActivity,
	private val nightMode: Boolean,
	private val articleUrl: String?
) : WebViewClient() {

	companion object {
		private const val PAGE_PREFIX_HTTP = "http://"
		private const val PAGE_PREFIX_HTTPS = "https://"
	}

	override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
		return handleUrl(request.url?.toString())
	}

	override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
		return handleUrl(url)
	}

	private fun handleUrl(rawUrl: String?): Boolean {
		if (rawUrl.isNullOrBlank()) {
			return false
		}
		val url = WikiArticleHelper.normalizeFileUrl(rawUrl)
		if (url.startsWith("#") || isSamePageAnchor(url)) {
			return false
		}
		return if (url.startsWith(PAGE_PREFIX_HTTP) || url.startsWith(PAGE_PREFIX_HTTPS)) {
			WikiArticleHelper.warnAboutExternalLoad(url, activity, nightMode)
			true
		} else {
			AndroidUtils.startActivityIfSafe(activity, Intent(Intent.ACTION_VIEW, Uri.parse(url)))
		}
	}

	private fun isSamePageAnchor(url: String): Boolean {
		if (!url.contains('#')) {
			return false
		}
		val currentUrl = articleUrl
			?.let(WikiArticleHelper::normalizeFileUrl)
			?.substringBefore('#')
			?: return false
		return url.substringBefore('#') == currentUrl
	}
}
