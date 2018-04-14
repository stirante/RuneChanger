package com.stirante.RuneChanger.gui;

import com.stirante.RuneChanger.util.LangHelper;

public enum ButtonType {
    FOUND(LangHelper.getLang().getString("button.available")),
    NOT_FOUND(LangHelper.getLang().getString("button.notFound")),
    SEARCHING(LangHelper.getLang().getString("button.searching")),
    SOURCE_ERROR(LangHelper.getLang().getString("button.error"));

    private final String message;

    ButtonType(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    /**
     * Returns button type for specified flags
     *
     * @param loading     loading flag
     * @param notFound    notFound flag
     * @param sourceError sourceError flag
     * @return button type
     */
    public static ButtonType getForParams(boolean loading, boolean notFound, boolean sourceError) {
        if (sourceError) return SOURCE_ERROR;
        else if (loading) return SEARCHING;
        else if (notFound) return NOT_FOUND;
        else return FOUND;
    }
}
