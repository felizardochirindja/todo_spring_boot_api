package com.personal.todo.modules.auth.infra.platforms.api.controllers.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.personal.todo.modules.auth.business.app.actions.AuthActions;
import com.personal.todo.modules.auth.business.app.actions.params.input.SignupInput;
import com.personal.todo.modules.auth.business.app.actions.ports.output.TokenGenerator;
import com.personal.todo.modules.auth.business.app.services.TokenBlacklistService;
import com.personal.todo.modules.auth.infra.platforms.api.controllers.v1.requests.LoginPayload;
import com.personal.todo.modules.auth.infra.platforms.api.controllers.v1.requests.SignupPayload;
import com.personal.todo.modules.user.business.entities.Role;
import com.personal.todo.modules.user.business.entities.User;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@WebMvcTest(AuthController.class)
class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;
    @Autowired
    private AuthController authController;
    @MockitoBean
    private AuthActions authActions;
    @MockitoBean
    private TokenGenerator tokenGenerator;

    @Test
    void shouldLoginSuccessfully() throws Exception {
        mockMvc = MockMvcBuilders
                .standaloneSetup(authController)
                .build();

        String newToken = "new_fake_token";
        String email = "felix@gmail.com";
        String password = "1234";

        var payload = new LoginPayload(email, password);

        Mockito.when(authActions.login(email, password)).thenReturn(newToken);

        // act & assert
        mockMvc.perform(
                MockMvcRequestBuilders.post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload))
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful());

        Mockito.verify(authActions).login(email, password);
    }

    void loginShouldFail() {
        
    }

    @Test
    void shouldSignupSuccessfully() throws Exception {
        mockMvc = MockMvcBuilders
                .standaloneSetup(authController)
                .build();

        String name = "felix";
        String email = "felix@gmail.com";
        String password = "1234";

        var payload = new SignupPayload(name, email, password, Role.Values.ADMIN);
        var params = new SignupInput(name, email, password, Role.Values.ADMIN);

        User expectedUser = new User(name, email, password, new Role(Role.Values.ADMIN, "description"));

        Mockito.when(authActions.signUp(params)).thenReturn(expectedUser);

        // act & assert
        mockMvc.perform(
                MockMvcRequestBuilders.post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload))
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful());

        Mockito.verify(authActions).signUp(params);
    }
}