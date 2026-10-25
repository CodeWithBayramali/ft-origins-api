package com.codewithbayramali.ftorigins.moderation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/** Runs against the real blocklist (classpath:moderation/blocked-terms.txt). */
class ProfanityFilterTest {

    private static final ProfanityFilter FILTER = new ProfanityFilter();

    @ParameterizedTest
    @ValueSource(strings = {
            // as written, in any case — the Turkish dotted and dotless I included
            "siktir", "SİKTİR", "SIKTIR", "Orospu", "pezevenk", "Şerefsiz", "fuck", "Bitch",
            // glued into a longer word
            "Siktirgit", "xOrospuX", "Superfuckers", "Ananısikeyim",
            // leetspeak
            "s1kt1r", "0r0spu", "@mk", "a$$hole", "sh1t", "b1tch",
            // spaced, dotted or spelled out
            "s i k t i r", "s.i.k", "S.İ.K", "sik_tir", "a m k", "oro spu", "F.U.C.K",
            // repeated letters
            "siiiiktir", "fuuuuck", "yarrrrrak", "amkkkk",
            // accents, full-width and math letters, a zero-width space
            "Fück", "ｆｕｃｋ", "𝐒𝐈𝐊𝐓𝐈𝐑", "si​ktir",
            // a short term as its own word in a longer name
            "Göt Spor", "Piç FK", "Aq Spor", "MK Gençlik", "Oç", "Puşt", "Taşak", "Sex Club", "Nazi Spor",
            "Heil Hitler",
            // a phrase over several words
            "Amına koyayım", "amınakoduğum", "Orospu çocuğu", "Dal yarak",
            // slurs written with a double letter
            "nigger", "N1GGA", "faggot"
    })
    void blocksProfanityAndItsDisguises(String name) {
        assertThat(FILTER.isProfane(name)).as(name).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            // Turkish words, places and names a short term hides in
            "Işık", "Işıkkent", "Işıkım", "Aşık", "Bisiklet Spor", "Musiki", "Camkıran", "Çamköy", "Kadıköy",
            "Adam", "Kayarak", "Sıkışık", "Eksik", "Kocaeli", "Ocak", "Gotik", "Göktürk", "Cumhuriyet", "Cuma",
            "Seksen", "Saksofon", "Nazilli", "Nazım", "Ahmet", "Kasımpaşa", "Malatya", "Pendikspor", "Bornovaspor",
            // names that only look like a term
            "Amina", "Nigar", "Niger", "Fagot",
            // English names and words a term hides in (the Scunthorpe problem)
            "Scunthorpe United", "Dickens", "Hitchcock", "Peacock", "Cockburn", "Essex", "Sussex", "Middlesex",
            "Therapist", "Yoshitaka", "Classic", "Gothic", "Spicy", "Raccoon", "Cocoon", "Cucumber", "Wankdorf",
            "Frankfurt am Main",
            // two innocent words whose seam spells a term
            "Işık Tiryaki", "Spor Nostalji", "Bakkal Takımı", "Okan Çıkrıkçı", "Omega Vatan", "Habib Neşet",
            "SK Trabzon",
            // initials are not a spelled-out two-letter word
            "M.K. Yılmaz", "A.Q.", "O.C. Spor",
            // ordinary team names
            "Caferağa FK", "Yıldızspor", "Kartal Gücü"
    })
    void allowsInnocentNames(String name) {
        assertThat(FILTER.isProfane(name)).as(name).isFalse();
    }

    @Test
    void blankTextIsNeverProfane() {
        assertThat(FILTER.isProfane(null)).isFalse();
        assertThat(FILTER.isProfane("")).isFalse();
        assertThat(FILTER.isProfane("   ")).isFalse();
    }
}
