package com.stirante.RuneChanger.util;

import java.io.*;
import java.util.HashMap;

public class SimplePreferences {

    private static HashMap<String, Object> values;

    public static void load() {
        File prefs = new File("RuneChanger.dat");
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(prefs))) {
            values = (HashMap<String, Object>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            if (!(e instanceof FileNotFoundException))
                e.printStackTrace();
        }
        if (values == null)
            values = new HashMap<>();
    }

    public static Object getValue(String key) {
        return values.get(key);
    }

    public static boolean containsKey(String key) {
        return values.containsKey(key);
    }

    public static void putValue(String key, Object value) {
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
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(prefs))) {
            oos.writeObject(values);
            oos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
