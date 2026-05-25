package com.biathlon.app;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

final class PlaceLogic {

    private PlaceLogic() {}

    static void recalculate(JdbcTemplate jdbc, int competitionId, int disciplineId) {
        String penaltyType = jdbc.queryForObject(
                "SELECT penalty_type FROM discipline WHERE discipline_id = ?",
                String.class,
                disciplineId);
        if ("RELAY_RULES".equals(penaltyType)) {
            recalculateRelay(jdbc, competitionId, disciplineId);
        } else {
            recalculateIndividual(jdbc, competitionId, disciplineId);
        }
    }

    private static void recalculateIndividual(JdbcTemplate jdbc, int competitionId, int disciplineId) {
        List<Integer> ids = jdbc.query("""
            SELECT result_id FROM result
            WHERE competition_id = ? AND discipline_id = ?
            ORDER BY final_time_seconds ASC, result_id ASC
        """, (rs, rowNum) -> rs.getInt(1), competitionId, disciplineId);

        for (int i = 0; i < ids.size(); i++) {
            jdbc.update("UPDATE result SET place = ? WHERE result_id = ?", i + 1, ids.get(i));
        }
    }

    private static void recalculateRelay(JdbcTemplate jdbc, int competitionId, int disciplineId) {
        List<Map<String, Object>> teams = jdbc.queryForList("""
            SELECT a.country_id, SUM(r.final_time_seconds) team_time, MIN(r.result_id) tie_break
            FROM result r
            JOIN athlete a ON a.athlete_id = r.athlete_id
            WHERE r.competition_id = ? AND r.discipline_id = ?
            GROUP BY a.country_id
            ORDER BY team_time ASC, tie_break ASC
        """, competitionId, disciplineId);

        int place = 1;
        for (Map<String, Object> team : teams) {
            int countryId = ((Number) team.get("country_id")).intValue();
            jdbc.update("""
                UPDATE result SET place = ?
                WHERE competition_id = ? AND discipline_id = ?
                  AND athlete_id IN (SELECT athlete_id FROM athlete WHERE country_id = ?)
            """, place, competitionId, disciplineId, countryId);
            place++;
        }
    }
}
