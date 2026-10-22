package org.example.discovery_diplom.controller;

import org.example.discovery_diplom.dto.CompilationDto;
import org.example.discovery_diplom.dto.NewCompilationDto;
import org.example.discovery_diplom.dto.UpdateCompilationRequest;
import org.example.discovery_diplom.service.CompilationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/compilations")
public class AdminCompilationController {

    private final CompilationService compilationService;

    public AdminCompilationController(CompilationService compilationService) {
        this.compilationService = compilationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompilationDto createCompilation(@RequestBody NewCompilationDto newDto) {
        return compilationService.createCompilation(newDto);
    }

    @PatchMapping("/{compilationId}")
    public CompilationDto updateCompilation(@PathVariable Long compilationId,
                                            @RequestBody UpdateCompilationRequest updateRequest) {
        return compilationService.updateCompilation(compilationId, updateRequest);
    }

    @DeleteMapping("/{compilationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCompilation(@PathVariable Long compilationId) {
        compilationService.deleteCompilation(compilationId);
    }
}
