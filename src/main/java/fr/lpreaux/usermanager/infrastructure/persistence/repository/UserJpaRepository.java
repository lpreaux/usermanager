package fr.lpreaux.usermanager.infrastructure.persistence.repository;

import fr.lpreaux.usermanager.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByLogin(String login);

    @Query("SELECT u FROM UserEntity u JOIN u.emails e WHERE e.email = :email")
    Optional<UserEntity> findByEmail(@Param("email") String email);

    boolean existsByLogin(String login);

    @Query("SELECT COUNT(u) > 0 FROM UserEntity u JOIN u.emails e WHERE e.email = :email")
    boolean existsByEmail(@Param("email") String email);
}