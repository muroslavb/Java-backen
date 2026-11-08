package com.example.demo.mapper;

import com.example.demo.dto.ModificationDto;
import com.example.demo.entity.Modification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ModificationMapper {

    @Mapping(source = "game.id", target = "gameId")
    ModificationDto toDto(Modification modification);

    @Mapping(source = "gameId", target = "game.id")
    Modification toEntity(ModificationDto dto);
}