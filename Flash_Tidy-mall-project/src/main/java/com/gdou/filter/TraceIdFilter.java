package com.gdou.filter;


import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * 每个 HTTP 请求生成/沿用 traceId，写入 MDC，回写到响应头
 */
@Component
public class TraceIdFilter implements Filter {
    private static final String TRACE_ID = "traceId";
    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String traceId = request.getHeader(TRACE_ID_HEADER);
//        请求头没带则生成16位uuid
        if (traceId == null|| traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
//        MDC中threadlocal存放traceId
        MDC.put(TRACE_ID, traceId);
//        回写响应头，方便查看
        response.addHeader(TRACE_ID_HEADER, traceId);
        try{
            filterChain.doFilter(request, response);
        }finally {
            MDC.clear();
        }
    }
}
