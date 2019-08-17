
package com.stirante.RuneChanger.model.InputSettings;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ShopEvents {

    @SerializedName("evtShopFocusSearch")
    @Expose
    public String evtShopFocusSearch;
    @SerializedName("evtShopSwitchTabs")
    @Expose
    public String evtShopSwitchTabs;

}
