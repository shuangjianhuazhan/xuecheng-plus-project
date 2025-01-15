package com.xuecheng.content.model.dto;

import lombok.Data;
import lombok.ToString;

import java.util.List;

/**
 * @description 课程预览数据模型
 **/

@Data
@ToString
public class CoursePreviewDto {

    //课程基本信息,课程营销信息
    private CourseBaseInfoDto courseBase;


    //课程计划信息
    private List<TeachplanDto> teachplans;

    //师资信息暂时不加...
}
