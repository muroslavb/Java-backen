package com.example.demo.controller;

import com.example.demo.annotation.CreateApiEndpoint;
import com.example.demo.annotation.CurrentAuthor;
import com.example.demo.dto.ModReportDto;
import com.example.demo.dto.ModificationDto;
import com.example.demo.service.ModReportService;
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
    private final ModReportService reportService;

    @GetMapping
    public List<ModificationDto> getAll() {
        return service.getAllMods();
    }

    @GetMapping("/{id}")
    public ModificationDto getById(@PathVariable Long id) {
        return service.getModById(id);
    }

    @GetMapping("/{id}/export")
    public ModificationDto export(@PathVariable Long id) {
        return service.getModForExport(id);
    }

    @GetMapping("/{id}/report")
    public ModReportDto report(@PathVariable Long id) {
        return reportService.buildReport(id);
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