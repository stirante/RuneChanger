package com.stirante.runechanger.sourcestore;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.stirante.eventbus.BusEvent;
import com.stirante.eventbus.EventBus;
import com.stirante.justpipe.Pipe;
import com.stirante.runechanger.gui.overlay.NotificationWindow;
import com.stirante.runechanger.model.client.Champion;
import generated.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TeamCompAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(TeamCompAnalyzer.class.getName());
    private static final String API_URL =
            "https://api.loltheory.gg/teamcomp/spot_recommendation/%position%?%spots%counter_cutoff=%counter_cutoff%";
    private static final Double COUNTER_CUTOFF = -0.03;

    private final LoadingCache<String, SpotRecommendation> cache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build(key -> {
                log.debug("No team comp cache, fetching...");
                String json = Pipe.from(new URL(key)).toString();
                return new Gson().fromJson(json, SpotRecommendation.class);
            });

    private String createUrl(Position position, Map<Position, Champion> allyTeam, List<Champion> enemyTeam, List<Champion> bans) {
        int positionIndex = getPositionIndex(position);
        StringBuilder spots = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            Position spotPosition = spotToPosition(i);
            if (allyTeam.containsKey(spotPosition)) {
                if (allyTeam.get(spotPosition) == null) {
                    continue;
                }
                spots.append("spot").append(i).append("=").append(allyTeam.get(spotPosition).getId()).append("&");
            }
        }
        for (int i = 0; i < 5; i++) {
            for (Champion champion : enemyTeam) {
                if (champion == null) {
                    continue;
                }
                spots.append("spot").append(i + 5).append("=").append(champion.getId()).append("&");
            }
        }
        for (Champion ban : bans) {
            if (ban == null) {
                continue;
            }
            spots.append("bans").append("=").append(ban.getId()).append("&");
        }
        return API_URL
                .replace("%position%", String.valueOf(positionIndex))
                .replace("%spots%", spots)
                .replace("%counter_cutoff%", String.valueOf(COUNTER_CUTOFF));
    }

    public static void main(String[] args) throws IOException {
        Champion.init();
        TeamCompAnalyzer analyzer = new TeamCompAnalyzer();
        Map<Position, Champion> allyTeam = new HashMap<>();
        allyTeam.put(Position.BOTTOM, Champion.getById(429));
        allyTeam.put(Position.MIDDLE, Champion.getById(63));
        allyTeam.put(Position.UTILITY, Champion.getById(53));
        allyTeam.put(Position.TOP, Champion.getById(3));
        allyTeam.put(Position.JUNGLE, Champion.getById(96));
        List<Champion> enemyTeam = new ArrayList<>(5);
        enemyTeam.add(Champion.getById(22));
        enemyTeam.add(Champion.getById(25));
        enemyTeam.add(Champion.getById(64));
        enemyTeam.add(Champion.getById(92));
        enemyTeam.add(Champion.getById(85));
        analyzer.analyze(Position.UTILITY, Champion.getById(53), allyTeam, enemyTeam, new ArrayList<>());
    }

    public void analyze(Position position, Champion playerChampion, Map<Position, Champion> allyTeam, List<Champion> enemyTeam, List<Champion> bans) throws IOException {
        String url = createUrl(position, allyTeam, enemyTeam, bans);
        SpotRecommendation spotRecommendation = cache.get(url);
        if (spotRecommendation == null) {
            log.debug("No recommendation found");
            return;
        }
        TeamComp comp = new TeamComp();
        if (playerChampion != null) {
            if (spotRecommendation.championSpotInfo != null &&
                    spotRecommendation.championSpotInfo.get(playerChampion.getId()) != null) {
                comp.playerPicked = new TeamCompChampion(playerChampion, spotRecommendation.championWinRates.stream()
                        .filter(championWinRate -> championWinRate.championId == playerChampion.getId())
                        .findFirst()
                        .map(championWinRate -> championWinRate.winRate)
                        .orElse(null), spotRecommendation.championSpotInfo.get(playerChampion.getId()).playRate);
                log.debug("Player picked " + playerChampion.getName() + ", win rate: " +
                        toPercentage(spotRecommendation.championWinRates.stream()
                                .filter(championWinRate -> championWinRate.championId == playerChampion.getId())
                                .findFirst()
                                .map(championWinRate -> championWinRate.winRate)
                                .orElse(null)));
            }
        }
        spotRecommendation.championWinRates
                .forEach(championWinRate -> {
                    comp.suggestions.add(new TeamCompChampion(Champion.getById(championWinRate.championId), championWinRate.winRate, spotRecommendation.championSpotInfo.get(championWinRate.championId).playRate));
                    log.debug(Champion.getById(championWinRate.championId).getName() + ": " +
                            toPercentage(championWinRate.winRate));
                });
        log.debug("Team comp win rate: " + toPercentage(spotRecommendation.winRate));
        EventBus.publish(NotificationWindow.NotificationMessageEvent.NAME, new NotificationWindow.NotificationMessageEvent(
                "Estimated win rate: " + toPercentage(spotRecommendation.winRate)));
        if (spotRecommendation.options != null && !spotRecommendation.options.isEmpty()) {
            log.debug("Most probable assignments:");
            List<ChampionSpot> assignment = spotRecommendation.options.get(0).assignment;
            for (ChampionSpot championSpot : assignment) {
                log.debug(
                        Champion.getById(championSpot.championId).getName() + ": " + spotToPosition(championSpot.spot));
                comp.estimatedPosition.put(Champion.getById(championSpot.championId), spotToPosition(championSpot.spot));
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

    public static class TeamCompAnalysisEvent implements BusEvent {
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

    private static class SpotRecommendation {
        @SerializedName("champ_win_rates")
        private List<ChampionWinRate> championWinRates;
        @SerializedName("win_rate")
        private double winRate;
        @SerializedName("champ_spot_info")
        private Map<Integer, ChampionSpotInfo> championSpotInfo;
        private List<Option> options;
        private int spot;
        private String patch;
    }

    private static class ChampionWinRate {
        @SerializedName("champ")
        private int championId;
        @SerializedName("win_rate")
        private double winRate;
        @SerializedName("counters")
        private List<Object> counters;
        @SerializedName("power_pick_performances")
        private List<PowerPickPerfomance> powerPickPerformances;
    }

    private static class ChampionSpotInfo {
        @SerializedName("win_rate")
        private double winRate;
        @SerializedName("play_rate")
        private double playRate;
    }

    private static class Option {
        @SerializedName("champ_win_rates")
        private List<ChampionWinRate> championWinRates;
        @SerializedName("win_rate")
        private double winRate;
        private double probablity;
        private List<ChampionSpot> assignment;
    }

    private static class ChampionSpot {
        @SerializedName("spot")
        private int spot;
        @SerializedName("champ")
        private int championId;
    }

    private static class PowerPickPerfomance {
        @SerializedName("champ")
        private int championId;
        @SerializedName("win_rate")
        private double winRate;
        @SerializedName("removed")
        private boolean removed;
    }
}
