package com.notivest.notificationservice.application.emailjob

data class EmailJobProcessingResult(
    val total: Int,
    val sent: Int,
    val failed: Int,
) {
    companion object {
        val empty = EmailJobProcessingResult(total = 0, sent = 0, failed = 0)
    }
}

interface ProcessEmailJobsUseCase {
    fun processDueJobs(): EmailJobProcessingResult
}
