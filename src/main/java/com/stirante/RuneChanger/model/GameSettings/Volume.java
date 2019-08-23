
package com.stirante.RuneChanger.model.GameSettings;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Volume {

    @SerializedName("AmbienceMute")
    @Expose
    public Boolean ambienceMute;
    @SerializedName("AmbienceVolume")
    @Expose
    public Long ambienceVolume;
    @SerializedName("AnnouncerMute")
    @Expose
    public Boolean announcerMute;
    @SerializedName("AnnouncerVolume")
    @Expose
    public Long announcerVolume;
    @SerializedName("MasterMute")
    @Expose
    public Boolean masterMute;
    @SerializedName("MasterVolume")
    @Expose
    public Double masterVolume;
    @SerializedName("MusicMute")
    @Expose
    public Boolean musicMute;
    @SerializedName("MusicVolume")
    @Expose
    public Double musicVolume;
    @SerializedName("PingsMute")
    @Expose
    public Boolean pingsMute;
    @SerializedName("PingsVolume")
    @Expose
    public Double pingsVolume;
    @SerializedName("SfxMute")
    @Expose
    public Boolean sfxMute;
    @SerializedName("SfxVolume")
    @Expose
    public Double sfxVolume;
    @SerializedName("VoiceMute")
    @Expose
    public Boolean voiceMute;
    @SerializedName("VoiceVolume")
    @Expose
    public Double voiceVolume;

}
