package com.example.demo.controller;

import com.example.demo.exception.AsyncExceptionAdvice;
import com.example.demo.exception.ReportTimeoutException;
import com.example.demo.service.ModReportService;
import com.example.demo.service.ModificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AsyncExceptionAdviceTest {

    @Mock
    private ModificationService modificationService;

    @Mock
    private ModReportService reportService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ModificationController(modificationService, reportService))
                .setControllerAdvice(new AsyncExceptionAdvice())
                .build();
    }

    @Test
    void rejectedTask_returns503WithRetryHintInsteadOf500() throws Exception {
        when(reportService.buildReport(1L)).thenThrow(new TaskRejectedException("executor is full"));

        MvcResult result = mockMvc.perform(get("/api/mods/1/report"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Retry-After", "5"))
                .andReturn();

        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(body.contains("спробуйте пізніше"));
    }

    @Test
    void reportTimeout_returns504() throws Exception {
        when(reportService.buildReport(2L)).thenThrow(new ReportTimeoutException("timeout"));

        MvcResult result = mockMvc.perform(get("/api/mods/2/report"))
                .andExpect(status().isGatewayTimeout())
                .andReturn();

        assertEquals(504, result.getResponse().getStatus());
    }
}
