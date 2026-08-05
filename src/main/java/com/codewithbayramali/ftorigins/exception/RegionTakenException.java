package com.codewithbayramali.ftorigins.exception;

import org.springframework.http.HttpStatus;

/**
 * The pinned registration location already falls inside an existing team's join radius and the
 * caller did not consent to joining it (see {@code RegisterRequest.joinExisting}). Carries that team
 * as the error {@code details} so the client can show a "Join / choose another location" popup
 * instead of silently adding the player to the team its founder started.
 */
public class RegionTakenException extends BusinessException {

    public RegionTakenException(Object team) {
        super(HttpStatus.CONFLICT, "REGION_TAKEN",
                "This location already belongs to a team. Join it or choose another location.", team);
    }
}
