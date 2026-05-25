package com.biathlon.app;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class AthleteListHelper {

    private AthleteListHelper() {}

    static List<Map<String, Object>> loadRows(JdbcTemplate jdbc, String sort, Integer countryId, String gender) {
        StringBuilder sql = new StringBuilder(
                """
            SELECT a.*, c.name_uk country_name, COALESCE(pts.total_points, 0) total_points
            FROM athlete a
            JOIN country c ON c.country_id = a.country_id
            LEFT JOIN """
                        + PointsLogic.ATHLETE_POINTS_SUBQUERY
                        + " pts ON pts.athlete_id = a.athlete_id\n            WHERE 1=1\n        ");
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, countryId, gender);
        sql.append(" ORDER BY ").append(orderBy(sort));
        return jdbc.queryForList(sql.toString(), args.toArray());
    }

    static List<Map<String, Object>> loadOptions(JdbcTemplate jdbc, String sort, Integer countryId, String gender) {
        List<Map<String, Object>> rows = loadRows(jdbc, sort, countryId, gender);
        List<Map<String, Object>> options = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            int id = ((Number) row.get("athlete_id")).intValue();
            int points = ((Number) row.get("total_points")).intValue();
            String name = row.get("last_name") + " " + row.get("first_name");
            options.add(Map.of(
                    "athlete_id", id,
                    "label", id + " — " + name + " (" + points + " б.)"
            ));
        }
        return options;
    }

    static List<Map<String, Object>> loadStandings(JdbcTemplate jdbc, int limit) {
        return loadRows(jdbc, "points_desc", null, null).stream().limit(limit).toList();
    }

    private static void appendFilters(StringBuilder sql, List<Object> args, Integer countryId, String gender) {
        if (countryId != null) {
            sql.append(" AND a.country_id = ?");
            args.add(countryId);
        }
        if (gender != null && !gender.isBlank() && ("M".equals(gender) || "W".equals(gender))) {
            sql.append(" AND a.gender = ?");
            args.add(gender);
        }
    }

    static String orderBy(String sort) {
        return switch (sort) {
            case "points_desc" -> "total_points DESC, a.athlete_id";
            case "points_asc" -> "total_points ASC, a.athlete_id";
            case "name" -> "a.last_name, a.first_name, a.athlete_id";
            case "country" -> "c.name_uk, a.athlete_id";
            case "id_desc" -> "a.athlete_id DESC";
            default -> "a.athlete_id";
        };
    }
}
