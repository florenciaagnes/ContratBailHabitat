package com.madagascar.contratsbail.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "app.documents")
@Data
public class StorageProperties {
    /** Repertoire racine ou sont stockes les PDF generes. */
    private String storagePath = "./data/contrats";
}
