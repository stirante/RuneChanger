
package com.stirante.RuneChanger.model.GameSettings;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class General {

    @SerializedName("AutoAcquireTarget")
    @Expose
    public Boolean autoAcquireTarget;
    @SerializedName("BindSysKeys")
    @Expose
    public Boolean bindSysKeys;
    @SerializedName("CursorOverride")
    @Expose
    public Boolean cursorOverride;
    @SerializedName("CursorScale")
    @Expose
    public Double cursorScale;
    @SerializedName("EnableAudio")
    @Expose
    public Boolean enableAudio;
    @SerializedName("EnableTargetedAttackMove")
    @Expose
    public Boolean enableTargetedAttackMove;
    @SerializedName("GameMouseSpeed")
    @Expose
    public Long gameMouseSpeed;
    @SerializedName("HideEyeCandy")
    @Expose
    public Boolean hideEyeCandy;
    @SerializedName("OSXMouseAcceleration")
    @Expose
    public Boolean oSXMouseAcceleration;
    @SerializedName("PredictMovement")
    @Expose
    public Boolean predictMovement;
    @SerializedName("RelativeTeamColors")
    @Expose
    public Boolean relativeTeamColors;
    @SerializedName("ShowCursorLocator")
    @Expose
    public Boolean showCursorLocator;
    @SerializedName("ShowGodray")
    @Expose
    public Boolean showGodray;
    @SerializedName("ShowTurretRangeIndicators")
    @Expose
    public Boolean showTurretRangeIndicators;
    @SerializedName("SnapCameraOnRespawn")
    @Expose
    public Boolean snapCameraOnRespawn;
    @SerializedName("ThemeMusic")
    @Expose
    public Long themeMusic;
    @SerializedName("WaitForVerticalSync")
    @Expose
    public Boolean waitForVerticalSync;
    @SerializedName("WindowMode")
    @Expose
    public Long windowMode;

}
