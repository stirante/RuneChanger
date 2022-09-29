package com.stirante.runechanger.gui.overlay;

import com.stirante.eventbus.EventBus;
import com.stirante.eventbus.EventPriority;
import com.stirante.eventbus.Subscribe;
import com.stirante.runechanger.RuneChanger;
import com.stirante.runechanger.api.Champion;
import com.stirante.runechanger.api.RuneChangerApi;
import com.stirante.runechanger.api.overlay.OverlayLayer;
import com.stirante.runechanger.client.ChampionSelection;
import com.stirante.runechanger.client.ChampionsImpl;
import com.stirante.runechanger.utils.Constants;
import com.stirante.runechanger.utils.SceneType;
import com.stirante.runechanger.sourcestore.TeamCompAnalyzer;
import com.stirante.runechanger.utils.SimplePreferences;
import com.stirante.runechanger.utils.UiEventExecutor;
import com.stirante.runechanger.utils.AsyncTask;
import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.font.LineMetrics;
import java.awt.geom.AffineTransform;
import java.util.List;
import java.util.stream.Collectors;

public class ChampionSuggestions extends OverlayLayer {
    private static final Logger log = LoggerFactory.getLogger(ChampionSuggestions.class);

    private int selectedChampionIndex = -1;
    private List<Champion> lastChampions;
    private List<Champion> bannedChampions;
    private float currentChampionsPosition = 0f;
    private List<TeamCompAnalyzer.TeamCompChampion> suggestions;

    ChampionSuggestions(ClientOverlayImpl overlay) {
        super(overlay);
        EventBus.register(this);
    }

    @Subscribe(value = ChampionSelection.CHAMPION_SELECT_SESSION_EVENT, priority = EventPriority.LOWEST)
    public void onSession() {
        ChampionSelection champSelect = RuneChanger.getInstance().getChampionSelectionModule();
        if (champSelect != null) {
            AsyncTask.EXECUTOR_SERVICE.execute(() -> {
                this.lastChampions = champSelect.getLastChampions();
                this.bannedChampions = champSelect.getBannedChampions();
                repaintLater();
            });
        }
        else {
            repaintLater();
        }
    }

    @Subscribe(value = TeamCompAnalyzer.TeamCompAnalysisEvent.NAME)
    public void onTeamCompAnalysis(TeamCompAnalyzer.TeamCompAnalysisEvent event) {
        if (event.teamComp != null) {
            suggestions = event.teamComp.suggestions;
        } else {
            suggestions = null;
        }
        repaintLater();
    }

    @Override
    protected void draw(Graphics g) {
        if (SimplePreferences.getBooleanValue(SimplePreferences.SettingsKeys.CHAMPION_SUGGESTIONS, true)) {
            if (getApi().getChampions().areImagesReady()) {
                boolean isSmart =
                        SimplePreferences.getBooleanValue(SimplePreferences.SettingsKeys.SMART_CHAMPION_SUGGESTIONS, true) && suggestions != null;
                if (lastChampions == null && !isSmart) {
                    return;
                }
                if (((RuneChanger) getApi()).getChampionSelectionModule().getGameMode() == null ||
                        !((RuneChanger) getApi()).getChampionSelectionModule().getGameMode().hasChampionSelection()) {
                    return;
                }
                if (getSceneType() != SceneType.CHAMPION_SELECT ||
                        ((RuneChanger) getApi()).getChampionSelectionModule().isChampionLocked()) {
                    if (currentChampionsPosition > 1f) {
                        currentChampionsPosition = ease(currentChampionsPosition, 0f);
                        repaintLater();
                    }
                    else {
                        currentChampionsPosition = 0f;
                    }
                }
                else {
                    if (currentChampionsPosition < 99f) {
                        currentChampionsPosition = ease(currentChampionsPosition, 100f);
                        repaintLater();
                    }
                    else {
                        currentChampionsPosition = 100f;
                    }
                }
                g.setColor(DARKER_TEXT_COLOR);
                int barWidth = (int) ((isSmart ? Constants.EXTRA_WIDTH : Constants.CHAMPION_SUGGESTION_WIDTH) * getHeight());
                int selectionWidth = (int) (Constants.CHAMPION_SUGGESTION_WIDTH * getHeight());
                g.drawRect(getClientWidth() + 1 + (int) (currentChampionsPosition / 100f * barWidth) - barWidth, 0,
                        barWidth - 2, getHeight() - 1);
                g.setColor(BACKGROUND_COLOR);
                g.fillRect(getClientWidth() + (int) (currentChampionsPosition / 100f * barWidth) - barWidth, 1,
                        barWidth - 1, getHeight() - 2);
                int tileIndex = 0;
                List<Pair<Champion, Double>> collect;
                if (suggestions == null || !isSmart) {
                    collect = lastChampions.stream()
                            .map(champion -> new Pair<>(champion, -1.0))
                            .collect(Collectors.toList());
                }
                else {
                    collect = suggestions.stream()
                            .map(teamCompChampion -> new Pair<>(teamCompChampion.champion, teamCompChampion.winRate))
                            .collect(Collectors.toList());
                }
                for (Pair<Champion, Double> suggestion : collect) {
                    if (bannedChampions != null && bannedChampions.contains(suggestion.getKey())) {
                        continue;
                    }
                    Image img = suggestion.getKey().getPortrait();
                    int tileSize = (int) (Constants.CHAMPION_TILE_SIZE * getHeight());
                    int rowSize = getHeight() / 6;
                    if (selectedChampionIndex == tileIndex) {
                        g.setColor(LIGHTEN_COLOR);
                        g.fillRect(getClientWidth(), rowSize * tileIndex, selectionWidth, rowSize);
                    }
                    int x = (getClientWidth() + (selectionWidth - tileSize) / 2) +
                            (int) (currentChampionsPosition / 100f * selectionWidth) - selectionWidth;
                    int y = (rowSize - tileSize) / 2 + (rowSize * tileIndex);
                    g.drawImage(img,
                            x,
                            y,
                            tileSize, tileSize, null);
                    if (suggestion.getValue() != -1) {
                        g.setColor(TEXT_COLOR);
                        String winrate = String.format("%.2f%%", suggestion.getValue() * 100);
                        FontMetrics fontMetrics = g.getFontMetrics();
                        LineMetrics metrics = fontMetrics.getLineMetrics(winrate, g);
                        g.drawString(winrate,
                                x + (tileSize / 2) - (fontMetrics.stringWidth(winrate) / 2), (int) (y + tileSize +
                                        metrics.getAscent()));
                    }
                    if (tileIndex >= 6) {
                        break;
                    }
                    tileIndex++;
                }
                if (isSmart) {
                    g.setColor(TEXT_COLOR);
                    AffineTransform trans = new AffineTransform();
                    trans.rotate(Math.toRadians(-90), 0, 0);
                    Font font = g.getFont();
                    Font rotatedFont = font.deriveFont(trans);
                    g.setFont(rotatedFont);
                    g.drawString("Powered by LoLTheory.gg", (int) ((getClientWidth() - 12) + (currentChampionsPosition / 100f * (selectionWidth + 12))), getHeight() - 10);
                    g.setFont(font);
                }
                clearRect(g, getClientWidth() - barWidth, 0, barWidth, getHeight());
            }
        }
    }

    @Subscribe(value = ChampionsImpl.IMAGES_READY_EVENT, eventExecutor = UiEventExecutor.class)
    public void onImagesReady() {
        repaintLater();
    }

    public void mouseReleased(MouseEvent e) {
        boolean isSmart =
                SimplePreferences.getBooleanValue(SimplePreferences.SettingsKeys.SMART_CHAMPION_SUGGESTIONS, true);
        if (selectedChampionIndex != -1 && bannedChampions != null &&
                (lastChampions != null || (suggestions != null && isSmart))) {
            List<Champion> collect;
            if (suggestions == null || !isSmart) {
                collect = lastChampions;
            }
            else {
                collect = suggestions.stream()
                        .map(teamCompChampion -> teamCompChampion.champion)
                        .collect(Collectors.toList());
            }
            // Fix wrong champion selected, when one or more of them are banned
            int index = selectedChampionIndex;
            for (int i = 0; i <= index; i++) {
                if (bannedChampions.contains(collect.get(i))) {
                    index++;
                }
            }
            if (index < collect.size()) {
                RuneChanger.getInstance().getChampionSelectionModule().selectChampion(collect.get(index));
            }
        }
    }

    public void mouseExited(MouseEvent e) {
        if (selectedChampionIndex != -1) {
            selectedChampionIndex = -1;
            repaintNow();
        }
    }

    public void mouseMoved(MouseEvent e) {
        int championIndex;
        if (e.getX() < getClientWidth()) {
            championIndex = -1;
        }
        else {
            championIndex = (int) ((float) e.getY() / (float) (getHeight() / 6));
            getClientOverlay().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        if (championIndex > 5) {
            championIndex = -1;
        }
        if (championIndex != selectedChampionIndex) {
            selectedChampionIndex = championIndex;
            repaintNow();
        }
    }
}
