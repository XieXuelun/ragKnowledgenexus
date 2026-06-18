package com.xxr.utils;

import com.xxr.properties.MinioProperties;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MinioUtil {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    /**
     * 上传头像 → minio.bucket.avatar
     */
    public String uploadAvatar(MultipartFile file) throws Exception {
        String originalName = file.getOriginalFilename();
        String suffix = originalName.substring(originalName.lastIndexOf("."));
        String fileName = "avatar/" + UUID.randomUUID() + suffix;

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(minioProperties.getBucket().getAvatar()) // 嵌套获取头像桶名
                        .object(fileName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );
        return minioProperties.getEndpoint() + "/" + minioProperties.getBucket().getAvatar() + "/" + fileName;
    }

    /**
     * 上传文档 → minio.bucket.document
     */
    public String uploadDocument(MultipartFile file) throws Exception {
        String originalName = file.getOriginalFilename();
        String suffix = originalName.substring(originalName.lastIndexOf("."));
        String fileName = "document/" + UUID.randomUUID() + suffix;

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(minioProperties.getBucket().getDocument()) // 嵌套获取文档桶名
                        .object(fileName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );
        return minioProperties.getEndpoint() + "/" + minioProperties.getBucket().getDocument() + "/" + fileName ;// 文档只返回对象路径，不返回完整URL
    }
    /**
     * 获取文档预签名URL
     * @param objectName 文档在MinIO中的路径
     * @param expireSeconds URL有效期（秒）
     * @return 预签名URL
     */
    public String getPresignedUrl(String objectName, Integer expireSeconds) throws Exception {
        String bucket = minioProperties.getBucket().getDocument();
        String endpoint = minioProperties.getEndpoint();
        String bucketPath = endpoint + "/" + bucket + "/";
        String objName = objectName.startsWith(bucketPath) ? objectName.substring(bucketPath.length()) : objectName;

        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucket)
                        .object(objName)
                        .expiry(expireSeconds)
                        .build()
        );
    }

    /**
     * 获取头像预签名URL
     */
    public String getPresignedAvatarUrl(String objectName, Integer expireSeconds) throws Exception {
        String bucket = minioProperties.getBucket().getAvatar();
        String endpoint = minioProperties.getEndpoint();
        String bucketPath = endpoint + "/" + bucket + "/";
        String objName = objectName.startsWith(bucketPath) ? objectName.substring(bucketPath.length()) : objectName;

        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucket)
                        .object(objName)
                        .expiry(expireSeconds)
                        .build()
        );
    }

    /**
     * 获取文档输入流
     * @param objectName 文档在MinIO中的路径
     * @return 输入流
     */
    public InputStream getObject(String objectName) throws Exception {
        String bucket = minioProperties.getBucket().getDocument();
        String endpoint = minioProperties.getEndpoint();
        String bucketPath = endpoint + "/" + bucket + "/";
        String objName = objectName.startsWith(bucketPath) ? objectName.substring(bucketPath.length()) : objectName;
        
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(objName)
                        .build()
        );
    }

}