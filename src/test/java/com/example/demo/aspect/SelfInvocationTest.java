package com.example.demo.aspect;

import com.example.demo.async.NotificationAsyncService;
import com.example.demo.dto.ModificationDto;
import com.example.demo.entity.Modification;
import com.example.demo.mapper.ModificationMapper;
import com.example.demo.repository.ModificationRepository;
import com.example.demo.service.ModificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.aop.support.AopUtils;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(SelfInvocationTest.Config.class)
class SelfInvocationTest {

    @Configuration
    @EnableAspectJAutoProxy
    @Import({AuthorMaskingAspect.class, RetryAspect.class, RateLimitAspect.class})
    static class Config {

        @Bean
        ModificationRepository modificationRepository() {
            return mock(ModificationRepository.class);
        }

        @Bean
        ModificationMapper modificationMapper() {
            return mock(ModificationMapper.class);
        }

        @Bean
        ModificationService modificationService(ModificationRepository repository, ModificationMapper mapper) {
            return new ModificationService(repository, mapper, mock(NotificationAsyncService.class));
        }
    }

    @Autowired
    private ModificationService service;

    @Autowired
    private ModificationRepository repository;

    @Autowired
    private ModificationMapper mapper;

    @BeforeEach
    void setUp() {
        reset(repository, mapper);
        Modification entity = new Modification();
        entity.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(mapper.toDto(entity)).thenAnswer(invocation -> {
            ModificationDto dto = new ModificationDto();
            dto.setId(1L);
            dto.setAuthorUsername("ivan_modder");
            return dto;
        });
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void serviceBeanIsWrappedInProxy() {
        assertTrue(AopUtils.isAopProxy(service));
    }

    @Test
    void externalCall_goesThroughProxyAndActivatesAspect() {
        ModificationDto result = service.getModById(1L);

        assertEquals("iv***", result.getAuthorUsername());
    }

    @Test
    void selfInvocation_bypassesProxyAndAspectDoesNotFire() {
        ModificationDto result = service.getModForExport(1L);

        assertEquals("ivan_modder", result.getAuthorUsername());
    }
}
