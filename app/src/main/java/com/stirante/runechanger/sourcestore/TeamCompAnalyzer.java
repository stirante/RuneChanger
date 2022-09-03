package com.stirante.runechanger.sourcestore;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.stirante.eventbus.EventBus;
import com.stirante.justpipe.Pipe;
import com.stirante.runechanger.RuneChanger;
import com.stirante.runechanger.api.Champion;
import com.stirante.runechanger.gui.overlay.NotificationWindow;
import com.stirante.runechanger.util.AnalyticsUtil;
import generated.Position;
import org.apache.hc.core5.net.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class TeamCompAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(TeamCompAnalyzer.class.getName());
    private static final String RANK_RANGE = "PLATINUM_PLUS";

    private final LoadingCache<String, SelectionPerformances> cache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build(key -> {
                log.debug("No team comp cache, fetching...");
                String json = Pipe.from(new URL(key)).toString();
                return new Gson().fromJson(json, SelectionPerformances.class);
            });

    private String createUrl(Position position, Map<Position, Champion> allyTeam, List<Champion> enemyTeam, List<Champion> bans) {
        //https://api.prod.loltheory.gg/team-comp/selection-performances/0/4-4?ally=0-266-0&ally=1-32-1&ally=2-136-2&ally=3-22-3&enemy=0-31&ban=103&ban=1&champ=166&rank-range=PLATINUM_PLUS
        URIBuilder uriBuilder = new URIBuilder()
                .setScheme("https")
                .setHost("api.prod.loltheory.gg")
                .setPath("/team-comp/selection-performances/0/" + getPositionIndex(position) + "-" +
                        getPositionIndex(position));
        List<Map.Entry<Position, Champion>> sorted = allyTeam.entrySet()
                .stream()
                .sorted(Comparator.comparingInt(o -> getPositionIndex(o.getKey())))
                .collect(Collectors.toList());
        for (Map.Entry<Position, Champion> entry : sorted) {
            if (entry.getKey() != position) {
                uriBuilder.addParameter("ally",
                        getPositionIndex(entry.getKey()) + "-" + entry.getValue().getId() + "-" +
                                getPositionIndex(entry.getKey()));
            }
        }

        for (int i = 0, enemyTeamSize = enemyTeam.size(); i < enemyTeamSize; i++) {
            Champion champion = enemyTeam.get(i);
            uriBuilder.addParameter("enemy", i + "-" + champion.getId());
        }

        for (Champion ban : bans) {
            uriBuilder.addParameter("ban", String.valueOf(ban.getId()));
        }

        if (allyTeam.containsKey(position) && allyTeam.get(position) != null) {
            uriBuilder.addParameter("champ", String.valueOf(allyTeam.get(position).getId()));
        }

        uriBuilder.addParameter("rank-range", RANK_RANGE);

        try {
            return uriBuilder.build().toString();
        } catch (URISyntaxException e) {
            log.error("Failed to build team comp url", e);
            AnalyticsUtil.addCrashReport(e, "Failed to build team comp url", false);
        }
        return null;
    }

    public void analyze(Position position, Champion playerChampion, Map<Position, Champion> allyTeam, List<Champion> enemyTeam, List<Champion> bans) throws IOException {
        String url = createUrl(position, allyTeam, enemyTeam, bans);
        if (url == null) {
            return;
        }
        SelectionPerformances selectionPerformances = cache.get(url);
        if (selectionPerformances == null || selectionPerformances.selectionPerformance == null || selectionPerformances.selectionPerformance.championPerformances == null) {
            log.debug("No recommendation found");
            return;
        }
        TeamComp comp = new TeamComp();
        if (playerChampion != null) {
            if (selectionPerformances.selectionPerformance.championPerformances.size() > 0 &&
                    selectionPerformances.selectionPerformance.championPerformances.get(0).champ ==
                            playerChampion.getId()) {
                ChampionPerformance championPerformance =
                        selectionPerformances.selectionPerformance.championPerformances.get(0);
                comp.playerPicked = new TeamCompChampion(playerChampion, championPerformance.scoreInfo.score, championPerformance.basePerformance.playRate);
                log.debug("Player picked " + playerChampion.getName() + ", score: " +
                        toPercentage(championPerformance.scoreInfo.score * 100));
            }
        }
        selectionPerformances.selectionPerformance.championPerformances
                .forEach(championWinRate -> {
                    comp.suggestions.add(new TeamCompChampion(RuneChanger.getInstance()
                            .getChampions()
                            .getById(championWinRate.champ), championWinRate.scoreInfo.score, championWinRate.basePerformance.playRate));
                    comp.suggestions.sort(Comparator.comparingDouble(o -> o.champion == playerChampion ? -1 : -o.winRate));
                    log.debug(RuneChanger.getInstance().getChampions().getById(championWinRate.champ).getName() +
                            ": " +
                            toPercentage(championWinRate.scoreInfo.score));
                });
        log.debug("Team comp win rate: " + toPercentage(selectionPerformances.teamPerformance.winRateInfo.winRate));
        EventBus.publish(NotificationWindow.NotificationMessageEvent.NAME, new NotificationWindow.NotificationMessageEvent(
                "Estimated win rate: " + toPercentage(selectionPerformances.teamPerformance.winRateInfo.winRate)));
        if (selectionPerformances.enemyTeamAssignment.roleAssignmentGrid != null && !selectionPerformances.enemyTeamAssignment.roleAssignmentGrid.isEmpty()) {
            log.debug("Most probable assignments:");
            for (Map.Entry<String, Map<String, RoleProbability>> championSpot : selectionPerformances.enemyTeamAssignment.roleAssignmentGrid.entrySet()) {
                Map<String, RoleProbability> value = championSpot.getValue();
                if (championSpot.getKey().equals("-1")) continue;
                Champion champion = RuneChanger.getInstance().getChampions().getById(Integer.parseInt(championSpot.getKey()));
                List<Map.Entry<String, RoleProbability>> entries = value.entrySet()
                        .stream()
                        .sorted(Comparator.comparingDouble((Map.Entry<String, RoleProbability> o) -> o.getValue().probability)
                                .reversed())
                        .collect(Collectors.toList());
                log.debug(champion.getName() + ": " +
                                toPosition(Integer.parseInt(entries.get(0).getKey())));
                comp.estimatedPosition.put(champion, toPosition(Integer.parseInt(entries.get(0).getKey())));
            }
        }
        EventBus.publish(TeamCompAnalysisEvent.NAME, new TeamCompAnalysisEvent(comp));
    }

    private String toPercentage(Double v) {
        return v == null ? "?" : String.format("%.2f", v * 100) + "%";
    }

    private static int getPositionIndex(Position position) {
        switch (position) {
            case TOP:
                return 0;
            case JUNGLE:
                return 1;
            case MIDDLE:
                return 2;
            case BOTTOM:
                return 3;
            case UTILITY:
                return 4;
            default:
                return 0;
        }
    }

    private static Position spotToPosition(int spot) {
        switch (spot) {
            case 0:
            case 5:
                return Position.TOP;
            case 1:
            case 6:
                return Position.JUNGLE;
            case 2:
            case 7:
                return Position.MIDDLE;
            case 3:
            case 8:
                return Position.BOTTOM;
            case 4:
            case 9:
                return Position.UTILITY;
            default:
                return Position.UNSELECTED;
        }
    }

    private static Position toPosition(int position) {
        switch (position) {
            case 0:
                return Position.TOP;
            case 1:
                return Position.JUNGLE;
            case 2:
                return Position.MIDDLE;
            case 3:
                return Position.BOTTOM;
            case 4:
                return Position.UTILITY;
            default:
                return Position.UNSELECTED;
        }
    }

    public static class TeamCompAnalysisEvent {
        public static final String NAME = "TeamCompAnalysisEvent";
        public final TeamComp teamComp;

        public TeamCompAnalysisEvent(TeamComp teamCompAnalysis) {
            this.teamComp = teamCompAnalysis;
        }
    }

    public static class TeamComp {
        public TeamCompChampion playerPicked;
        public List<TeamCompChampion> suggestions = new ArrayList<>();
        public double winRate;
        public final Map<Champion, Position> estimatedPosition = new HashMap<>();
    }

    public static class TeamCompChampion {
        public Champion champion;
        public double winRate = 0;
        public double playRate;

        public TeamCompChampion(Champion champion, Double winRate, double playRate) {
            this.champion = champion;
            if (winRate != null) {
                this.winRate = winRate;
            }
            this.playRate = playRate;
        }
    }

    private static class SelectionPerformances {
        private int userSide;
        private int userRole;
        @SerializedName("team_performance")
        private TeamPerformance teamPerformance;
        @SerializedName("selection_performance")
        private SelectionPreference selectionPerformance;
        @SerializedName("enemy_team_assignment")
        private EnemyTeamAssignment enemyTeamAssignment;
        @SerializedName("pick_difference")
        private Map<String, PickDifference> pickDifference;
        private String patch;
    }

    private static class TeamPerformance {
        @SerializedName("risk_info")
        private RiskInfo riskInfo;
        @SerializedName("score_info")
        private ScoreInfo scoreInfo;
        @SerializedName("win_rate_info")
        private WinRateInfo winRateInfo;
    }

    private static class ScoreInfo {
        private double score;
    }

    private static class WinRateInfo {
        @SerializedName("win_rate")
        private double winRate;
        private Breakdown breakdown;
    }

    private static class RiskInfo {
        private double risk;
        @SerializedName("future_pick_risk")
        private PickRisk futurePickRisk;
        @SerializedName("flex_pick_risk")
        private PickRisk flexPickRisk;
    }

    private static class PickRisk {
        private double risk;
        @SerializedName("highest_risks")
        private List<Risk> highestRisks;
    }

    private static class Risk {
        private int champ;
        private double risk;
        private int role;
        private int spot;
    }

    private static class SelectionPreference {
        @SerializedName("champ_performances")
        private List<ChampionPerformance> championPerformances;
        private int userRole;
    }

    private static class ChampionPerformance {
        @SerializedName("risk_info")
        private RiskInfo riskInfo;
        @SerializedName("score_info")
        private ScoreInfo scoreInfo;
        private int champ;
        @SerializedName("base_performance")
        private Performance basePerformance;
        @SerializedName("win_rate_info")
        private WinRateInfo winRateInfo;
    }

    private static class Performance {
        @SerializedName("play_rate")
        private double playRate;
        @SerializedName("win_rate")
        private double winRate;
    }

    private static class Breakdown {
        private List<ChampDiffs> ally;
        private List<ChampDiffs> enemy;
    }

    private static class ChampDiffs {
        private int champ;
        private List<Difference> differences;
    }

    private static class Difference {
        private int spot;
        private double scale;
    }

    private static class EnemyTeamAssignment {
        @SerializedName("role_assignment_grid")
        private Map<String, Map<String, RoleProbability>> roleAssignmentGrid;
    }

    private static class RoleProbability {
        private double probability;
        @SerializedName("in_most_likely_team_assignment")
        private Boolean inMostLikelyTeamAssignment;
    }

    private static class PickDifference {
        public double overall;
    }
}
