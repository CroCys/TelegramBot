package com.vadim.telegrambot.repository;

import com.vadim.telegrambot.model.Subscription;
import com.vadim.telegrambot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.vadim.telegrambot.model.Payment;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("SELECT p FROM Payment p JOIN FETCH p.user JOIN FETCH p.subscription WHERE p.id = :id")
    Payment findByIdWithUserAndSubscription(@Param("id") Long id);

    @Query("""
                SELECT p FROM Payment p
                JOIN FETCH p.user
                JOIN FETCH p.subscription
                WHERE p.isPayed = false
            """)
    List<Payment> findAllDebtors();

    @Modifying
    @Query("DELETE FROM Payment p WHERE p.user = :user AND p.subscription = :subscription")
    void deleteByUserAndSubscription(@Param("user") User user, @Param("subscription") Subscription subscription);
}
