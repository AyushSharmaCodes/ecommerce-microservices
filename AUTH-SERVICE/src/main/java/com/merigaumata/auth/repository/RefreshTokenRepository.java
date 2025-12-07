package com.merigaumata.auth.repository;

import com.merigaumata.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    List<RefreshToken> findByUserIdAndRevokedFalse(String userId);

    List<RefreshToken> findByRevokedFalse();

    void deleteByExpiryDateBefore(Instant date);

    Optional<RefreshToken> findByTokenHashAndRevokedFalse(String tokenHash);
}
