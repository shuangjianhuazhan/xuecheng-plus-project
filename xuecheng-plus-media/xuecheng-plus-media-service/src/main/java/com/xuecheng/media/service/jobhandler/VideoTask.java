package com.xuecheng.media.service.jobhandler;

import com.xuecheng.base.utils.Mp4VideoUtil;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileProcessService;
import com.xuecheng.media.service.MediaFileService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 处理任务类
 */
@Slf4j
@Component
public class VideoTask {

    @Autowired
    MediaFileService mediaFileService;

    @Autowired
    MediaFileProcessService mediaFileProcessService;

    @Value("${videoprocess.ffmpegpath}")
    private String ffmpegpath;

    /**
     * 分片广播任务
     */
    @XxlJob("videoJobHandler")
    public void shardingJobHandler() throws Exception {

        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();

        // 确定CPU核心数
        int cpuNum = 4;

        // 查询待处理任务
        List<MediaProcess> mediaProcessList = mediaFileProcessService.getMediaProcessList(shardIndex, shardTotal, cpuNum);

        // 任务数量
        int size = mediaProcessList.size();
        if (size == 0) {
            return;
        }

        // 创建一个线程池
        ExecutorService executorService = Executors.newFixedThreadPool(size);
        // 使用计数器
        CountDownLatch countDownLatch = new CountDownLatch(size);

        mediaProcessList.forEach(mediaProcess -> {
            // 将任务加入线程池
            executorService.execute(() -> {
                try {

                    // 任务id
                    Long taskId = mediaProcess.getId();
                    // 文件id
                    String fileId = mediaProcess.getFileId();
                    // 桶
                    String bucket = mediaProcess.getBucket();
                    // objectName
                    String filePath = mediaProcess.getFilePath();

                    // 开启任务
                    boolean b = mediaFileProcessService.startTask(taskId);
                    if (!b) {
                        log.debug("抢占任务失败，任务id为：{}", taskId);
                        return;
                    }
                    // 下载Minio的视频到本地
                    File file = mediaFileService.downloadFileFromMinIO(bucket, filePath);
                    if (file == null) {
                        log.debug("下载文件到本地失败");
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", fileId, null, "下载待处理文件失败");
                        return;
                    }

                    //ffmpeg的路径
                    String ffmpeg_path = ffmpegpath;//ffmpeg的安装位置
                    //源avi视频的路径
                    String video_path = file.getAbsolutePath();

                    File mp4File = null;
                    try {
                        mp4File = File.createTempFile("minio", ".mp4");
                    } catch (IOException e) {
                        log.debug("创建临时文件异常，{}", e.getMessage());
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", fileId, null, "创建临时文件异常");
                        return;
                    }

                    //转换后mp4文件的路径
                    String mp4_path = mp4File.getAbsolutePath();
                    //创建工具类对象
                    Mp4VideoUtil videoUtil = new Mp4VideoUtil(ffmpeg_path, video_path, mp4File.getName(), mp4_path);

                    //开始视频转换，成功将返回success
                    String result = videoUtil.generateMp4();
                    if (!result.equals("success")) {
                        log.debug("视频转码失败，原因：{}", result);
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", fileId, null, "视频转码失败");
                        return;
                    }

                    // 上传到Minio
                    //mp4在minio的存储路径
                    String objectName = getFilePath(fileId);
                    boolean upload = mediaFileService.addMediaFilesToMinIO(mp4File.getAbsolutePath(), "video/mp4", bucket, objectName);
                    if (!upload) {
                        log.debug("上传mp4到Minio失败");
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", fileId, null, "上传mp4到Minio失败");
                        return;
                    }

                    // 保存处理结果
                    String url = "/" + bucket + "/" + objectName;
                    mediaFileProcessService.saveProcessFinishStatus(taskId, "2", fileId, url, null);
                } finally {

                    // 计数器-1
                    countDownLatch.countDown();
                }
            });
        });

        // 阻塞，指定最大限度的等待时间
        countDownLatch.await(30, TimeUnit.MINUTES);
    }

    private String getFilePath(String fileMd5) {
        return fileMd5.charAt(0) + "/" + fileMd5.charAt(1) + "/" + fileMd5 + "/" + fileMd5 + ".mp4";
    }
}
