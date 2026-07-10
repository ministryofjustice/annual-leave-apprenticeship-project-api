package uk.gov.justice.digital.hmpps.annualleaveapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class CorsConfiguration(
  @Value("\${app.cors.allowed-origins:http://localhost:3000}") private val allowedOrigins: List<String>,
) : WebMvcConfigurer {

  override fun addCorsMappings(registry: CorsRegistry) {
    registry.addMapping("/**")
      .allowedOrigins(*allowedOrigins.toTypedArray())
      .allowedMethods("GET", "POST", "PATCH", "DELETE", "OPTIONS")
      .allowedHeaders("*")
      .allowCredentials(true)
  }
}
