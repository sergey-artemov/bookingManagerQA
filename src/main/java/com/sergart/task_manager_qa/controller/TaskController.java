package com.sergart.task_manager_qa.controller;

import com.sergart.task_manager_qa.db.Task;
import com.sergart.task_manager_qa.repository.TaskRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskRepository taskRepository;

    public TaskController(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    // Получить все задачи
    @GetMapping
    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    // Создать задачу
    @PostMapping("/{new}")
    public ResponseEntity<Task> createTask(@Valid @RequestBody Task task) {
        Task saved = taskRepository.save(task);
        return ResponseEntity.status(201).body(saved);
    }

    // Получить одну задачу
    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(@PathVariable Long id) {
        return taskRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Обновить задачу
    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(@PathVariable Long id, @Valid @RequestBody Task task) {
        if (!taskRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        task.setId(id);
        Task updated = taskRepository.save(task);
        return ResponseEntity.ok(updated);
    }

    // Удалить задачу
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        if (!taskRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        taskRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}