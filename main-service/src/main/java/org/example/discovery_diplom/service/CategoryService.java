package org.example.discovery_diplom.service;

import org.example.discovery_diplom.dto.CategoryDto;
import org.example.discovery_diplom.dto.NewCategoryDto;
import org.example.discovery_diplom.exception.CategoryNotFoundException;
import org.example.discovery_diplom.model.Category;
import org.example.discovery_diplom.repository.CategoryRepository;
import org.example.discovery_diplom.repository.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;

    public CategoryService(CategoryRepository categoryRepository,
                           EventRepository eventRepository) {
        this.categoryRepository = categoryRepository;
        this.eventRepository = eventRepository;
    }

    public CategoryDto createCategory(NewCategoryDto newCategoryDto){
        validateName(newCategoryDto.getName());
        if (categoryRepository.existsByName(newCategoryDto.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Категория с таким именем уже существует");
        }

        Category category = new Category();
        category.setName(newCategoryDto.getName());

        Category savedCategory = categoryRepository.save(category);

        CategoryDto categoryDto = new CategoryDto();
        categoryDto.setId(savedCategory.getId());
        categoryDto.setName(savedCategory.getName());

        return categoryDto;
    }

    public CategoryDto updateCategory(Long categoryId, CategoryDto updateDto) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(
                        "Категория с id " + categoryId + " не найдена"));

        validateName(updateDto.getName());
        if (categoryRepository.existsByNameAndIdNot(updateDto.getName(), categoryId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Категория с таким именем уже существует");
        }

        category.setName(updateDto.getName());
        return toDto(categoryRepository.save(category));
    }

    public void deleteCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(
                        "Категория с id " + categoryId + " не найдена"));

        if (eventRepository.existsByCategory_Id(categoryId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Нельзя удалить категорию, к которой привязаны события");
        }

        categoryRepository.delete(category);
    }

    private void validateName(String name) {
        if (name == null || name.isBlank() || name.length() > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Название категории должно содержать от 1 до 50 символов");
        }
    }

    private CategoryDto toDto(Category category) {
        CategoryDto dto = new CategoryDto();
        dto.setId(category.getId());
        dto.setName(category.getName());
        return dto;
    }

    public List<CategoryDto> getAllCategories(){
        return getAllCategories(0, Integer.MAX_VALUE);
    }

    public List<CategoryDto> getAllCategories(int from, int size){
        if (from < 0 || size < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "from должен быть >= 0, size должен быть >= 1");
        }
        List<CategoryDto> categoryDtoList = new ArrayList<>();
        List<Category> categories = categoryRepository.findAll();

        int start = Math.min(from, categories.size());
        int end = (int) Math.min((long) from + size, categories.size());
        for (Category category : categories.subList(start, end)) {
            CategoryDto categoryDto = new CategoryDto();
            categoryDto.setId(category.getId());
            categoryDto.setName(category.getName());
            categoryDtoList.add(categoryDto);
        }

        return categoryDtoList;
    }

    public CategoryDto getCategoryById(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(
                        "Категория с id " + categoryId + " не найдена"));

        CategoryDto categoryDto = new CategoryDto();
        categoryDto.setId(category.getId());
        categoryDto.setName(category.getName());
        return categoryDto;
    }
}
