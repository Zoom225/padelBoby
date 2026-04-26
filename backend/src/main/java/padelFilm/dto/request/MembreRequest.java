package com.padelPlay.dto.request;

import com.padelPlay.entity.enums.TypeMembre;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembreRequest {

    @NotBlank(message = "Matricule is required")
    private String matricule;

    @NotBlank(message = "Last name is required")
    private String nom;

    @NotBlank(message = "First name is required")
    private String prenom;

    @Email(message = "Email format is invalid")
    private String email;

    @NotNull(message = "Member type is required")
    private TypeMembre typeMembre;

    private Long siteId;
}
