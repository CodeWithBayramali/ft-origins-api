-- Finer reverse-geocoded place labels (OSM/Nominatim): neighborhood (semt/mahalle) + district (ilçe).
-- country_code (ülke) and city_name (il/şehir) already exist on regions.
ALTER TABLE regions ADD COLUMN neighborhood VARCHAR(200);
ALTER TABLE regions ADD COLUMN district     VARCHAR(200);

-- Speeds up the proximity (bounding-box) search that assigns a player to a nearby existing team.
CREATE INDEX idx_regions_center ON regions (center_lat, center_lng);
