package com.codewithbayramali.ftorigins.geo.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Generates a random default team name for a freshly created region.
 * Members can rename the team once it fills up (handled later, in match-making).
 */
@Component
public class RegionNameGenerator {

    private static final String[] ADJECTIVES = {
            "Mavi", "Kırmızı", "Yıldız", "Şimşek", "Fırtına", "Yıldırım", "Gece",
            "Altın", "Gümüş", "Vahşi", "Cesur", "Hızlı", "Çelik", "Ateş", "Zafer",
            "Kartal", "Aslan", "Şahin", "Bozkurt", "Ejder"
    };

    private static final String[] NOUNS = {
            "Şimşekler", "Kartallar", "Aslanlar", "Şahinler", "Kurtlar", "Yıldızlar",
            "Spor", "Gücü", "Birliği", "United", "Gençlik", "Fatihler", "Akıncılar",
            "Savaşçılar", "Yıldırımlar", "Ejderler", "Boğalar", "Atmacalar"
    };

    public String generate(String h3Index) {
        String adjective = ADJECTIVES[ThreadLocalRandom.current().nextInt(ADJECTIVES.length)];
        String noun = NOUNS[ThreadLocalRandom.current().nextInt(NOUNS.length)];
        return adjective + " " + noun + " #" + shortSuffix(h3Index);
    }

    private String shortSuffix(String h3Index) {
        if (h3Index == null || h3Index.isBlank()) {
            return Integer.toHexString(ThreadLocalRandom.current().nextInt(0x1000)).toUpperCase();
        }
        String tail = h3Index.length() >= 4 ? h3Index.substring(h3Index.length() - 4) : h3Index;
        return tail.toUpperCase();
    }
}
