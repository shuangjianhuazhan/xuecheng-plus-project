package com.xuecheng.content.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.CommonError;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.*;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.*;
import com.xuecheng.content.service.CourseBaseInfoService;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.content.service.TeachplanService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 课程发布相关接口实现
 */

@Service
public class CoursePublishServiceImpl implements CoursePublishService {

    @Autowired
    private CourseBaseInfoService courseBaseInfoService;

    @Autowired
    private TeachplanService teachplanService;

    @Autowired
    private CourseMarketMapper courseMarketMapper;

    @Autowired
    private CourseTeacherMapper courseTeacherMapper;

    @Autowired
    private CoursePublishPreMapper coursePublishPreMapper;

    @Autowired
    private CourseBaseMapper courseBaseMapper;

    @Autowired
    private CoursePublishMapper coursePublishMapper;

    @Autowired
    private MqMessageService mqMessageService;

    @Override
    public CoursePreviewDto getCoursePreviewInfo(Long courseId) {
        CoursePreviewDto coursePreviewDto = new CoursePreviewDto();

        // 课程基本信息
        CourseBaseInfoDto courseBaseInfoDto = courseBaseInfoService.getCourseBaseInfo(courseId);

        // 课程计划信息
        List<TeachplanDto> teachplanList = teachplanService.findTeachplanTree(courseId);

        // 封装数据
        coursePreviewDto.setCourseBase(courseBaseInfoDto);
        coursePreviewDto.setTeachplans(teachplanList);
        return coursePreviewDto;
    }

    @Override
    @Transactional
    public void commitAudit(Long companyId, Long courseId) {

        // 获取课程基本信息
        CourseBaseInfoDto courseBaseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);
        if (courseBaseInfo == null) {
            XueChengPlusException.cast("课程不存在哦");
        }

        if (!courseBaseInfo.getCompanyId().equals(companyId)) {
            XueChengPlusException.cast("不允许提交其它机构的课程哦");
        }

        // 审核状态
        String status = courseBaseInfo.getAuditStatus();

        // 已提交的课程不允许再次提交
        if (status.equals("202003")) {
            XueChengPlusException.cast("已提交审核，请您耐心等待！");
        }

        // 图片和课程计划不存在不允许提交
        String pic = courseBaseInfo.getPic();
        if (StringUtils.isEmpty(pic)) {
            XueChengPlusException.cast("课程图片为空，请上传课程图片");
        }
        List<TeachplanDto> teachplanList = teachplanService.findTeachplanTree(courseId);
        if (teachplanList == null || teachplanList.isEmpty()) {
            XueChengPlusException.cast("课程计划不能为空，请上传计划");
        }

        // 查询到课程基本信息，营销信息，课程计划等信息插入到课程预发布表
        CoursePublishPre coursePublishPre = new CoursePublishPre();
        BeanUtils.copyProperties(courseBaseInfo, coursePublishPre);

        // 营销信息
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        String courseMarketStr = JSON.toJSONString(courseMarket);
        coursePublishPre.setMarket(courseMarketStr);

        // 计划信息
        String teachPlanStr = JSON.toJSONString(teachplanList);
        coursePublishPre.setTeachplan(teachPlanStr);

        // 教师信息
        LambdaQueryWrapper<CourseTeacher> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CourseTeacher::getCourseId, courseId);
        List<CourseTeacher> teacherList = courseTeacherMapper.selectList(queryWrapper);
        String teacherStr = JSON.toJSONString(teacherList);
        coursePublishPre.setTeachers(teacherStr);

        // 状态
        coursePublishPre.setStatus("202003");

        //教学机构id
        coursePublishPre.setCompanyId(companyId);

        // 时间
        coursePublishPre.setCreateDate(LocalDateTime.now());

        // 查询预发布表，有记录则更新，没有则插入
        CoursePublishPre coursePublishPreRe = coursePublishPreMapper.selectById(courseId);
        if (coursePublishPreRe == null) {
            // 插入
            coursePublishPreMapper.insert(coursePublishPre);
        } else {
            coursePublishPreMapper.updateById(coursePublishPre);
        }

        // 更新课程基本信息表的审核状态
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        courseBase.setAuditStatus("202003");
        courseBaseMapper.updateById(courseBase);
    }

    @Override
    @Transactional
    public void publish(Long companyId, Long courseId) {

        // 查询预发布表
        CoursePublishPre coursePublishPre = coursePublishPreMapper.selectById(courseId);
        if (coursePublishPre == null) {
            XueChengPlusException.cast("课程没有审核记录，无法发布");
        }

        //本机构只允许提交本机构的课程
        if (!coursePublishPre.getCompanyId().equals(companyId)) {
            XueChengPlusException.cast("不允许提交其它机构的课程。");
        }

        if (!coursePublishPre.getStatus().equals("202004")) {
            XueChengPlusException.cast("课程没有审核通过，暂时不能发布");
        }

        // 向课程发布表写入数据
        CoursePublish coursePublish = new CoursePublish();
        BeanUtils.copyProperties(coursePublishPre, coursePublish);
        CoursePublish coursePublish1 = coursePublishMapper.selectById(courseId);
        if (coursePublish1 == null) {
            coursePublishMapper.insert(coursePublish);
        } else {
            coursePublishMapper.updateById(coursePublish);
        }

        // 向消息表写数据
        saveCoursePublishMessage(courseId);

        // 将预发布表数据删除
        coursePublishPreMapper.deleteById(courseId);
    }

    /**
     * @param courseId 课程id
     * @description 保存消息表记录
     */
    private void saveCoursePublishMessage(Long courseId) {
        MqMessage mqMessage = mqMessageService.addMessage("course_publish", String.valueOf(courseId), null, null);
        if (mqMessage == null) {
            XueChengPlusException.cast(CommonError.UNKOWN_ERROR);
        }
    }
}
