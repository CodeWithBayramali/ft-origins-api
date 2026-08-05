package com.codewithbayramali.ftorigins.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables @Scheduled tasks (the match tick — see MatchTickScheduler). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
