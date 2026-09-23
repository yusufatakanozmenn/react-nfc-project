package com.webonix.webonix_tap_backend.repository;

import com.webonix.webonix_tap_backend.entity.NfcCard;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NfcCardRepository extends JpaRepository<NfcCard, Long> {
        boolean existsByCode(String code);
}