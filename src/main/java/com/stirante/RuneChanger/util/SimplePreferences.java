package com.stirante.RuneChanger.util;

import java.io.*;
import java.util.HashMap;

public class SimplePreferences {

    private static HashMap<String, String> values;

    public static void load() {
        File prefs = new File("RuneChanger.dat");
        try (DataInputStream ois = new DataInputStream(new FileInputStream(prefs))) {
            values = new HashMap<>();
            int size = ois.readInt();
            for (int i = 0; i < size; i++) {
                String key = ois.readUTF();
                String value = ois.readUTF();
                values.put(key, value);
            }
        } catch (IOException e) {
            if (!(e instanceof FileNotFoundException)) {
                e.printStackTrace();
            }
        }
        if (values == null) {
            values = new HashMap<>();
        }
    }

    public static String getValue(String key) {
        return values.get(key);
    }

    public static boolean containsKey(String key) {
        return values.containsKey(key);
    }

    public static void putValue(String key, String value) {
        values.put(key, value);
        save();
    }

    public static void removeValue(String key) {
        values.remove(key);
        save();
    }

    public static void save() {
        File prefs = new File("RuneChanger.dat");
        if (!prefs.exists()) {
            try {
                prefs.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try (DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(prefs))) {
            dataOutputStream.writeInt(values.size());
            for (String key : values.keySet()) {
                dataOutputStream.writeUTF(key);
                dataOutputStream.writeUTF(values.get(key));
            }
            dataOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
