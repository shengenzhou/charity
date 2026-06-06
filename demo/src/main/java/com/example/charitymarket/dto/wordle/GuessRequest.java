package com.example.charitymarket.dto.wordle;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuessRequest {

    @NotBlank(message = "Guess is required")
    @Pattern(regexp = "[A-Za-z]{5}", message = "Guess must be exactly 5 letters")
    private String guess;
}
