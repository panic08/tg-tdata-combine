package ru.marthastudios.combine.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.marthastudios.combine.payload.CheckUnloadOnExistResponse;
import ru.marthastudios.combine.service.UnloadService;

@RestController
@RequestMapping("/api/v1/unload")
@RequiredArgsConstructor
public class UnloadController {
    private final UnloadService unloadService;

    @GetMapping("/{workerId}/exist")
    public CheckUnloadOnExistResponse unloadExist(@PathVariable("workerId") String workerId) {
        return unloadService.checkOnExist(workerId);
    }

    @PostMapping
    public void create(@RequestParam("workerId") String workerId, @RequestParam("file") MultipartFile multipartFile) {
        unloadService.create(workerId, multipartFile);
    }
}
