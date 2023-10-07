package net.osmand.plus.keyevent;

import android.util.SparseArray;
import android.view.KeyEvent;

import androidx.annotation.NonNull;

public class KeySymbolMapper {

	private final SparseArray<String> keySymbolMap = new SparseArray<>();

	private KeySymbolMapper() {
		// Letter key codes
		keySymbolMap.put(KeyEvent.KEYCODE_A, "A");
		keySymbolMap.put(KeyEvent.KEYCODE_B, "B");
		keySymbolMap.put(KeyEvent.KEYCODE_C, "C");
		keySymbolMap.put(KeyEvent.KEYCODE_D, "D");
		keySymbolMap.put(KeyEvent.KEYCODE_E, "E");
		keySymbolMap.put(KeyEvent.KEYCODE_F, "F");
		keySymbolMap.put(KeyEvent.KEYCODE_G, "G");
		keySymbolMap.put(KeyEvent.KEYCODE_H, "H");
		keySymbolMap.put(KeyEvent.KEYCODE_I, "I");
		keySymbolMap.put(KeyEvent.KEYCODE_J, "J");
		keySymbolMap.put(KeyEvent.KEYCODE_K, "K");
		keySymbolMap.put(KeyEvent.KEYCODE_L, "L");
		keySymbolMap.put(KeyEvent.KEYCODE_M, "M");
		keySymbolMap.put(KeyEvent.KEYCODE_N, "N");
		keySymbolMap.put(KeyEvent.KEYCODE_O, "O");
		keySymbolMap.put(KeyEvent.KEYCODE_P, "P");
		keySymbolMap.put(KeyEvent.KEYCODE_Q, "Q");
		keySymbolMap.put(KeyEvent.KEYCODE_R, "R");
		keySymbolMap.put(KeyEvent.KEYCODE_S, "S");
		keySymbolMap.put(KeyEvent.KEYCODE_T, "T");
		keySymbolMap.put(KeyEvent.KEYCODE_U, "U");
		keySymbolMap.put(KeyEvent.KEYCODE_V, "V");
		keySymbolMap.put(KeyEvent.KEYCODE_W, "W");
		keySymbolMap.put(KeyEvent.KEYCODE_X, "X");
		keySymbolMap.put(KeyEvent.KEYCODE_Y, "Y");
		keySymbolMap.put(KeyEvent.KEYCODE_Z, "Z");

		keySymbolMap.put(KeyEvent.KEYCODE_PLUS, "+");
		keySymbolMap.put(KeyEvent.KEYCODE_EQUALS, "=");
		keySymbolMap.put(KeyEvent.KEYCODE_MINUS, "-");

		// Add more key code to symbol mappings here
		keySymbolMap.put(KeyEvent.KEYCODE_DPAD_UP, "↑");
		keySymbolMap.put(KeyEvent.KEYCODE_DPAD_DOWN, "↓");
		keySymbolMap.put(KeyEvent.KEYCODE_DPAD_LEFT, "←");
		keySymbolMap.put(KeyEvent.KEYCODE_DPAD_RIGHT, "→");
		keySymbolMap.put(KeyEvent.KEYCODE_DPAD_CENTER, "CENTER");

		keySymbolMap.put(KeyEvent.KEYCODE_SPACE, "space");
		keySymbolMap.put(KeyEvent.KEYCODE_ENTER, "↵");
		keySymbolMap.put(KeyEvent.KEYCODE_TAB, "⇥");
		keySymbolMap.put(KeyEvent.KEYCODE_BACK, "⇦");
		keySymbolMap.put(KeyEvent.KEYCODE_SHIFT_LEFT, "⇧");
		keySymbolMap.put(KeyEvent.KEYCODE_SHIFT_RIGHT, "⇧");
		keySymbolMap.put(KeyEvent.KEYCODE_CAPS_LOCK, "⇪");
		keySymbolMap.put(KeyEvent.KEYCODE_DEL, "⌫");
		keySymbolMap.put(KeyEvent.KEYCODE_ESCAPE, "⎋");

		keySymbolMap.put(KeyEvent.KEYCODE_NUM_LOCK, "Num");
		keySymbolMap.put(KeyEvent.KEYCODE_ALT_LEFT, "Alt");
		keySymbolMap.put(KeyEvent.KEYCODE_ALT_RIGHT, "Alt");
		keySymbolMap.put(KeyEvent.KEYCODE_CTRL_LEFT, "Ctrl");
		keySymbolMap.put(KeyEvent.KEYCODE_CTRL_RIGHT, "Ctrl");

		keySymbolMap.put(KeyEvent.KEYCODE_PAGE_UP, "⇞");
		keySymbolMap.put(KeyEvent.KEYCODE_PAGE_DOWN, "⇟");
		keySymbolMap.put(KeyEvent.KEYCODE_HOME, "⇱");
		keySymbolMap.put(KeyEvent.KEYCODE_MOVE_END, "⇲");
		keySymbolMap.put(KeyEvent.KEYCODE_INSERT, "Ins");

		keySymbolMap.put(KeyEvent.KEYCODE_F1, "F1");
		keySymbolMap.put(KeyEvent.KEYCODE_F2, "F2");
		keySymbolMap.put(KeyEvent.KEYCODE_F3, "F3");
		keySymbolMap.put(KeyEvent.KEYCODE_F4, "F4");
		keySymbolMap.put(KeyEvent.KEYCODE_F5, "F5");
		keySymbolMap.put(KeyEvent.KEYCODE_F6, "F6");
		keySymbolMap.put(KeyEvent.KEYCODE_F7, "F7");
		keySymbolMap.put(KeyEvent.KEYCODE_F8, "F8");
		keySymbolMap.put(KeyEvent.KEYCODE_F9, "F9");
		keySymbolMap.put(KeyEvent.KEYCODE_F10, "F10");
		keySymbolMap.put(KeyEvent.KEYCODE_F11, "F11");
		keySymbolMap.put(KeyEvent.KEYCODE_F12, "F12");

		keySymbolMap.put(KeyEvent.KEYCODE_BREAK, "Pause");
		keySymbolMap.put(KeyEvent.KEYCODE_SCROLL_LOCK, "Scroll");
		keySymbolMap.put(KeyEvent.KEYCODE_SYSRQ, "SysRq");
		keySymbolMap.put(KeyEvent.KEYCODE_MEDIA_PLAY, "Play");
		keySymbolMap.put(KeyEvent.KEYCODE_MEDIA_PAUSE, "Pause");
		keySymbolMap.put(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, "Play/Pause");
		keySymbolMap.put(KeyEvent.KEYCODE_MEDIA_STOP, "Stop");
		keySymbolMap.put(KeyEvent.KEYCODE_MEDIA_PREVIOUS, "Previous");
		keySymbolMap.put(KeyEvent.KEYCODE_MEDIA_NEXT, "Next");
		keySymbolMap.put(KeyEvent.KEYCODE_VOLUME_UP, "Vol Up");
		keySymbolMap.put(KeyEvent.KEYCODE_VOLUME_DOWN, "Vol Down");
		keySymbolMap.put(KeyEvent.KEYCODE_VOLUME_MUTE, "Mute");
		keySymbolMap.put(KeyEvent.KEYCODE_CAMERA, "Camera");
		keySymbolMap.put(KeyEvent.KEYCODE_POWER, "Power");
		keySymbolMap.put(KeyEvent.KEYCODE_NOTIFICATION, "Notification");
		keySymbolMap.put(KeyEvent.KEYCODE_SOFT_SLEEP, "Sleep");
		keySymbolMap.put(KeyEvent.KEYCODE_WAKEUP, "Wakeup");
	}

	@NonNull
	public String getKeySymbol(int keyCode) {
		String symbol = keySymbolMap.get(keyCode);
		return symbol != null ? symbol : String.valueOf((char) keyCode);
	}

	public static KeySymbolMapper load() {
		return new KeySymbolMapper();
	}
}
