package com.example.demo.controller;

import com.example.demo.dto.ModificationDto;
import com.example.demo.service.ModificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class UiController {

    private final ModificationService service;

    // Читання (Read) - відображення сторінки зі списком
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("mods", service.getAllMods());
        model.addAttribute("newMod", new ModificationDto());
        return "index";
    }

    // Створення (Create) - обробка форми
    @PostMapping("/ui/mods")
    public String createMod(@ModelAttribute ModificationDto newMod) {
        newMod.setGameId(1L); // Для простоти жорстко прив'язуємо до першої гри
        newMod.setAuthorUsername("ui_user");
        service.createMod(newMod);
        return "redirect:/"; // Після додавання оновлюємо сторінку
    }

    // Видалення (Delete) - обробка кнопки
    @PostMapping("/ui/mods/delete/{id}")
    public String deleteMod(@PathVariable Long id) {
        service.deleteMod(id);
        return "redirect:/";
    }
}