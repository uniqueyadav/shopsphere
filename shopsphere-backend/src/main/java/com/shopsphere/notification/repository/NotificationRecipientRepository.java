package com.shopsphere.notification.repository;

import java.util.List;
import com.shopsphere.common.enums.Role;
import com.shopsphere.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRecipientRepository extends JpaRepository<User,Long>{List<User> findByRole(Role role);}
