package com.example.charitymarket.controller;

import com.example.charitymarket.dto.TradeRequest;
import com.example.charitymarket.dto.TradeResponse;
import com.example.charitymarket.service.TradeService;
import com.example.charitymarket.repository.TradeRepository;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TradeController {

    private final TradeService tradeService;
    private final TradeRepository tradeRepository;

    @PostMapping("/trades")
    public TradeResponse createTrade(@Valid @RequestBody TradeRequest request) {
        return tradeService.executeTrade(request);
    }

    @GetMapping("/trades/user/{userId}")
    public List<TradeResponse> getTradesByUser(@PathVariable Long userId) {
        return tradeRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(tradeService::toTradeResponse)
                .toList();
    }
}
