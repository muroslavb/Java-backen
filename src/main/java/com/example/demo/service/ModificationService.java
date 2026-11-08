package com.example.demo.service;

import com.example.demo.dto.ModificationDto;
import com.example.demo.entity.Modification;
import com.example.demo.mapper.ModificationMapper;
import com.example.demo.repository.ModificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ModificationService {

    private final ModificationRepository repository;
    private final ModificationMapper mapper;

    public List<ModificationDto> getAllMods() {
        return repository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    public ModificationDto getModById(Long id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new RuntimeException("Mod not found"));
    }

    @Transactional
    public ModificationDto createMod(ModificationDto dto) {
        Modification mod = mapper.toEntity(dto);
        return mapper.toDto(repository.save(mod));
    }

    @Transactional
    public void deleteMod(Long id) {

        //repository.deleteById(id);
    }
}