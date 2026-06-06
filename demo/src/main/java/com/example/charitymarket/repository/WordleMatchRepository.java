package com.example.charitymarket.repository;

import com.example.charitymarket.model.WordleMatch;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WordleMatchRepository extends JpaRepository<WordleMatch, Long> {

    List<WordleMatch> findAllByOrderByCreatedAtDesc();
}
