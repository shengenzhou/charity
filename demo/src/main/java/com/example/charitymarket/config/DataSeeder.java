package com.example.charitymarket.config;

import com.example.charitymarket.model.Charity;
import com.example.charitymarket.model.Market;
import com.example.charitymarket.model.MarketStatus;
import com.example.charitymarket.model.User;
import com.example.charitymarket.repository.CharityRepository;
import com.example.charitymarket.repository.MarketRepository;
import com.example.charitymarket.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedDemoData(
            CharityRepository charityRepository,
            UserRepository userRepository,
            MarketRepository marketRepository) {
        return args -> {
            if (userRepository.count() > 0) {
                return;
            }

            Charity redCross = Charity.builder()
                    .name("Red Cross")
                    .description("Provides emergency assistance, disaster relief, and humanitarian aid.")
                    .totalDonationsReceived(new BigDecimal("0.00"))
                    .build();
            Charity wwf = Charity.builder()
                    .name("WWF")
                    .description("Works to conserve nature and reduce the most pressing threats to biodiversity.")
                    .totalDonationsReceived(new BigDecimal("0.00"))
                    .build();
            Charity doctorsWithoutBorders = Charity.builder()
                    .name("Doctors Without Borders")
                    .description("Delivers medical humanitarian aid where it is needed most.")
                    .totalDonationsReceived(new BigDecimal("0.00"))
                    .build();

            charityRepository.saveAll(List.of(redCross, wwf, doctorsWithoutBorders));

            userRepository.saveAll(List.of(
                    User.builder()
                            .name("Alice")
                            .email("alice@example.com")
                            .balance(new BigDecimal("1000.00"))
                            .selectedCharity(redCross)
                            .build(),
                    User.builder()
                            .name("Bob")
                            .email("bob@example.com")
                            .balance(new BigDecimal("1000.00"))
                            .selectedCharity(wwf)
                            .build(),
                    User.builder()
                            .name("Charlie")
                            .email("charlie@example.com")
                            .balance(new BigDecimal("1000.00"))
                            .selectedCharity(doctorsWithoutBorders)
                            .build()));

            marketRepository.saveAll(List.of(
                    Market.builder()
                            .question("Will it rain in Amsterdam tomorrow?")
                            .yesPrice(new BigDecimal("0.42"))
                            .noPrice(new BigDecimal("0.58"))
                            .status(MarketStatus.OPEN)
                            .build(),
                    Market.builder()
                            .question("Will Bitcoin be above $100k by the end of the month?")
                            .yesPrice(new BigDecimal("0.61"))
                            .noPrice(new BigDecimal("0.39"))
                            .status(MarketStatus.OPEN)
                            .build(),
                    Market.builder()
                            .question("Will Team A win the final?")
                            .yesPrice(new BigDecimal("0.35"))
                            .noPrice(new BigDecimal("0.65"))
                            .status(MarketStatus.OPEN)
                            .build()));
        };
    }
}
