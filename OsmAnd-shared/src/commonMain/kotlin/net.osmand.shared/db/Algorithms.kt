package net.osmand.shared.db

class Algorithms {
	companion object {
		fun colorToString(color: Int): String? {
			return if (-0x1000000 and color == -0x1000000) {
				"#" + format(
					6,
					(color and 0x00FFFFFF).toString(16)) //$NON-NLS-1$
			} else {
				"#" + format(
					8,
					color.toString(16)) //$NON-NLS-1$
			}
		}

		private fun format(i: Int, hexString: String): String {
			var varHexString = hexString
			while (varHexString.length < i) {
				varHexString = "0$varHexString"
			}
			return varHexString
		}

	}
}