package com.example.charitymarket.repository;

import com.example.charitymarket.model.CharityDonation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CharityDonationRepository extends JpaRepository<CharityDonation, Long> {

    List<CharityDonation> findByUserIdOrderByCreatedAtDesc(Long userId);
}
