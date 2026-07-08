package com.codewithbayramali.ftorigins.geo.service;

import com.codewithbayramali.ftorigins.geo.model.Country;
import com.codewithbayramali.ftorigins.geo.repository.CountryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads GeoNames "countryInfo" into the countries table on startup.
 * Source: https://download.geonames.org/export/dump/countryInfo.txt
 * Tab-separated; comment lines start with '#'. Columns used: 0 ISO code, 4 country name.
 *
 * Data file resolution order:
 *   1. app.geo.countries-file (filesystem path), if set and present;
 *   2. classpath:geo/countryInfo.txt, if bundled.
 * Skips gracefully when absent. Idempotent: skips when the table is already populated.
 */
@Component
@Order(1)
public class CountryDataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CountryDataLoader.class);

    private final CountryRepository countryRepository;
    private final String countriesFilePath;

    public CountryDataLoader(CountryRepository countryRepository,
                             @Value("${app.geo.countries-file:}") String countriesFilePath) {
        this.countryRepository = countryRepository;
        this.countriesFilePath = countriesFilePath;
    }

    @Override
    public void run(String... args) throws Exception {
        long existing = countryRepository.count();
        if (existing > 0) {
            log.info("Countries already loaded ({} rows) — skipping GeoNames import.", existing);
            return;
        }
        InputStream source = openSource();
        if (source == null) {
            log.warn("No GeoNames countryInfo file found (set app.geo.countries-file or bundle "
                    + "classpath:geo/countryInfo.txt). Countries table will stay empty.");
            return;
        }
        long loaded = importFrom(source);
        log.info("Loaded {} countries from GeoNames.", loaded);
    }

    private InputStream openSource() throws Exception {
        if (countriesFilePath != null && !countriesFilePath.isBlank()) {
            Path path = Path.of(countriesFilePath);
            if (Files.exists(path)) {
                log.info("Loading countries from file: {}", path.toAbsolutePath());
                return Files.newInputStream(path);
            }
            log.warn("Configured app.geo.countries-file does not exist: {}", countriesFilePath);
        }
        ClassPathResource classpath = new ClassPathResource("geo/countryInfo.txt");
        if (classpath.exists()) {
            log.info("Loading countries from classpath:geo/countryInfo.txt");
            return classpath.getInputStream();
        }
        return null;
    }

    private long importFrom(InputStream source) throws Exception {
        List<Country> batch = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(source, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.charAt(0) == '#') {
                    continue;
                }
                String[] f = line.split("\t", -1);
                if (f.length < 5 || f[0].isBlank() || f[4].isBlank()) {
                    continue;
                }
                Country country = new Country();
                country.setCode(f[0].trim());
                country.setName(f[4].trim());
                batch.add(country);
            }
        }
        countryRepository.saveAll(batch);
        return batch.size();
    }
}
