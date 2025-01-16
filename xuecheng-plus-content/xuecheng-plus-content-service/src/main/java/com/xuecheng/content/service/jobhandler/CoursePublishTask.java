package com.xuecheng.content.service.jobhandler;

import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.feignclient.SearchServiceClient;
import com.xuecheng.content.mapper.CoursePublishMapper;
import com.xuecheng.content.model.dto.CourseIndex;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MessageProcessAbstract;
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * 课程发布任务类
 */
@Slf4j
@Component
public class CoursePublishTask extends MessageProcessAbstract {

    @Autowired
    private CoursePublishService coursePublishService;

    @Autowired
    private SearchServiceClient searchServiceClient;

    @Autowired
    private CoursePublishMapper coursePublishMapper;

    @XxlJob("CoursePublishJobHandler")
    public void coursePublishJobHandler() throws Exception {

        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();

        // 调用抽象类执行任务
        // 参数:分片序号、分片总数、消息类型、一次最多取到的任务数量、一次任务调度执行的超时时间
        process(shardIndex, shardTotal, "course_publish", 30, 60);
    }

    // 执行课程发布任务的逻辑
    @Override
    public boolean execute(MqMessage mqMessage) {
        // 从mqMessage拿到课程id
        Long courseId = Long.parseLong(mqMessage.getBusinessKey1());

        // 课程静态化上传到Minio
        generateCourseHtml(mqMessage, courseId);

        // 向elasticsearch写索引数据
        saveCourseIndex(mqMessage, courseId);

        // 向redis写缓存
        saveCourseCache(mqMessage, courseId);

        // 返回true表示完成
        return true;
    }

    private void generateCourseHtml(MqMessage mqMessage, Long courseId) {

        // 任务id
        Long taskId = mqMessage.getId();

        // 任务幂等性处理
        // 取出该阶段的执行状态
        MqMessageService mqMessageService = this.getMqMessageService();
        int state_1 = mqMessageService.getStageOne(taskId);
        if (state_1 > 0) {
            log.debug("课程静态化任务已完成，已完成进度：1/3");
            return;
        }

        // 开始进行课程静态化 生成html界面
        File file = coursePublishService.generateCourseHtml(courseId);
        if (file == null) {
            XueChengPlusException.cast("生成的静态页面为空");
        }

        // 将html上传到Minio
        coursePublishService.uploadCourseHtml(courseId, file);

        // 第一阶段任务处理完成
        mqMessageService.completedStageOne(taskId);
    }

    //保存课程索引信息
    private void saveCourseIndex(MqMessage mqMessage, Long courseId) {

        // 任务id
        Long taskId = mqMessage.getId();

        // 任务幂等性处理
        // 取出该阶段的执行状态
        MqMessageService mqMessageService = this.getMqMessageService();
        int state_2 = mqMessageService.getStageTwo(taskId);
        if (state_2 > 0) {
            log.debug("课程索引任务已完成，已完成进度：2/3");
            return;
        }

        //取出课程发布信息
        CoursePublish coursePublish = coursePublishMapper.selectById(courseId);

        // 远程调用
        CourseIndex courseIndex = new CourseIndex();
        BeanUtils.copyProperties(coursePublish, courseIndex);
        Boolean add = searchServiceClient.add(courseIndex);
        if (!add) {
            XueChengPlusException.cast("添加课程索引失败");
        }

        // 第二阶段任务处理完成
        mqMessageService.completedStageTwo(taskId);
    }

    // 向redis写入信息
    private void saveCourseCache(MqMessage mqMessage, long courseId) {

        // 任务id
        Long taskId = mqMessage.getId();

        // 任务幂等性处理
        // 取出该阶段的执行状态
        MqMessageService mqMessageService = this.getMqMessageService();
        int state_3 = mqMessageService.getStageThree(taskId);
        if (state_3 > 0) {
            log.debug("内存写入已完成，已完成进度：3/3");
            return;
        }
        // 写入redis
        // TODO

        // 第二阶段任务处理完成
        mqMessageService.completedStageThree(taskId);
    }
}
