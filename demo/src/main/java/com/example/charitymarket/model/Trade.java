package com.example.charitymarket.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private User user;

    @ManyToOne(optional = false)
    private Market market;

    @Enumerated(EnumType.STRING)
    private Outcome outcome;

    @Enumerated(EnumType.STRING)
    private TradeSide side;

    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal tradeValue;
    private BigDecimal fee;

    @ManyToOne(optional = false)
    private Charity charity;

    private LocalDateTime createdAt;
}
