package com.stirante.RuneChanger.util;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;

public class NativeUtils {
    private static final String LEAGUE_CLIENT_UX_EXECUTABLE = "LeagueClientUx.exe";
    private static final String LEAGUE_CLIENT_UX_TITLE = "League of Legends";
    /**
     * Returns process image name. From https://stackoverflow.com/a/50890393/6459649
     */
    public static String getImageName(WinDef.HWND window) {
        // Get the process ID of the window
        IntByReference procId = new IntByReference();
        User32.INSTANCE.GetWindowThreadProcessId(window, procId);

        // Open the process to get permissions to the image name
        WinNT.HANDLE procHandle = Kernel32.INSTANCE.OpenProcess(
                Kernel32.PROCESS_QUERY_LIMITED_INFORMATION,
                false,
                procId.getValue()
        );

        // Get the image name
        char[] buffer = new char[4096];
        IntByReference bufferSize = new IntByReference(buffer.length);
        boolean success = Kernel32.INSTANCE.QueryFullProcessImageName(procHandle, 0, buffer, bufferSize);

        // Clean up: close the opened process
        Kernel32.INSTANCE.CloseHandle(procHandle);

        return success ? new String(buffer, 0, bufferSize.getValue()) : "";
    }

    public static boolean isLeagueOfLegendsClientWindow(WinDef.HWND window) {
        String exePath = NativeUtils.getImageName(window);
        char[] windowText = new char[512];
        User32.INSTANCE.GetWindowText(window, windowText, 512);
        String windowTitle = Native.toString(windowText);
        return windowTitle.equalsIgnoreCase(LEAGUE_CLIENT_UX_TITLE) && exePath.endsWith(LEAGUE_CLIENT_UX_EXECUTABLE);
    }
}
