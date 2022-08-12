package com.aktimetrix.aktimetrix.reference.data.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AmazonConfig {

    @Bean
    public S3Client s3(AWSAccountProperties accountProperties) {
        AwsBasicCredentials basicCredentials = AwsBasicCredentials
                .create(accountProperties.getAccessKeyId(), accountProperties.getSecretAccessKey());

        return S3Client.builder().credentialsProvider(StaticCredentialsProvider.create(basicCredentials))
                .region(Region.of(accountProperties.getRegion()))
                .build();

    }
}
