package com.example.demo.controller;

import com.example.demo.dto.ModificationDto;
import com.example.demo.service.ModificationService;
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

    @PostMapping
    @PreAuthorize("hasRole('AUTHOR')") // Метод заблокується, якщо у юзера немає цієї ролі
    public ModificationDto create(@RequestBody ModificationDto dto) {
        return service.createMod(dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.deleteMod(id);
    }
}