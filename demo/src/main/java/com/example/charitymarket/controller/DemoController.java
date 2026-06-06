package com.example.charitymarket.controller;

import com.example.charitymarket.model.Charity;
import com.example.charitymarket.model.Market;
import com.example.charitymarket.model.User;
import com.example.charitymarket.repository.CharityRepository;
import com.example.charitymarket.repository.MarketRepository;
import com.example.charitymarket.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DemoController {

    private final UserRepository userRepository;
    private final CharityRepository charityRepository;
    private final MarketRepository marketRepository;

    @GetMapping("/users")
    public List<User> getUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/charities")
    public List<Charity> getCharities() {
        return charityRepository.findAll();
    }

    @GetMapping("/markets")
    public List<Market> getMarkets() {
        return marketRepository.findAll();
    }
}
