package com.notivest.notificationservice.infrastructure.adapters.`in`.scheduler

import com.notivest.notificationservice.application.emailjob.ProcessEmailJobsUseCase
import com.notivest.notificationservice.bootstrap.EmailJobWorkerProperties
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class EmailJobWorker(
    private val processEmailJobsUseCase: ProcessEmailJobsUseCase,
    private val workerProperties: EmailJobWorkerProperties,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${notification.email.worker.fixed-delay-ms:5000}")
    fun pollQueue() {
        if (!workerProperties.enabled) {
            return
        }

        val result = processEmailJobsUseCase.processDueJobs()
        if (result.total > 0) {
            logger.info(
                "Processed {} email jobs (sent={}, failed={})",
                result.total,
                result.sent,
                result.failed,
            )
        }
    }
}
