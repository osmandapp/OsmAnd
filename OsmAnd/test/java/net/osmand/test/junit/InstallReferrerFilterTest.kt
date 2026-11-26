package net.osmand.test.junit

import androidx.test.ext.junit.runners.AndroidJUnit4
import net.osmand.plus.inapp.InstallReferrer
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [InstallReferrer.filter] keeps the campaign of a Google Play install referrer.
 *
 * The referrer used to be parsed with `Uri.parse("?" + referrer)`, and `Uri.parse` takes the first
 * ':' anywhere in the string for the scheme separator. A ':' in a value made the Uri opaque, so
 * `getQueryParameter` threw and the referrer was never stored; a "://" in a value turned the whole
 * referrer into a bogus scheme with no query, so nothing was kept and the install was marked as
 * checked for good.
 */
@RunWith(AndroidJUnit4::class)
class InstallReferrerFilterTest {

	@Test
	fun keepsCampaignOfPlainReferrer() {
		assertEquals("utm_source=google-play&utm_medium=organic",
			InstallReferrer.filter("utm_source=google-play&utm_medium=organic"))
	}

	@Test
	fun keepsCampaignWhenValueHasColon() {
		assertEquals("utm_source=google&utm_medium=cpc&utm_campaign=sale%3A2026",
			InstallReferrer.filter("utm_source=google&utm_medium=cpc&utm_campaign=sale:2026"))
		assertEquals("utm_source=apps.facebook.com&utm_campaign=fb4a",
			InstallReferrer.filter("utm_source=apps.facebook.com&utm_campaign=fb4a&utm_content={\"app\":0,\"t\":1}"))
	}

	@Test
	fun keepsCampaignWhenValueHasUrl() {
		assertEquals("utm_source=site&utm_medium=web&utm_campaign=c1",
			InstallReferrer.filter("utm_source=site&utm_medium=web&utm_campaign=c1&utm_content=https://osmand.net/docs"))
	}

	@Test
	fun replacesAdClickIdWithFlag() {
		assertEquals("utm_source=google&utm_medium=cpc&ad_click=1",
			InstallReferrer.filter("gclid=Cj0KCQjw&utm_source=google&utm_medium=cpc"))
	}
}
