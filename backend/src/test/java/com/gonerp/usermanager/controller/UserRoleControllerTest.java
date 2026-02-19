package com.gonerp.usermanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gonerp.config.JwtTokenProvider;
import com.gonerp.usermanager.dto.UserRoleRequest;
import com.gonerp.usermanager.dto.UserRoleResponse;
import com.gonerp.usermanager.model.enums.RoleName;
import com.gonerp.usermanager.service.UserRoleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserRoleController.class)
class UserRoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRoleService userRoleService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void findAll_returnsRoles() throws Exception {
        UserRoleResponse role = UserRoleResponse.builder()
                .id(1L).name(RoleName.ADMIN).description("Admin").build();
        when(userRoleService.findAll()).thenReturn(List.of(role));

        mockMvc.perform(get("/api/user-roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("ADMIN"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_validRequest_returnsCreated() throws Exception {
        UserRoleRequest request = new UserRoleRequest();
        request.setName(RoleName.USER);
        request.setDescription("Standard user");

        UserRoleResponse response = UserRoleResponse.builder()
                .id(2L).name(RoleName.USER).description("Standard user").build();
        when(userRoleService.create(any())).thenReturn(response);

        mockMvc.perform(post("/api/user-roles")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("USER"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_existingRole_returnsOk() throws Exception {
        doNothing().when(userRoleService).delete(1L);

        mockMvc.perform(delete("/api/user-roles/1").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    void create_asNonAdmin_returnsForbidden() throws Exception {
        UserRoleRequest request = new UserRoleRequest();
        request.setName(RoleName.USER);

        mockMvc.perform(post("/api/user-roles")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
