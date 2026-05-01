package com.sijareca.scan4java.internal.twain;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.win32.W32APIOptions;

interface Kernel32Heap extends Library {

    Kernel32Heap INSTANCE = Native.load(
        "kernel32", Kernel32Heap.class, W32APIOptions.DEFAULT_OPTIONS);

    Pointer GlobalLock(Pointer hMem);
    boolean GlobalUnlock(Pointer hMem);
    Pointer GlobalFree(Pointer hMem);
}