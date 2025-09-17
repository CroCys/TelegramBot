package com.vadim.telegrambot.service;

import com.vadim.telegrambot.model.Payment;
import com.vadim.telegrambot.model.Subscription;
import com.vadim.telegrambot.model.User;
import com.vadim.telegrambot.repository.PaymentRepository;
import com.vadim.telegrambot.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    @Transactional
    public Payment findByIdWithRelations(Long paymentId) {
        return paymentRepository.findByIdWithUserAndSubscription(paymentId);
    }

    @Transactional
    public List<Payment> findAllDebtors() {
        return paymentRepository.findAllDebtors();
    }

    @Transactional
    public void createPayment(User user, Subscription subscription) {
        Payment payment = Payment.builder()
                .user(user)
                .subscription(subscription)
                .month(LocalDateTime.now().getMonth())
                .year(LocalDateTime.now().getYear())
                .isPayed(false)
                .build();

        paymentRepository.save(payment);
    }

    @Transactional
    public void generatePaymentsForCurrentMonth() {
        List<User> users = userRepository.findAll();

        for (User user : users) {
            for (Subscription subscription : user.getSubscriptions()) {
                Payment payment = Payment.builder()
                        .user(user)
                        .subscription(subscription)
                        .month(LocalDateTime.now().getMonth())
                        .year(LocalDateTime.now().getYear())
                        .isPayed(false)
                        .build();

                paymentRepository.save(payment);
            }
        }
    }

    @Transactional
    public void setPaymentStatusTrue(Long paymentId) {
        Payment payment = paymentRepository.getReferenceById(paymentId);
        payment.setPayed(true);
    }

    @Transactional
    public void deletePayment(User user, Subscription subscription) {
        paymentRepository.deleteByUserAndSubscription(user, subscription);
    }
}
