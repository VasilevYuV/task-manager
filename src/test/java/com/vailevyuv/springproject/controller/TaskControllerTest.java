package com.vailevyuv.springproject.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vailevyuv.springproject.dto.TaskRequest;
import com.vailevyuv.springproject.model.TaskStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createTask_WithValidData_ShouldReturn201() throws Exception {
        TaskRequest request = new TaskRequest(
                "Новая задача",
                "Описание задачи",
                TaskStatus.TODO,
                LocalDateTime.now().plusDays(1)
        );

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Новая задача"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void createTask_WithEmptyTitle_ShouldReturn400() throws Exception {
        TaskRequest request = new TaskRequest("", null, TaskStatus.TODO, null);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("title"))
                .andExpect(jsonPath("$.errors[0].message").exists());
    }

    @Test
    void createTask_WithTooLongTitle_ShouldReturn400() throws Exception {
        String longTitle = "A".repeat(101);
        TaskRequest request = new TaskRequest(longTitle, null, TaskStatus.TODO, null);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("title"));
    }

    @Test
    void createTask_WithoutStatus_ShouldReturn400() throws Exception {
        String requestJson = """
            {
                "title": "Test Task",
                "description": "Test Description"
            }
            """;

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("status"));
    }

    @Test
    void createTask_WithPastDueDate_ShouldReturn400() throws Exception {
        TaskRequest request = new TaskRequest(
                "Test Task",
                "Test Description",
                TaskStatus.TODO,
                LocalDateTime.now().minusDays(1)
        );

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void getTask_WithNonExistingId_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/tasks/" + java.util.UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllTasks_ShouldReturnEmptyList() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void createAndRetrieveTask_ShouldWorkCorrectly() throws Exception {
        TaskRequest request = new TaskRequest(
                "Task for Retrieval",
                "Description",
                TaskStatus.IN_PROGRESS,
                LocalDateTime.now().plusDays(3)
        );

        String response = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        var task = objectMapper.readTree(response);
        String taskId = task.get("id").asText();

        mockMvc.perform(get("/api/tasks/" + taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Task for Retrieval"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void updateTask_WithValidData_ShouldReturn200() throws Exception {
        TaskRequest createRequest = new TaskRequest(
                "Original Task",
                "Original Description",
                TaskStatus.TODO,
                null
        );

        String createResponse = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        var createdTask = objectMapper.readTree(createResponse);
        String taskId = createdTask.get("id").asText();

        TaskRequest updateRequest = new TaskRequest(
                "Updated Task",
                "Updated Description",
                TaskStatus.DONE,
                LocalDateTime.now().plusDays(5)
        );

        mockMvc.perform(put("/api/tasks/" + taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Task"))
                .andExpect(jsonPath("$.status").value("DONE"));
    }

    @Test
    void deleteTask_WithExistingId_ShouldReturn204() throws Exception {
        TaskRequest request = new TaskRequest(
                "Task to Delete",
                "Will be deleted",
                TaskStatus.TODO,
                null
        );

        String response = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        var task = objectMapper.readTree(response);
        String taskId = task.get("id").asText();

        mockMvc.perform(delete("/api/tasks/" + taskId))
                .andExpect(status().isNoContent());
    }

    @Test
    void createTask_WithTooLongDescription_ShouldReturn400() throws Exception {
        String longDescription = "D".repeat(501);
        TaskRequest request = new TaskRequest(
                "Test Task",
                longDescription,
                TaskStatus.TODO,
                null
        );

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("description"));
    }
}