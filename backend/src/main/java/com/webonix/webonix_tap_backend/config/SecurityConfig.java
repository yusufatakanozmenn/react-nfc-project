package com.webonix.webonix_tap_backend.config;

import com.webonix.webonix_tap_backend.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import jakarta.servlet.DispatcherType;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository()))
                .logout(logout -> logout.disable())
                .headers(headers -> headers.contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'")))

                .cors(cors -> {})

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/logout", "/api/auth/forgot-password", "/api/auth/reset-password").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf", "/r/**").permitAll()
                        .requestMatchers("/api/auth/register").denyAll()

                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        )
                        .permitAll()

                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/cards").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/cards/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/cards/*/owner").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/cards", "/api/cards/*", "/api/statistics", "/api/auth/me").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.PUT, "/api/cards/*", "/api/auth/me", "/api/auth/me/password").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.PATCH, "/api/cards/*/status").hasAnyRole("ADMIN", "USER")
                        .anyRequest().denyAll()
                )

                .formLogin(form -> form.disable())

                .httpBasic(basic -> basic.disable())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> response.setStatus(401))
                        .accessDeniedHandler((request, response, exception) -> response.setStatus(403)))

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration() {
        var registration = new FilterRegistrationBean<>(jwtAuthenticationFilter);
        // Run only inside Spring Security, after its security context has been initialized.
        registration.setEnabled(false);
        return registration;
    }

    @org.springframework.beans.factory.annotation.Value("${app.cookies.secure:false}")
    private boolean secureCookies;

    @Bean
    public org.springframework.security.web.csrf.CookieCsrfTokenRepository csrfTokenRepository() {
        var repository = new org.springframework.security.web.csrf.CookieCsrfTokenRepository();
        repository.setCookieName(secureCookies ? "__Host-webonix_csrf" : "webonix_csrf");
        repository.setCookiePath("/");
        repository.setCookieCustomizer(cookie -> cookie.httpOnly(true).secure(secureCookies).sameSite("Strict"));
        return repository;
    }

    @Bean
    public org.springframework.security.core.userdetails.UserDetailsService userDetailsService() {
        // Authentication is handled by AuthService; disable Boot's generated development account.
        return username -> { throw new org.springframework.security.core.userdetails.UsernameNotFoundException("Unsupported authentication method"); };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
