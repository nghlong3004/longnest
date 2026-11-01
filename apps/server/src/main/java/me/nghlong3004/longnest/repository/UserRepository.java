package me.nghlong3004.longnest.repository;

import java.util.UUID;
import me.nghlong3004.longnest.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {}
