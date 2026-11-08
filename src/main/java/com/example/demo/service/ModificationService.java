package com.example.demo.service;

import com.example.demo.annotation.MaskAuthor;
import com.example.demo.annotation.RateLimited;
import com.example.demo.annotation.RetryOnFailure;
import com.example.demo.async.NotificationAsyncService;
import com.example.demo.dto.ModificationDto;
import com.example.demo.entity.Modification;
import com.example.demo.mapper.ModificationMapper;
import com.example.demo.repository.ModificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModificationService {

    private final ModificationRepository repository;
    private final ModificationMapper mapper;
    private final NotificationAsyncService notificationService;

    @MaskAuthor
    @RetryOnFailure
    public List<ModificationDto> getAllMods() {
        return repository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    @MaskAuthor
    @RetryOnFailure
    public ModificationDto getModById(Long id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new RuntimeException("Mod not found"));
    }

    @RateLimited(maxCalls = 5, windowSeconds = 60)
    @Transactional
    public ModificationDto createMod(ModificationDto dto) {
        Modification mod = mapper.toEntity(dto);
        ModificationDto created = mapper.toDto(repository.save(mod));
        notifyCreated(created);
        return created;
    }

    private void notifyCreated(ModificationDto created) {
        try {
            notificationService.sendModCreatedEmail(created);
        } catch (TaskRejectedException ex) {
            log.warn("Лист про створення моду {} пропущено: виконавець переповнений", created.getId());
        }
    }

    public ModificationDto getModForExport(Long id) {
        return this.getModById(id);
    }

    @Transactional
    public void deleteMod(Long id) {

        //repository.deleteById(id);
    }
}