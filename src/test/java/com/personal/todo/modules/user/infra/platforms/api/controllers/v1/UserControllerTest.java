package com.personal.todo.modules.user.infra.platforms.api.controllers.v1;

import com.personal.todo.modules.auth.business.app.actions.ports.output.TokenGenerator;
import com.personal.todo.modules.auth.business.app.services.TokenBlacklistService;
import com.personal.todo.modules.auth.infra.platforms.api.middlewares.TokenAuthFilter;
import com.personal.todo.modules.task.business.app.actions.TaskActions;
import com.personal.todo.modules.task.business.entities.Task;
import com.personal.todo.modules.user.business.app.UserActions;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

@WebMvcTest(UserController.class)
@ActiveProfiles("test")
class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private TokenGenerator tokenGenerator;
    @MockitoBean
    private UserDetailsService userDetailsService;
    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;
    @Autowired
    private TokenAuthFilter tokenAuthFilter;
    @Autowired
    @MockitoSpyBean
    private UserController userController;
    @MockitoBean
    private UserActions userActions;
    @MockitoBean
    private TaskActions taskActions;

    @Test
    void shouldReadByIdSuccessfully() throws Exception {
        mockMvc = MockMvcBuilders
                .standaloneSetup(userController)
                .addFilter(tokenAuthFilter)
                .build();

        String fakeToken = "fake_token";
        String email = "felix@gmail.com";

        User user = new User(
                "felix", email, "1234",
                new Role(Role.Values.USER, "description")
        );

        int userId = 1;

        Mockito.when(userActions.readById(userId)).thenReturn(user);
        Mockito.when(tokenGenerator.validateToken(fakeToken)).thenReturn(email);

        UserDetails authUser = org.springframework.security.core.userdetails.User
                .withUsername(email)
                .password("12345")
                .build();

        Mockito.when(userDetailsService.loadUserByUsername(email))
                .thenReturn(authUser);

        // act & assert
        mockMvc.perform(
                MockMvcRequestBuilders.get("/users/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + fakeToken)
                )
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("success"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("user read successfully"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.user.name").value(user.getName()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.user.email").value(user.getEmail()))
        ;

        Mockito.verify(userActions).readById(userId);
        Mockito.verify(tokenGenerator).validateToken(fakeToken);
        Mockito.verify(userDetailsService).loadUserByUsername(email);
    }

    @Test
    void shouldReadAllTasksSuccessfully() throws Exception {
        mockMvc = MockMvcBuilders
                .standaloneSetup(userController)
                .addFilter(tokenAuthFilter)
                .build();

        String fakeToken = "fake_token";
        String email = "felix@gmail.com";

        User user = new User(
                "felix", email, "1234",
                new Role(Role.Values.USER, "description")
        );

        List<Task> tasks = List.of(
                new Task("task 1", user),
                new Task("task 2", user)
        );

        int userId = 1;

        Mockito.when(taskActions.readAllByUserId(userId)).thenReturn(tasks);
        Mockito.when(tokenGenerator.validateToken(fakeToken)).thenReturn(email);

        UserDetails authUser = org.springframework.security.core.userdetails.User
                .withUsername(email)
                .password("12345")
                .build();

        Mockito.when(userDetailsService.loadUserByUsername(email))
                .thenReturn(authUser);

        // act & assert
        mockMvc.perform(
                MockMvcRequestBuilders.get("/users/" + userId + "/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + fakeToken)
                )
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("success"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("user tasks read successfully!"))

                .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].title").value(tasks.get(0).getTitle()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].user.name").value(tasks.get(0).getUser().getName()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].user.email").value(tasks.get(0).getUser().getEmail()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].user.role.name").value(tasks.get(0).getUser().getRole().getName()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].user.role.description").value(tasks.get(0).getUser().getRole().getDescription()))

                .andExpect(MockMvcResultMatchers.jsonPath("$.data[1].title").value(tasks.get(1).getTitle()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[1].user.name").value(tasks.get(1).getUser().getName()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[1].user.email").value(tasks.get(1).getUser().getEmail()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[1].user.role.name").value(tasks.get(1).getUser().getRole().getName()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[1].user.role.description").value(tasks.get(1).getUser().getRole().getDescription()))
                ;
        // the filed password should not be returned

        Mockito.verify(taskActions).readAllByUserId(userId);
        Mockito.verify(tokenGenerator).validateToken(fakeToken);
        Mockito.verify(userDetailsService).loadUserByUsername(email);
    }

    @Test
    void readAllTasksShouldReturn401WhenTokenIsInvalid() throws Exception {
        mockMvc = MockMvcBuilders
                .standaloneSetup(UserController.class)
                .addFilter(tokenAuthFilter)
                .build();

        int userId = 1;
        String invalidToken = "invalid token";

        Mockito.when(tokenGenerator.validateToken(invalidToken)).thenReturn(null);

        mockMvc.perform(
                MockMvcRequestBuilders.get("/users/" + userId + "/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + invalidToken)
                )
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());

        Mockito.verifyNoInteractions(taskActions);
        Mockito.verify(userController, Mockito.never()).readAllTasks(Mockito.anyInt());
        // garantir que nao houve interacao com doFilterInternal do tokenAuthFilter
    }

    @Test
    void readAllTasksShouldReturn403WhenTokenIsMissing() throws Exception {
        mockMvc = MockMvcBuilders
                .standaloneSetup(UserController.class)
                .addFilter(tokenAuthFilter)
                .build();

        int userId = 1;

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/users/" + userId + "/tasks")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(MockMvcResultMatchers.status().isForbidden());

        Mockito.verifyNoInteractions(taskActions);
        Mockito.verify(userController, Mockito.never()).readAllTasks(Mockito.anyInt());
        // garantir que nao houve interacao com doFilterInternal do tokenAuthFilter
    }
}