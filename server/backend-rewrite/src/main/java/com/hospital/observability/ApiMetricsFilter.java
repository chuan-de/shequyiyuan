package com.hospital.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ApiMetricsFilter extends OncePerRequestFilter {
    private final MeterRegistry meterRegistry;
    public ApiMetricsFilter(MeterRegistry meterRegistry) { this.meterRegistry = meterRegistry; }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) { return !request.getRequestURI().startsWith("/api/v1/"); }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Timer.Sample sample = Timer.start(meterRegistry);
        try { filterChain.doFilter(request, response); }
        finally {
            String uri = request.getRequestURI();
            String method = request.getMethod();
            String status = String.valueOf(response.getStatus());
            Counter.builder("hospital_api_requests_total").tag("uri", uri).tag("method", method).tag("status", status).register(meterRegistry).increment();
            if (response.getStatus() >= 400) {
                Counter.builder("hospital_api_errors_total").tag("uri", uri).tag("method", method).tag("status", status).register(meterRegistry).increment();
            }
            sample.stop(Timer.builder("hospital_api_request_duration_ms").description("API request latency").tag("uri", uri).tag("method", method).register(meterRegistry));
        }
    }
}
