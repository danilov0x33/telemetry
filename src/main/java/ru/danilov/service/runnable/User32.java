package ru.danilov.service.runnable;

import com.sun.jna.*;
import com.sun.jna.win32.W32APIOptions;

import java.util.Arrays;
import java.util.List;

public class User32 {
    static {
        Native.register(NativeLibrary.getInstance("user32", W32APIOptions.DEFAULT_OPTIONS));
    }

    public static final int MOD_ALT = 0x0001;
    public static final int MOD_CONTROL = 0x0002;
    public static final int MOD_NOREPEAT = 0x4000;
    public static final int MOD_SHIFT = 0x0004;
    public static final int MOD_WIN = 0x0008;
    public static final int WM_HOTKEY = 0x0312;
    public static final int VK_MEDIA_NEXT_TRACK = 0xB0;
    public static final int VK_MEDIA_PREV_TRACK = 0xB1;
    public static final int VK_MEDIA_STOP = 0xB2;
    public static final int VK_MEDIA_PLAY_PAUSE = 0xB3;
    public static final int PM_REMOVE = 0x0001;

    /**Регистрация клавиш.*/
    public static native boolean RegisterHotKey(Pointer hWnd, int id, int fsModifiers, int vk);
    /**Убрать из регистрации клавишу.*/
    public static native boolean UnregisterHotKey(Pointer hWnd, int id);
    /**Получить системное сообщение.*/
    public static native boolean GetMessage(MSG lpMsg, Pointer hWnd, int wMsgFilterMin, int wMsgFilterMax, int wRemoveMsg);
    /**Получить положение клавиши.*/
    public static native byte GetKeyState(int key);
    /**Пролучить евент клавиатуры.*/
    public static native void keybd_event(int i, int j,int dwFlags,long dwExtraInfo);
    /**Получить раскладку.*/
    public static native byte GetKeyboardLayout(int idT);
    /**Получить активное окно.*/
    public static native Pointer GetForegroundWindow();
    /**Получить PID окна.*/
    public static native int GetWindowThreadProcessId(Pointer hWnd, int id);
    
    /**Структура сообщения.*/
    public static class MSG extends Structure {
        public Pointer hWnd;
        public int message;
        public Parameter wParam;
        public Parameter lParam;
        public int time;
        public int x;
        public int y;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("hWnd", "message", "wParam", "lParam", "time", "x", "y");
        }
    }

    public static class Parameter extends IntegerType {
		private static final long serialVersionUID = 1927890759835439976L;

		public Parameter() {
            this(0);
        }

        public Parameter(long value) {
            super(Pointer.SIZE, value);
        }
    }
}
