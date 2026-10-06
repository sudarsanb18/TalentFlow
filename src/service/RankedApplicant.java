package service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * One applicant of a job, with the computed match percentage.
 * Static helpers hold the ranking, cut-off, top-N and CSV rules so they can be tested without a database.
 */
public record RankedApplicant(int applicationId, int candidateId, String name, String skills,
                              double experience, int matchPercent, String status,
                              long appliedAtMillis, List<String> missingRequired) {

    /** Higher match first, then more experience, then the earlier application. */
    public static final Comparator<RankedApplicant> ORDER =
            Comparator.comparingInt(RankedApplicant::matchPercent).reversed()
                    .thenComparing(Comparator.comparingDouble(RankedApplicant::experience).reversed())
                    .thenComparingLong(RankedApplicant::appliedAtMillis);

    public static List<RankedApplicant> rank(List<RankedApplicant> applicants) {
        List<RankedApplicant> sorted = new ArrayList<>(applicants);
        sorted.sort(ORDER);
        return sorted;
    }

    public static List<RankedApplicant> aboveCutoff(List<RankedApplicant> applicants, int cutoff) {
        List<RankedApplicant> out = new ArrayList<>();
        for (RankedApplicant a : applicants) {
            if (a.matchPercent() >= cutoff) out.add(a);
        }
        return out;
    }

    /** The first n applicants of an already ranked list that are still in the Applied stage. */
    public static List<RankedApplicant> topToShortlist(List<RankedApplicant> ranked, int n) {
        List<RankedApplicant> out = new ArrayList<>();
        int limit = Math.min(Math.max(n, 0), ranked.size());
        for (int i = 0; i < limit; i++) {
            if ("Applied".equalsIgnoreCase(ranked.get(i).status())) out.add(ranked.get(i));
        }
        return out;
    }

    public static void writeCsv(Path file, List<RankedApplicant> ranked) throws IOException {
        if (file.getParent() != null) Files.createDirectories(file.getParent());
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write("Rank,Application ID,Candidate ID,Name,Skills,Experience (yrs),Match %,Missing Required,Status");
            w.newLine();
            int rank = 1;
            for (RankedApplicant a : ranked) {
                w.write(rank++ + "," + a.applicationId() + "," + a.candidateId() + "," + csv(a.name()) + ","
                        + csv(a.skills()) + "," + a.experience() + "," + a.matchPercent() + ","
                        + csv(String.join("; ", a.missingRequired())) + "," + csv(a.status()));
                w.newLine();
            }
        }
    }

    private static String csv(String s) {
        String v = s == null ? "" : s;
        return (v.contains(",") || v.contains("\"")) ? "\"" + v.replace("\"", "\"\"") + "\"" : v;
    }
}
