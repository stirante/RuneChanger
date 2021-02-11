package com.stirante.runechanger.util;

import com.stirante.runechanger.RuneChanger;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class LolHelper {
    private static final Logger log = LoggerFactory.getLogger(LolHelper.class);

    private static WinUser.HHOOK hhk;
    private static WinUser.LowLevelKeyboardProc keyboardHook;

    private static long lastClick = 0;
    private static int counter = 0;

    public static void init() {
        RuneChanger.EXECUTOR_SERVICE.submit(() -> {
            final User32 lib = User32.INSTANCE;
            WinDef.HMODULE hMod = Kernel32.INSTANCE.GetModuleHandle(null);
            keyboardHook = (nCode, wParam, info) -> {
                if (nCode >= 0) {
                    boolean isAlt = (info.flags & 32) != 0;
                    boolean isReleased = (info.flags & 128) != 0;
                    boolean consumed = false;
                    if (isReleased && isAlt && (info.vkCode == 111 || info.vkCode == 110)) {
                        consumed = true;
                        if (System.currentTimeMillis() - lastClick > 500) {
                            lastClick = System.currentTimeMillis();
                            counter = 1;
                        }
                        else {
                            lastClick = System.currentTimeMillis();
                            counter++;
                        }
                        if (counter >= 3) {
                            counter = 0;
                            if (info.vkCode == 111) {
                                killGame();
                            }
                            else if (info.vkCode == 110) {
                                killClient();
                            }
                        }
                    }

                    if (consumed) {
                        return new WinDef.LRESULT(1);
                    }
                }
                Pointer ptr = info.getPointer();
                long peer = Pointer.nativeValue(ptr);
                return lib.CallNextHookEx(hhk, nCode, wParam, new WinDef.LPARAM(peer));
            };
            hhk = lib.SetWindowsHookEx(WinUser.WH_KEYBOARD_LL, keyboardHook, hMod, 0);

            // This bit never returns from GetMessage
            int result;
            WinUser.MSG msg = new WinUser.MSG();
            while ((result = lib.GetMessage(msg, null, 0, 0)) != 0) {
                if (result == -1) {
                    log.warn("Error in GetMessage");
                    break;
                }
                else {
                    log.info("GetMessage actually returned something");
                    lib.TranslateMessage(msg);
                    lib.DispatchMessage(msg);
                }
            }
            lib.UnhookWindowsHookEx(hhk);
        });
    }

    public static void stop() {
        if (User32.INSTANCE.UnhookWindowsHookEx(hhk)) {
            log.info("Unhooked keyboard");
        }
    }

    public static void killClient() {
        log.info("Killing the client");
        try {
            Runtime.getRuntime().exec("taskkill /F /IM LeagueClient.exe /T");
            Runtime.getRuntime().exec("taskkill /F /IM LeagueClientUx.exe /T");
            Runtime.getRuntime().exec("taskkill /F /IM RiotClientUx.exe /T");
            Runtime.getRuntime().exec("taskkill /F /IM RiotClientServices.exe /T");
            String path = SimplePreferences.getStringValue(SimplePreferences.InternalKeys.CLIENT_PATH, null);
            if (path != null) {
                new File(path, "lockfile").delete();
            }
        } catch (IOException e) {
            log.warn("Execption occurred while killing League of Legends", e);
        }
    }

    public static void killGame() {
        log.info("Killing the game");
        try {
            Runtime.getRuntime().exec("taskkill /F /IM \"League of Legends.exe\" /T");
        } catch (IOException e) {
            log.warn("Execption occurred while killing League of Legends", e);
        }
    }

}
