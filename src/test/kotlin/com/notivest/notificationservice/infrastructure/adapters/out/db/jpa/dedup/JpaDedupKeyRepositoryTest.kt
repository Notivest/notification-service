package com.notivest.notificationservice.infrastructure.adapters.out.db.jpa.dedup

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import java.time.Instant
import java.util.UUID

@DataJpaTest
@EntityScan(basePackageClasses = [DedupKeyEntity::class])
@EnableJpaRepositories(basePackageClasses = [DedupKeyJpaRepository::class])
@Import(JpaDedupKeyRepositoryTest.JpaTestConfiguration::class)
class JpaDedupKeyRepositoryTest {

    @Autowired
    private lateinit var repository: JpaDedupKeyRepository

    @Autowired
    private lateinit var jpaRepository: DedupKeyJpaRepository

    @BeforeEach
    fun cleanDatabase() {
        jpaRepository.deleteAll()
    }

    @Test
    fun `insertIfAbsent inserts only once per bucket`() {
        val userId = UUID.randomUUID()
        val fingerprint = "fp-123"
        val bucket = Instant.parse("2024-06-01T10:15:00Z")

        val firstInsert = repository.insertIfAbsent(userId, fingerprint, bucket)
        val secondInsert = repository.insertIfAbsent(userId, fingerprint, bucket)

        assertThat(firstInsert).isTrue()
        assertThat(secondInsert).isFalse()
    }

    @Test
    fun `insertIfAbsent allows different bucket`() {
        val userId = UUID.randomUUID()
        val fingerprint = "fp-123"
        val earlier = Instant.parse("2024-06-01T10:15:00Z")
        val later = Instant.parse("2024-06-01T10:20:00Z")

        repository.insertIfAbsent(userId, fingerprint, earlier)
        val inserted = repository.insertIfAbsent(userId, fingerprint, later)

        assertThat(inserted).isTrue()
    }

    @TestConfiguration(proxyBeanMethods = false)
    class JpaTestConfiguration {
        @Bean
        fun jpaDedupKeyRepository(repository: DedupKeyJpaRepository): JpaDedupKeyRepository =
            JpaDedupKeyRepository(repository)
    }
}
