package com.codewithbayramali.ftorigins.moderation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Blocks profane / hateful text from user-chosen public names: team names, usernames (the player's name
 * on the pitch) and display names. A shared moderation concern, kept out of any one feature.
 *
 * <p>Matching runs on a CANONICAL form of the text, so the obvious evasions don't slip through: it is
 * lower-cased in Turkish locale, accents and invisible characters are stripped (ş → s, full-width and
 * zero-width tricks), leetspeak is folded (0→o, 1→i, @→a, $→s ...) and repeated letters are squashed
 * ("siiik" → "sik"). Anything that is not a letter splits the text into words.
 *
 * <p>The blocklist ({@code classpath:moderation/blocked-terms.txt}) has two kinds of term:
 * <ul>
 *   <li><b>roots</b> — unambiguous stems, found inside a word ("Siktirgit", "xorospux") or spread over the
 *       words that follow the start of one ("s i k t i r", "oro spu"). A root that only shows up across the
 *       seam of two innocent words does not count: "Işık Tiryaki" is not "siktir", "Spor Nostalji" not
 *       "porno".</li>
 *   <li><b>words</b> (a line starting with {@code =}) — short or ambiguous terms, matched only as a whole
 *       word, because inside other words they are innocent: "sik" is in Işık and bisiklet, "cock" in
 *       Hitchcock, "sex" in Essex. One of three letters or more is also caught spelled out over several
 *       words ("s.i.k", "a m k"); a two-letter one is not, as that is how initials look ("M.K.").</li>
 * </ul>
 * A term written with a double letter ("yarrak", "nigger") is compared with runs kept at two, so it does
 * not also block the single-letter word next to it ("Niger"); every other term with runs squashed to one.
 *
 * <p>The list is read once at startup. If the file is absent the filter is INERT — it allows everything
 * and logs a warning — so a missing list never blocks registration.
 */
@Component
public class ProfanityFilter {

    private static final Logger log = LoggerFactory.getLogger(ProfanityFilter.class);
    private static final Locale TR = Locale.forLanguageTag("tr-TR");
    private static final String RESOURCE = "moderation/blocked-terms.txt";
    /** Shortest word term also caught spelled out over several words; two letters look like initials. */
    private static final int SPELLED_MIN = 3;

    /** One blocked term in its comparison form: a root or a whole word, with runs of a letter kept at one
     *  or at two. */
    private record Term(String form, boolean word, int run) {
    }

    private final List<Term> terms;

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
        List<String> words = words(fold(text));
        Reading[] readings = { new Reading(words, 1), new Reading(words, 2) };
        for (Term term : terms) {
            Reading reading = readings[term.run() - 1];
            if (term.word() ? reading.hasWord(term.form()) : reading.hasRoot(term.form())) {
                return true;
            }
        }
        return false;
    }

    private List<Term> load() {
        List<Term> loaded = new ArrayList<>();
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
                boolean word = raw.startsWith("=");
                String letters = String.join("", words(fold(word ? raw.substring(1) : raw)));
                if (letters.isEmpty()) {
                    continue;
                }
                int run = hasDoubleLetter(letters) ? 2 : 1;
                loaded.add(new Term(squash(letters, run), word, run));
            }
        } catch (Exception e) {
            log.warn("Failed to load moderation blocklist: {}", e.toString());
        }
        return loaded;
    }

    /** TR-lowercase, accents and invisible characters stripped, leetspeak to letters. Everything else is
     *  kept, so it can still split words. */
    private static String fold(String text) {
        // Decomposed BEFORE lower-casing: a math or styled letter ("𝐒") has no lower case of its own, only
        // the plain letter it decomposes to does.
        String decomposed = Normalizer.normalize(text, Normalizer.Form.NFKD).toLowerCase(TR);
        StringBuilder sb = new StringBuilder(decomposed.length());
        for (int i = 0; i < decomposed.length(); i++) {
            char c = decomposed.charAt(i);
            switch (Character.getType(c)) {
                // An accent left by the decomposition (ş → s + cedilla) or a zero-width character: dropped,
                // so it neither stays on the letter nor splits the word.
                case Character.NON_SPACING_MARK, Character.ENCLOSING_MARK, Character.COMBINING_SPACING_MARK,
                     Character.FORMAT -> { }
                default -> sb.append(fold(c));
            }
        }
        return sb.toString();
    }

    private static char fold(char c) {
        return switch (c) {
            case 'ı' -> 'i';   // the dotless i has no decomposition, and TR-lowercasing makes every "I" one
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

    /** Runs of letters; anything else (space, punctuation, a digit that is not leetspeak) ends a word. */
    private static List<String> words(String folded) {
        List<String> words = new ArrayList<>();
        StringBuilder word = new StringBuilder();
        for (int i = 0; i <= folded.length(); i++) {
            if (i < folded.length() && Character.isLetter(folded.charAt(i))) {
                word.append(folded.charAt(i));
            } else if (!word.isEmpty()) {
                words.add(word.toString());
                word.setLength(0);
            }
        }
        return words;
    }

    /** Every run of one letter cut to at most `run` ("siiik" → "sik" at 1, "yarrrak" → "yarrak" at 2). */
    private static String squash(String letters, int run) {
        StringBuilder sb = new StringBuilder(letters.length());
        int same = 0;
        char last = 0;
        for (int i = 0; i < letters.length(); i++) {
            char c = letters.charAt(i);
            same = c == last ? same + 1 : 1;
            last = c;
            if (same <= run) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static boolean hasDoubleLetter(String letters) {
        for (int i = 1; i < letters.length(); i++) {
            if (letters.charAt(i) == letters.charAt(i - 1)) {
                return true;
            }
        }
        return false;
    }

    /** The text as words, with every run of a letter cut to the same length (one or two). */
    private static final class Reading {

        private final List<String> words;

        Reading(List<String> words, int run) {
            this.words = words.stream().map(w -> squash(w, run)).toList();
        }

        /** Inside a word, or from the start of one on into the words after it. */
        boolean hasRoot(String root) {
            for (int i = 0; i < words.size(); i++) {
                if (words.get(i).contains(root) || joinedFrom(i, root.length()).startsWith(root)) {
                    return true;
                }
            }
            return false;
        }

        /** A whole word, or whole words spelling it out when it is too long to be initials. */
        boolean hasWord(String word) {
            for (int i = 0; i < words.size(); i++) {
                String candidate = word.length() >= SPELLED_MIN ? joinedFrom(i, word.length()) : words.get(i);
                if (candidate.equals(word)) {
                    return true;
                }
            }
            return false;
        }

        /** The words from the i-th on, joined until they are at least `length` letters long. */
        private String joinedFrom(int i, int length) {
            StringBuilder sb = new StringBuilder(words.get(i));
            for (int j = i + 1; j < words.size() && sb.length() < length; j++) {
                sb.append(words.get(j));
            }
            return sb.toString();
        }
    }
}
