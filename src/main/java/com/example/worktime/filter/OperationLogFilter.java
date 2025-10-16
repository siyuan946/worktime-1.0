package com.example.worktime.filter;

import com.example.worktime.model.OperationLog;
import com.example.worktime.repository.OperationLogRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class OperationLogFilter extends OncePerRequestFilter {
    private final OperationLogRepository repository;

    public OperationLogFilter(OperationLogRepository repository) {
        this.repository = repository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String user = request.getHeader("X-User");
        filterChain.doFilter(request, response);
        if (user != null && request.getAttribute("operationLogged") == null
                && !request.getRequestURI().startsWith("/api/logs")) {
            OperationLog log = new OperationLog();
            log.setUsername(user);
            String path = request.getRequestURI().replaceFirst("^/api", "");
            log.setAction(request.getMethod() + " " + path);
            log.setTimestamp(LocalDateTime.now());
            repository.save(log);
        }
    }
}


