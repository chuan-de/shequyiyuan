package com.hospital.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TraceIdFilter extends OncePerRequestFilter {
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String traceId = Optional.ofNullable(request.getHeader(TRACE_ID_HEADER)).filter(s -> !s.isBlank()).orElseGet(() -> UUID.randomUUID().toString());
        TraceContext.setTraceId(traceId);
        MDC.put("traceId", traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        try { filterChain.doFilter(request, response); } finally { MDC.remove("traceId"); TraceContext.clear(); }
    }
}
