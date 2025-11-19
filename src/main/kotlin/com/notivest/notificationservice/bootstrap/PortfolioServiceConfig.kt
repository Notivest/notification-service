package com.notivest.notificationservice.bootstrap

import com.notivest.notificationservice.infrastructure.adapters.out.http.PortfolioServiceTokenProvider
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.web.client.RestTemplate
import java.time.Clock

@Configuration
@EnableConfigurationProperties(PortfolioServiceProperties::class)
class PortfolioServiceConfig(
    private val restTemplateBuilder: RestTemplateBuilder,
) {

    @Bean
    fun portfolioServiceTokenProvider(
        properties: PortfolioServiceProperties,
        clock: Clock,
    ): PortfolioServiceTokenProvider =
        PortfolioServiceTokenProvider(
            restTemplate = restTemplateBuilder
                .rootUri(properties.auth.domain.trimEnd('/'))
                .connectTimeout(properties.auth.connectTimeout)
                .readTimeout(properties.auth.readTimeout)
                .build(),
            properties = properties.auth,
            clock = clock,
        )

    @Bean("portfolioServiceRestTemplate")
    fun portfolioServiceRestTemplate(
        properties: PortfolioServiceProperties,
        tokenProvider: PortfolioServiceTokenProvider,
    ): RestTemplate =
        restTemplateBuilder
            .rootUri(properties.baseUrl)
            .connectTimeout(properties.connectTimeout)
            .readTimeout(properties.readTimeout)
            .additionalInterceptors(
                ClientHttpRequestInterceptor { request, body, execution ->
                    request.headers.setBearerAuth(tokenProvider.getAccessToken())
                    execution.execute(request, body)
                },
            )
            .build()
}
