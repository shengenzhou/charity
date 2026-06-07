package com.example.charitymarket.config;

import com.example.charitymarket.config.AuthMode;
import com.example.charitymarket.config.HackathonAuthProperties;
import com.example.charitymarket.model.Charity;
import com.example.charitymarket.model.Market;
import com.example.charitymarket.model.MarketStatus;
import com.example.charitymarket.model.User;
import com.example.charitymarket.repository.CharityRepository;
import com.example.charitymarket.repository.MarketRepository;
import com.example.charitymarket.repository.UserRepository;
import com.example.charitymarket.service.SimulationService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSeeder {

    private static final int DEFAULT_EXPIRY_TIMESTAMP = 4;
    private static final BigDecimal INITIAL_DONATIONS = new BigDecimal("0.00");

    @Bean
    CommandLineRunner seedDemoData(
            CharityRepository charityRepository,
            UserRepository userRepository,
            MarketRepository marketRepository,
            SimulationService simulationService,
            HackathonAuthProperties authProperties) {
        return args -> {
            if (userRepository.count() > 0) {
                simulationService.initializeSimulation();
                return;
            }

            Charity redCross = Charity.builder()
                    .name("Red Cross")
                    .description("Provides emergency assistance, disaster relief, and humanitarian aid.")
                    .totalDonationsReceived(INITIAL_DONATIONS)
                    .build();
            Charity wwf = Charity.builder()
                    .name("WWF")
                    .description("Works to conserve nature and reduce the most pressing threats to biodiversity.")
                    .totalDonationsReceived(INITIAL_DONATIONS)
                    .build();
            Charity doctorsWithoutBorders = Charity.builder()
                    .name("Doctors Without Borders")
                    .description("Delivers medical humanitarian aid where it is needed most.")
                    .totalDonationsReceived(INITIAL_DONATIONS)
                    .build();

            charityRepository.saveAll(List.of(redCross, wwf, doctorsWithoutBorders));

            if (authProperties.getMode() != AuthMode.INVITE) {
                userRepository.saveAll(List.of(
                        User.builder()
                                .name("Alice")
                                .email("alice@example.com")
                                .balance(new BigDecimal("1000.00"))
                                .usernameConfigured(true)
                                .selectedCharity(redCross)
                                .build(),
                        User.builder()
                                .name("Bob")
                                .email("bob@example.com")
                                .balance(new BigDecimal("1000.00"))
                                .usernameConfigured(true)
                                .selectedCharity(wwf)
                                .build(),
                        User.builder()
                                .name("Charlie")
                                .email("charlie@example.com")
                                .balance(new BigDecimal("1000.00"))
                                .usernameConfigured(true)
                                .selectedCharity(doctorsWithoutBorders)
                                .build()));
            }

            marketRepository.saveAll(List.of(
                    market("Will the malaria incidence rate in Region X decrease by at least 15% by December 2028?", "0.42", 1),
                    market("Will Country X achieve over 90% childhood vaccination coverage by the end of 2027?", "0.61", 2),
                    market("Will Charity X distribute 1 million mosquito nets by December 2027?", "0.35", 3),
                    market("Will Project Y provide clean drinking water access to 500,000 people by 2028?", "0.57", 4),
                    market("Will Literacy Program A improve reading proficiency by at least 10 percentage points by 2028?", "0.48", 5),
                    market("Will School Initiative B increase school attendance above 85% by the end of 2027?", "0.66", 6),
                    market("Will NGO X provide educational access to 100,000 new students by 2028?", "0.53", 7),
                    market("Will Program Y achieve a student graduation rate above 75% by 2029?", "0.39", 8),
                    market("Will 90% of households affected by Hurricane X regain electricity within 30 days?", "0.58", 9),
                    market("Will Emergency Housing Project Y provide shelter to 50,000 displaced people within 6 months?", "0.44", 10),
                    market("Will Flood Recovery Fund X rebuild at least 80% of damaged schools by 2028?", "0.63", 11),
                    market("Will Microfinance Program X achieve a loan repayment rate above 95% by 2027?", "0.55", 12),
                    market("Will Community Project Y create 10,000 sustainable jobs by 2028?", "0.37", 13),
                    market("Will extreme poverty in Region X decrease by at least 5 percentage points by 2030?", "0.46", 14),
                    market("Will Reforestation Project X plant 10 million trees by 2028?", "0.69", 15),
                    market("Will Conservation Program Y increase the population of Species Z by at least 20% by 2030?", "0.41", 16),
                    market("Will Carbon Removal Initiative X remove 100,000 tons of CO2 by 2028?", "0.34", 17),
                    market("Will Charity X achieve at least 90% of its stated annual impact targets in 2027?", "0.62", 18),
                    market("Will Project Y complete construction before its announced deadline?", "0.51", 19),
                    market("Will NGO Z keep administrative expenses below 15% of total spending in 2027?", "0.47", 20),
                    market("Will Region X experience famine conditions before June 2028?", "0.28", 21),
                    market("Will refugee displacement in Region Y exceed 100,000 people by the end of 2027?", "0.59", 22),
                    market("Will Disease Outbreak Z exceed 50,000 reported cases before December 2027?", "0.33", 23)));

            simulationService.initializeSimulation();
        };
    }

    private Market market(String question, String yesPrice, int dayOffset) {
        BigDecimal yes = new BigDecimal(yesPrice);
        BigDecimal no = BigDecimal.ONE.subtract(yes);

        return Market.builder()
                .question(question)
                .yesPrice(yes)
                .noPrice(no)
                .initialYesPrice(yes)
                .initialNoPrice(no)
                .currentTimestamp(0)
                .expiryTimestamp(DEFAULT_EXPIRY_TIMESTAMP)
                .expiresAt(LocalDateTime.now().plusDays(dayOffset))
                .resolvedOutcome(null)
                .payoutCompleted(false)
                .status(MarketStatus.OPEN)
                .build();
    }
}
