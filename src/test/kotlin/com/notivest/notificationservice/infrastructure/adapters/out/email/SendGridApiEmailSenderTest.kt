package com.notivest.notificationservice.infrastructure.adapters.out.email

import com.notivest.notificationservice.domain.email.port.OutboundEmail
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent
import org.springframework.web.client.RestClient

class SendGridApiEmailSenderTest {

    @Test
    fun `sends HTML email through SendGrid HTTPS API`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val sender = SendGridApiEmailSender(
            restClientBuilder = builder,
            apiKey = "test-api-key",
            apiUrl = "https://api.sendgrid.com/v3/mail/send",
            defaultFrom = "alerts@notivest.app",
        )
        server.expect(requestTo("https://api.sendgrid.com/v3/mail/send"))
            .andExpect(method(org.springframework.http.HttpMethod.POST))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-api-key"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json("""{"from":{"email":"alerts@notivest.app"},"personalizations":[{"to":[{"email":"user@example.com"}]}],"subject":"Alert","content":[{"type":"text/html","value":"<p>Triggered</p>"}]}"""))
            .andRespond(withNoContent())

        sender.send(OutboundEmail("", "user@example.com", "Alert", "<p>Triggered</p>"))

        server.verify()
    }

    @Test
    fun `rejects SendGrid API provider without a key`() {
        val sender = SendGridApiEmailSender(
            restClientBuilder = RestClient.builder(),
            apiKey = "",
            apiUrl = "https://api.sendgrid.com/v3/mail/send",
            defaultFrom = "alerts@notivest.app",
        )

        assertThatIllegalStateException().isThrownBy {
            sender.send(OutboundEmail("", "user@example.com", "Alert", "<p>Triggered</p>"))
        }.withMessageContaining("SENDGRID_API_KEY")
    }
}
