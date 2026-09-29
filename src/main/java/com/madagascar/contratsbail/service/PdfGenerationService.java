package com.madagascar.contratsbail.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.madagascar.contratsbail.entity.*;
import com.madagascar.contratsbail.entity.enums.RolePartie;
import com.madagascar.contratsbail.entity.enums.TypePartie;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Construit le PDF du contrat de bail en respectant la structure demandee :
 * titre centre, bloc "Entre" (proprietaire / locataire), articles numerotes,
 * date/lieu et signatures.
 *
 * Aucune image de reference n'a ete fournie dans la conversation : la mise en page
 * ci-dessous suit fidelement la structure textuelle decrite dans la specification
 * (section 5) et devra etre affinee si un modele visuel est communique ensuite.
 */
@Service
public class PdfGenerationService {

    private static final DateTimeFormatter DATE_FR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Font titreFont = new Font(Font.HELVETICA, 16, Font.BOLD);
    private final Font sousTitreFont = new Font(Font.HELVETICA, 11, Font.BOLD);
    private final Font texteFont = new Font(Font.HELVETICA, 11, Font.NORMAL);
    private final Font texteGrasFont = new Font(Font.HELVETICA, 11, Font.BOLD);
    private final Font signatureLabelFont = new Font(Font.HELVETICA, 10, Font.BOLD);

    public byte[] genererPdf(Contrat contrat) {
        try {
            Document document = new Document(PageSize.A4, 60, 60, 50, 50);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            document.open();

            ajouterTitre(document);
            ajouterParties(document, contrat);
            ajouterIntroductionArticles(document);
            ajouterArticles(document, contrat);
            ajouterDateEtLieu(document);
            ajouterSignatures(document, contrat, p -> contrat.getSignatures().stream()
                    .filter(sg -> sg.getPersonne().getId().equals(p.getId()))
                    .map(Signature::getSignatureData)
                    .findFirst());

            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("Erreur lors de la generation du PDF du contrat " + contrat.getNumero(), e);
        }
    }


    /** PDF de l'avenant : reference au contrat, parties, clauses, signatures des parties. */
    public byte[] genererPdfAvenant(Avenant avenant) {
        try {
            Contrat contrat = avenant.getContrat();
            Document document = new Document(PageSize.A4, 60, 60, 50, 50);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            document.open();

            String numero = avenant.getNumero();
            int idx = numero.lastIndexOf("-AV");
            String rang = idx >= 0 ? numero.substring(idx + 3) : numero;

            Paragraph titre = new Paragraph("AVENANT N° " + rang + " AU CONTRAT DE BAIL", titreFont);
            titre.setAlignment(Element.ALIGN_CENTER);
            titre.setSpacingAfter(8f);
            document.add(titre);

            Paragraph reference = new Paragraph("Contrat de bail N° " + contrat.getNumero()
                    + " établi le " + contrat.getDateCreation().toLocalDate().format(DATE_FR), texteFont);
            reference.setAlignment(Element.ALIGN_CENTER);
            reference.setSpacingAfter(20f);
            document.add(reference);

            ajouterParties(document, contrat);

            Paragraph convenu = new Paragraph("Il a été convenu ce qui suit :", sousTitreFont);
            convenu.setSpacingBefore(10f);
            convenu.setSpacingAfter(10f);
            document.add(convenu);

            Paragraph objet = new Paragraph();
            objet.add(new Chunk("Objet : ", texteGrasFont));
            objet.add(new Chunk(avenant.getObjet(), texteFont));
            objet.add(Chunk.NEWLINE);
            objet.add(new Chunk("Date d'effet : ", texteGrasFont));
            objet.add(new Chunk(avenant.getDateEffet().format(DATE_FR), texteFont));
            objet.setSpacingAfter(10f);
            document.add(objet);

            int numeroArticle = 1;
            for (AvenantClause clause : avenant.getClauses()) {
                ajouterArticleAvenant(document, numeroArticle++, clause.getTitre(), clause.getContenu());
            }
            if (avenant.getNouvelleDateFin() != null) {
                ajouterArticleAvenant(document, numeroArticle, "NOUVELLE DATE DE FIN",
                        "Les parties conviennent que le bail prend fin le "
                                + avenant.getNouvelleDateFin().format(DATE_FR) + ".");
            }

            Paragraph maintien = new Paragraph(
                    "Toutes les autres clauses et conditions du contrat de bail initial demeurent inchangées.",
                    texteFont);
            maintien.setSpacingBefore(14f);
            document.add(maintien);

            ajouterDateEtLieu(document);
            ajouterSignatures(document, contrat, p -> avenant.getSignatures().stream()
                    .filter(sg -> sg.getPersonne().getId().equals(p.getId()))
                    .map(SignatureAvenant::getSignatureData)
                    .findFirst());

            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("Erreur lors de la generation du PDF de l'avenant " + avenant.getNumero(), e);
        }
    }

    private void ajouterArticleAvenant(Document document, int numero, String titre, String contenu)
            throws DocumentException {
        Paragraph t = new Paragraph("Article " + numero + " : " + titre, texteGrasFont);
        t.setSpacingBefore(10f);
        t.setSpacingAfter(4f);
        document.add(t);

        Paragraph c = new Paragraph(contenu, texteFont);
        c.setAlignment(Element.ALIGN_JUSTIFIED);
        c.setIndentationLeft(10f);
        c.setSpacingAfter(6f);
        document.add(c);
    }

    private void ajouterTitre(Document document) throws DocumentException {
        Paragraph titre = new Paragraph("CONTRAT DE BAIL", titreFont);
        titre.setAlignment(Element.ALIGN_CENTER);
        titre.setSpacingAfter(24f);
        document.add(titre);
    }

    private void ajouterParties(Document document, Contrat contrat) throws DocumentException {
        Paragraph entre = new Paragraph("Entre", sousTitreFont);
        entre.setSpacingAfter(8f);
        document.add(entre);

        Optional<PartieContrat> proprietaire = contrat.getParties().stream()
                .filter(p -> p.getRole() == RolePartie.PROPRIETAIRE).findFirst();
        Optional<PartieContrat> locataire = contrat.getParties().stream()
                .filter(p -> p.getRole() == RolePartie.LOCATAIRE).findFirst();

        proprietaire.ifPresent(p -> ajouterBlocPartie(document, p, "d'une part"));
        locataire.ifPresent(p -> ajouterBlocPartie(document, p, "d'autre part"));
    }

    private void ajouterBlocPartie(Document document, PartieContrat partie, String mention) {
        try {
            if (partie.getTypePartie() == TypePartie.PERSONNE && partie.getPersonne() != null) {
                Personne p = partie.getPersonne();
                Paragraph bloc = new Paragraph();
                bloc.add(new Chunk("Monsieur/Madame : ", texteFont));
                bloc.add(new Chunk(p.getNomComplet(), texteGrasFont));
                bloc.add(Chunk.NEWLINE);
                bloc.add(new Chunk("CIN N° : " + valeurOuVide(p.getCin())
                        + (p.getDateDelivranceCin() != null ? ", délivrée le " + p.getDateDelivranceCin().format(DATE_FR) : "")
                        + (p.getLieuDelivranceCin() != null ? " à " + p.getLieuDelivranceCin() : ""), texteFont));
                bloc.add(Chunk.NEWLINE);
                if (p.getAdresse() != null && !p.getAdresse().isBlank()) {
                    bloc.add(new Chunk("Demeurant à : " + p.getAdresse(), texteFont));
                    bloc.add(Chunk.NEWLINE);
                }
                bloc.add(new Chunk(mention, texteFont));
                bloc.setSpacingAfter(14f);
                bloc.setIndentationLeft(20f);
                document.add(bloc);
            } else if (partie.getTypePartie() == TypePartie.ORGANISATION && partie.getOrganisation() != null) {
                Organisation org = partie.getOrganisation();
                Paragraph bloc = new Paragraph();
                bloc.add(new Chunk("La société : ", texteFont));
                bloc.add(new Chunk(org.getNom(), texteGrasFont));
                if (org.getFormeJuridique() != null) {
                    bloc.add(new Chunk(" (" + org.getFormeJuridique() + ")", texteFont));
                }
                bloc.add(Chunk.NEWLINE);
                if (org.getSiegeSocial() != null) {
                    bloc.add(new Chunk("Siège social : " + org.getSiegeSocial(), texteFont));
                    bloc.add(Chunk.NEWLINE);
                }
                List<RepresentantOrganisation> representants = org.getRepresentants();
                if (representants != null && !representants.isEmpty()) {
                    bloc.add(new Chunk("représentée par :", texteFont));
                    bloc.add(Chunk.NEWLINE);
                    for (RepresentantOrganisation r : representants) {
                        Personne p = r.getPersonne();
                        bloc.add(new Chunk("• Monsieur/Madame " + p.getNomComplet()
                                + (r.getFonction() != null ? " - " + r.getFonction() : ""), texteFont));
                        bloc.add(Chunk.NEWLINE);
                        bloc.add(new Chunk("  CIN N° : " + valeurOuVide(p.getCin())
                                + (p.getDateDelivranceCin() != null ? ", délivrée le " + p.getDateDelivranceCin().format(DATE_FR) : "")
                                + (p.getLieuDelivranceCin() != null ? " à " + p.getLieuDelivranceCin() : ""), texteFont));
                        bloc.add(Chunk.NEWLINE);
                        if (p.getAdresse() != null && !p.getAdresse().isBlank()) {
                            bloc.add(new Chunk("  Demeurant au : " + p.getAdresse(), texteFont));
                            bloc.add(Chunk.NEWLINE);
                        }
                    }
                }
                bloc.add(new Chunk(mention, texteFont));
                bloc.setSpacingAfter(14f);
                bloc.setIndentationLeft(20f);
                document.add(bloc);
            }
        } catch (DocumentException e) {
            throw new IllegalStateException(e);
        }
    }

    private void ajouterIntroductionArticles(Document document) throws DocumentException {
        Paragraph intro = new Paragraph("II/ Il a été convenu le présent contrat de bail stipulant :", sousTitreFont);
        intro.setSpacingBefore(10f);
        intro.setSpacingAfter(14f);
        document.add(intro);
    }

    private void ajouterArticles(Document document, Contrat contrat) throws DocumentException {
        int numero = 1;
        for (ContratArticle ca : contrat.getArticles()) {
            Paragraph titreArticle = new Paragraph(
                    "Article " + numero + " : " + ca.getModeleArticle().getTitre(), texteGrasFont);
            titreArticle.setSpacingBefore(10f);
            titreArticle.setSpacingAfter(4f);
            document.add(titreArticle);

            Paragraph contenu = new Paragraph(ca.getContenuFinal(), texteFont);
            contenu.setAlignment(Element.ALIGN_JUSTIFIED);
            contenu.setIndentationLeft(10f);
            contenu.setSpacingAfter(6f);
            document.add(contenu);

            numero++;
        }
    }

    private void ajouterDateEtLieu(Document document) throws DocumentException {
        Paragraph dateLieu = new Paragraph(
                "Antananarivo, le " + LocalDate.now().format(DATE_FR), texteFont);
        dateLieu.setAlignment(Element.ALIGN_CENTER);
        dateLieu.setSpacingBefore(24f);
        dateLieu.setSpacingAfter(20f);
        document.add(dateLieu);
    }

    private void ajouterSignatures(Document document, Contrat contrat,
                                   Function<Personne, Optional<String>> signatureDe) throws DocumentException {
        Optional<PartieContrat> proprietaire = contrat.getParties().stream()
                .filter(p -> p.getRole() == RolePartie.PROPRIETAIRE).findFirst();
        Optional<PartieContrat> locataire = contrat.getParties().stream()
                .filter(p -> p.getRole() == RolePartie.LOCATAIRE).findFirst();

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);

        PdfPCell colProprietaire = new PdfPCell();
        colProprietaire.setBorder(Rectangle.NO_BORDER);
        colProprietaire.addElement(new Paragraph("Le Propriétaire", signatureLabelFont));
        proprietaire.ifPresent(p -> ajouterSignaturesPartie(colProprietaire, p, signatureDe));

        PdfPCell colLocataire = new PdfPCell();
        colLocataire.setBorder(Rectangle.NO_BORDER);
        colLocataire.addElement(new Paragraph("Le Locataire", signatureLabelFont));
        locataire.ifPresent(p -> ajouterSignaturesPartie(colLocataire, p, signatureDe));

        table.addCell(colProprietaire);
        table.addCell(colLocataire);
        document.add(table);
    }

    private void ajouterSignaturesPartie(PdfPCell cellule, PartieContrat partie,
                                         Function<Personne, Optional<String>> signatureDe) {
        try {
            List<Personne> signataires;
            if (partie.getTypePartie() == TypePartie.PERSONNE) {
                signataires = List.of(partie.getPersonne());
            } else {
                signataires = partie.getOrganisation().getRepresentants().stream()
                        .map(RepresentantOrganisation::getPersonne).toList();
                if (!signataires.isEmpty()) {
                    cellule.addElement(new Paragraph("Représentée par", texteFont));
                }
            }

            for (Personne p : signataires) {
                Optional<String> signature = signatureDe.apply(p);

                if (signature.isPresent()) {
                    Image image = decoderSignature(signature.get());
                    if (image != null) {
                        image.scaleToFit(160f, 60f);
                        cellule.addElement(image);
                    }
                } else {
                    Paragraph placeholder = new Paragraph("(non signé)", texteFont);
                    placeholder.setSpacingBefore(20f);
                    cellule.addElement(placeholder);
                }
                cellule.addElement(new Paragraph("M/Mme " + p.getNomComplet(), texteFont));
            }
        } catch (Exception e) {
            cellule.addElement(new Paragraph("(erreur d'affichage de la signature)", texteFont));
        }
    }

    private Image decoderSignature(String dataUrl) {
        try {
            String base64 = dataUrl.contains(",") ? dataUrl.substring(dataUrl.indexOf(',') + 1) : dataUrl;
            byte[] bytes = Base64.getDecoder().decode(base64);
            return Image.getInstance(bytes);
        } catch (Exception e) {
            return null;
        }
    }

    private String valeurOuVide(String valeur) {
        return valeur == null ? "" : valeur;
    }
}
