package com.notivest.notificationservice

import com.ninjasquad.springmockk.MockkBean
import com.notivest.notificationservice.domain.contact.port.UserContactRepository
import com.notivest.notificationservice.domain.dedup.port.DedupKeyRepository
import com.notivest.notificationservice.domain.email.port.EmailEventRepository
import com.notivest.notificationservice.domain.emailjob.port.EmailJobRepository
import com.notivest.notificationservice.infrastructure.adapters.out.db.jpa.dedup.DedupKeyJpaRepository
import com.notivest.notificationservice.infrastructure.adapters.out.db.jpa.emailjob.EmailJobJpaRepository
import com.notivest.notificationservice.infrastructure.adapters.out.db.jpa.usercontact.UserContactJpaRepository
import com.notivest.notificationservice.observability.NotificationMetrics
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    properties = [
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
            "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration," +
            "org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration",
        "spring.flyway.enabled=false",
    ],
)
class NotificationServiceApplicationTests {

    @MockkBean
    private lateinit var userContactRepository: UserContactRepository

    @MockkBean
    private lateinit var dedupKeyRepository: DedupKeyRepository

    @MockkBean
    private lateinit var emailJobRepository: EmailJobRepository

    @MockkBean
    private lateinit var emailEventRepository: EmailEventRepository

    @MockkBean(relaxed = true)
    private lateinit var notificationMetrics: NotificationMetrics

    @MockkBean
    private lateinit var emailJobJpaRepository: EmailJobJpaRepository

    @MockkBean
    private lateinit var dedupKeyJpaRepository: DedupKeyJpaRepository

    @MockkBean
    private lateinit var userContactJpaRepository: UserContactJpaRepository

    @Test
    fun contextLoads() {
    }
}
