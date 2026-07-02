package com.example.common.web.config;

import com.example.common.web.dto.ApiResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
public class GlobalResponseWrapper implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // Do not wrap if it's already an ApiResponse
        if (returnType.getParameterType().isAssignableFrom(ApiResponse.class)) {
            return false;
        }
        // Do not wrap String because it causes ClassCastException with StringHttpMessageConverter
        if (returnType.getParameterType().isAssignableFrom(String.class)) {
            return false;
        }
        // Do not wrap byte[]
        if (returnType.getParameterType().isAssignableFrom(byte[].class)) {
            return false;
        }
        // Do not wrap ProblemDetail (errors)
        if (returnType.getParameterType().isAssignableFrom(ProblemDetail.class)) {
            return false;
        }
        // Do not wrap ResponseEntity if its generic type is ProblemDetail
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        
        String path = request.getURI().getPath();
        
        // Exclude Swagger and Actuator endpoints from wrapping
        if (path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui") || path.startsWith("/actuator")) {
            return body;
        }
        
        // Exclude ProblemDetail explicitly if it slips through
        if (body instanceof ProblemDetail) {
            return body;
        }

        // Handle successful responses
        int statusCode = 200;
        if (response instanceof ServletServerHttpResponse servletResponse) {
            statusCode = servletResponse.getServletResponse().getStatus();
        }
        
        // Only wrap successful responses (2xx)
        if (statusCode >= 200 && statusCode < 300) {
            return ApiResponse.success(body, statusCode);
        }

        return body;
    }
}
