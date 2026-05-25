package com.biathlon.app;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

@Component
public class DbInitializer implements CommandLineRunner {
    private final JdbcTemplate jdbc;
    private final DataSource dataSource;

    public DbInitializer(JdbcTemplate jdbc, DataSource dataSource) {
        this.jdbc = jdbc;
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) throws Exception {
        Connection conn = DataSourceUtils.getConnection(dataSource);
        ScriptUtils.executeSqlScript(conn, new ClassPathResource("db/schema.sql"));
        migrateSchema();

        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM country", Integer.class);
        if (count != null && count == 0) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("db/seed.sql"));
            recalculateAllPlaces();
        } else {
            recalculateRelayPlacesIfNeeded();
        }
    }

    private void migrateSchema() {
        ensureColumn("athlete", "gender", "gender TEXT NOT NULL DEFAULT 'M'");
        ensureColumn("result", "leg_number", "leg_number INTEGER");
        jdbc.update("""
            UPDATE athlete SET gender = 'M'
            WHERE gender IS NULL OR gender NOT IN ('M', 'W')
        """);
    }

    private void ensureColumn(String table, String column, String ddl) {
        List<String> columns = jdbc.query("PRAGMA table_info(" + table + ")", (rs, i) -> rs.getString("name"));
        if (!columns.contains(column)) {
            jdbc.execute("ALTER TABLE " + table + " ADD COLUMN " + ddl);
        }
    }

    private void recalculateAllPlaces() {
        List<Map<String, Object>> pairs = jdbc.queryForList("""
            SELECT DISTINCT competition_id, discipline_id FROM result
        """);
        for (Map<String, Object> pair : pairs) {
            int competitionId = ((Number) pair.get("competition_id")).intValue();
            int disciplineId = ((Number) pair.get("discipline_id")).intValue();
            PlaceLogic.recalculate(jdbc, competitionId, disciplineId);
        }
    }

    private void recalculateRelayPlacesIfNeeded() {
        List<Map<String, Object>> relayPairs = jdbc.queryForList("""
            SELECT DISTINCT r.competition_id, r.discipline_id
            FROM result r
            JOIN discipline d ON d.discipline_id = r.discipline_id
            WHERE d.penalty_type = 'RELAY_RULES'
        """);
        for (Map<String, Object> pair : relayPairs) {
            int competitionId = ((Number) pair.get("competition_id")).intValue();
            int disciplineId = ((Number) pair.get("discipline_id")).intValue();
            PlaceLogic.recalculate(jdbc, competitionId, disciplineId);
        }
    }
}
