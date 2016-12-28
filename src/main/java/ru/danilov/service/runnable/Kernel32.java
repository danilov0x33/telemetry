package ru.danilov.service.runnable;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.ptr.IntByReference;

public interface Kernel32 extends Library {

	Kernel32 INSTANCE = (Kernel32) Native.loadLibrary("kernel32", Kernel32.class);

	boolean GetComputerNameA(byte[] name, IntByReference intRef);

}