// shared/src/main/java/org/example/shared/config/AuditAspect.java
package org.example.shared.config;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.example.shared.entity.AuditLog;
import org.example.shared.interfaces.Auditable;
import org.example.shared.repository.AuditLogRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnBean(AuditLogRepository.class)
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;
    private final HttpServletRequest request;

    @AfterReturning(
        pointcut = "@annotation(auditable)",
        returning = "result"
    )
    public void auditSuccess(JoinPoint jp, Auditable auditable, Object result) {
        saveAudit(auditable, true, null);
    }

    @AfterThrowing(
        pointcut = "@annotation(auditable)",
        throwing = "ex"
    )
    public void auditFailure(JoinPoint jp, Auditable auditable, Exception ex) {
        saveAudit(auditable, false, ex.getMessage());
    }

    private void saveAudit(Auditable auditable, boolean success, String errorMessage) {
        try {
            String userId = request.getHeader("X-User-Id");
            String userRole = request.getHeader("X-User-Role");
            String correlationId = request.getHeader("X-Correlation-ID");
            String ipAddress = request.getRemoteAddr();

            AuditLog auditLog = AuditLog.builder()
                .userId(userId)
                .userRole(userRole)
                .action(auditable.action())
                .resource(auditable.resource())
                .ipAddress(ipAddress)
                .correlationId(correlationId)
                .success(success)
                .errorMessage(errorMessage)
                .timestamp(LocalDateTime.now())
                .build();

            auditLogRepository.save(auditLog);

            log.info("AUDIT User={} Role={} Action={} Resource={} Success={} IP={} CorrelationId={}",
                userId, userRole, auditable.action(), auditable.resource(),
                success, ipAddress, correlationId);

        } catch (Exception e) {
            log.error("Failed to save audit log", e);
        }
    }
}