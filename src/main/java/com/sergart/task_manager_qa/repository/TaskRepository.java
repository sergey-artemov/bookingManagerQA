package com.sergart.task_manager_qa.repository;

import com.sergart.task_manager_qa.db.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByCompleted(boolean completed);
}