package resources;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import net.osmand.LogUtil;

import org.apache.commons.logging.Log;


public class _R {
	private static final Log log = LogUtil.getLog(_R.class);

	public static byte[] getIconData(String data) {
		InputStream io = _R.class.getResourceAsStream("h_" + data + ".png");
		if (io == null) {
			io = _R.class.getResourceAsStream("g_" + data + ".png");
		}
		if (io != null) {
			try {
				byte[] buf = new byte[1024];
				int cnt;
				ByteArrayOutputStream ous = new ByteArrayOutputStream();
				while ((cnt = io.read(buf)) != -1) {
					ous.write(buf, 0, cnt);
				}
				return ous.toByteArray();
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
		}
		return null;
	}
}
