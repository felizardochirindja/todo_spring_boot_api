package com.personal.todo.modules.user.infra.platforms.api.controllers.v1;

import com.personal.todo.modules.auth.business.entities.AuthUser;
import com.personal.todo.modules.task.business.app.params.output.ReadRemoteTasksOutput;
import com.personal.todo.modules.task.business.app.actions.TaskActions;
import com.personal.todo.modules.task.infra.platforms.api.controllers.v1.responses.TaskApiResponse;
import com.personal.todo.modules.user.business.app.UserActions;
import com.personal.todo.modules.task.business.entities.Task;
import com.personal.todo.modules.user.business.entities.User;
import com.personal.todo.modules.task.infra.platforms.api.controllers.v1.responses.RemoteTaskApi;
import com.personal.todo.modules.task.infra.platforms.api.controllers.v1.responses.RemoteTasksApiResponse;
import com.personal.todo.modules.user.infra.platforms.api.controllers.v1.responses.UserApi;
import com.personal.todo.modules.user.infra.platforms.api.controllers.v1.responses.UserApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserActions userActions;
    @Autowired
    private TaskActions taskActions;
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @GetMapping("/{id}")
    @Operation(
            summary = "read user by id",
            method = "GET",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "user read",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = UserApiResponse.class)
                            )
                    ),
            }
    )
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UserApiResponse> readById(@PathVariable Integer id) {
        User user = userActions.readById(id);

        UserApiResponse response = new UserApiResponse(
                "success",
                "user read successfully",
                UserApi.fromEntity(user)
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/tasks")
    @Operation(
            summary = "read all tasks",
            method = "GET",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "user tasks read",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = TaskApiResponse.class)
                            )
                    ),
            }
    )
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<TaskApiResponse<List<Task>>> readAllTasks(@PathVariable @NotNull Integer id) {
        var response = new TaskApiResponse<>(
                "success",
                "user tasks read successfully!",
                taskActions.readAllByUserId(id)
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("{id}/tasks/remote")
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "read remote tasks",
            method = "GET",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "remote tasks read",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = RemoteTasksApiResponse.class)
                            )
                    ),
            }
    )
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<RemoteTasksApiResponse> readAllRemoteTasksByUserId(
            @PathVariable @NotNull Integer id,
            Authentication auth
    ) {
        String authEmail = ((AuthUser) auth.getPrincipal()).getUsername();
        User user = userActions.readByEmail(authEmail);

        if (!user.getId().equals(id)) {
            logger.atWarn()
                    .setMessage("auth user id does not match the path user id")
                    .addKeyValue("authUserId", user.getId())
                    .addKeyValue("pathUserId", id)
                    .log();

            throw new IllegalArgumentException("auth user id does not match the path user id");
        }

        ReadRemoteTasksOutput output = taskActions.readRemoteTasksByUserId(id);

        List<RemoteTaskApi> taskList = output.tasks()
                .stream()
                .map(RemoteTaskApi::fromEntity)
                .toList();

        var response = new RemoteTasksApiResponse(
                taskList,
                output.total(),
                output.skip(),
                output.limit(),
                UserApi.fromEntity(output.user())
        );

        return ResponseEntity.ok(response);
    }
}
