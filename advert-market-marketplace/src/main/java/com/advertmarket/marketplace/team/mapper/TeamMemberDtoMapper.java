package com.advertmarket.marketplace.team.mapper;

import com.advertmarket.marketplace.api.dto.TeamMemberDto;
import com.advertmarket.marketplace.api.model.ChannelMembershipRole;
import com.advertmarket.marketplace.api.model.ChannelRight;
import com.advertmarket.shared.json.JsonFacade;
import java.util.Set;
import org.jooq.JSON;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for {@link TeamMemberRow} to {@link TeamMemberDto}.
 */
@Mapper(componentModel = "spring")
public interface TeamMemberDtoMapper {

    /** Maps row projection to API DTO. */
    @Mapping(target = "role", source = "role")
    @Mapping(target = "rights", source = "rights")
    TeamMemberDto toDto(TeamMemberRow row,
                        @Context JsonFacade json);

    /**
     * Converts stored role code to {@link ChannelMembershipRole}.
     */
    default ChannelMembershipRole toRole(String role) {
        return ChannelMembershipRole.valueOf(role);
    }

    /**
     * Parses rights JSON into a set of {@link ChannelRight} values.
     */
    default Set<ChannelRight> toRights(
            JSON rightsJson,
            @Context JsonFacade json) {
        return ChannelRightsJsonConverter.parseRights(
                rightsJson, json);
    }
}
