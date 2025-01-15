package com.xuecheng.content.service;

import com.xuecheng.content.model.po.CourseTeacher;

import java.util.List;

/**
 * 课程教师接口
 */
public interface CourseTeacherService {
    /**
     * 获取课程教师信息列表
     *
     * @param courseId
     * @return
     */
    List<CourseTeacher> getCourseTeacherList(Long courseId);

    /**
     * 保存教师信息
     *
     * @param courseTeacher
     * @return
     */
    CourseTeacher saveCourseTeacher(Long companyId, CourseTeacher courseTeacher);

    /**
     * 删除教师信息
     *
     * @param courseId
     * @param teacherId
     */
    void deleteCourseTeacher(Long companyId, Long courseId, Long teacherId);
}
