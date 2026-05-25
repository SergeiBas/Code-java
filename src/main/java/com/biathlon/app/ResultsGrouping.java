package com.biathlon.app;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ResultsGrouping {

    record CompetitionGroup(int competitionId, String name, List<DisciplineGroup> disciplines) {}

    record DisciplineGroup(
            int disciplineId,
            String name,
            String penaltyType,
            boolean relay,
            List<Map<String, Object>> results,
            List<RelayTeamGroup> relayTeams) {}

    record RelayTeamGroup(
            int countryId,
            String countryName,
            int place,
            int teamFinalSeconds,
            List<Map<String, Object>> legs) {}

    private ResultsGrouping() {}

    static List<CompetitionGroup> group(List<Map<String, Object>> competitions, List<Map<String, Object>> rows) {
        LinkedHashMap<Integer, CompetitionGroup> byCompetition = new LinkedHashMap<>();
        for (Map<String, Object> comp : competitions) {
            int id = ((Number) comp.get("competition_id")).intValue();
            byCompetition.put(id, new CompetitionGroup(id, (String) comp.get("name"), new ArrayList<>()));
        }

        LinkedHashMap<String, List<Map<String, Object>>> rowsByDiscipline = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            int competitionId = ((Number) row.get("competition_id")).intValue();
            int disciplineId = ((Number) row.get("discipline_id")).intValue();
            rowsByDiscipline.computeIfAbsent(competitionId + ":" + disciplineId, k -> new ArrayList<>()).add(row);
        }

        for (Map<String, Object> row : rows) {
            int competitionId = ((Number) row.get("competition_id")).intValue();
            CompetitionGroup competition = byCompetition.get(competitionId);
            if (competition == null) {
                continue;
            }

            int disciplineId = ((Number) row.get("discipline_id")).intValue();
            if (competition.disciplines().stream().anyMatch(d -> d.disciplineId() == disciplineId)) {
                continue;
            }

            String key = competitionId + ":" + disciplineId;
            List<Map<String, Object>> disciplineRows = rowsByDiscipline.getOrDefault(key, List.of());
            String disciplineName = (String) row.get("discipline_name");
            String penaltyType = (String) row.get("penalty_type");
            boolean relay = "RELAY_RULES".equals(penaltyType);

            if (relay) {
                competition.disciplines().add(new DisciplineGroup(
                        disciplineId, disciplineName, penaltyType, true, List.of(), buildRelayTeams(disciplineRows)));
            } else {
                List<Map<String, Object>> sorted = new ArrayList<>(disciplineRows);
                sorted.sort(Comparator
                        .comparingInt((Map<String, Object> r) -> placeOrMax(r))
                        .thenComparingInt(r -> ((Number) r.get("result_id")).intValue()));
                competition.disciplines().add(new DisciplineGroup(
                        disciplineId, disciplineName, penaltyType, false, sorted, List.of()));
            }
        }

        for (CompetitionGroup competition : byCompetition.values()) {
            competition.disciplines().sort(Comparator.comparingInt(DisciplineGroup::disciplineId));
        }

        return new ArrayList<>(byCompetition.values());
    }

    static List<RelayTeamGroup> buildRelayTeams(List<Map<String, Object>> rows) {
        LinkedHashMap<Integer, List<Map<String, Object>>> byCountry = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            int countryId = ((Number) row.get("country_id")).intValue();
            byCountry.computeIfAbsent(countryId, k -> new ArrayList<>()).add(row);
        }

        List<RelayTeamGroup> teams = new ArrayList<>();
        for (var entry : byCountry.entrySet()) {
            List<Map<String, Object>> legs = new ArrayList<>(entry.getValue());
            legs.sort(Comparator
                    .comparingInt((Map<String, Object> r) -> legOrMax(r))
                    .thenComparingInt(r -> ((Number) r.get("result_id")).intValue()));

            int teamFinal = legs.stream()
                    .mapToInt(r -> ((Number) r.get("final_time_seconds")).intValue())
                    .sum();
            int place = legs.stream()
                    .mapToInt(ResultsGrouping::placeOrMax)
                    .filter(p -> p < Integer.MAX_VALUE)
                    .findFirst()
                    .orElse(Integer.MAX_VALUE);

            String countryName = (String) legs.get(0).get("country_name");
            teams.add(new RelayTeamGroup(entry.getKey(), countryName, place, teamFinal, legs));
        }

        teams.sort(Comparator
                .comparingInt(RelayTeamGroup::place)
                .thenComparingInt(RelayTeamGroup::teamFinalSeconds)
                .thenComparingInt(RelayTeamGroup::countryId));

        return teams;
    }

    private static int placeOrMax(Map<String, Object> row) {
        Object place = row.get("place");
        return place == null ? Integer.MAX_VALUE : ((Number) place).intValue();
    }

    private static int legOrMax(Map<String, Object> row) {
        Object leg = row.get("leg_number");
        return leg == null ? Integer.MAX_VALUE : ((Number) leg).intValue();
    }
}
