package com.codewithbayramali.ftorigins.geo.service;

import com.codewithbayramali.ftorigins.exception.BusinessException;
import com.codewithbayramali.ftorigins.geo.dto.CrestDto;
import com.codewithbayramali.ftorigins.geo.model.Region;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * The one way a crest gets onto a team — shared by founding (registration) and the founder's later
 * changes. The id ranges are bean-validated on {@link CrestDto}, which holds the catalog sizes; this adds
 * the rule annotations can't express: the symbol is drawn in the detail colour ON the base colour, so the
 * two must differ.
 */
@Component
public class TeamCrestPolicy {

    /** Writes a bean-validated crest onto the team; 400 CREST_COLOURS_SAME when base == detail. */
    public void apply(Region region, CrestDto crest) {
        if (crest.getBase().equals(crest.getDetail())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "CREST_COLOURS_SAME",
                    "Crest base and detail colours must differ");
        }
        region.setCrestShape(crest.getShape());
        region.setCrestSymbol(crest.getSymbol());
        region.setCrestBase(crest.getBase());
        region.setCrestDetail(crest.getDetail());
    }
}
