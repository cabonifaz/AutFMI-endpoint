package org.app.autfmi.service.impl;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.app.autfmi.repository.RequirementRepository;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.util.TimeZone;
import java.util.concurrent.ScheduledFuture;

@Service
@RequiredArgsConstructor
public class ScheduledJobService {
    private final TaskScheduler taskSchedulerAsyncJob;
    private final RequirementRepository requerimientoRepository;

    private ScheduledFuture<?> scheduledFutureAsyncJob;

    @PostConstruct
    public void scheduleJob() {
        scheduleNextAsyncJob();
    }

    private void scheduleNextAsyncJob() {
        System.out.println("Ejecutando jobs asíncronos");
        if (scheduledFutureAsyncJob != null) {
            scheduledFutureAsyncJob.cancel(false);
        }

        // CRON para 7:00 AM todos los días, junto con la zona horaria de Lima
        String cronExpresion = "0 0 7 * * *";

        System.out.println("Tarea asignada a las 7am");
        scheduledFutureAsyncJob = taskSchedulerAsyncJob.schedule(
                this::invokeAsyncMethods,
                new CronTrigger(cronExpresion, TimeZone.getTimeZone("America/Lima"))
        );
    }

    public void invokeAsyncMethods() {
        try {
            System.out.println("Invocando métodos asíncronos");
            requerimientoRepository.updateRequirementAlertJob();
        } catch (Exception e) {
            System.out.println("ERROR AL EJECUTAR invokeAsyncMethods");
            System.out.println(e.getMessage());
            scheduleNextAsyncJob();
        }
    }


}
