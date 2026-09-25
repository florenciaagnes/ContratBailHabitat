package com.legaltech.bail.config;

import com.legaltech.bail.entity.*;
import com.legaltech.bail.repository.*;
import com.legaltech.bail.service.ContractService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final ContractService contractService;
    private final ContractRepository contractRepository;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() > 0) {
            log.info("Données déjà présentes en base.");
            return;
        }

        log.info("Initialisation des données de démonstration LegalTech Madagascar...");

        // 1. Users
        User landlord = User.builder()
                .nom("RAKOTOARISOA")
                .prenom("Jean")
                .email("jean.rakoto@gmail.com")
                .telephone("+261 34 12 345 67")
                .passwordHash("hashed_password_123")
                .cinNumero("101234156789")
                .role(Role.LANDLORD)
                .dateNaissance(LocalDate.of(1980, 5, 12))
                .genre(Gender.HOMME)
                .situationMatrimoniale(MaritalStatus.MARIE)
                .build();
        userRepository.save(landlord);

        User tenant = User.builder()
                .nom("RABEMANANJARA")
                .prenom("Aina")
                .email("aina.rabe@gmail.com")
                .telephone("+261 32 98 765 43")
                .passwordHash("hashed_password_456")
                .cinNumero("101987265432")
                .role(Role.TENANT)
                .dateNaissance(LocalDate.of(1995, 8, 24))
                .genre(Gender.FEMME)
                .situationMatrimoniale(MaritalStatus.CELIBATAIRE)
                .build();
        userRepository.save(tenant);

        // 2. Property
        Property prop = Property.builder()
                .proprietaire(landlord)
                .adresse("Villa Soa, Lot IVG 45 Bis")
                .quartier("Isoraka")
                .ville("Antananarivo")
                .refCadastre("CAD-TNR-2024-889")
                .typeBien(PropertyType.APPARTEMENT)
                .superficie(95.0)
                .nombrePieces(4)
                .nombreChambres(3)
                .etage(2)
                .meuble(true)
                .parking(true)
                .compteurJirama(true)
                .build();
        propertyRepository.save(prop);

        // 3. Create Sample Contract WITH LEGAL ANOMALY to showcase Audit Engine!
        Contract contract1 = Contract.builder()
                .reference("BAIL-2026-ISORAKA")
                .bailleur(landlord)
                .locataire(tenant)
                .bien(prop)
                .loyer(new BigDecimal("1000000.00")) // 1,000,000 Ariary
                .charges(new BigDecimal("100000.00"))
                .depot(new BigDecimal("3000000.00"))  // 3,000,000 Ariary -> VIOLATION: > 2 months rent!
                .dateDebut(LocalDate.now())
                .dateFin(LocalDate.now().plusYears(1))
                .taciteReconduction(true)
                .build();

        List<ContractClause> clauses1 = new ArrayList<>();
        clauses1.add(ContractClause.builder()
                .ordre(1)
                .titre("Destination des lieux")
                .texte("Le bien loué est destiné exclusivement à l'usage d'habitation principale du locataire et de sa famille.")
                .build());

        clauses1.add(ContractClause.builder()
                .ordre(2)
                .titre("Dépôt de garantie")
                .texte("Le locataire verse au moment de la signature la somme de 3 000 000 Ar à titre de dépôt de garantie (soit 3 mois de loyer).")
                .build());

        clauses1.add(ContractClause.builder()
                .ordre(3)
                .titre("Résiliation et voies de fait")
                .texte("En cas de retard de loyer de plus de 5 jours, le bailleur se réserve le droit de procéder au changement des serrures et à la coupure d'eau et d'électricité sans intervention du tribunal.")
                .build());

        contractService.createContract(contract1, clauses1);
        log.info("Données de démo initialisées avec succès.");
    }
}
