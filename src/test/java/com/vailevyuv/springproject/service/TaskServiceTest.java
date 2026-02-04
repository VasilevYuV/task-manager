package com.vailevyuv.springproject.service;

import com.vailevyuv.springproject.dto.TaskRequest;
import com.vailevyuv.springproject.exception.TaskNotFoundException;
import com.vailevyuv.springproject.model.Task;
import com.vailevyuv.springproject.model.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskService = new TaskService();
    }

    @Test
    void createTask_WithValidRequest_ShouldCreateTask() {
        TaskRequest request = new TaskRequest(
                "Test Task",
                "Test Description",
                TaskStatus.TODO,
                LocalDateTime.now().plusDays(1)
        );

        Task task = taskService.createTask(request);

        assertNotNull(task);
        assertNotNull(task.id());
        assertEquals("Test Task", task.title());
        assertEquals(TaskStatus.TODO, task.status());
        assertNotNull(task.createdAt());
    }

    @Test
    void getTaskById_WithExistingId_ShouldReturnTask() {
        TaskRequest request = new TaskRequest(
                "Test Task",
                "Test Description",
                TaskStatus.TODO,
                null
        );

        Task createdTask = taskService.createTask(request);
        var foundTask = taskService.getTaskById(createdTask.id());

        assertTrue(foundTask.isPresent());
        assertEquals(createdTask.id(), foundTask.get().id());
    }

    @Test
    void getTaskById_WithNonExistingId_ShouldReturnEmpty() {
        UUID nonExistingId = UUID.randomUUID();
        var result = taskService.getTaskById(nonExistingId);

        assertTrue(result.isEmpty());
    }

    @Test
    void getAllTasks_ShouldReturnAllTasks() {
        taskService.createTask(new TaskRequest("Task 1", null, TaskStatus.TODO, null));
        taskService.createTask(new TaskRequest("Task 2", null, TaskStatus.IN_PROGRESS, null));

        List<Task> tasks = taskService.getAllTasks();

        assertEquals(2, tasks.size());
    }

    @Test
    void updateTask_WithExistingId_ShouldUpdateTask() {
        TaskRequest createRequest = new TaskRequest(
                "Original Title",
                "Original Description",
                TaskStatus.TODO,
                null
        );

        Task createdTask = taskService.createTask(createRequest);
        LocalDateTime originalCreatedAt = createdTask.createdAt();

        TaskRequest updateRequest = new TaskRequest(
                "Updated Title",
                "Updated Description",
                TaskStatus.IN_PROGRESS,
                LocalDateTime.now().plusDays(2)
        );

        Task updatedTask = taskService.updateTask(createdTask.id(), updateRequest);

        assertEquals("Updated Title", updatedTask.title());
        assertEquals(TaskStatus.IN_PROGRESS, updatedTask.status());
        assertEquals(originalCreatedAt, updatedTask.createdAt());
    }

    @Test
    void updateTask_WithNonExistingId_ShouldThrowException() {
        UUID nonExistingId = UUID.randomUUID();
        TaskRequest request = new TaskRequest(
                "Test",
                null,
                TaskStatus.TODO,
                null
        );

        assertThrows(TaskNotFoundException.class,
                () -> taskService.updateTask(nonExistingId, request));
    }

    @Test
    void deleteTask_WithExistingId_ShouldDeleteTask() {
        TaskRequest request = new TaskRequest(
                "Task to delete",
                null,
                TaskStatus.TODO,
                null
        );

        Task task = taskService.createTask(request);
        taskService.deleteTask(task.id());

        var result = taskService.getTaskById(task.id());
        assertTrue(result.isEmpty());
    }

    @Test
    void deleteTask_WithNonExistingId_ShouldThrowException() {
        UUID nonExistingId = UUID.randomUUID();

        assertThrows(TaskNotFoundException.class,
                () -> taskService.deleteTask(nonExistingId));
    }

    @Test
    void createTask_WithPastDueDate_ShouldThrowException() {
        TaskRequest request = new TaskRequest(
                "Test Task",
                "Test Description",
                TaskStatus.TODO,
                LocalDateTime.now().minusDays(1)
        );

        assertThrows(IllegalArgumentException.class,
                () -> taskService.createTask(request));
    }

    @Test
    void updateTask_ShouldPreserveCreatedAt() {
        TaskRequest createRequest = new TaskRequest(
                "Original",
                null,
                TaskStatus.TODO,
                null
        );

        Task createdTask = taskService.createTask(createRequest);
        LocalDateTime originalCreatedAt = createdTask.createdAt();

        TaskRequest updateRequest = new TaskRequest(
                "Updated",
                "New Description",
                TaskStatus.DONE,
                LocalDateTime.now().plusDays(3)
        );

        Task updatedTask = taskService.updateTask(createdTask.id(), updateRequest);

        assertEquals(originalCreatedAt, updatedTask.createdAt());
    }
}