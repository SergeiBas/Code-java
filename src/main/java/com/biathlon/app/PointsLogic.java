package com.biathlon.app;

/**
 * Бали Кубка світу IBU за місцем (індивідуальні старти та етапи естафет).
 */
public final class PointsLogic {

    /** SQL-вираз: бали за полем {@code r.place}. */
    static final String POINTS_FOR_PLACE_SQL = """
        CASE r.place
            WHEN 1 THEN 90 WHEN 2 THEN 75 WHEN 3 THEN 60 WHEN 4 THEN 50
            WHEN 5 THEN 45 WHEN 6 THEN 40 WHEN 7 THEN 36 WHEN 8 THEN 34
            WHEN 9 THEN 32 WHEN 10 THEN 31 WHEN 11 THEN 30 WHEN 12 THEN 29
            WHEN 13 THEN 28 WHEN 14 THEN 27 WHEN 15 THEN 26 WHEN 16 THEN 25
            WHEN 17 THEN 24 WHEN 18 THEN 23 WHEN 19 THEN 22 WHEN 20 THEN 21
            WHEN 21 THEN 20 WHEN 22 THEN 19 WHEN 23 THEN 18 WHEN 24 THEN 17
            WHEN 25 THEN 16 WHEN 26 THEN 15 WHEN 27 THEN 14 WHEN 28 THEN 13
            WHEN 29 THEN 12 WHEN 30 THEN 11 WHEN 31 THEN 10 WHEN 32 THEN 9
            WHEN 33 THEN 8 WHEN 34 THEN 7 WHEN 35 THEN 6 WHEN 36 THEN 5
            WHEN 37 THEN 4 WHEN 38 THEN 3 WHEN 39 THEN 2 WHEN 40 THEN 1
            ELSE 0
        END
        """;

    static final String ATHLETE_POINTS_SUBQUERY = """
        (SELECT r.athlete_id, SUM(""" + POINTS_FOR_PLACE_SQL + """
            ) AS total_points
            FROM result r
            WHERE r.place IS NOT NULL
            GROUP BY r.athlete_id)
        """;

    private PointsLogic() {}

    public static int pointsForPlace(Integer place) {
        if (place == null || place < 1 || place > 40) {
            return 0;
        }
        return switch (place) {
            case 1 -> 90;
            case 2 -> 75;
            case 3 -> 60;
            case 4 -> 50;
            case 5 -> 45;
            case 6 -> 40;
            case 7 -> 36;
            case 8 -> 34;
            case 9 -> 32;
            case 10 -> 31;
            case 11 -> 30;
            case 12 -> 29;
            case 13 -> 28;
            case 14 -> 27;
            case 15 -> 26;
            case 16 -> 25;
            case 17 -> 24;
            case 18 -> 23;
            case 19 -> 22;
            case 20 -> 21;
            case 21 -> 20;
            case 22 -> 19;
            case 23 -> 18;
            case 24 -> 17;
            case 25 -> 16;
            case 26 -> 15;
            case 27 -> 14;
            case 28 -> 13;
            case 29 -> 12;
            case 30 -> 11;
            case 31 -> 10;
            case 32 -> 9;
            case 33 -> 8;
            case 34 -> 7;
            case 35 -> 6;
            case 36 -> 5;
            case 37 -> 4;
            case 38 -> 3;
            case 39 -> 2;
            case 40 -> 1;
            default -> 0;
        };
    }
}
