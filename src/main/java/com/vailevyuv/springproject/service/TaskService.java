package com.vailevyuv.springproject.service;

import com.vailevyuv.springproject.dto.TaskRequest;
import com.vailevyuv.springproject.exception.TaskNotFoundException;
import com.vailevyuv.springproject.model.Task;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TaskService {
    private final ConcurrentHashMap<UUID, Task> tasks = new ConcurrentHashMap<>();
    
    public Task createTask(TaskRequest request) {
        validateDueDate(request.dueDate());
        
        Task task = new Task(
            UUID.randomUUID(),
            request.title(),
            request.description(),
            request.status(),
            LocalDateTime.now(),
            request.dueDate()
        );
        
        tasks.put(task.id(), task);
        return task;
    }
    
    public Optional<Task> getTaskById(UUID id) {
        return Optional.ofNullable(tasks.get(id));
    }
    
    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }
    
    public Task updateTask(UUID id, TaskRequest request) {
        validateDueDate(request.dueDate());
        
        Task existingTask = tasks.get(id);
        if (existingTask == null) {
            throw new TaskNotFoundException(id);
        }
        
        Task updatedTask = new Task(
            id,
            request.title(),
            request.description(),
            request.status(),
            existingTask.createdAt(),
            request.dueDate()
        );
        
        tasks.put(id, updatedTask);
        return updatedTask;
    }
    
    public void deleteTask(UUID id) {
        if (!tasks.containsKey(id)) {
            throw new TaskNotFoundException(id);
        }
        tasks.remove(id);
    }
    
    private void validateDueDate(LocalDateTime dueDate) {
        if (dueDate != null && dueDate.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Due date must be in the future");
        }
    }
}