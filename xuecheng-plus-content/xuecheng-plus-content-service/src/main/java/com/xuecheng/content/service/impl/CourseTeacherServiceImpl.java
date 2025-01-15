package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseTeacherMapper;
import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CourseTeacherService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CourseTeacherServiceImpl implements CourseTeacherService {

    @Autowired
    private CourseTeacherMapper courseTeacherMapper;

    @Autowired
    private CourseBaseMapper courseBaseMapper;

    @Override
    public List<CourseTeacher> getCourseTeacherList(Long courseId) {
        LambdaQueryWrapper<CourseTeacher> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CourseTeacher::getCourseId, courseId);
        return courseTeacherMapper.selectList(queryWrapper);
    }

    @Override
    @Transactional
    public CourseTeacher saveCourseTeacher(Long companyId, CourseTeacher courseTeacher) {
        Long courseId = courseTeacher.getCourseId();
        if (!courseBaseMapper.selectById(courseId).getCompanyId().equals(companyId)) {
            XueChengPlusException.cast("只能对本机构的教师进行操作哦！");
        }
        Long id = courseTeacher.getId();
        CourseTeacher teacher;
        if (id == null) {
            // 新增
            teacher = new CourseTeacher();
            BeanUtils.copyProperties(courseTeacher, teacher);
            teacher.setCreateDate(LocalDateTime.now());
            int i =  courseTeacherMapper.insert(teacher);
            if (i <= 0) {
                XueChengPlusException.cast("添加教师信息失败！");
            }
        } else {
            // 修改
            teacher = courseTeacherMapper.selectById(id);
            BeanUtils.copyProperties(courseTeacher, teacher);
            int i = courseTeacherMapper.updateById(teacher);
            if (i <= 0) {
                XueChengPlusException.cast("修改教师信息失败！");
            }
        }
        return courseTeacherMapper.selectById(teacher.getId());
    }

    @Override
    @Transactional
    public void deleteCourseTeacher(Long companyId, Long courseId, Long teacherId) {
        if (!courseBaseMapper.selectById(courseId).getCompanyId().equals(companyId)) {
            XueChengPlusException.cast("只能对本机构的教师进行操作哦！");
        }
        LambdaQueryWrapper<CourseTeacher> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CourseTeacher::getId, teacherId)
                .eq(CourseTeacher::getCourseId, courseId);
        int i = courseTeacherMapper.delete(queryWrapper);
        if (i <= 0) {
            XueChengPlusException.cast("删除教师信息学失败！");
        }
    }
}
