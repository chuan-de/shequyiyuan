package com.hospital.audit;

import com.hospital.observability.TraceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");

    public void log(String action, String actor, String target, String detail) {
        auditLog.info("action={}, actor={}, target={}, detail={}, traceId={}", action, actor, target, detail, TraceContext.getTraceId());
    }
}
