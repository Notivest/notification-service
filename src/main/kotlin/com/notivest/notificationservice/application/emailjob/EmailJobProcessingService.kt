package com.notivest.notificationservice.application.emailjob

import com.notivest.notificationservice.bootstrap.EmailJobWorkerProperties
import com.notivest.notificationservice.domain.contact.EmailStatus
import com.notivest.notificationservice.domain.contact.port.UserContactRepository
import com.notivest.notificationservice.domain.email.EmailTemplateRenderer
import com.notivest.notificationservice.domain.email.port.EmailSender
import com.notivest.notificationservice.domain.email.port.OutboundEmail
import com.notivest.notificationservice.domain.emailjob.EmailJob
import com.notivest.notificationservice.domain.emailjob.port.EmailJobRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

@Service
class EmailJobProcessingService(
    private val emailJobRepository: EmailJobRepository,
    private val userContactRepository: UserContactRepository,
    private val emailTemplateRenderer: EmailTemplateRenderer,
    private val emailSender: EmailSender,
    private val clock: Clock,
    private val workerProperties: EmailJobWorkerProperties,
) : ProcessEmailJobsUseCase {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun processDueJobs(): EmailJobProcessingResult {
        val limit = workerProperties.batchSize
        if (limit <= 0) {
            return EmailJobProcessingResult.empty
        }

        val now = Instant.now(clock)
        val jobs = emailJobRepository.findDue(now, limit)
        if (jobs.isEmpty()) {
            return EmailJobProcessingResult.empty
        }

        var sent = 0
        var failed = 0

        jobs.forEach { job ->
            when (processJob(job)) {
                JobOutcome.Sent -> sent++
                is JobOutcome.Failed -> failed++
            }
        }

        return EmailJobProcessingResult(
            total = jobs.size,
            sent = sent,
            failed = failed,
        )
    }

    private fun processJob(job: EmailJob): JobOutcome {
        val attemptInstant = Instant.now(clock)
        return try {
            val contact = userContactRepository.findByUserId(job.userId)
                ?: throw SkippableJobException("Contact not found")

            if (contact.channels["email"] != true) {
                throw SkippableJobException("Email channel disabled")
            }

            if (contact.emailStatus == EmailStatus.BOUNCED || contact.emailStatus == EmailStatus.UNSUB) {
                throw SkippableJobException("Email status ${contact.emailStatus} blocks delivery")
            }

            val rendered = emailTemplateRenderer.render(job.templateKey, contact.locale, job.templateData)
            val outbound =
                OutboundEmail(
                    from = "",
                    to = contact.primaryEmail,
                    subject = rendered.subject,
                    body = rendered.body,
                )
            emailSender.send(outbound)

            val updatedJob = job.markSent(attemptInstant)
            emailJobRepository.save(updatedJob)
            JobOutcome.Sent
        } catch (ex: SkippableJobException) {
            logger.info("Skipping email job {}: {}", job.id, ex.message)
            val updatedJob = job.markFailed(attemptInstant, ex.message ?: "Skipped")
            emailJobRepository.save(updatedJob)
            JobOutcome.Failed(ex.message)
        } catch (ex: Exception) {
            logger.warn("Failed to process email job {}: {}", job.id, ex.message, ex)
            val updatedJob = job.markFailed(attemptInstant, ex.message ?: ex.javaClass.simpleName)
            emailJobRepository.save(updatedJob)
            JobOutcome.Failed(ex.message)
        }
    }

    private sealed interface JobOutcome {
        data object Sent : JobOutcome
        data class Failed(val reason: String?) : JobOutcome
    }

    private class SkippableJobException(message: String) : RuntimeException(message)
}
