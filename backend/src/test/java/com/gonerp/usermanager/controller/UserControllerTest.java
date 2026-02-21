package com.gonerp.usermanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gonerp.config.JwtTokenProvider;
import com.gonerp.usermanager.dto.UserRequest;
import com.gonerp.usermanager.dto.UserResponse;
import com.gonerp.usermanager.model.enums.UserStatus;
import com.gonerp.usermanager.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private UserService userService;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void findAll_asAdmin_returnsUsers() throws Exception {
        UserResponse user = UserResponse.builder()
                .id(1L).userName("john").firstName("John").status(UserStatus.ACTIVE).build();
        when(userService.findAll(any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of(user)));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].userName").value("john"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void findAll_asNonAdmin_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_validUser_returnsCreated() throws Exception {
        UserRequest request = new UserRequest();
        request.setUserName("jane");
        request.setFirstName("Jane");
        request.setPassword("pass123");
        request.setRoleId(1L);

        UserResponse response = UserResponse.builder()
                .id(2L).userName("jane").firstName("Jane").status(UserStatus.PENDING).build();
        when(userService.create(any())).thenReturn(response);

        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.userName").value("jane"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void softDelete_changesStatusToDeleted() throws Exception {
        UserResponse response = UserResponse.builder()
                .id(1L).userName("john").status(UserStatus.DELETED).build();
        when(userService.softDelete(1L)).thenReturn(response);

        mockMvc.perform(delete("/api/users/1").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DELETED"));
    }
}
