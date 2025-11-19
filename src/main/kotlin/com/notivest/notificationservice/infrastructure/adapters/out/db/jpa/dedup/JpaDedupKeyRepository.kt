package com.notivest.notificationservice.infrastructure.adapters.out.db.jpa.dedup

import com.notivest.notificationservice.domain.dedup.port.DedupKeyRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.sql.SQLException
import java.time.Instant
import java.util.UUID

@Repository
class JpaDedupKeyRepository(
    private val repository: DedupKeyJpaRepository,
) : DedupKeyRepository {

    @Transactional(
        propagation = Propagation.REQUIRES_NEW,
        noRollbackFor = [DataIntegrityViolationException::class],
    )
    override fun insertIfAbsent(userId: UUID, fingerprint: String, bucket: Instant): Boolean {
        return try {
            val key = DedupKeyId(userId = userId, fingerprint = fingerprint, bucket = bucket)
            if (repository.existsById(key)) {
                return false
            }

            repository.saveAndFlush(
                DedupKeyEntity(
                    dedupKeyId = key,
                    createdAt = Instant.now(),
                ),
            )
            true
        } catch (ex: DataIntegrityViolationException) {
            if (ex.isUniqueConstraintViolation()) {
                false
            } else {
                throw ex
            }
        }
    }

    private fun DataIntegrityViolationException.isUniqueConstraintViolation(): Boolean {
        val sqlException = when (val root = mostSpecificCause) {
            is SQLException -> root
            else -> cause as? SQLException
        }
        return sqlException?.sqlState == UNIQUE_VIOLATION_SQL_STATE
    }

    private companion object {
        private const val UNIQUE_VIOLATION_SQL_STATE = "23505"
    }
}
