package com.xuecheng.media;

import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import io.minio.*;
import io.minio.errors.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 测试Minio
 */
public class MinioTest {

    static MinioClient minioClient = MinioClient.builder().endpoint("http://192.168.101.65:9000").credentials("minioadmin", "minioadmin").build();

    @Test
    public void upload() {
        //根据扩展名取出mimeType
        ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(".jpg");
        String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;//通用mimeType，字节流
        if (extensionMatch != null) {
            mimeType = extensionMatch.getMimeType();
        }
        try {
            UploadObjectArgs testbucket = UploadObjectArgs.builder().bucket("testbucket")
//                    .object("test001.mp4")
                    .object("1.jpg")//添加子目录
                    .filename("E:\\Java\\images\\1.jpg").contentType(mimeType)//默认根据扩展名确定文件内容类型，也可以指定
                    .build();
            minioClient.uploadObject(testbucket);
            System.out.println("上传成功");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("上传失败");
        }

    }

    @Test
    public void delete() {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket("testbucket").object("1.jpg").build());
            System.out.println("删除成功");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("删除失败");
        }
    }

    //查询文件
    @Test
    public void getFile() throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        GetObjectArgs getObjectArgs = GetObjectArgs.builder().bucket("testbucket").object("1.jpg").build();
        FilterInputStream inputStream = minioClient.getObject(getObjectArgs);
        FileOutputStream outputStream = new FileOutputStream(new File("E:\\Java\\images\\2.jpg"));
        String source_md5 = DigestUtils.md5Hex(inputStream);
        IOUtils.copy(inputStream, outputStream);
        //校验文件的完整性对文件的内容进行md5
        FileInputStream fileInputStream = new FileInputStream(new File("E:\\Java\\images\\2.jpg"));
        String local_md5 = DigestUtils.md5Hex(fileInputStream);
        if (source_md5.equals(local_md5)) {
            System.out.println("下载成功");
        }
    }

    // 将分块文件上传到Minio
    @Test
    public void uploadChunk() throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        for (int i = 0; i < 5; i++) {
            UploadObjectArgs testbucket = UploadObjectArgs.builder()
                    .bucket("testbucket")
                    .object("chunk/" + i)//添加子目录
                    .filename("E:\\Java\\videos\\chunk\\" + i)
                    .build();
            minioClient.uploadObject(testbucket);
            System.out.println("上传分块" + i + "成功");
        }
    }

    // 调用Minio接口合并分块
    @Test
    public void mergeChunk() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {

        // 存储源文件的信息
        List<ComposeSource> sources = Stream.iterate(0, i -> ++i).limit(5).map(i ->
                ComposeSource.builder()
                        .bucket("testbucket")
                        .object("chunk/".concat(Integer.toString(i)))
                        .build()
        ).collect(Collectors.toList());

        // 指定合并后的objectName等信息
        ComposeObjectArgs composeObjectArgs = ComposeObjectArgs.builder()
                .bucket("testbucket")
                .object("merge.mp4")
                .sources(sources) // 指定源文件
                .build();

        // 合并文件
        minioClient.composeObject(composeObjectArgs);
    }

    // 批量清理分块文件

}