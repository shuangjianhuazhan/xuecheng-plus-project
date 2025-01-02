package com.xuecheng.content.service.impl;

import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.model.po.CourseCategory;
import com.xuecheng.content.service.CourseCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CourseCategoryServiceImpl implements CourseCategoryService {

    @Autowired
    CourseCategoryMapper courseCategoryMapper;

    @Override
    public List<CourseCategoryTreeDto> queryTreeNodes(String id) {

        // 调用Mapper递归查询出分类信息
        List<CourseCategoryTreeDto> courseCategoryTreeDtos = courseCategoryMapper.selectTreeNodes(id);

        // 封装List<CourseCategoryTreeDto>
        // 将list转成map
        Map<String, CourseCategoryTreeDto> mapTemp = courseCategoryTreeDtos.stream()
                .filter(item -> !id.equals(item.getId()))
                .collect(Collectors.toMap(CourseCategory::getId, value -> value, (key1, key2) -> key2));

        // 定义一个List作为最终返回的List
        List<CourseCategoryTreeDto> courseCategoryTreeList = new ArrayList<>();

        // 从头遍历courseCategoryTreeDtos，一边遍历一边找子节点放入父节点的childrenTreeNodes
        courseCategoryTreeDtos.stream().filter(item -> !id.equals(item.getId())).forEach(item -> {
            // 向Lsit写入元素,写入的时候去除根节点
            if (item.getParentid().equals(id)) {
                courseCategoryTreeList.add(item);
            }
            // 找到节点的父节点
            CourseCategoryTreeDto courseCategoryTreeDto = mapTemp.get(item.getParentid());
            if (courseCategoryTreeDto != null) {
                if (courseCategoryTreeDto.getChildrenTreeNodes() == null) {
                    // 如果父节点的ChildrenTreeNodes属性为空要new一个集合，因为需要存放它的子节点
                    courseCategoryTreeDto.setChildrenTreeNodes(new ArrayList<CourseCategoryTreeDto>());
                }
                courseCategoryTreeDto.getChildrenTreeNodes().add(item);
            }
        });
        return courseCategoryTreeList;
    }
}
