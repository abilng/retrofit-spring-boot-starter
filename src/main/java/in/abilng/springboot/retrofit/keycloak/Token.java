package in.abilng.springboot.retrofit.keycloak;

import java.time.LocalDateTime;

public record Token(String accessToken, LocalDateTime expireTime) {}
