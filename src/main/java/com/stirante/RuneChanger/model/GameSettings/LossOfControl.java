
package com.stirante.RuneChanger.model.GameSettings;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class LossOfControl {

    @SerializedName("LossOfControlEnabled")
    @Expose
    public Boolean lossOfControlEnabled;
    @SerializedName("ShowSlows")
    @Expose
    public Boolean showSlows;

}
