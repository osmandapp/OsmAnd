package net.osmand.util;

import net.osmand.data.LatLon;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by rominf on 4/16/16.
 */

public class GeoPolylineParserUtilTest {
	@Test
	public void testDecode() {


		Assert.assertEquals(Arrays.asList(
				new LatLon(52.503032, 13.420526),
				new LatLon(52.503240, 13.420671),
				new LatLon(52.503669, 13.420973),
				new LatLon(52.504054, 13.421244),
				new LatLon(52.504302, 13.421418),
				new LatLon(52.504454, 13.421525),
				new LatLon(52.504616, 13.421639),
				new LatLon(52.504843, 13.421798),
				new LatLon(52.505043, 13.421939),
				new LatLon(52.505102, 13.421981),
				new LatLon(52.505092, 13.422116),
				new LatLon(52.505075, 13.422305),
				new LatLon(52.505063, 13.422509),
				new LatLon(52.505050, 13.422942),
				new LatLon(52.505055, 13.423287),
				new LatLon(52.505071, 13.423649),
				new LatLon(52.505092, 13.423895),
				new LatLon(52.505160, 13.424429),
				new LatLon(52.505204, 13.424704),
				new LatLon(52.505278, 13.425052),
				new LatLon(52.505370, 13.425399),
				new LatLon(52.505508, 13.425830),
				new LatLon(52.505680, 13.426272),
				new LatLon(52.505796, 13.426507),
				new LatLon(52.505851, 13.426619),
				new LatLon(52.505995, 13.426914),
				new LatLon(52.506250, 13.427290),
				new LatLon(52.506366, 13.427431),
				new LatLon(52.506438, 13.427521),
				new LatLon(52.506637, 13.427728),
				new LatLon(52.506849, 13.427905),
				new LatLon(52.507004, 13.428004),
				new LatLon(52.507104, 13.428081),
				new LatLon(52.507253, 13.428195),
				new LatLon(52.507353, 13.428258),
				new LatLon(52.507484, 13.428282),
				new LatLon(52.507651, 13.428288),
				new LatLon(52.507947, 13.428300),
				new LatLon(52.508137, 13.428360),
				new LatLon(52.508293, 13.428475),
				new LatLon(52.508412, 13.428562),
				new LatLon(52.508687, 13.428804),
				new LatLon(52.508874, 13.428973),
				new LatLon(52.509587, 13.429607),
				new LatLon(52.509697, 13.429708),
				new LatLon(52.510056, 13.430027),
				new LatLon(52.510192, 13.430113),
				new LatLon(52.510476, 13.430249),
				new LatLon(52.510559, 13.430042),
				new LatLon(52.510925, 13.429097),
				new LatLon(52.511293, 13.428160),
				new LatLon(52.511772, 13.427079),
				new LatLon(52.511958, 13.427142),
				new LatLon(52.512213, 13.427215),
				new LatLon(52.512322, 13.427244),
				new LatLon(52.512495, 13.427291),
				new LatLon(52.512879, 13.427406),
				new LatLon(52.513202, 13.427515),
				new LatLon(52.513547, 13.427699),
				new LatLon(52.514054, 13.427939),
				new LatLon(52.514941, 13.428551),
				new LatLon(52.515179, 13.428724),
				new LatLon(52.515530, 13.428902),
				new LatLon(52.515872, 13.429033),
				new LatLon(52.516514, 13.429265),
				new LatLon(52.516582, 13.429288)),
				GeoPolylineParserUtil.parse("" +
						"o~occB{}brX_LaHyY{QaW}OoN{IoHuEcIcFeM}HoKyGuBsARmG`@yJVwKXaZIqT_@sUi@kNgCk`@wAePsCwTwDuTsG}Y" +
						"wIsZgFuMmB_F_HmQ}NoVgFyGoCsDmK}KgLaJuHeEgEyCiHcFgE}BeGo@mIKoQW{JwBwHeFmFmDePcNuJqIqk@sf@{EiE" +
						"mU}RoGkDwPoGeD|K{U`z@_Vpy@}\\pbAsJ}B}NqCyEy@yI}A_WeFeSyEqToJu^_Nmv@ge@{MyI}TcJkTeGcg@oMgCm@",
						GeoPolylineParserUtil.PRECISION_6));
	}
}
