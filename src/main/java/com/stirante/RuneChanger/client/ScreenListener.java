package com.stirante.RuneChanger.client;

import com.stirante.RuneChanger.gui.GuiHandler;
import com.stirante.RuneChanger.util.NativeUtils;
import com.sun.jna.platform.win32.GDI32Util;
import com.sun.jna.platform.win32.WinDef;

import java.awt.image.BufferedImage;
import java.io.IOException;

public class ScreenListener {

    public static void main(String[] args) throws IOException, InterruptedException {
        Thread.sleep(1000);
        WinDef.HWND hwnd = GuiHandler.User32Extended.INSTANCE.GetForegroundWindow();
        if (!NativeUtils.isLeagueOfLegendsClientWindow(hwnd)) {
            System.out.println("NOT LOL");
            return;
        }
        while (true) {
            BufferedImage screenshot = GDI32Util.getScreenshot(hwnd);
            float x = 0.1234375f;
            float y = 0.1736111f;
            int rgb = screenshot.getRGB((int) (x * screenshot.getWidth()), (int) (y * screenshot.getHeight()));
            System.out.println(Integer.toHexString(rgb));
            if (areColorsSimilar(rgb, 0xffcdbe91) || areColorsSimilar(rgb, 0xfff0e6d2)) {
                System.out.println("YES");
            }
            else {
                System.out.println("NO");
            }
            Thread.sleep(1000);
        }
        //ffc1b489
        //ffcdbe91
    }

    private static boolean areColorsSimilar(int c1, int c2) {
        int r1 = (c1 >> 16) & 0xff;
        int g1 = (c1 >> 8) & 0xff;
        int b1 = c1 & 0xff;
        int r2 = (c2 >> 16) & 0xff;
        int g2 = (c2 >> 8) & 0xff;
        int b2 = c2 & 0xff;
        double distance = Math.pow(r1 - r2, 2) + Math.pow(g1 - g2, 2) + Math.pow(b1 - b2, 2);
        return distance < 400;
    }

}
