package com.personal.todo.modules.shared.schedulers;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.personal.todo.modules.task.adapters.repositories.TaskRepository;

@Service("taskJobScheduler")
@ConditionalOnProperty(
    name = "app.scheduling.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class TaskScheduler {
    @Autowired
    private TaskRepository taskRepository;

    @Scheduled(initialDelay = 10000, fixedDelay = 50000)
    public void archiveOldTasks() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);

        System.out.println("Running archive job... " + threshold);

        long count = taskRepository.count();

        System.out.println("Total tasks in DB: " + count);
    }
}
