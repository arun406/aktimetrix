package com.aktimetrix.aktimetrix.reference.data.service;

import com.aktimetrix.aktimetrix.reference.data.exception.S3StorageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;

@Service
public class S3StorageService {

    @Autowired
    private S3Client s3Client;

    /**
     * Upload stream to a s3 bucket
     *
     * @param inputStream
     * @param bucketName
     * @param key
     * @throws S3StorageException
     */
    public void upload(InputStream inputStream, String bucketName, String key) throws S3StorageException {

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        try {
            PutObjectResponse putObjectResponse = this.s3Client.putObject(objectRequest, RequestBody.fromInputStream(inputStream, inputStream.available()));
        } catch (IOException e) {
            throw new S3StorageException(e);
        }
    }

    /**
     * @param bucketName
     * @param key
     * @return
     */
    public Resource download(String bucketName, String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        try {
            ResponseInputStream<GetObjectResponse> inputStream = this.s3Client.getObject(getObjectRequest);
            return new InputStreamResource(inputStream);
        } catch (NoSuchKeyException e) {
            throw new S3StorageException(e);
        }
    }
}
