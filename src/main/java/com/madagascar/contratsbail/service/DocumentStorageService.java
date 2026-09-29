package com.madagascar.contratsbail.service;

import com.madagascar.contratsbail.config.StorageProperties;
import com.madagascar.contratsbail.entity.Contrat;
import com.madagascar.contratsbail.entity.DocumentContrat;
import com.madagascar.contratsbail.repository.DocumentContratRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

/**
 * Gere la sauvegarde, le nommage, le hash et la recuperation des PDF generes.
 */
@Service
@RequiredArgsConstructor
public class DocumentStorageService {

    private final StorageProperties storageProperties;
    private final DocumentContratRepository documentContratRepository;

    /** PDF du contrat : BAIL-2026-001.pdf */
    @Transactional
    public DocumentContrat enregistrer(Contrat contrat, byte[] contenuPdf, boolean archiver) {
        return enregistrer(contrat, contrat.getNumero() + ".pdf", "PDF_CONTRAT", contenuPdf, archiver);
    }

    /** PDF quelconque rattache a un contrat (ex. avenant : BAIL-2026-001-AV1.pdf). */
    @Transactional
    public DocumentContrat enregistrer(Contrat contrat, String nomFichier, String typeDocument,
                                       byte[] contenuPdf, boolean archiver) {
        try {
            Path repertoire = Path.of(storageProperties.getStoragePath());
            Files.createDirectories(repertoire);

            Path chemin = repertoire.resolve(nomFichier);
            Files.write(chemin, contenuPdf);

            DocumentContrat document = DocumentContrat.builder()
                    .contrat(contrat)
                    .nomFichier(nomFichier)
                    .cheminFichier(chemin.toAbsolutePath().toString())
                    .typeDocument(typeDocument)
                    .dateGeneration(LocalDateTime.now())
                    .dateArchivage(archiver ? LocalDateTime.now() : null)
                    .hashDocument(calculerHash(contenuPdf))
                    .build();

            return documentContratRepository.save(document);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible d'enregistrer le PDF " + nomFichier, e);
        }
    }

    public byte[] lire(DocumentContrat document) {
        try {
            return Files.readAllBytes(Path.of(document.getCheminFichier()));
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de lire le fichier : " + document.getCheminFichier(), e);
        }
    }

    private String calculerHash(byte[] contenu) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(contenu));
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
