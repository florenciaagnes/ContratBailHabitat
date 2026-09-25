package com.legaltech.bail.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.legaltech.bail.entity.*;
import com.legaltech.bail.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class SignatureService {

    private static final Logger log = LoggerFactory.getLogger(SignatureService.class);

    private final ContractRepository contractRepository;
    private final SignatureRepository signatureRepository;
    private final UserRepository userRepository;

    // Memory cache for OTP codes (contractId + userId -> OTP)
    private final Map<String, String> otpCache = new ConcurrentHashMap<>();

    public String generateOtp(Long contractId, Long userId) {
        // Generate 6-digit random code or default demo code 123456
        String key = contractId + ":" + userId;
        String otp = String.format("%06d", (int) (Math.random() * 900000) + 100000);
        otpCache.put(key, otp);
        log.info("[OTP GENERATED] Key: {}, OTP: {}", key, otp);
        return otp;
    }

    @Transactional
    public Signature signContract(Long contractId, Long userId, String otp, String ipAddress) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contrat introuvable : " + contractId));

        if (contract.getStatut() == ContractStatus.ACTION_REQUIRED) {
            throw new IllegalStateException("Le contrat contient des infractions juridiques non résolues. Signature bloquée.");
        }

        User signataire = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable : " + userId));

        // OTP verification (Accept generated OTP or standard demo OTP '123456')
        String key = contractId + ":" + userId;
        String cachedOtp = otpCache.get(key);
        boolean isValid = "123456".equals(otp) || (cachedOtp != null && cachedOtp.equals(otp));

        if (!isValid) {
            throw new IllegalArgumentException("Code OTP invalide ou expiré.");
        }

        LocalDateTime now = LocalDateTime.now();
        String hashInput = contract.getReference() + "|" +
                signataire.getCinNumero() + "|" +
                signataire.getEmail() + "|" +
                contract.getLoyer() + "|" +
                now.toString();

        String sha256Hash = computeSha256(hashInput);

        // Save Signature
        Signature sig = Signature.builder()
                .contract(contract)
                .signataire(signataire)
                .hashSha256(sha256Hash)
                .timestamp(now)
                .ipAddress(ipAddress != null ? ipAddress : "127.0.0.1")
                .otpValide(true)
                .build();

        Signature savedSig = signatureRepository.save(sig);
        contract.addSignature(savedSig);

        // Check if both parties (or at least 1 signature) complete signature process
        long sigCount = signatureRepository.findByContractId(contractId).size();
        if (sigCount >= 1) {
            contract.setStatut(ContractStatus.SIGNED);
            log.info("Contrat {} scellé et signé avec succès ! (Hash: {})", contract.getReference(), sha256Hash);
        }

        contractRepository.save(contract);
        otpCache.remove(key);
        return savedSig;
    }

    public byte[] generateSealedPdf(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contrat introuvable : " + contractId));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 50, 50);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Header Title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLUE);
            Paragraph title = new Paragraph("CONTRAT DE BAIL D'HABITATION (ORD. N° 62-100)", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);
            Paragraph sub = new Paragraph("République de Madagascar - Fitiavana - Tanindrazana - Fandrosoana", subFont);
            sub.setAlignment(Element.ALIGN_CENTER);
            document.add(sub);
            document.add(Chunk.NEWLINE);

            // Ref & Dates
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font normFont = FontFactory.getFont(FontFactory.HELVETICA, 11);

            document.add(new Paragraph("Référence Contrat : " + contract.getReference(), boldFont));
            document.add(new Paragraph("Date d'effet : " + contract.getDateDebut() + " au " + contract.getDateFin(), normFont));
            document.add(new Paragraph("Statut : " + contract.getStatut(), boldFont));
            document.add(Chunk.NEWLINE);

            // Parties Table with generous padding and line spacing
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(15f);

            Font partyHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(30, 60, 114));
            Font partyBodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);

            // Cellule Bailleur
            PdfPCell cell1 = new PdfPCell();
            cell1.setPaddingTop(12f);
            cell1.setPaddingBottom(14f);
            cell1.setPaddingLeft(14f);
            cell1.setPaddingRight(14f);
            cell1.setBackgroundColor(new Color(245, 247, 250));
            cell1.setBorderColor(new Color(210, 215, 225));

            Paragraph p1Header = new Paragraph("BAILLEUR (Propriétaire) :", partyHeaderFont);
            p1Header.setSpacingAfter(6f);
            cell1.addElement(p1Header);

            Paragraph p1Body = new Paragraph(
                    contract.getBailleur().getNomComplet() + "\n" +
                    "CIN N° : " + (contract.getBailleur().getCinNumero() != null ? contract.getBailleur().getCinNumero() : "N/A") + "\n" +
                    "Email : " + contract.getBailleur().getEmail(), partyBodyFont);
            p1Body.setLeading(16f);
            cell1.addElement(p1Body);

            // Cellule Locataire
            PdfPCell cell2 = new PdfPCell();
            cell2.setPaddingTop(12f);
            cell2.setPaddingBottom(14f);
            cell2.setPaddingLeft(14f);
            cell2.setPaddingRight(14f);
            cell2.setBackgroundColor(new Color(245, 247, 250));
            cell2.setBorderColor(new Color(210, 215, 225));

            Paragraph p2Header = new Paragraph("LOCATAIRE (Preneur) :", partyHeaderFont);
            p2Header.setSpacingAfter(6f);
            cell2.addElement(p2Header);

            Paragraph p2Body = new Paragraph(
                    contract.getLocataire().getNomComplet() + "\n" +
                    "CIN N° : " + (contract.getLocataire().getCinNumero() != null ? contract.getLocataire().getCinNumero() : "N/A") + "\n" +
                    "Email : " + contract.getLocataire().getEmail(), partyBodyFont);
            p2Body.setLeading(16f);
            cell2.addElement(p2Body);

            table.addCell(cell1);
            table.addCell(cell2);
            document.add(table);

            // Property Details
            document.add(new Paragraph("DESIGNATION DU BIEN LOUÉ :", boldFont));
            document.add(new Paragraph("Adresse : " + contract.getBien().getAdresse() + ", " + contract.getBien().getQuartier() + ", " + contract.getBien().getVille(), normFont));
            document.add(new Paragraph("Loyer Mensuel : " + contract.getLoyer() + " Ariary | Charges : " + contract.getCharges() + " Ariary | Dépôt : " + contract.getDepot() + " Ariary", normFont));
            document.add(Chunk.NEWLINE);

            // Clauses
            document.add(new Paragraph("CLAUSES DU CONTRAT :", boldFont));
            for (ContractClause c : contract.getClauses()) {
                document.add(new Paragraph("Clause " + c.getOrdre() + " - " + c.getTitre(), boldFont));
                document.add(new Paragraph(c.getTexte(), normFont));
                document.add(Chunk.NEWLINE);
            }

            // Cryptographic Signatures Seal
            document.add(Chunk.NEWLINE);
            Paragraph sealTitle = new Paragraph("EMPREINTE NUMÉRIQUE ET SCELLÉ DE SÉCURITÉ SHA-256", boldFont);
            sealTitle.setAlignment(Element.ALIGN_CENTER);
            document.add(sealTitle);

            List<Signature> signatures = signatureRepository.findByContractId(contractId);
            if (!signatures.isEmpty()) {
                for (Signature sig : signatures) {
                    Paragraph sigInfo = new Paragraph("Signé par " + sig.getSignataire().getNomComplet() +
                            " le " + sig.getTimestamp().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) +
                            " (IP: " + sig.getIpAddress() + ")\nHash SHA-256 : " + sig.getHashSha256(),
                            FontFactory.getFont(FontFactory.COURIER, 9, Color.DARK_GRAY));
                    document.add(sigInfo);
                }
            } else {
                document.add(new Paragraph("[En attente de signature numérique]", normFont));
            }

            document.close();
        } catch (Exception e) {
            log.error("Erreur génération PDF OpenPDF: ", e);
        }

        return out.toByteArray();
    }

    private String computeSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Erreur de calcul du hash SHA-256", e);
        }
    }
}
