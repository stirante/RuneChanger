
package com.stirante.RuneChanger.model.GameSettings;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class HUD {

    @SerializedName("AutoDisplayTarget")
    @Expose
    public Boolean autoDisplayTarget;
    @SerializedName("CameraLockMode")
    @Expose
    public Long cameraLockMode;
    @SerializedName("ChatScale")
    @Expose
    public Long chatScale;
    @SerializedName("DisableHudSpellClick")
    @Expose
    public Boolean disableHudSpellClick;
    @SerializedName("DrawHealthBars")
    @Expose
    public Boolean drawHealthBars;
    @SerializedName("EnableLineMissileVis")
    @Expose
    public Boolean enableLineMissileVis;
    @SerializedName("FlashScreenWhenDamaged")
    @Expose
    public Boolean flashScreenWhenDamaged;
    @SerializedName("FlashScreenWhenStunned")
    @Expose
    public Boolean flashScreenWhenStunned;
    @SerializedName("FlipMiniMap")
    @Expose
    public Boolean flipMiniMap;
    @SerializedName("GlobalScale")
    @Expose
    public Double globalScale;
    @SerializedName("KeyboardScrollSpeed")
    @Expose
    public Double keyboardScrollSpeed;
    @SerializedName("MapScrollSpeed")
    @Expose
    public Double mapScrollSpeed;
    @SerializedName("MiddleClickDragScrollEnabled")
    @Expose
    public Boolean middleClickDragScrollEnabled;
    @SerializedName("MinimapMoveSelf")
    @Expose
    public Boolean minimapMoveSelf;
    @SerializedName("MinimapScale")
    @Expose
    public Long minimapScale;
    @SerializedName("MirroredScoreboard")
    @Expose
    public Boolean mirroredScoreboard;
    @SerializedName("NumericCooldownFormat")
    @Expose
    public Long numericCooldownFormat;
    @SerializedName("ObjectTooltips")
    @Expose
    public Boolean objectTooltips;
    @SerializedName("ScrollSmoothingEnabled")
    @Expose
    public Boolean scrollSmoothingEnabled;
    @SerializedName("ShowAllChannelChat")
    @Expose
    public Boolean showAllChannelChat;
    @SerializedName("ShowAlliedChat")
    @Expose
    public Boolean showAlliedChat;
    @SerializedName("ShowAttackRadius")
    @Expose
    public Boolean showAttackRadius;
    @SerializedName("ShowNeutralCamps")
    @Expose
    public Boolean showNeutralCamps;
    @SerializedName("ShowSpellCosts")
    @Expose
    public Boolean showSpellCosts;
    @SerializedName("ShowSummonerNames")
    @Expose
    public Boolean showSummonerNames;
    @SerializedName("ShowSummonerNamesInScoreboard")
    @Expose
    public Boolean showSummonerNamesInScoreboard;
    @SerializedName("ShowTeamFramesOnLeft")
    @Expose
    public Boolean showTeamFramesOnLeft;
    @SerializedName("ShowTimestamps")
    @Expose
    public Boolean showTimestamps;
    @SerializedName("SmartCastOnKeyRelease")
    @Expose
    public Boolean smartCastOnKeyRelease;
    @SerializedName("SmartCastWithIndicator_CastWhenNewSpellSelected")
    @Expose
    public Boolean smartCastWithIndicatorCastWhenNewSpellSelected;

}
