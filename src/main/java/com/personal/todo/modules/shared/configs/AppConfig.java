package com.personal.todo.modules.shared.configs;

import com.personal.todo.modules.user.business.app.CreateUserAction;
import com.personal.todo.modules.user.business.app.ports.input.UserCreator;
import com.personal.todo.modules.task.business.app.ports.output.remotetask.RemoteTaskSyncFetcher;
import com.personal.todo.modules.task.infra.adapters.libs.DummyJsonWebClientSyncTaskFetcher;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableCaching
@EnableScheduling
public class AppConfig {
    @Bean
    public RemoteTaskSyncFetcher remoteTaskSyncFetcher() {
        return new DummyJsonWebClientSyncTaskFetcher();
    }

    @Bean
    public UserCreator userCreator() {
        return new CreateUserAction();
    }
}
