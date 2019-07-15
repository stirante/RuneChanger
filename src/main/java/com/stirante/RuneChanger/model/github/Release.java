package com.stirante.RuneChanger.model.github;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.util.List;

public class Release {

    @SerializedName("url")
    @Expose
    public String url;
    @SerializedName("assets_url")
    @Expose
    public String assetsUrl;
    @SerializedName("upload_url")
    @Expose
    public String uploadUrl;
    @SerializedName("html_url")
    @Expose
    public String htmlUrl;
    @SerializedName("id")
    @Expose
    public Integer id;
    @SerializedName("node_id")
    @Expose
    public String nodeId;
    @SerializedName("tag_name")
    @Expose
    public String tagName;
    @SerializedName("target_commitish")
    @Expose
    public String targetCommitish;
    @SerializedName("name")
    @Expose
    public String name;
    @SerializedName("draft")
    @Expose
    public Boolean draft;
    @SerializedName("author")
    @Expose
    public Author author;
    @SerializedName("prerelease")
    @Expose
    public Boolean prerelease;
    @SerializedName("created_at")
    @Expose
    public Date createdAt;
    @SerializedName("published_at")
    @Expose
    public Date publishedAt;
    @SerializedName("assets")
    @Expose
    public List<Asset> assets = null;
    @SerializedName("tarball_url")
    @Expose
    public String tarballUrl;
    @SerializedName("zipball_url")
    @Expose
    public String zipballUrl;
    @SerializedName("body")
    @Expose
    public String body;

}
