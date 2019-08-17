
package com.stirante.RuneChanger.model.GameSettings;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Voice {

    @SerializedName("ShowVoiceChatHalos")
    @Expose
    public Boolean showVoiceChatHalos;
    @SerializedName("ShowVoicePanelWithScoreboard")
    @Expose
    public Boolean showVoicePanelWithScoreboard;

}
