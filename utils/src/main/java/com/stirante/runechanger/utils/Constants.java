package com.stirante.runechanger.utils;

import javafx.scene.text.Font;

public class Constants {
    //app name
    public static final String APP_NAME = "RuneChanger";

    //percentage positions and lengths
    public static final float RUNE_BUTTON_POSITION_X = 0.3046875f;
    public static final float RUNE_BUTTON_POSITION_Y = 0.9305556f ;
    public static final float RUNE_BUTTON_SIZE = 0.039f;
    public static final float RUNE_MENU_X = 0.36640626f;
    public static final float RUNE_MENU_Y = 0.9236111f;
    public static final float RUNE_ITEM_HEIGHT = 0.050000012f;
    public static final float RUNE_ITEM_WIDTH = 0.15000004f;
    public static final float QUICK_CHAT_X = 0.0171875f;
    public static final float QUICK_CHAT_Y = 0.9736111f;
    public static final float FONT_SIZE = 0.0178333f;
    public static final float CHAMPION_TILE_SIZE = 0.097222224f;
    public static final float CHAMPION_SUGGESTION_WIDTH = 0.1388889f;
    public static final float WARNING_X = 0.4984375f;
    public static final float WARNING_Y = 0.10972222f;

    //fonts
    public static final Font BUTTON_FONT = LangHelper.getLocale().getLanguage().equalsIgnoreCase("ar") ?
            Font.loadFont(Constants.class.getResource("/fonts/Cairo-Regular.ttf").toExternalForm(), 14) :
            Font.loadFont(Constants.class.getResource("/fonts/Roboto-Regular.ttf").toExternalForm(), 12);

    //font spacing
    public static final double BUTTON_FONT_SPACING = 0.5d;
}
