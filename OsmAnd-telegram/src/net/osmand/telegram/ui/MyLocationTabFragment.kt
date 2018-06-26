package net.osmand.telegram.ui

import android.animation.*
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.*
import android.widget.*
import net.osmand.telegram.R
import net.osmand.telegram.TelegramApplication

class MyLocationTabFragment : Fragment() {

	private var textMarginSmall: Int = 0
	private var textMarginBig: Int = 0
	private var searchBoxHeight: Int = 0
	private var searchBoxSidesMargin: Int = 0

	private val app: TelegramApplication
		get() = activity?.application as TelegramApplication

	private lateinit var appBarLayout: AppBarLayout
	private lateinit var textContainer: LinearLayout
	private lateinit var title: TextView
	private lateinit var description: TextView
	private lateinit var searchBox: FrameLayout

	private lateinit var searchBoxBg: GradientDrawable

	private var appBarCollapsed = false
	private lateinit var appBarOutlineProvider: ViewOutlineProvider

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		textMarginSmall = resources.getDimensionPixelSize(R.dimen.content_padding_standard)
		textMarginBig = resources.getDimensionPixelSize(R.dimen.my_location_text_sides_margin)
		searchBoxHeight = resources.getDimensionPixelSize(R.dimen.search_box_height)
		searchBoxSidesMargin = resources.getDimensionPixelSize(R.dimen.content_padding_half)

		val mainView = inflater.inflate(R.layout.fragment_my_location_tab, container, false)

		appBarLayout = mainView.findViewById<AppBarLayout>(R.id.app_bar_layout).apply {
			if (Build.VERSION.SDK_INT >= 21) {
				appBarOutlineProvider = outlineProvider
				outlineProvider = null
			}
			addOnOffsetChangedListener { appBar, offset ->
				val collapsed = Math.abs(offset) == appBar.totalScrollRange
				if (collapsed != appBarCollapsed) {
					appBarCollapsed = collapsed
					adjustText()
					adjustSearchBox()
				}
			}
		}

		textContainer = mainView.findViewById<LinearLayout>(R.id.text_container).apply {
			if (Build.VERSION.SDK_INT >= 16) {
				layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
			}
			title = findViewById(R.id.title)
			description = findViewById(R.id.description)
		}

		searchBoxBg = GradientDrawable().apply {
			shape = GradientDrawable.RECTANGLE
			setColor(ContextCompat.getColor(context!!, R.color.screen_bg_light))
			cornerRadius = (searchBoxHeight / 2).toFloat()
		}

		searchBox = mainView.findViewById<FrameLayout>(R.id.search_box).apply {
			if (Build.VERSION.SDK_INT >= 16) {
				background = searchBoxBg
			} else {
				@Suppress("DEPRECATION")
				setBackgroundDrawable(searchBoxBg)
			}
			findViewById<View>(R.id.search_button).setOnClickListener {
				Toast.makeText(context, "Search", Toast.LENGTH_SHORT).show()
			}
			findViewById<ImageView>(R.id.search_icon)
				.setImageDrawable(app.uiUtils.getThemedIcon(R.drawable.ic_action_search_dark))
		}

		return mainView
	}

	private fun adjustText() {
		val gravity = if (appBarCollapsed) Gravity.START else Gravity.CENTER
		val padding = if (appBarCollapsed) textMarginSmall else textMarginBig
		textContainer.setPadding(padding, 0, padding, 0)
		title.gravity = gravity
		description.gravity = gravity
	}

	private fun adjustSearchBox() {
		val cornerRadiusFrom = if (appBarCollapsed) searchBoxHeight / 2 else 0
		val cornerRadiusTo = if (appBarCollapsed) 0 else searchBoxHeight / 2
		val marginFrom = if (appBarCollapsed) searchBoxSidesMargin else 0
		val marginTo = if (appBarCollapsed) 0 else searchBoxSidesMargin

		val cornerAnimator = ObjectAnimator.ofFloat(
			searchBoxBg,
			"cornerRadius",
			cornerRadiusFrom.toFloat(),
			cornerRadiusTo.toFloat()
		)

		val marginAnimator = ValueAnimator.ofInt(marginFrom, marginTo)
		marginAnimator.addUpdateListener {
			val value = it.animatedValue as Int
			val params = searchBox.layoutParams as LinearLayout.LayoutParams
			params.setMargins(value, params.topMargin, value, params.bottomMargin)
			searchBox.layoutParams = params
		}

		val animatorSet = AnimatorSet()
		animatorSet.duration = 200
		animatorSet.playTogether(cornerAnimator, marginAnimator)
		if (Build.VERSION.SDK_INT >= 21) {
			if (appBarCollapsed) {
				animatorSet.addListener(object : AnimatorListenerAdapter() {
					override fun onAnimationEnd(animation: Animator?) {
						if (Build.VERSION.SDK_INT >= 21 && appBarCollapsed) {
							appBarLayout.outlineProvider = appBarOutlineProvider
						}
					}
				})
			} else {
				appBarLayout.outlineProvider = null
			}
		}
		animatorSet.start()
	}
}
