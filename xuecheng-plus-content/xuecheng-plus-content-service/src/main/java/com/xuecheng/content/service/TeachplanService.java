package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.BindTeachplanMediaDto;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;

import java.util.List;

public interface TeachplanService {
    /**
     * 查询课程计划树形结构
     *
     * @param courseId
     * @return
     */
    public List<TeachplanDto> findTeachplanTree(long courseId);

    /**
     * 保存课程计划
     *
     * @param teachplanDto
     */
    public void saveTeachplan(SaveTeachplanDto teachplanDto);

    /**
     * 删除课程信息
     *
     * @param teachplanId
     */
    void deleteTeachplan(Long teachplanId);

    /**
     * 对课程计划进行排序
     *
     * @param moveType
     * @param teachplanId
     */
    void orderByTeachplan(String moveType, Long teachplanId);

    /**
     * 绑定课程信息和媒资
     * @param bindTeachplanMediaDto
     */
    public void associationMedia(BindTeachplanMediaDto bindTeachplanMediaDto);

    /**
     * 解除绑定的媒资
     * @param teachPlanId
     * @param mediaId
     */
    public void deleteMedia(Long teachPlanId, String mediaId);
}
