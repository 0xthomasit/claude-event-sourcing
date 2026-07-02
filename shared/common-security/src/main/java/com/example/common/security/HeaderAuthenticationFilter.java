package com.example.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Downstream filter for services behind the API Gateway.
 * Gateway already validated JWT and injected X-User-Id / X-User-Roles headers.
 * This filter reads those trusted headers and populates the SecurityContext.
 */
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String userId     = request.getHeader(SecurityConstants.HEADER_USER_ID);
        String rolesHeader = request.getHeader(SecurityConstants.HEADER_USER_ROLES);

        if (StringUtils.hasText(userId)) {
            List<SimpleGrantedAuthority> authorities = parseRoles(rolesHeader);
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        filterChain.doFilter(request, response);
    }

    private List<SimpleGrantedAuthority> parseRoles(String rolesHeader) {
        if (!StringUtils.hasText(rolesHeader)) return Collections.emptyList();
        return Arrays.stream(rolesHeader.split(","))
                .map(e -> e.trim())
                .filter(StringUtils::hasText)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }
}
