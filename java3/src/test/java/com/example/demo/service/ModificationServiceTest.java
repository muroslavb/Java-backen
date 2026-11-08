package com.example.demo.service;

import com.example.demo.dto.ModificationDto;
import com.example.demo.entity.Modification;
import com.example.demo.mapper.ModificationMapper;
import com.example.demo.repository.ModificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModificationServiceTest {

    @Mock
    private ModificationRepository repository;

    @Mock
    private ModificationMapper mapper;

    @InjectMocks
    private ModificationService modificationService;

    // --- Тестування методу getAllMods() ---

    @Test
    void getAllMods_modsExist_returnsListOfDtos() {
        // Arrange (Given)
        Modification mod = new Modification();
        mod.setId(1L);
        ModificationDto dto = new ModificationDto();
        dto.setId(1L);

        when(repository.findAll()).thenReturn(List.of(mod));
        when(mapper.toDto(mod)).thenReturn(dto);

        // Act (When)
        List<ModificationDto> result = modificationService.getAllMods();

        // Assert (Then)
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        verify(repository, times(1)).findAll();
    }

    // --- Тестування методу getModById() ---

    // AC 1: Очікувані вхідні дані, реальні твердження
    @Test
    void getModById_existingId_returnsDto() {
        // Arrange
        Long id = 1L;
        Modification mod = new Modification();
        mod.setId(id);
        ModificationDto dto = new ModificationDto();
        dto.setId(id);

        when(repository.findById(id)).thenReturn(Optional.of(mod));
        when(mapper.toDto(mod)).thenReturn(dto);

        // Act
        ModificationDto result = modificationService.getModById(id);

        // Assert
        assertNotNull(result);
        assertEquals(id, result.getId());
    }

    // AC 2: Обробка невалідних даних / винятків
    @Test
    void getModById_nonExistingId_throwsException() {
        // Arrange
        Long id = 999L;
        when(repository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            modificationService.getModById(id);
        });

        assertEquals("Mod not found", exception.getMessage());
        verify(repository, times(1)).findById(id);
    }

    // --- Тестування методу createMod() ---

    // AC 7: Перевірка факту виклику та параметрів через ArgumentCaptor
    @Test
    void createMod_validDto_savesAndReturnsDto() {
        // Arrange
        ModificationDto inputDto = new ModificationDto();
        inputDto.setName("New Mod");

        Modification modToSave = new Modification();
        modToSave.setName("New Mod");

        Modification savedMod = new Modification();
        savedMod.setId(1L);
        savedMod.setName("New Mod");

        ModificationDto outputDto = new ModificationDto();
        outputDto.setId(1L);
        outputDto.setName("New Mod");

        when(mapper.toEntity(inputDto)).thenReturn(modToSave);
        when(repository.save(modToSave)).thenReturn(savedMod);
        when(mapper.toDto(savedMod)).thenReturn(outputDto);

        // Act
        ModificationDto result = modificationService.createMod(inputDto);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());

        // AC 7: Захоплюємо аргумент, щоб перевірити, що саме пішло в БД
        ArgumentCaptor<Modification> captor = ArgumentCaptor.forClass(Modification.class);
        verify(repository).save(captor.capture());
        assertEquals("New Mod", captor.getValue().getName());
    }

    // --- Тестування методу deleteMod() ---

    @Test
    void deleteMod_existingId_callsRepositoryDelete() {
        // Arrange
        Long id = 1L;

        // Act
        modificationService.deleteMod(id);

        // Assert (AC 7: перевіряємо факт виклику побічного ефекту)
        verify(repository, times(1)).deleteById(id);
    }
}