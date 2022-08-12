package com.aktimetrix.aktimetrix.reference.data.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "aws.s3")
public class S3StorageProperties {
    private String bucketName;
}