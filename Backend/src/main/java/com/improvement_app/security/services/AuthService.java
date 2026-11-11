package com.improvement_app.security.services;

import com.improvement_app.security.config.SecurityProperties;
import com.improvement_app.security.entity.*;
import com.improvement_app.security.exceptions.AccountLockedException;
import com.improvement_app.security.exceptions.EmailNotVerifiedException;
import com.improvement_app.security.exceptions.RoleNotFoundException;
import com.improvement_app.security.exceptions.UserAlreadyExistsException;
import com.improvement_app.security.jwt.JwtUtils;
import com.improvement_app.security.repository.RoleRepository;
import com.improvement_app.security.repository.UserRepository;
import com.improvement_app.security.request.LoginRequest;
import com.improvement_app.security.request.SignupRequest;
import com.improvement_app.security.response.JwtResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;
    private final EmailService emailService;
    private final UserTokenService userTokenService;
    private final SecurityProperties securityProperties;
    private final LoginAttemptService loginAttemptService;

    public record AuthResult(String accessToken, String refreshToken, JwtResponse userInfo) {}
    public record RefreshResult(String accessToken, long expiresAt) {}

    @Transactional
    public AuthResult authenticateUser(LoginRequest loginRequest) {
        log.debug("Authenticating user: {}", loginRequest.username());

        UserEntity userEntity = userRepository.findByUsername(loginRequest.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (userEntity.isAccountLocked()) {
            log.warn("Blocked login attempt for locked account: {}", loginRequest.username());
            throw new AccountLockedException("Account is temporarily locked. Try again later.");
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password()));
        } catch (BadCredentialsException e) {
            loginAttemptService.recordFailedAttempt(userEntity.getId());
            throw new BadCredentialsException("Invalid credentials");
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);

        if (!userEntity.getEmailVerified()) {
            throw new EmailNotVerifiedException("Email must be verified before login");
        }

        userEntity.resetFailedAttempts();
        userEntity.setLastLogin(Instant.now());
        userRepository.save(userEntity);

        String accessToken = jwtUtils.generateJwtToken(userEntity.getUsername(), userEntity.getRolesString());
        String refreshToken = jwtUtils.generateRefreshToken(authentication);

        long accessTokenExpiresAt = System.currentTimeMillis() + securityProperties.getJwt().getAccessTokenExpirationMs();
        long refreshTokenExpiresAt = System.currentTimeMillis() + securityProperties.getJwt().getRefreshTokenExpirationMs();

        log.debug("User authenticated successfully: {}", userEntity.getUsername());

        JwtResponse userInfo = new JwtResponse(
                userEntity.getId(), userEntity.getUsername(), userEntity.getEmail(),
                userEntity.getRolesString(), accessTokenExpiresAt, refreshTokenExpiresAt
        );

        return new AuthResult(accessToken, refreshToken, userInfo);
    }

    @Transactional(readOnly = true)
    public RefreshResult refreshToken(String rawRefreshToken) {
        try {
            Claims claims = jwtUtils.validateToken(rawRefreshToken);
            String username = claims.getSubject();

            UserEntity userEntity = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

            String newAccessToken = jwtUtils.generateJwtToken(username, userEntity.getRolesString());
            long expiresAt = System.currentTimeMillis() + securityProperties.getJwt().getAccessTokenExpirationMs();

            return new RefreshResult(newAccessToken, expiresAt);

        } catch (JwtException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token niepoprawny");
        }
    }

    @Transactional
    public void registerUser(SignupRequest signUpRequest) {
        log.debug("Registering new user: {}", signUpRequest.getUsername());

        validateUserRegistration(signUpRequest);

        UserEntity userEntity = new UserEntity(
                signUpRequest.getUsername(),
                signUpRequest.getEmail(),
                encoder.encode(signUpRequest.getPassword()),
                signUpRequest.getName(),
                signUpRequest.getSurname()
        );

        Set<Role> roles = determineUserRoles(signUpRequest.getRole());
        userEntity.setRoles(roles);

        userEntity.setEmailVerified(false);
        userEntity.setIsActive(false);
        UserEntity savedUser = userRepository.save(userEntity);

        UserTokenEntity token = userTokenService.createToken(savedUser, TokenTypeEnum.EMAIL_VERIFICATION);
        emailService.sendVerificationEmail(savedUser.getEmail(), token);

        log.info("User registered successfully: {}. Verification email sent.", userEntity.getUsername());
    }

    @Transactional
    public String verifyEmail(String token) {
        log.debug("Verifying email with token: {}", token);

        UserEntity userEntity = userTokenService.validateAndUseToken(token, TokenTypeEnum.EMAIL_VERIFICATION);

        userEntity.setEmailVerified(true);
        userEntity.setIsActive(true);

        userRepository.save(userEntity);

        log.info("Email verified successfully for user: {}", userEntity.getUsername());

        return "Email verified successfully. You can now log in.";
    }

    @Transactional
    public String resendVerificationEmail(String username) {
        log.debug("Resending verification email to: {}", username);

        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("No account found with this username"));

        UserTokenEntity verificationToken = userTokenService.createToken(userEntity, TokenTypeEnum.EMAIL_VERIFICATION);
        emailService.sendVerificationEmail(userEntity.getEmail(), verificationToken);

        log.info("Verification email resent to: {}", userEntity.getUsername());

        return "Verification email sent successfully.";
    }

    private void validateUserRegistration(SignupRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            throw new UserAlreadyExistsException("Username is already taken: " + signUpRequest.getUsername());
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new UserAlreadyExistsException("Email is already in use: " + signUpRequest.getEmail());
        }
    }

    private Set<Role> determineUserRoles(Set<String> strRoles) {
        Set<Role> roles = new HashSet<>();

        if (strRoles == null || strRoles.isEmpty()) {
            Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                    .orElseThrow(() -> new RoleNotFoundException("Default user role not found"));
            roles.add(userRole);
        } else {
            strRoles.forEach(role -> {
                switch (role.toLowerCase()) {
                    case "admin" -> {
                        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                                .orElseThrow(() -> new RoleNotFoundException("Admin role not found"));
                        roles.add(adminRole);
                    }
                    case "user" -> {
                        Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                                .orElseThrow(() -> new RoleNotFoundException("User role not found"));
                        roles.add(userRole);
                    }
                    default -> throw new RoleNotFoundException("Role not found: " + role);
                }
            });
        }

        return roles;
    }
}
