package com.stirante.runechanger.sourcestore;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.stirante.justpipe.Pipe;
import com.stirante.runechanger.model.client.Champion;
import generated.Position;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TeamCompAnalyzer {

    private static final String API_URL =
            "https://api.loltheory.gg/teamcomp/spot_recommendation/%position%?%spots%counter_cutoff=%counter_cutoff%";
    private static final Double COUNTER_CUTOFF = -0.03;

    private String createUrl(Position position, Champion playerChampion, List<Champion> allyTeam, List<Champion> enemyTeam) {
        int positionIndex = getPositionIndex(position);
        allyTeam.remove(playerChampion);
        allyTeam.add(Math.min(positionIndex, allyTeam.size()), playerChampion);
        StringBuilder spots = new StringBuilder();
        for (int i = 0, allyTeamSize = allyTeam.size(); i < allyTeamSize; i++) {
            Champion champion = allyTeam.get(i);
            if (champion != null) {
                spots.append("spot")
                        .append(champion == playerChampion ? positionIndex : i)
                        .append("=")
                        .append(champion.getId())
                        .append("&");
            }
        }
        for (int j = 0; j < 5; j++) {
            for (Champion champion : enemyTeam) {
                spots.append("spot").append(j + 5).append("=").append(champion.getId()).append("&");
            }
        }
        return API_URL
                .replace("%position%", String.valueOf(positionIndex))
                .replace("%spots%", spots)
                .replace("%counter_cutoff%", String.valueOf(COUNTER_CUTOFF));
    }

    public static void main(String[] args) throws IOException {
        Champion.init();
        TeamCompAnalyzer analyzer = new TeamCompAnalyzer();
        List<Champion> allyTeam = new ArrayList<>(5);
        allyTeam.add(Champion.getById(429));
        allyTeam.add(Champion.getById(63));
        allyTeam.add(Champion.getById(53));
        allyTeam.add(Champion.getById(3));
        allyTeam.add(Champion.getById(96));
        List<Champion> enemyTeam = new ArrayList<>(5);
        enemyTeam.add(Champion.getById(22));
        enemyTeam.add(Champion.getById(25));
        enemyTeam.add(Champion.getById(64));
        enemyTeam.add(Champion.getById(92));
        enemyTeam.add(Champion.getById(85));
        analyzer.analyze(Position.UTILITY, Champion.getById(53), allyTeam, enemyTeam);
    }

    private void analyze(Position position, Champion playerChampion, List<Champion> allyTeam, List<Champion> enemyTeam) throws IOException {
        String url = createUrl(position, playerChampion, allyTeam, enemyTeam);
        System.out.println(url);
        String json = Pipe.from(new URL(url)).toString();
        SpotRecommendation spotRecommendation = new Gson().fromJson(json, SpotRecommendation.class);
        if (playerChampion != null) {
            System.out.println("Player picked " + playerChampion.getName() + ", win rate: " +
                    spotRecommendation.championWinRates.stream()
                            .filter(championWinRate -> championWinRate.championId == playerChampion.getId())
                            .findFirst()
                            .map(championWinRate -> championWinRate.winRate)
                            .orElse(null));
        }
        spotRecommendation.championWinRates.stream().filter(championWinRate -> playerChampion == null || championWinRate.championId != playerChampion.getId()).forEach(championWinRate -> {
            System.out.println(Champion.getById(championWinRate.championId).getName() + ": " + championWinRate.winRate);
        });
        System.out.println("Team comp win rate: " + spotRecommendation.winRate);
        if (spotRecommendation.options != null && !spotRecommendation.options.isEmpty()) {
            System.out.println("Most probable assignments:");
            List<ChampionSpot> assignment = spotRecommendation.options.get(0).assignment;
            System.out.println("Ally team:");
            for (int i = 0, assignmentSize = assignment.size(); i < assignmentSize; i++) {
                ChampionSpot championSpot = assignment.get(i);
                if (i == 4) {
                    System.out.println("Enemy team:");
                }
                System.out.println(
                        Champion.getById(championSpot.championId).getName() + ": " + spotToPosition(championSpot.spot));
            }
        }
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
        }
        return 0;
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
}
