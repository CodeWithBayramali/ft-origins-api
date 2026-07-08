package com.codewithbayramali.ftorigins.geo.service;

import com.codewithbayramali.ftorigins.geo.model.City;
import com.codewithbayramali.ftorigins.geo.repository.CityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
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
 * Loads GeoNames "cities1000" into the cities table on startup.
 * Source: https://download.geonames.org/export/dump/cities1000.zip (extract cities1000.txt).
 * Tab-separated; columns used: 0 geonameid, 1 name, 4 lat, 5 lng, 8 country, 10 admin1 code, 14 population.
 *
 * Data file resolution order:
 *   1. app.geo.cities-file (filesystem path), if set and present;
 *   2. classpath:geo/cities1000.txt, if bundled.
 * If neither exists the loader logs a warning and skips — the app still boots
 * (region city/country labels just stay empty until data is provided).
 * Idempotent: skips when the cities table is already populated.
 */
@Component
public class CityDataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CityDataLoader.class);
    private static final int BATCH_SIZE = 2000;

    private final CityRepository cityRepository;
    private final GeoService geoService;
    private final String citiesFilePath;

    public CityDataLoader(CityRepository cityRepository,
                          GeoService geoService,
                          @Value("${app.geo.cities-file:}") String citiesFilePath) {
        this.cityRepository = cityRepository;
        this.geoService = geoService;
        this.citiesFilePath = citiesFilePath;
    }

    @Override
    public void run(String... args) throws Exception {
        long existing = cityRepository.count();
        if (existing > 0) {
            log.info("Cities already loaded ({} rows) — skipping GeoNames import.", existing);
            return;
        }
        InputStream source = openSource();
        if (source == null) {
            log.warn("No GeoNames cities file found (set app.geo.cities-file or bundle "
                    + "classpath:geo/cities1000.txt). Region city/country labels will stay empty.");
            return;
        }
        long loaded = importFrom(source);
        geoService.invalidateCityCache();
        log.info("Loaded {} cities from GeoNames.", loaded);
    }

    private InputStream openSource() throws Exception {
        if (citiesFilePath != null && !citiesFilePath.isBlank()) {
            Path path = Path.of(citiesFilePath);
            if (Files.exists(path)) {
                log.info("Loading cities from file: {}", path.toAbsolutePath());
                return Files.newInputStream(path);
            }
            log.warn("Configured app.geo.cities-file does not exist: {}", citiesFilePath);
        }
        ClassPathResource classpath = new ClassPathResource("geo/cities1000.txt");
        if (classpath.exists()) {
            log.info("Loading cities from classpath:geo/cities1000.txt");
            return classpath.getInputStream();
        }
        return null;
    }

    private long importFrom(InputStream source) throws Exception {
        long count = 0;
        List<City> batch = new ArrayList<>(BATCH_SIZE);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(source, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                City city = parse(line);
                if (city == null) {
                    continue;
                }
                batch.add(city);
                if (batch.size() >= BATCH_SIZE) {
                    cityRepository.saveAll(batch);
                    count += batch.size();
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                cityRepository.saveAll(batch);
                count += batch.size();
            }
        }
        return count;
    }

    private City parse(String line) {
        String[] f = line.split("\t", -1);
        if (f.length < 15) {
            return null;
        }
        try {
            City city = new City();
            city.setGeonameId(Long.parseLong(f[0].trim()));
            city.setName(f[1].trim());
            city.setLatitude(Double.parseDouble(f[4].trim()));
            city.setLongitude(Double.parseDouble(f[5].trim()));
            city.setCountryCode(f[8].trim());
            city.setAdmin1Code(f[10].isBlank() ? null : f[10].trim());
            city.setPopulation(f[14].isBlank() ? 0L : Long.parseLong(f[14].trim()));
            return city;
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
