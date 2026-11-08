package com.example.demo.controller;

import com.example.demo.annotation.CreateApiEndpoint;
import com.example.demo.annotation.CurrentAuthor;
import com.example.demo.dto.ModificationDto;
import com.example.demo.service.ModificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mods")
@RequiredArgsConstructor
public class ModificationController {

    private final ModificationService service;

    @GetMapping
    public List<ModificationDto> getAll() {
        return service.getAllMods();
    }

    @GetMapping("/{id}")
    public ModificationDto getById(@PathVariable Long id) {
        return service.getModById(id);
    }

    @CreateApiEndpoint
    @PreAuthorize("hasRole('AUTHOR')")
    public ModificationDto create(
            @RequestBody @Valid ModificationDto dto,
            @CurrentAuthor String author) {

        dto.setAuthorUsername(author);
        return service.createMod(dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.deleteMod(id);
    }
}