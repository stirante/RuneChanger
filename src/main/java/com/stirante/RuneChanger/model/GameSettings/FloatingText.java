
package com.stirante.RuneChanger.model.GameSettings;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class FloatingText {

    @SerializedName("Dodge_Enabled")
    @Expose
    public Boolean dodgeEnabled;
    @SerializedName("EnemyPhysicalDamage_Enabled")
    @Expose
    public Boolean enemyPhysicalDamageEnabled;
    @SerializedName("Experience_Enabled")
    @Expose
    public Boolean experienceEnabled;
    @SerializedName("Gold_Enabled")
    @Expose
    public Boolean goldEnabled;
    @SerializedName("Heal_Enabled")
    @Expose
    public Boolean healEnabled;
    @SerializedName("Invulnerable_Enabled")
    @Expose
    public Boolean invulnerableEnabled;
    @SerializedName("Level_Enabled")
    @Expose
    public Boolean levelEnabled;
    @SerializedName("ManaDamage_Enabled")
    @Expose
    public Boolean manaDamageEnabled;
    @SerializedName("PhysicalDamage_Enabled")
    @Expose
    public Boolean physicalDamageEnabled;
    @SerializedName("QuestReceived_Enabled")
    @Expose
    public Boolean questReceivedEnabled;
    @SerializedName("Score_Enabled")
    @Expose
    public Boolean scoreEnabled;
    @SerializedName("Special_Enabled")
    @Expose
    public Boolean specialEnabled;

}
