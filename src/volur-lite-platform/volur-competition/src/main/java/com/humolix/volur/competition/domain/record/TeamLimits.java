package com.humolix.volur.competition.domain.record;

import com.humolix.volur.competition.domain.entity.CompetitionConfig;

public record TeamLimits(int minMembers, int maxMembers, int maxLeaders) {
    public static TeamLimits from(CompetitionConfig config) {
        return new TeamLimits (
                config != null && config.getMinTeamMembers() != null ? config.getMinTeamMembers() : 1,
                config != null && config.getMaxTeamMembers() != null ? config.getMaxTeamMembers() : 4,
                config != null && config.getMaxLeaders() != null ? config.getMaxLeaders() : 1
        );
    }
}
