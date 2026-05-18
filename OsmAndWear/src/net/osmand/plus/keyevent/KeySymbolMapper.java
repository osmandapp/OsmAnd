package net.osmand.plus.keyevent;

import android.content.Context;
import android.os.Build;
import android.util.SparseArray;
import android.view.KeyEvent;

import androidx.annotation.NonNull;

import net.osmand.plus.R;

public class KeySymbolMapper {

	private static final SparseArray<String> keySymbolMap = new SparseArray<>();

	@NonNull
	public static String getKeySymbol(@NonNull Context context, int keyCode) {
		if (keySymbolMap.size() == 0) {
			load();
		}
		if (keyCode == KeyEvent.KEYCODE_UNKNOWN) {
			return context.getString(R.string.shared_string_none);
		}
		String symbol = keySymbolMap.get(keyCode);
		return symbol != null ? symbol : String.valueOf((char) keyCode);
	}

	public static void load() {
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
		keySymbolMap.put(KeyEvent.KEYCODE_DPAD_CENTER, "Center");

		keySymbolMap.put(KeyEvent.KEYCODE_SPACE, "␣");
		keySymbolMap.put(KeyEvent.KEYCODE_ENTER, "↵");
		keySymbolMap.put(KeyEvent.KEYCODE_TAB, "⇥");
		keySymbolMap.put(KeyEvent.KEYCODE_BACK, "Back");
		keySymbolMap.put(KeyEvent.KEYCODE_SHIFT_LEFT, "⇧");
		keySymbolMap.put(KeyEvent.KEYCODE_SHIFT_RIGHT, "⇧");
		keySymbolMap.put(KeyEvent.KEYCODE_CAPS_LOCK, "⇪");
		keySymbolMap.put(KeyEvent.KEYCODE_DEL, "⌫");
		keySymbolMap.put(KeyEvent.KEYCODE_ESCAPE, "Esc");
		keySymbolMap.put(KeyEvent.KEYCODE_MENU, "Menu");
		keySymbolMap.put(KeyEvent.KEYCODE_SEMICOLON, ";");
		keySymbolMap.put(KeyEvent.KEYCODE_APOSTROPHE, "'");
		keySymbolMap.put(KeyEvent.KEYCODE_SLASH, "/");
		keySymbolMap.put(KeyEvent.KEYCODE_GRAVE, "~");

		keySymbolMap.put(KeyEvent.KEYCODE_NUM_LOCK, "Num Lock");
		keySymbolMap.put(KeyEvent.KEYCODE_ALT_LEFT, "Alt");
		keySymbolMap.put(KeyEvent.KEYCODE_ALT_RIGHT, "Alt");
		keySymbolMap.put(KeyEvent.KEYCODE_CTRL_LEFT, "Ctrl");
		keySymbolMap.put(KeyEvent.KEYCODE_CTRL_RIGHT, "Ctrl");

		keySymbolMap.put(KeyEvent.KEYCODE_PAGE_UP, "Page Up ⇞");
		keySymbolMap.put(KeyEvent.KEYCODE_PAGE_DOWN, "Page Down ⇟");
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

		keySymbolMap.put(KeyEvent.KEYCODE_0, "0");
		keySymbolMap.put(KeyEvent.KEYCODE_1, "1");
		keySymbolMap.put(KeyEvent.KEYCODE_2, "2");
		keySymbolMap.put(KeyEvent.KEYCODE_3, "3");
		keySymbolMap.put(KeyEvent.KEYCODE_4, "4");
		keySymbolMap.put(KeyEvent.KEYCODE_5, "5");
		keySymbolMap.put(KeyEvent.KEYCODE_6, "6");
		keySymbolMap.put(KeyEvent.KEYCODE_7, "7");
		keySymbolMap.put(KeyEvent.KEYCODE_8, "8");
		keySymbolMap.put(KeyEvent.KEYCODE_9, "9");

		keySymbolMap.put(KeyEvent.KEYCODE_LEFT_BRACKET, "[");
		keySymbolMap.put(KeyEvent.KEYCODE_RIGHT_BRACKET, "]");

		keySymbolMap.put(KeyEvent.KEYCODE_COMMA, ",");
		keySymbolMap.put(KeyEvent.KEYCODE_PERIOD, ".");

		keySymbolMap.put(KeyEvent.KEYCODE_BREAK, "Pause");
		keySymbolMap.put(KeyEvent.KEYCODE_SCROLL_LOCK, "Scroll lock");
		keySymbolMap.put(KeyEvent.KEYCODE_MOVE_HOME, "Home");
		keySymbolMap.put(KeyEvent.KEYCODE_FORWARD_DEL, "Forward Delete");
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
		keySymbolMap.put(KeyEvent.KEYCODE_WAKEUP, "Wakeup");
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			keySymbolMap.put(KeyEvent.KEYCODE_SOFT_SLEEP, "Sleep");
		}

		// Add Numeric Keypad key codes
		keySymbolMap.put(KeyEvent.KEYCODE_NUMPAD_0, "Num 0");
		keySymbolMap.put(KeyEvent.KEYCODE_NUMPAD_1, "Num 1");
		keySymbolMap.put(KeyEvent.KEYCODE_NUMPAD_2, "Num 2");
		keySymbolMap.put(KeyEvent.KEYCODE_NUMPAD_3, "Num 3");
		keySymbolMap.put(KeyEvent.KEYCODE_NUMPAD_4, "Num 4");
		keySymbolMap.put(KeyEvent.KEYCODE_NUMPAD_5, "Num 5");
		keySymbolMap.put(KeyEvent.KEYCODE_NUMPAD_6, "Num 6");
		keySymbolMap.put(KeyEvent.KEYCODE_NUMPAD_7, "Num 7");
		keySymbolMap.put(KeyEvent.KEYCODE_NUMPAD_8, "Num 8");
		keySymbolMap.put(KeyEvent.KEYCODE_NUMPAD_9, "Num 9");
		keySymbolMap.put(KeyEvent.KEYCODE_NUMPAD_DIVIDE, "Num /");
		keySymbolMap.put(KeyEvent.KEYCODE_NUMPAD_MULTIPLY, "Num *");
		keySymbolMap.put(KeyEvent.KEYCODE_NUMPAD_SUBTRACT, "Num -");
		keySymbolMap.put(KeyEvent.KEYCODE_NUMPAD_ADD, "Num +");
		keySymbolMap.put(KeyEvent.KEYCODE_NUMPAD_DOT, "Num .");
		keySymbolMap.put(KeyEvent.KEYCODE_NUMPAD_COMMA, "Num ,");
		keySymbolMap.put(KeyEvent.KEYCODE_NUMPAD_ENTER, "Num Enter");
		keySymbolMap.put(KeyEvent.KEYCODE_NUMPAD_EQUALS, "Num =");
		keySymbolMap.put(KeyEvent.KEYCODE_NUMPAD_LEFT_PAREN, "Num (");
		keySymbolMap.put(KeyEvent.KEYCODE_NUMPAD_RIGHT_PAREN, "Num )");
	}
}
