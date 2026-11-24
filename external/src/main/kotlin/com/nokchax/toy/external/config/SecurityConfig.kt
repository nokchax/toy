package com.nokchax.toy.external.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain

@ConfigurationProperties("toy.external")
data class ExternalProperties(
    val username: String = "",
    /** 로그인 비밀번호. 비어 있으면 앱이 뜨지 않는다 (공개 페이지라 로그인 없이 띄우지 않는다) */
    val password: String = "",
)

@Configuration
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain = http
        .authorizeHttpRequests {
            it.requestMatchers("/health").permitAll()
            it.anyRequest().authenticated()
        }
        .formLogin { it.defaultSuccessUrl("/", true) }
        .logout { it.logoutSuccessUrl("/login?logout") }
        .build()

    @Bean
    fun passwordEncoder(): PasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()

    @Bean
    fun userDetailsService(properties: ExternalProperties, encoder: PasswordEncoder): UserDetailsService {
        check(properties.username.isNotBlank() && properties.password.isNotBlank()) {
            "TOY_EXTERNAL_USERNAME / TOY_EXTERNAL_PASSWORD 가 비어 있다. 공개 페이지라 로그인 계정 없이 띄우지 않는다"
        }
        val user = User.withUsername(properties.username)
            .password(encoder.encode(properties.password))
            .roles("OWNER")
            .build()
        return InMemoryUserDetailsManager(user)
    }
}
