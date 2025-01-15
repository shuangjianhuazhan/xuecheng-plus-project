package com.xuecheng.content;

import com.xuecheng.content.config.MultipartSupportConfig;
import com.xuecheng.content.feignclient.MediaServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

/**
 * 测试使用feign远程上传文件
 */
@SpringBootTest
public class FeignUploadTest {

    @Autowired
    MediaServiceClient mediaServiceClient;

    //远程调用，上传文件
    @Test
    public void test() {

        // 将File转成MultipartFile
        MultipartFile multipartFile = MultipartSupportConfig.getMultipartFile(new File("E:\\Java\\test\\test.html"));
        // 上传到Minio
        mediaServiceClient.uploadFile(multipartFile, "course/test20250115.html");
    }
}
