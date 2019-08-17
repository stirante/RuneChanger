
package com.stirante.RuneChanger.model.GameSettings;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class GameSettings {

    @SerializedName("FloatingText")
    @Expose
    public FloatingText floatingText;
    @SerializedName("General")
    @Expose
    public General general;
    @SerializedName("HUD")
    @Expose
    public HUD hUD;
    @SerializedName("LossOfControl")
    @Expose
    public LossOfControl lossOfControl;
    @SerializedName("Performance")
    @Expose
    public Performance performance;
    @SerializedName("Voice")
    @Expose
    public Voice voice;
    @SerializedName("Volume")
    @Expose
    public Volume volume;

}
