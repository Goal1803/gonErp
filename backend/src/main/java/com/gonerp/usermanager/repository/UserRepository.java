package com.gonerp.usermanager.repository;

import com.gonerp.usermanager.model.User;
import com.gonerp.usermanager.model.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserName(String userName);

    boolean existsByUserName(String userName);

    @Query("""
        SELECT u FROM User u
        WHERE (:status IS NULL OR u.status = :status)
          AND (:search IS NULL OR :search = ''
              OR LOWER(u.userName) LIKE LOWER(CONCAT('%', :search, '%'))
              OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
              OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    Page<User> findAllWithFilters(
            @Param("status") UserStatus status,
            @Param("search") String search,
            Pageable pageable
    );
}
