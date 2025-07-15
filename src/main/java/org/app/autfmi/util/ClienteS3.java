package org.app.autfmi.util;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

public class ClienteS3 {

    private static final S3Client s3Client;

    static {
        String regionName = System.getenv("AWS_REGION");

        s3Client = S3Client.builder()
                .region(Region.of(regionName))
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build();
    }

    private ClienteS3() {

    }

    public static S3Client getInstance() {
        return s3Client;
    }
}
