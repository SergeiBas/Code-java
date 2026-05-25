package com.biathlon.app;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class WebController {
    private final JdbcTemplate jdbc;

    public WebController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("title", "Головна");
        return "index";
    }

    @GetMapping("/countries")
    public String countries(@RequestParam(defaultValue = "id") String sort, Model model) {
        model.addAttribute("title", "Країни");
        model.addAttribute("sort", sort);
        model.addAttribute("showNameSort", true);
        model.addAttribute("resetUrl", "/countries");
        model.addAttribute("rows", jdbc.queryForList(
                "SELECT * FROM country ORDER BY " + orderByCountry(sort)));
        return "countries";
    }

    @PostMapping("/countries/add")
    public String countriesAdd(@RequestParam String iso_code, @RequestParam String name_uk) {
        jdbc.update("INSERT INTO country (iso_code, name_uk) VALUES (?, ?)", iso_code.trim().toUpperCase(), name_uk.trim());
        return "redirect:/countries";
    }

    @PostMapping("/countries/{id}/delete")
    public String countriesDelete(@PathVariable int id) {
        jdbc.update("DELETE FROM country WHERE country_id = ?", id);
        return "redirect:/countries";
    }

    @GetMapping("/athletes")
    public String athletes(@RequestParam(defaultValue = "points_desc") String sort,
                           @RequestParam(required = false) Integer country_id,
                           @RequestParam(required = false) String gender,
                           Model model) {
        model.addAttribute("title", "Спортсмени");
        addAthleteFilterModel(model, sort, country_id, gender, null, "/athletes");
        model.addAttribute("rows", AthleteListHelper.loadRows(jdbc, sort, country_id, gender));
        return "athletes";
    }

    @PostMapping("/athletes/add")
    public String athletesAdd(@RequestParam int country_id, @RequestParam String last_name, @RequestParam String first_name,
                              @RequestParam String birth_date, @RequestParam String gender) {
        jdbc.update("INSERT INTO athlete (country_id, last_name, first_name, birth_date, gender) VALUES (?, ?, ?, ?, ?)",
                country_id, last_name.trim(), first_name.trim(), birth_date.trim(), gender);
        return "redirect:/athletes";
    }

    @PostMapping("/athletes/{id}/delete")
    public String athletesDelete(@PathVariable int id) {
        jdbc.update("DELETE FROM athlete WHERE athlete_id = ?", id);
        return "redirect:/athletes";
    }

    @GetMapping("/competitions")
    public String competitions(@RequestParam(defaultValue = "id") String sort, Model model) {
        model.addAttribute("title", "Змагання");
        model.addAttribute("sort", sort);
        model.addAttribute("showDateSort", true);
        model.addAttribute("resetUrl", "/competitions");
        model.addAttribute("rows", jdbc.queryForList(
                "SELECT * FROM competition ORDER BY " + orderByCompetition(sort)));
        return "competitions";
    }

    @PostMapping("/competitions/add")
    public String competitionsAdd(@RequestParam String name, @RequestParam String venue, @RequestParam String start_date,
                                  @RequestParam(required = false) String end_date, @RequestParam int season_year) {
        String end = (end_date == null || end_date.isBlank()) ? null : end_date;
        jdbc.update("INSERT INTO competition (name, venue, start_date, end_date, season_year) VALUES (?, ?, ?, ?, ?)",
                name.trim(), venue.trim(), start_date.trim(), end, season_year);
        return "redirect:/competitions";
    }

    @PostMapping("/competitions/{id}/delete")
    public String competitionsDelete(@PathVariable int id) {
        jdbc.update("DELETE FROM competition WHERE competition_id = ?", id);
        return "redirect:/competitions";
    }

    @GetMapping("/disciplines")
    public String disciplines(@RequestParam(defaultValue = "id") String sort, Model model) {
        model.addAttribute("title", "Дисципліни");
        model.addAttribute("sort", sort);
        model.addAttribute("showNameSort", true);
        model.addAttribute("resetUrl", "/disciplines");
        model.addAttribute("rows", jdbc.queryForList(
                "SELECT * FROM discipline ORDER BY " + orderByDiscipline(sort)));
        return "disciplines";
    }

    @PostMapping("/disciplines/add")
    public String disciplinesAdd(@RequestParam String name_uk, @RequestParam double distance_km, @RequestParam String gender,
                                 @RequestParam(defaultValue = "0") int rounds_prone, @RequestParam(defaultValue = "0") int rounds_standing,
                                 @RequestParam(required = false) String shooting_order, @RequestParam String penalty_type) {
        jdbc.update("""
            INSERT INTO discipline (name_uk, distance_km, gender, rounds_prone, rounds_standing, shooting_order, penalty_type)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """, name_uk.trim(), distance_km, gender, rounds_prone, rounds_standing,
                (shooting_order == null || shooting_order.isBlank()) ? null : shooting_order.trim(), penalty_type);
        return "redirect:/disciplines";
    }

    @PostMapping("/disciplines/{id}/delete")
    public String disciplinesDelete(@PathVariable int id) {
        jdbc.update("DELETE FROM discipline WHERE discipline_id = ?", id);
        return "redirect:/disciplines";
    }

    @GetMapping("/results")
    public String results(@RequestParam(defaultValue = "points_desc") String sort,
                          @RequestParam(required = false) Integer country_id,
                          @RequestParam(required = false) String gender,
                          @RequestParam(required = false) Integer athlete_id,
                          Model model) {
        model.addAttribute("title", "Результати");
        addAthleteFilterModel(model, sort, country_id, gender, athlete_id, "/results");

        List<Map<String, Object>> competitions = jdbc.queryForList(
                "SELECT competition_id, name FROM competition ORDER BY competition_id");

        StringBuilder sql = new StringBuilder(
                """
            SELECT r.*, a.last_name || ' ' || a.first_name athlete_name,
                   a.country_id, co.name_uk country_name, c.name competition_name,
                   d.name_uk discipline_name, d.penalty_type,
                   """
                        + PointsLogic.POINTS_FOR_PLACE_SQL
                        + """
                    AS race_points
            FROM result r
            JOIN athlete a ON a.athlete_id = r.athlete_id
            JOIN country co ON co.country_id = a.country_id
            JOIN competition c ON c.competition_id = r.competition_id
            JOIN discipline d ON d.discipline_id = r.discipline_id
            WHERE 1=1
        """);
        List<Object> args = new ArrayList<>();
        if (athlete_id != null) {
            sql.append(" AND r.athlete_id = ?");
            args.add(athlete_id);
        }
        if (country_id != null) {
            sql.append(" AND a.country_id = ?");
            args.add(country_id);
        }
        if (gender != null && !gender.isBlank() && ("M".equals(gender) || "W".equals(gender))) {
            sql.append(" AND a.gender = ?");
            args.add(gender);
        }
        sql.append(" ORDER BY c.competition_id, d.discipline_id, r.place, r.leg_number, r.result_id");

        List<Map<String, Object>> rows = jdbc.queryForList(sql.toString(), args.toArray());
        model.addAttribute("competitionGroups", ResultsGrouping.group(competitions, rows));
        List<Map<String, Object>> standings = AthleteListHelper.loadRows(jdbc, "points_desc", country_id, gender);
        if (athlete_id != null) {
            standings = standings.stream()
                    .filter(r -> athlete_id.equals(((Number) r.get("athlete_id")).intValue()))
                    .toList();
        } else {
            standings = standings.stream().limit(15).toList();
        }
        model.addAttribute("standings", standings);
        model.addAttribute("athletes", AthleteListHelper.loadOptions(jdbc, sort, country_id, gender));
        model.addAttribute("competitions", competitions);
        model.addAttribute("disciplines", jdbc.queryForList(
                "SELECT discipline_id, name_uk FROM discipline ORDER BY discipline_id"));
        return "results";
    }

    @PostMapping("/results/add")
    public String resultsAdd(@RequestParam int athlete_id, @RequestParam int competition_id, @RequestParam int discipline_id,
                             @RequestParam int ski_time_seconds, @RequestParam(defaultValue = "0") int misses_prone,
                             @RequestParam(defaultValue = "0") int misses_standing, @RequestParam(defaultValue = "0") int penalty_loops,
                             @RequestParam(defaultValue = "0") int penalty_time_manual,
                             @RequestParam(required = false) Integer leg_number) {
        String penaltyType = jdbc.queryForObject("SELECT penalty_type FROM discipline WHERE discipline_id = ?", String.class, discipline_id);
        ResultLogic.Calc calc = ResultLogic.compute(penaltyType, ski_time_seconds, misses_prone, misses_standing, penalty_loops, penalty_time_manual);
        Integer leg = (leg_number != null && leg_number >= 1 && leg_number <= 4) ? leg_number : null;

        jdbc.update("""
            INSERT INTO result (athlete_id, competition_id, discipline_id, ski_time_seconds, misses_prone, misses_standing,
                                penalty_time_seconds, penalty_loops, final_time_seconds, place, leg_number)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, ?)
        """, athlete_id, competition_id, discipline_id, ski_time_seconds, misses_prone, misses_standing,
                calc.penaltyTime(), calc.penaltyLoops(), calc.finalTime(), leg);

        PlaceLogic.recalculate(jdbc, competition_id, discipline_id);
        return "redirect:/results";
    }

    @PostMapping("/results/{id}/delete")
    public String resultsDelete(@PathVariable int id) {
        Map<String, Object> row = jdbc.queryForMap("SELECT competition_id, discipline_id FROM result WHERE result_id = ?", id);
        int competitionId = ((Number) row.get("competition_id")).intValue();
        int disciplineId = ((Number) row.get("discipline_id")).intValue();
        jdbc.update("DELETE FROM result WHERE result_id = ?", id);
        PlaceLogic.recalculate(jdbc, competitionId, disciplineId);
        return "redirect:/results";
    }

    @GetMapping("/reports")
    public String reports(@RequestParam(required = false) Integer competition_id,
                          @RequestParam(required = false) Integer discipline_id,
                          Model model) {
        model.addAttribute("title", "Звіт");
        model.addAttribute("comps", jdbc.queryForList(
                "SELECT competition_id, name FROM competition ORDER BY competition_id"));
        model.addAttribute("discs", jdbc.queryForList(
                "SELECT discipline_id, name_uk FROM discipline ORDER BY discipline_id"));
        model.addAttribute("competition_id", competition_id);
        model.addAttribute("discipline_id", discipline_id);

        if (competition_id != null && discipline_id != null) {
            String penaltyType = jdbc.queryForObject(
                    "SELECT penalty_type FROM discipline WHERE discipline_id = ?", String.class, discipline_id);
            List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT r.*, a.last_name || ' ' || a.first_name athlete_name,
                       a.country_id, co.name_uk country_name, d.penalty_type
                FROM result r
                JOIN athlete a ON a.athlete_id = r.athlete_id
                JOIN country co ON co.country_id = a.country_id
                JOIN discipline d ON d.discipline_id = r.discipline_id
                WHERE r.competition_id = ? AND r.discipline_id = ?
                ORDER BY r.place, r.leg_number, r.result_id
            """, competition_id, discipline_id);
            model.addAttribute("penaltyType", penaltyType);
            model.addAttribute("relay", "RELAY_RULES".equals(penaltyType));
            if ("RELAY_RULES".equals(penaltyType)) {
                model.addAttribute("relayTeams", ResultsGrouping.buildRelayTeams(rows));
            } else {
                model.addAttribute("rows", rows);
            }
        }

        return "reports";
    }

    private static String orderByCountry(String sort) {
        return switch (sort) {
            case "name" -> "name_uk, country_id";
            case "id_desc" -> "country_id DESC";
            default -> "country_id";
        };
    }

    private void addAthleteFilterModel(Model model, String sort, Integer countryId, String gender,
                                      Integer athleteId, String resetUrl) {
        model.addAttribute("sort", sort);
        model.addAttribute("country_id", countryId);
        model.addAttribute("gender", gender);
        model.addAttribute("athlete_id", athleteId);
        model.addAttribute("countries", jdbc.queryForList("SELECT * FROM country ORDER BY country_id"));
        model.addAttribute("athleteOptions", AthleteListHelper.loadOptions(jdbc, sort, countryId, gender));
        model.addAttribute("resetUrl", resetUrl);
        model.addAttribute("showAthletePicker", "/results".equals(resetUrl));
    }

    private static String orderByCompetition(String sort) {
        return switch (sort) {
            case "date" -> "start_date DESC, competition_id";
            case "id_desc" -> "competition_id DESC";
            default -> "competition_id";
        };
    }

    private static String orderByDiscipline(String sort) {
        return switch (sort) {
            case "name" -> "name_uk, discipline_id";
            case "id_desc" -> "discipline_id DESC";
            default -> "discipline_id";
        };
    }
}
