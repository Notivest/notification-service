package com.notivest.notificationservice.application.userdata

import com.notivest.notificationservice.infrastructure.adapters.out.db.jpa.dedup.DedupKeyJpaRepository
import com.notivest.notificationservice.infrastructure.adapters.out.db.jpa.emailjob.EmailJobJpaRepository
import com.notivest.notificationservice.infrastructure.adapters.out.db.jpa.usercontact.UserContactJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

data class UserDataDeletionSummary(
    val deletedEmailJobs: Long,
    val deletedDedupKeys: Long,
    val deletedUserContacts: Long,
)

@Service
class UserDataDeletionService(
    private val emailJobJpaRepository: EmailJobJpaRepository,
    private val dedupKeyJpaRepository: DedupKeyJpaRepository,
    private val userContactJpaRepository: UserContactJpaRepository,
) {
    private val logger = LoggerFactory.getLogger(UserDataDeletionService::class.java)

    @Transactional
    fun deleteUserData(userId: UUID): UserDataDeletionSummary {
        val deletedEmailJobs = emailJobJpaRepository.deleteByUserId(userId)
        val deletedDedupKeys = dedupKeyJpaRepository.deleteByUserId(userId).toLong()
        val deletedUserContacts = userContactJpaRepository.deleteByUserId(userId)

        logger.info(
            "user-data-delete completed service=notification userId={} deletedEmailJobs={} deletedDedupKeys={} deletedUserContacts={}",
            userId,
            deletedEmailJobs,
            deletedDedupKeys,
            deletedUserContacts,
        )

        return UserDataDeletionSummary(
            deletedEmailJobs = deletedEmailJobs,
            deletedDedupKeys = deletedDedupKeys,
            deletedUserContacts = deletedUserContacts,
        )
    }
}

