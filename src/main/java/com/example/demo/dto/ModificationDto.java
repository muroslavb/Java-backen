package com.example.demo.dto;

import com.example.demo.annotation.ValidModVersion;
import lombok.Data;

@Data
public class ModificationDto {
    private Long id;
    private Long gameId;
    private String name;
    private String description;
    @ValidModVersion
    private String version;
    private String authorUsername;
}