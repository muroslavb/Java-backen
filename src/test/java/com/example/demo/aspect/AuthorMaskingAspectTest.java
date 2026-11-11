package com.example.demo.aspect;

import com.example.demo.annotation.MaskAuthor;
import com.example.demo.dto.ModificationDto;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthorMaskingAspectTest {

    private AuthorMaskingAspect aspect;
    private ProceedingJoinPoint joinPoint;
    private MaskAuthor maskAuthor;

    @BeforeEach
    void setUp() throws Exception {
        aspect = new AuthorMaskingAspect();
        joinPoint = mock(ProceedingJoinPoint.class);
        maskAuthor = Stub.class.getDeclaredMethod("masked").getAnnotation(MaskAuthor.class);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private ModificationDto dto(String author) {
        ModificationDto dto = new ModificationDto();
        dto.setAuthorUsername(author);
        return dto;
    }

    @Test
    void anonymousUser_authorIsMasked() throws Throwable {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        ModificationDto dto = dto("ivan_modder");
        when(joinPoint.proceed()).thenReturn(dto);

        Object result = aspect.mask(joinPoint, maskAuthor);

        assertSame(dto, result);
        assertEquals("iv***", dto.getAuthorUsername());
        verify(joinPoint, times(1)).proceed();
    }

    @Test
    void noAuthentication_authorIsMasked() throws Throwable {
        ModificationDto dto = dto("ivan_modder");
        when(joinPoint.proceed()).thenReturn(dto);

        aspect.mask(joinPoint, maskAuthor);

        assertEquals("iv***", dto.getAuthorUsername());
    }

    @Test
    void regularAuthor_authorIsMasked() throws Throwable {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("petro", "pw", "ROLE_AUTHOR"));
        ModificationDto dto = dto("ivan_modder");
        when(joinPoint.proceed()).thenReturn(dto);

        aspect.mask(joinPoint, maskAuthor);

        assertEquals("iv***", dto.getAuthorUsername());
    }

    @Test
    void admin_authorIsNotMasked() throws Throwable {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("root", "pw", "ROLE_ADMIN"));
        ModificationDto dto = dto("ivan_modder");
        when(joinPoint.proceed()).thenReturn(dto);

        aspect.mask(joinPoint, maskAuthor);

        assertEquals("ivan_modder", dto.getAuthorUsername());
        verify(joinPoint, times(1)).proceed();
    }

    @Test
    void listResult_everyAuthorIsMasked() throws Throwable {
        ModificationDto first = dto("ivan_modder");
        ModificationDto second = dto("olena");
        when(joinPoint.proceed()).thenReturn(List.of(first, second));

        aspect.mask(joinPoint, maskAuthor);

        assertEquals("iv***", first.getAuthorUsername());
        assertEquals("ol***", second.getAuthorUsername());
    }

    @Test
    void shortAuthorName_isFullyHidden() throws Throwable {
        ModificationDto dto = dto("a");
        when(joinPoint.proceed()).thenReturn(dto);

        aspect.mask(joinPoint, maskAuthor);

        assertEquals("***", dto.getAuthorUsername());
    }

    @Test
    void nullAuthor_staysNull() throws Throwable {
        ModificationDto dto = dto(null);
        when(joinPoint.proceed()).thenReturn(dto);

        aspect.mask(joinPoint, maskAuthor);

        assertNull(dto.getAuthorUsername());
    }

    @Test
    void unrelatedResultType_isReturnedUntouched() throws Throwable {
        when(joinPoint.proceed()).thenReturn("plain string");

        assertEquals("plain string", aspect.mask(joinPoint, maskAuthor));
    }

    @Test
    void exceptionFromTarget_propagatesWithoutMasking() throws Throwable {
        when(joinPoint.proceed()).thenThrow(new IllegalStateException("boom"));

        assertThrows(IllegalStateException.class, () -> aspect.mask(joinPoint, maskAuthor));
    }

    private static class Stub {

        @MaskAuthor
        void masked() {
        }
    }
}
