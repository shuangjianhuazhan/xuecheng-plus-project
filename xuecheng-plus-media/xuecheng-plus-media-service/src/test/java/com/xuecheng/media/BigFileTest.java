package com.xuecheng.media;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

/**
 * 测试大文件上传方法
 */
public class BigFileTest {

    // 分块测试
    @Test
    public void testChunk() throws IOException {
        // 源文件
        File sourceFile = new File("E:\\Java\\videos\\oceans.mp4");
        // 分块文件存储路径
        String chunkFilePath = "E:\\Java\\videos\\chunk\\";
        // 分块文件大小(1MB)
        int chunkSize = 1024 * 1024 * 5;
        // 分块文件的个数
        int chunkNum = (int) Math.ceil(sourceFile.length() * 1.0 / chunkSize);
        // 使用流从源文件读数据，向分块文件中写数据
        RandomAccessFile raf_r = new RandomAccessFile(sourceFile, "r");
        // 缓冲区
        byte[] bytes = new byte[1024];
        for (int i = 0; i < chunkNum; i++) {
            File chunkFile = new File(chunkFilePath + i);
            // 分块文件写入流
            RandomAccessFile raf_rw = new RandomAccessFile(chunkFile, "rw");
            int len = -1;
            while ((len = raf_r.read(bytes)) != -1) {
                raf_rw.write(bytes, 0, len);
                if (chunkFile.length() >= chunkSize) {
                    break;
                }
            }
            raf_rw.close();
        }
        raf_r.close();
    }

    // 将分块进行合并
    @Test
    public void testMerge() throws IOException {
        // 源文件
        File sourceFile = new File("E:\\Java\\videos\\oceans.mp4");
        // 分块文件存储路径
        File chunkFolder = new File("E:\\Java\\videos\\chunk\\");
        // 合并后的文件
        File mergeFile = new File("E:\\Java\\videos\\oceans_merger.mp4");
        // 取出分块文件
        File[] files = chunkFolder.listFiles();
        // 文件排序
        Arrays.sort(files, 0, files.length, (a, b) -> {
            return Integer.parseInt(a.getName()) - Integer.parseInt(b.getName());
        });
        // 向合并文件写入的流
        RandomAccessFile raf_rw = new RandomAccessFile(mergeFile, "rw");
        // 缓存区
        byte[] bytes = new byte[1024];

        // 遍历分块文件，合并
        for (File file : files) {
            // 读分块的流
            RandomAccessFile raf_r = new RandomAccessFile(file, "r");
            int len = -1;
            while ((len = raf_r.read(bytes)) != -1) {
                raf_rw.write(bytes, 0, len);
            }
            raf_r.close();
        }
        raf_rw.close();
        // 校验
        //校验文件
        try (
                FileInputStream fileInputStream = new FileInputStream(sourceFile);
                FileInputStream mergeFileStream = new FileInputStream(mergeFile);
        ) {
            //取出原始文件的md5
            String originalMd5 = DigestUtils.md5Hex(fileInputStream);
            //取出合并文件的md5进行比较
            String mergeFileMd5 = DigestUtils.md5Hex(mergeFileStream);
            if (originalMd5.equals(mergeFileMd5)) {
                System.out.println("合并文件成功");
            } else {
                System.out.println("合并文件失败");
            }

        }
    }
}
