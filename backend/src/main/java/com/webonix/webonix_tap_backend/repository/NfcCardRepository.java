package com.webonix.webonix_tap_backend.repository;

import com.webonix.webonix_tap_backend.entity.NfcCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;
import java.util.Optional;

public interface NfcCardRepository extends JpaRepository<NfcCard, Long> {
    boolean existsByCode(String code);
    @EntityGraph(attributePaths = "owner")
    List<NfcCard> findAllByOrderByIdDesc();
    @EntityGraph(attributePaths = "owner")
    List<NfcCard> findByOwner_IdOrderByIdDesc(Long ownerId);
    @EntityGraph(attributePaths = "owner")
    Optional<NfcCard> findByIdAndOwner_Id(Long id, Long ownerId);
}
