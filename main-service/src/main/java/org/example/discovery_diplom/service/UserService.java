package org.example.discovery_diplom.service;

import org.example.discovery_diplom.dto.NewUserDto;
import org.example.discovery_diplom.dto.UserDto;
import org.example.discovery_diplom.exception.UserNotFoundException;
import org.example.discovery_diplom.model.User;
import org.example.discovery_diplom.repository.CommentRepository;
import org.example.discovery_diplom.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final CommentRepository commentRepository;

    public UserService(UserRepository userRepository, CommentRepository commentRepository) {
        this.userRepository = userRepository;
        this.commentRepository = commentRepository;
    }

    public UserDto createUser(NewUserDto newUserDto) {
        if (newUserDto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Тело запроса обязательно");
        }

        validateName(newUserDto.getName());
        validateEmail(newUserDto.getEmail());
        if (userRepository.existsByEmail(newUserDto.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Пользователь с таким email уже существует");
        }

        User user = new User();
        user.setEmail(newUserDto.getEmail());
        user.setName(newUserDto.getName());

        User savedUser = userRepository.save(user);

        return toDto(savedUser);
    }

    private void validateName(String name) {
        if (name == null || name.isBlank() || name.length() < 2 || name.length() > 250) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Имя должно содержать от 2 до 250 символов");
        }
    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank() || email.length() < 6 || email.length() > 254) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Email должен содержать от 6 до 254 символов");
        }

        int at = email.indexOf('@');
        if (at <= 0 || at != email.lastIndexOf('@') || at > 64 || at == email.length() - 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Некорректный email");
        }

        String localPart = email.substring(0, at);
        String domain = email.substring(at + 1);
        if (localPart.startsWith(".") || localPart.endsWith(".") || localPart.contains("..")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Некорректный email");
        }

        String[] domainParts = domain.split("\\.", -1);
        if (domainParts.length < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Некорректный email");
        }
        for (String part : domainParts) {
            if (part.isEmpty() || part.length() > 63 || part.startsWith("-") || part.endsWith("-")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Некорректный email");
            }
            for (int i = 0; i < part.length(); i++) {
                char ch = part.charAt(i);
                if (!Character.isLetterOrDigit(ch) && ch != '-') {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Некорректный email");
                }
            }
        }
    }

    public List<UserDto> getUsers(List<Long> ids, int from, int size) {
        if (from < 0 || size <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Параметры from и size должны задавать корректную страницу");
        }

        Pageable pageable = new OffsetPageRequest(from, size, Sort.by("id").ascending());
        Page<User> users = ids == null || ids.isEmpty()
                ? userRepository.findAll(pageable)
                : userRepository.findByIdIn(ids, pageable);

        return users.map(this::toDto).getContent();
    }

    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(
                        "Пользователь с id=" + userId + " не найден"));
        commentRepository.deleteAllByAuthor_Id(userId);
        userRepository.delete(user);
    }

    private UserDto toDto(User user) {
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setEmail(user.getEmail());
        userDto.setName(user.getName());
        return userDto;
    }
}
