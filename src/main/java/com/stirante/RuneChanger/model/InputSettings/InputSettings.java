
package com.stirante.RuneChanger.model.InputSettings;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class InputSettings {

    @SerializedName("GameEvents")
    @Expose
    public GameEvents gameEvents;
    @SerializedName("HUDEvents")
    @Expose
    public HUDEvents hUDEvents;
    @SerializedName("Quickbinds")
    @Expose
    public Quickbinds quickbinds;
    @SerializedName("ShopEvents")
    @Expose
    public ShopEvents shopEvents;

}
