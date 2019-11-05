package com.stirante.runechanger.client;

import com.stirante.runechanger.gui.GuiHandler;
import com.stirante.runechanger.util.NativeUtils;
import com.sun.jna.platform.win32.GDI32Util;
import com.sun.jna.platform.win32.WinDef;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ScreenPointChecker {

    public static final ScreenPoint CHAMPION_SELECTION_RUNE_PAGE_EDIT = new ScreenPoint(0.1234375f, 0.1736111f, 0xffcdbe91, 0xfff0e6d2);

    public static boolean testScreenPoint(WinDef.HWND window, ScreenPoint point) {
        BufferedImage screenshot = GDI32Util.getScreenshot(window);
        int rgb = screenshot.getRGB((int) (point.getX() * screenshot.getWidth()), (int) (point.getY() * screenshot.getHeight()));
        for (Integer color : point.getColors()) {
            if (areColorsSimilar(rgb, color)) return true;
        }
        return false;
    }

    private static boolean areColorsSimilar(int c1, int c2) {
        //We tread r, g, b as x, y, z for point in 3D space and calculate squared distance between those points
        int r1 = (c1 >> 16) & 0xff;
        int g1 = (c1 >> 8) & 0xff;
        int b1 = c1 & 0xff;
        int r2 = (c2 >> 16) & 0xff;
        int g2 = (c2 >> 8) & 0xff;
        int b2 = c2 & 0xff;
        double distance = Math.pow(r1 - r2, 2) + Math.pow(g1 - g2, 2) + Math.pow(b1 - b2, 2);
        return distance < 400;
    }

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
    }


    public static class ScreenPoint {
        private List<Integer> colors = new ArrayList<>();
        private float x;
        private float y;

        public ScreenPoint(float x, float y, int... colors) {
            for (int color : colors) {
                this.colors.add(color);
            }
            this.x = x;
            this.y = y;
        }

        public List<Integer> getColors() {
            return colors;
        }

        public float getX() {
            return x;
        }

        public float getY() {
            return y;
        }
    }
}
