package com.personal.todo.modules.task.infra.platforms.api.controllers.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.personal.todo.modules.auth.business.app.actions.ports.output.TokenGenerator;
import com.personal.todo.modules.auth.business.app.services.TokenBlacklistService;
import com.personal.todo.modules.auth.infra.platforms.api.middlewares.TokenAuthFilter;
import com.personal.todo.modules.task.business.app.actions.TaskActions;
import com.personal.todo.modules.task.business.app.params.input.CreateTaskInput;
import com.personal.todo.modules.task.business.app.params.input.UpdateTaskInput;
import com.personal.todo.modules.task.business.entities.Task;
import com.personal.todo.modules.task.business.types.TaskStatus;
import com.personal.todo.modules.task.infra.platforms.api.controllers.v1.requests.CreateTaskPayload;
import com.personal.todo.modules.task.infra.platforms.api.controllers.v1.requests.UpdateTaskPayload;
import com.personal.todo.modules.user.business.entities.Role;
import com.personal.todo.modules.user.business.entities.User;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@WebMvcTest(TaskController.class)
@ActiveProfiles("test")
class TaskControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private TaskActions taskActions;
    @MockitoBean
    private TokenGenerator tokenGenerator;
    @MockitoBean
    private UserDetailsService userDetailsService;
    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;
    @Autowired
    private TokenAuthFilter tokenAuthFilter;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TaskController taskController;

    @Test
    void shouldCreateTaskSuccessfully() throws Exception {
        mockMvc = MockMvcBuilders.standaloneSetup(taskController)
                .addFilter(tokenAuthFilter)
                .build();

        // arrange
        int userId = 1;
        String taskTitle = "task 1";

        CreateTaskPayload payload = new CreateTaskPayload(taskTitle, userId);
        Task task = new Task(1, taskTitle, TaskStatus.PENDING, new User());

        String token = "fake_token";
        String email = "felix@gmail.com";

        var params = new  CreateTaskInput(taskTitle, userId);

        Mockito.when(taskActions.create(params)).thenReturn(task);
        Mockito.when(tokenGenerator.validateToken(token)).thenReturn(email);

        UserDetails user = org.springframework.security.core.userdetails.User
                .withUsername(email)
                .password("12345")
                .authorities("ROLE_USER")
                .build();

        Mockito.when(userDetailsService.loadUserByUsername(email))
                .thenReturn(user);

        // Act & Assert
        mockMvc.perform(
                MockMvcRequestBuilders.post("/api/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload))
                )
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("success"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("todo created successfully"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.title").value(taskTitle))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.status").value(TaskStatus.PENDING.name()));

        Mockito.verify(userDetailsService).loadUserByUsername(email);
        Mockito.verify(taskActions).create(params);
        Mockito.verify(tokenGenerator).validateToken(token);
    }

    @Test
    void shouldReadByIdTaskSuccessfully() throws Exception {
        mockMvc = MockMvcBuilders.standaloneSetup(taskController)
                .addFilter(tokenAuthFilter)
                .build();

        // arrange
        String token = "fake_token";

        int taskId = 1;
        String userEmail = "felix@gmail.com";

        User taskUser = new User(
                "felix", userEmail, "1234",
                new Role(Role.Values.USER, "description")
        );

        Task expectedTask = new Task(taskId, "task 1", TaskStatus.PENDING, taskUser);

        Mockito.when(taskActions.readById(taskId)).thenReturn(expectedTask);
        Mockito.when(tokenGenerator.validateToken(token)).thenReturn(userEmail);

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(userEmail)
                .password("1234")
                .authorities("ROLE_USER")
                .build();

        Mockito.when(userDetailsService.loadUserByUsername(userEmail)).thenReturn(userDetails);

        // act & assert
        mockMvc.perform(
                MockMvcRequestBuilders.get("/api/tasks/" + taskId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful());

        Mockito.verify(taskActions).readById(taskId);
        Mockito.verify(userDetailsService).loadUserByUsername(userEmail);
        Mockito.verify(tokenGenerator).validateToken(token);
    }

    @Test
    void shouldUpdateTaskSuccessfully() throws Exception {
        // arrange
        mockMvc = MockMvcBuilders
                .standaloneSetup(taskController)
                .addFilter(tokenAuthFilter)
                .build();

        String email = "felix@gmail.com";
        String token = "fake_token";

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(email)
                .password("1234")
                .authorities("ROLE_USER")
                .build();

        int taskId = 1;
        String taskTitle = "task 1";
        TaskStatus status = TaskStatus.PENDING;

        var payload = new UpdateTaskPayload(taskTitle, status);
        Task expectedTask = new Task(taskId, taskTitle, status, new User());

        var params = new UpdateTaskInput(taskId, taskTitle, status);

        Mockito.when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        Mockito.when(tokenGenerator.validateToken(token)).thenReturn(email);
        Mockito.when(taskActions.update(params)).thenReturn(expectedTask);

        // act & assert
        mockMvc.perform(
                MockMvcRequestBuilders.put("/api/tasks/1")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload))
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful());

        Mockito.verify(userDetailsService).loadUserByUsername(email);
        Mockito.verify(tokenGenerator).validateToken(token);
        Mockito.verify(taskActions).update(params);
    }
}