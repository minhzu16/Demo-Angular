package com.tiki.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tiki.common.dto.ComplaintDto;
import com.tiki.common.dto.CreateComplaintRequest;
import com.tiki.common.dto.ResolveComplaintRequest;
import com.tiki.common.entity.ComplaintEntity;
import com.tiki.order.service.ComplaintService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ComplaintControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ComplaintService complaintService;

    @InjectMocks
    private ComplaintController complaintController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(complaintController).build();
    }

    @Test
    @DisplayName("POST /api/v1/complaints creates complaint and returns 201 CREATED")
    void testCreateComplaint() throws Exception {
        CreateComplaintRequest request = CreateComplaintRequest.builder()
                .orderId(101L)
                .title("Lỗi vỡ màn hình")
                .description("Hàng giao bị vỡ màn hình")
                .build();

        ComplaintDto dto = new ComplaintDto();
        dto.setId(1L);
        dto.setOrderId(101L);
        dto.setBuyerId(20L);
        dto.setTitle("Lỗi vỡ màn hình");
        dto.setStatus("PENDING");

        when(complaintService.createComplaint(eq(20L), any(CreateComplaintRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/v1/complaints")
                        .header("X-User-Id", 20L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Lỗi vỡ màn hình"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("GET /api/v1/complaints/my returns list of complaints for buyer")
    void testGetMyComplaints() throws Exception {
        ComplaintDto dto = new ComplaintDto();
        dto.setId(1L);
        dto.setOrderId(101L);
        dto.setBuyerId(20L);

        when(complaintService.getMyComplaints(20L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/complaints/my")
                        .header("X-User-Id", 20L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].orderId").value(101));
    }

    @Test
    @DisplayName("PUT /api/v1/complaints/{id}/resolve resolves complaint")
    void testResolveComplaint() throws Exception {
        ResolveComplaintRequest request = ResolveComplaintRequest.builder()
                .status(ComplaintEntity.Status.RESOLVED)
                .resolution("Chấp nhận hoàn tiền")
                .build();

        ComplaintDto dto = new ComplaintDto();
        dto.setId(1L);
        dto.setStatus("RESOLVED");
        dto.setResolution("Chấp nhận hoàn tiền");

        when(complaintService.resolveComplaint(eq(1L), eq(999L), any(ResolveComplaintRequest.class))).thenReturn(dto);

        mockMvc.perform(put("/api/v1/complaints/1/resolve")
                        .header("X-User-Id", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolution").value("Chấp nhận hoàn tiền"));
    }
}
