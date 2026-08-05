package com.codewithbayramali.ftorigins.moderation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Blocks profane / hateful text from user-chosen public names (team names now; player names, chat
 * later). A shared moderation concern, kept out of any one feature so it can be reused.
 *
 * <p>Matching is done on a CANONICAL form of the text, not the raw characters, so the obvious evasions
 * don't slip through: it lower-cases in Turkish locale, folds Turkish diacritics and common leetspeak
 * (0→o, 1→i, @→a, $→s ...), drops everything that isn't a letter/digit (so "s i k" and "s.i.k" collapse
 * to "sik"), and squashes repeated letters ("siiik" → "sik"). Blocked terms are canonicalised the same
 * way and matched as substrings.
 *
 * <p>The blocklist is loaded once from {@code classpath:moderation/blocked-terms.txt} (one term per
 * line, '#' comments, blanks ignored). If the file is absent the filter is INERT — it allows everything
 * and logs a warning — so a missing list never blocks registration; it just means no moderation until
 * the list is provided. Substring matching favours catching evasions over avoiding the odd false
 * positive (the "Scunthorpe problem"); curate the list with that in mind.
 */
@Component
public class ProfanityFilter {

    private static final Logger log = LoggerFactory.getLogger(ProfanityFilter.class);
    private static final Locale TR = new Locale("tr", "TR");
    private static final String RESOURCE = "moderation/blocked-terms.txt";

    private final Set<String> terms;

    public ProfanityFilter() {
        this.terms = load();
        if (terms.isEmpty()) {
            log.warn("No moderation blocklist at classpath:{} — profanity filtering is INERT.", RESOURCE);
        } else {
            log.info("Loaded {} moderation terms from classpath:{}.", terms.size(), RESOURCE);
        }
    }

    /** True when the text contains a blocked term (after canonicalisation). Blank text is never profane. */
    public boolean isProfane(String text) {
        if (text == null || text.isBlank() || terms.isEmpty()) {
            return false;
        }
        String canonical = canonical(text);
        if (canonical.isEmpty()) {
            return false;
        }
        for (String term : terms) {
            if (canonical.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> load() {
        Set<String> loaded = new HashSet<>();
        ClassPathResource resource = new ClassPathResource(RESOURCE);
        if (!resource.exists()) {
            return loaded;
        }
        try (InputStream in = resource.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String raw = line.trim();
                if (raw.isEmpty() || raw.startsWith("#")) {
                    continue;
                }
                String term = canonical(raw);
                if (!term.isEmpty()) {
                    loaded.add(term);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load moderation blocklist: {}", e.toString());
        }
        return loaded;
    }

    /** Fold to the comparison form: TR-lowercase, strip diacritics + leet, keep only letters/digits,
     *  then collapse runs of the same character so padded/stretched evasions still match. */
    private static String canonical(String text) {
        String lower = text.toLowerCase(TR);
        StringBuilder sb = new StringBuilder(lower.length());
        char last = 0;
        for (int i = 0; i < lower.length(); i++) {
            char mapped = fold(lower.charAt(i));
            if (!Character.isLetterOrDigit(mapped)) {
                continue;                       // spaces / punctuation: drop, don't reset the run tracker
            }
            if (mapped != last) {
                sb.append(mapped);
                last = mapped;
            }
        }
        return sb.toString();
    }

    private static char fold(char c) {
        return switch (c) {
            case 'ç' -> 'c';
            case 'ğ' -> 'g';
            case 'ı' -> 'i';
            case 'ö' -> 'o';
            case 'ş' -> 's';
            case 'ü' -> 'u';
            case '0' -> 'o';
            case '1' -> 'i';
            case '3' -> 'e';
            case '4' -> 'a';
            case '5' -> 's';
            case '7' -> 't';
            case '@' -> 'a';
            case '$' -> 's';
            default -> c;
        };
    }
}
