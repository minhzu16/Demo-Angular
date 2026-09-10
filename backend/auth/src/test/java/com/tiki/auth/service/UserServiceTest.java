package com.tiki.auth.service;

import com.tiki.auth.dto.UpdateProfileRequest;
import com.tiki.auth.entity.User;
import com.tiki.auth.repository.LoyaltyTransactionRepository;
import com.tiki.auth.repository.UserAddressRepository;
import com.tiki.auth.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceTest - Validating Bug 13 Privilege Escalation Defense")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserAddressRepository userAddressRepository;

    @Mock
    private LoyaltyTransactionRepository loyaltyTransactionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User sampleBuyer;

    @BeforeEach
    void setUp() {
        sampleBuyer = new User();
        sampleBuyer.setId(10L);
        sampleBuyer.setUsername("buyer_alice");
        sampleBuyer.setEmail("alice@gmail.com");
        sampleBuyer.setFullName("Alice Nguyen");
        sampleBuyer.setRole(User.Role.BUYER);

        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("buyer_alice");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Bug 13: updateProfile allows safe profile fields but PREVENTS role tampering or privilege escalation")
    void testUpdateProfile_CannotChangeUserRole() {
        when(userRepository.findByUsername("buyer_alice")).thenReturn(Optional.of(sampleBuyer));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("Alice Updated");
        request.setPhoneNumber("0987654321");
        request.setAddress("456 Nguyen Trai, Ha Noi");

        User updated = userService.updateProfile(request);

        assertThat(updated).isNotNull();
        assertThat(updated.getFullName()).isEqualTo("Alice Updated");
        assertThat(updated.getPhoneNumber()).isEqualTo("0987654321");
        assertThat(updated.getAddress()).isEqualTo("456 Nguyen Trai, Ha Noi");

        // Role MUST strictly remain BUYER, immune to client elevation attacks
        assertThat(updated.getRole()).isEqualTo(User.Role.BUYER);
        verify(userRepository).save(sampleBuyer);
    }
}
