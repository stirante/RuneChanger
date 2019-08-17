
package com.stirante.RuneChanger.model.InputSettings;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class HUDEvents {

    @SerializedName("evtHoldShowScoreBoard")
    @Expose
    public String evtHoldShowScoreBoard;
    @SerializedName("evtToggleFPSAndLatency")
    @Expose
    public String evtToggleFPSAndLatency;
    @SerializedName("evtToggleMouseClip")
    @Expose
    public String evtToggleMouseClip;
    @SerializedName("evtTogglePlayerStats")
    @Expose
    public String evtTogglePlayerStats;

}
