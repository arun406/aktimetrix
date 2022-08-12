package com.aktimetrix.aktimetrix.reference.data.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "aws")
public class AWSAccountProperties {
    private String accessKeyId;
    private String secretAccessKey;
    private String region;
}
