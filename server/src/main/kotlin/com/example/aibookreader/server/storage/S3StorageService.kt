package com.example.aibookreader.server.storage

import com.example.aibookreader.server.AppConfig
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.HeadBucketRequest
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchBucketException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.net.URI
import java.time.Duration

class S3StorageService(private val config: AppConfig) {

    private val credentials = StaticCredentialsProvider.create(
        AwsBasicCredentials.create(config.s3AccessKey, config.s3SecretKey)
    )

    private val s3Internal: S3Client = S3Client.builder()
        .endpointOverride(URI.create(config.s3Endpoint))
        .region(Region.of(config.s3Region))
        .credentialsProvider(credentials)
        .serviceConfiguration(
            S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build()
        )
        .build()

    private val presigner: S3Presigner = S3Presigner.builder()
        .endpointOverride(URI.create(config.s3PublicEndpoint))
        .region(Region.of(config.s3Region))
        .credentialsProvider(credentials)
        .serviceConfiguration(
            S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build()
        )
        .build()

    fun ensureBucketExists() {
        try {
            s3Internal.headBucket(HeadBucketRequest.builder().bucket(config.s3Bucket).build())
        } catch (_: NoSuchBucketException) {
            s3Internal.createBucket(CreateBucketRequest.builder().bucket(config.s3Bucket).build())
        }
    }

    fun headObject(key: String): Long? = try {
        val r = s3Internal.headObject(
            HeadObjectRequest.builder()
                .bucket(config.s3Bucket)
                .key(key)
                .build()
        )
        r.contentLength()
    } catch (_: Exception) {
        null
    }

    fun presignPut(key: String, contentType: String, contentLength: Long): String {
        val put = PutObjectRequest.builder()
            .bucket(config.s3Bucket)
            .key(key)
            .contentType(contentType)
            .contentLength(contentLength)
            .build()
        val presign = PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofSeconds(config.presignTtlSeconds))
            .putObjectRequest(put)
            .build()
        return presigner.presignPutObject(presign).url().toString()
    }

    fun presignGet(key: String): String {
        val get = GetObjectRequest.builder()
            .bucket(config.s3Bucket)
            .key(key)
            .build()
        val presign = GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofSeconds(config.presignTtlSeconds))
            .getObjectRequest(get)
            .build()
        return presigner.presignGetObject(presign).url().toString()
    }

    /**
     * Тестовая загрузка не используется в проде; presigned PUT делает клиент.
     * Оставлено на случай отладки.
     */
    fun putTestObject(key: String, bytes: ByteArray, contentType: String) {
        s3Internal.putObject(
            PutObjectRequest.builder()
                .bucket(config.s3Bucket)
                .key(key)
                .contentType(contentType)
                .contentLength(bytes.size.toLong())
                .build(),
            RequestBody.fromBytes(bytes)
        )
    }
}
