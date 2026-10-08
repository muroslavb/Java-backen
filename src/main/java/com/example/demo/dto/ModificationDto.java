package com.example.demo.dto;

import lombok.Data;

@Data
public class ModificationDto {
    private Long id;
    private Long gameId;
    private String name;
    private String description;
    private String version;
    private String authorUsername;
}