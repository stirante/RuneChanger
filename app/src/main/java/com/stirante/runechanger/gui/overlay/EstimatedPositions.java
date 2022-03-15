package com.stirante.runechanger.gui.overlay;

import com.stirante.eventbus.EventBus;
import com.stirante.eventbus.Subscribe;
import com.stirante.runechanger.DebugConsts;
import com.stirante.runechanger.RuneChanger;
import com.stirante.runechanger.api.Champion;
import com.stirante.runechanger.api.overlay.OverlayLayer;
import com.stirante.runechanger.client.ChampionSelection;
import com.stirante.runechanger.sourcestore.TeamCompAnalyzer;
import com.stirante.runechanger.util.AnalyticsUtil;
import com.stirante.runechanger.utils.SceneType;
import com.stirante.runechanger.utils.SwingUtils;
import generated.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class EstimatedPositions extends OverlayLayer {
    private static final Logger log = LoggerFactory.getLogger(EstimatedPositions.class);

    private final BufferedImage[] icons = new BufferedImage[5];
    private TeamCompAnalyzer.TeamComp analysis;

    EstimatedPositions(ClientOverlayImpl overlay) {
        super(overlay);
        try {
            icons[0] =
                    SwingUtils.getScaledImage(16, 16, ImageIO.read(getClass().getResourceAsStream("/images/icon-position-top.png")));
            icons[1] =
                    SwingUtils.getScaledImage(16, 16, ImageIO.read(getClass().getResourceAsStream("/images/icon-position-middle.png")));
            icons[2] =
                    SwingUtils.getScaledImage(16, 16, ImageIO.read(getClass().getResourceAsStream("/images/icon-position-bottom.png")));
            icons[3] =
                    SwingUtils.getScaledImage(16, 16, ImageIO.read(getClass().getResourceAsStream("/images/icon-position-jungle.png")));
            icons[4] =
                    SwingUtils.getScaledImage(16, 16, ImageIO.read(getClass().getResourceAsStream("/images/icon-position-utility.png")));
        } catch (IOException e) {
            log.error("Exception occurred while loading position icons", e);
            AnalyticsUtil.addCrashReport(e, "Exception occurred while loading position icons", false);
        }
        EventBus.register(this);
    }


    @Subscribe(value = TeamCompAnalyzer.TeamCompAnalysisEvent.NAME)
    public void onTeamCompAnalysis(TeamCompAnalyzer.TeamCompAnalysisEvent event) {
        analysis = event.teamComp;
        repaintLater();
    }

    @Override
    protected void draw(Graphics g) {
        if (DebugConsts.MOCK_SESSION) {
            analysis = new TeamCompAnalyzer.TeamComp();
            analysis.estimatedPosition.put(getApi().getChampions().getByName("Mordekaiser"), Position.TOP);
            analysis.estimatedPosition.put(getApi().getChampions().getByName("Nunu"), Position.JUNGLE);
            analysis.estimatedPosition.put(getApi().getChampions().getByName("Lulu"), Position.UTILITY);
            analysis.estimatedPosition.put(getApi().getChampions().getByName("Vayne"), Position.BOTTOM);
            analysis.estimatedPosition.put(getApi().getChampions().getByName("Yasuo"), Position.MIDDLE);
        }
        //[x = 0.9820312 y = 0.21666667]
        //[x = 0.9820312 y = 0.3263889]
        //[x = 0.9820312 y = 0.44166666]
        //[x = 0.9820312 y = 0.5541667]
        //[x = 0.9820312 y = 0.6638889]
        double x = 0.9770312;
        double[] positions = new double[]{
                0.21666667,
                0.3263889,
                0.44166666,
                0.5541667,
                0.6638889
        };
        if (getSceneType() == SceneType.CHAMPION_SELECT && analysis != null && !analysis.estimatedPosition.isEmpty()) {
            ChampionSelection champSelect = ((RuneChanger) getApi()).getChampionSelectionModule();
            List<Champion> enemyTeam = champSelect.getEnemyTeam();
            if (DebugConsts.MOCK_SESSION) {
                enemyTeam =
                        Arrays.asList(getApi().getChampions().getByName("Mordekaiser"), getApi().getChampions()
                                .getByName("Nunu"), getApi().getChampions().getByName("Lulu"), getApi().getChampions()
                                .getByName("Vayne"), getApi().getChampions().getByName("Yasuo"));
            }
            for (int i = 0, enemyTeamSize = enemyTeam.size(); i < enemyTeamSize; i++) {
                Champion champion = enemyTeam.get(i);
                if (analysis.estimatedPosition.containsKey(champion)) {
                    BufferedImage icon = icons[analysis.estimatedPosition.get(champion).ordinal()];
                    g.drawImage(icon, (int) (getClientWidth() * x), (int) (getHeight() * positions[i]), null);
                }
            }

        }
    }

}
