package com.stirante.RuneChanger.util;

import java.io.*;
import java.util.HashMap;

public class SimplePreferences {

    private static HashMap<String, String> settingsValues;
	public static HashMap<String, String> runeBookValues;

    public static void load() {
        File settingsValuesFile = new File("RuneChangerSettings.dat");
		File runeBookValuesFile = new File("RuneChangerRuneBook.dat");
        try (DataInputStream ois = new DataInputStream(new FileInputStream(settingsValuesFile))) {
            settingsValues = new HashMap<>();
            int size = ois.readInt();
            for (int i = 0; i < size; i++) {
                String key = ois.readUTF();
                String value = ois.readUTF();
                settingsValues.put(key, value);
            }
        } catch (IOException e) {
            if (!(e instanceof FileNotFoundException)) {
                e.printStackTrace();
            }
        }
        if (settingsValues == null) {
            settingsValues = new HashMap<>();
        }

		try (DataInputStream ois = new DataInputStream(new FileInputStream(runeBookValuesFile))) {
			runeBookValues = new HashMap<>();
			int size = ois.readInt();
			for (int i = 0; i < size; i++) {
				String key = ois.readUTF();
				String value = ois.readUTF();
				runeBookValues.put(key, value);
			}
		} catch (IOException e) {
			if (!(e instanceof FileNotFoundException)) {
				e.printStackTrace();
			}
		}
		if (runeBookValues == null) {
			runeBookValues = new HashMap<>();
		}
    }

    public static String getValue(String key) {
        return settingsValues.get(key);
    }

	public static String getRuneBookValue(String key) {
		return runeBookValues.get(key);
	}

    public static boolean containsKey(String key) {
        return settingsValues.containsKey(key);
    }

    public static void putValue(String key, String value) {
        settingsValues.put(key, value);
        save();
    }

	public static void putRuneBookValue(String key, String value) {
		runeBookValues.put(key, value);
		save();
	}

    public static void removeValue(String key) {
        settingsValues.remove(key);
        save();
    }

	public static void removeRuneBookValue(String key) {
		runeBookValues.remove(key);
		save();
	}

    public static void save() {
        File prefs = new File("RuneChangerSettings.dat");
        if (!prefs.exists()) {
            try {
                prefs.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try (DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(prefs))) {
            dataOutputStream.writeInt(settingsValues.size());
            for (String key : settingsValues.keySet()) {
                dataOutputStream.writeUTF(key);
                dataOutputStream.writeUTF(settingsValues.get(key));
            }
            dataOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }


		File runeBookFile = new File("RuneChangerRuneBook.dat");
		if (!runeBookFile.exists()) {
			try {
				runeBookFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try (DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(runeBookFile))) {
			dataOutputStream.writeInt(runeBookValues.size());
			for (String key : runeBookValues.keySet()) {
				dataOutputStream.writeUTF(key);
				dataOutputStream.writeUTF(runeBookValues.get(key));
			}
			dataOutputStream.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}


}
