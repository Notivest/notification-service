package com.notivest.notificationservice.infrastructure.adapters.`in`.scheduler

import com.notivest.notificationservice.application.emailjob.ProcessEmailJobsUseCase
import com.notivest.notificationservice.bootstrap.EmailJobWorkerProperties
import com.notivest.notificationservice.observability.CorrelationContext
import com.notivest.notificationservice.observability.NotificationMetrics
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class EmailJobWorker(
    private val processEmailJobsUseCase: ProcessEmailJobsUseCase,
    private val workerProperties: EmailJobWorkerProperties,
    private val notificationMetrics: NotificationMetrics,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${notification.email.worker.fixed-delay-ms:5000}")
    fun pollQueue() {
        if (!workerProperties.enabled) {
            return
        }

        val generatedCorrelationId = CorrelationContext.currentCorrelationId() == null
        if (generatedCorrelationId) {
            CorrelationContext.setCorrelationId("email-worker-${UUID.randomUUID()}")
        }

        val sample = notificationMetrics.startWorkerPollTimer()
        try {
            val result = processEmailJobsUseCase.processDueJobs()
            notificationMetrics.recordWorkerPoll(result)
            if (result.total > 0) {
                logger.info(
                    "email-job-worker.completed total={} sent={} failed={}",
                    result.total,
                    result.sent,
                    result.failed,
                )
            }
        } finally {
            notificationMetrics.stopWorkerPollTimer(sample)
            if (generatedCorrelationId) {
                CorrelationContext.clear()
            }
        }
    }
}
