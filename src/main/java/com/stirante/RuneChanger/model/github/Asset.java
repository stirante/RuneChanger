package com.stirante.RuneChanger.model.github;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Asset {

    @SerializedName("url")
    @Expose
    public String url;
    @SerializedName("id")
    @Expose
    public Integer id;
    @SerializedName("node_id")
    @Expose
    public String nodeId;
    @SerializedName("name")
    @Expose
    public String name;
    @SerializedName("label")
    @Expose
    public Object label;
    @SerializedName("uploader")
    @Expose
    public Uploader uploader;
    @SerializedName("content_type")
    @Expose
    public String contentType;
    @SerializedName("state")
    @Expose
    public String state;
    @SerializedName("size")
    @Expose
    public Integer size;
    @SerializedName("download_count")
    @Expose
    public Integer downloadCount;
    @SerializedName("created_at")
    @Expose
    public String createdAt;
    @SerializedName("updated_at")
    @Expose
    public String updatedAt;
    @SerializedName("browser_download_url")
    @Expose
    public String browserDownloadUrl;

}
