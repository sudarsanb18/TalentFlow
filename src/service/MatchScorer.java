package service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * MatchScorer computes an explainable weighted match percentage between a candidate and a job.
 * Pure functions only: no database access, so every rule can be unit tested directly.
 *
 * Formula:
 *   requiredScore  = matchedRequired / totalRequired
 *   preferredScore = matchedPreferred / totalPreferred
 *   base           = 0.70 * requiredScore + 0.30 * preferredScore   (when the job lists preferred skills)
 *                  = requiredScore                                    (when it does not)
 *   expFactor      = min(1, candidateExperience / minExperience)      (1 when minExperience is 0)
 *   percent        = round(base * expFactor * 100)
 *
 * Skills are comma separated and compared case-insensitively by exact token, so "Java" never matches "JavaScript".
 */
public final class MatchScorer {

    public static final double REQUIRED_WEIGHT = 0.70;
    public static final double PREFERRED_WEIGHT = 0.30;

    private MatchScorer() {
    }

    /** Outcome of one candidate-versus-job comparison. */
    public record Result(int percent, List<String> missingRequired, List<String> missingPreferred) {
    }

    /**
     * Splits a comma separated skill list into trimmed, non-empty, de-duplicated tokens (original spelling kept).
     */
    public static List<String> parseSkills(String csv) {
        Map<String, String> unique = new LinkedHashMap<>();
        if (csv != null) {
            for (String part : csv.split(",")) {
                String token = part.trim();
                if (!token.isEmpty()) {
                    unique.putIfAbsent(token.toLowerCase(Locale.ROOT), token);
                }
            }
        }
        return new ArrayList<>(unique.values());
    }

    public static Result score(String candidateSkills, double candidateExperience,
                               String requiredSkills, String preferredSkills, double minExperience) {
        List<String> have = parseSkills(candidateSkills);
        List<String> required = parseSkills(requiredSkills);
        List<String> preferred = parseSkills(preferredSkills);

        List<String> missingRequired = missing(required, have);
        List<String> missingPreferred = missing(preferred, have);

        double requiredScore = required.isEmpty() ? 1.0
                : (double) (required.size() - missingRequired.size()) / required.size();
        double base;
        if (preferred.isEmpty()) {
            base = requiredScore;
        } else {
            double preferredScore = (double) (preferred.size() - missingPreferred.size()) / preferred.size();
            base = REQUIRED_WEIGHT * requiredScore + PREFERRED_WEIGHT * preferredScore;
        }

        double expFactor = (minExperience <= 0.0) ? 1.0 : Math.min(1.0, Math.max(0.0, candidateExperience) / minExperience);
        int percent = (int) Math.round(base * expFactor * 100.0);
        percent = Math.max(0, Math.min(100, percent));
        return new Result(percent, missingRequired, missingPreferred);
    }

    private static List<String> missing(List<String> wanted, List<String> have) {
        List<String> haveLower = new ArrayList<>();
        for (String h : have) haveLower.add(h.toLowerCase(Locale.ROOT));
        List<String> out = new ArrayList<>();
        for (String w : wanted) {
            if (!haveLower.contains(w.toLowerCase(Locale.ROOT))) out.add(w);
        }
        return out;
    }
}
