package com.example.demo.aspect;

import com.example.demo.annotation.MaskAuthor;
import com.example.demo.dto.ModificationDto;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Aspect
@Component
public class AuthorMaskingAspect {

    private static final String MASK = "***";

    @Around("@annotation(maskAuthor)")
    public Object mask(ProceedingJoinPoint joinPoint, MaskAuthor maskAuthor) throws Throwable {
        Object result = joinPoint.proceed();

        if (isExempt(maskAuthor.exemptRole())) {
            return result;
        }

        if (result instanceof ModificationDto dto) {
            maskDto(dto, maskAuthor.visibleChars());
        } else if (result instanceof Collection<?> items) {
            for (Object item : items) {
                if (item instanceof ModificationDto dto) {
                    maskDto(dto, maskAuthor.visibleChars());
                }
            }
        }
        return result;
    }

    private boolean isExempt(String exemptRole) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        String expected = "ROLE_" + exemptRole;
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> expected.equals(authority.getAuthority()));
    }

    private void maskDto(ModificationDto dto, int visibleChars) {
        String author = dto.getAuthorUsername();
        if (author == null) {
            return;
        }
        int visible = Math.max(0, Math.min(visibleChars, author.length() - 1));
        dto.setAuthorUsername(author.substring(0, visible) + MASK);
    }
}
