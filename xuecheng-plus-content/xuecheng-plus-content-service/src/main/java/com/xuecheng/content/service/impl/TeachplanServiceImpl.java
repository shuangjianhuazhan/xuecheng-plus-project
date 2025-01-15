package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.mapper.TeachplanMediaMapper;
import com.xuecheng.content.model.dto.BindTeachplanMediaDto;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import com.xuecheng.content.service.TeachplanService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 课程计划Service接口实现类
 */
@Service
public class TeachplanServiceImpl implements TeachplanService {

    @Autowired
    TeachplanMapper teachplanMapper;

    @Autowired
    TeachplanMediaMapper teachplanMediaMapper;

    @Override
    public List<TeachplanDto> findTeachplanTree(long courseId) {
        return teachplanMapper.selectTreeNodes(courseId);
    }

    @Override
    @Transactional
    public void saveTeachplan(SaveTeachplanDto saveTeachplanDto) {

        // 通过课程计划id判断是新增还是修改
        Long teachplanId = saveTeachplanDto.getId();
        if (teachplanId == null) {
            // 新增
            // 确定排序字段，找到同级节点的个数，排序字段就是个数 + 1
            LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Teachplan::getCourseId, saveTeachplanDto.getCourseId())
                    .eq(Teachplan::getParentid, saveTeachplanDto.getParentid())
                    .orderByDesc(Teachplan::getOrderby)
                    .last("LIMIT 1");
            Teachplan lastTeachplan = teachplanMapper.selectOne(queryWrapper);
            int count = (lastTeachplan == null) ? 0 : lastTeachplan.getOrderby();
            Teachplan teachplan = new Teachplan();
            // 设置排序号
            teachplan.setOrderby(count + 1);
            // 设置新增时间
            teachplan.setCreateDate(LocalDateTime.now());
            // 属性拷贝
            BeanUtils.copyProperties(saveTeachplanDto, teachplan);
            // 插入数据库
            teachplanMapper.insert(teachplan);
        } else {
            // 修改
            Teachplan teachplan = teachplanMapper.selectById(teachplanId);
            BeanUtils.copyProperties(saveTeachplanDto, teachplan);
            teachplanMapper.updateById(teachplan);
        }
    }

    @Override
    @Transactional
    public void deleteTeachplan(Long teachplanId) {
        if (teachplanId == null) {
            XueChengPlusException.cast("课程计划id为空");
        }
        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Teachplan::getParentid, teachplanId);
        // 获取一下查询的条目数
        Integer count = teachplanMapper.selectCount(queryWrapper);
        // 如果当前课程计划下有小节，则抛异常
        if (count > 0)
            XueChengPlusException.cast("课程计划信息还有子级信息，无法操作");
        else {
            // 课程计划下无小节，直接删除该课程计划和对应的媒资信息
            teachplanMapper.deleteById(teachplanId);
            LambdaQueryWrapper<TeachplanMedia> mediaLambdaQueryWrapper = new LambdaQueryWrapper<>();
            mediaLambdaQueryWrapper.eq(TeachplanMedia::getTeachplanId, teachplanId);
            teachplanMediaMapper.delete(mediaLambdaQueryWrapper);
        }
    }

    @Override
    @Transactional
    public void orderByTeachplan(String moveType, Long teachplanId) {
        Teachplan teachplan = teachplanMapper.selectById(teachplanId);
        Integer grade = teachplan.getGrade();
        Integer orderby = teachplan.getOrderby();
        Long courseId = teachplan.getCourseId();
        Long parentId = teachplan.getParentid();
        if (moveType.equals("moveup")) {
            if (grade == 1) {
                // 章节上移
                LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(Teachplan::getGrade, 1)
                        .eq(Teachplan::getCourseId, courseId)
                        .lt(Teachplan::getOrderby, orderby)
                        .orderByDesc(Teachplan::getOrderby)
                        .last("LIMIT 1");
                Teachplan temp = teachplanMapper.selectOne(queryWrapper);
                exchangeOrderby(teachplan, temp);
            } else if (grade == 2) {
                LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(Teachplan::getParentid, parentId)
                        .lt(Teachplan::getOrderby, orderby)
                        .orderByDesc(Teachplan::getOrderby)
                        .last("LIMIT 1");
                Teachplan tmp = teachplanMapper.selectOne(queryWrapper);
                exchangeOrderby(teachplan, tmp);
            }
        } else if (moveType.equals("movedown")) {
            if (grade == 1) {
                // 章节上移
                LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(Teachplan::getGrade, 1)
                        .eq(Teachplan::getCourseId, courseId)
                        .gt(Teachplan::getOrderby, orderby)
                        .orderByAsc(Teachplan::getOrderby)
                        .last("LIMIT 1");
                Teachplan temp = teachplanMapper.selectOne(queryWrapper);
                exchangeOrderby(teachplan, temp);
            } else if (grade == 2) {
                LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(Teachplan::getParentid, parentId)
                        .gt(Teachplan::getOrderby, orderby)
                        .orderByAsc(Teachplan::getOrderby)
                        .last("LIMIT 1");
                Teachplan tmp = teachplanMapper.selectOne(queryWrapper);
                exchangeOrderby(teachplan, tmp);
            }
        }
    }

    @Override
    @Transactional
    public void associationMedia(BindTeachplanMediaDto bindTeachplanMediaDto) {

        // 教学计划id
        Long teachplanId = bindTeachplanMediaDto.getTeachplanId();
        Teachplan teachplan = teachplanMapper.selectById(teachplanId);

        if (teachplan == null) {
            XueChengPlusException.cast("教学计划不存在");
        }

        if (teachplan.getGrade() != 2) {
            XueChengPlusException.cast("目前只允许第二级教学计划绑定媒资文件");
        }

        // 课程id
        Long courseId = teachplan.getCourseId();

        // 先删除原始记录
        teachplanMediaMapper.delete(new LambdaQueryWrapper<TeachplanMedia>().eq(TeachplanMedia::getTeachplanId, teachplanId));

        // 再添加绑定媒资后的记录
        TeachplanMedia teachplanMedia = new TeachplanMedia();
        teachplanMedia.setCourseId(courseId);
        teachplanMedia.setTeachplanId(teachplanId);
        teachplanMedia.setMediaFilename(bindTeachplanMediaDto.getFileName());
        teachplanMedia.setMediaId(bindTeachplanMediaDto.getMediaId());
        teachplanMedia.setCreateDate(LocalDateTime.now());
        teachplanMediaMapper.insert(teachplanMedia);
    }

    @Override
    public void deleteMedia(Long teachPlanId, String mediaId) {
        LambdaQueryWrapper<TeachplanMedia> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TeachplanMedia::getTeachplanId, teachPlanId)
                .eq(TeachplanMedia::getMediaId, mediaId);
        teachplanMediaMapper.delete(queryWrapper);
    }

    /**
     * 更换课程计划排序
     *
     * @param teachplan
     * @param temp
     */
    private void exchangeOrderby(Teachplan teachplan, Teachplan temp) {
        if (temp == null) {
            XueChengPlusException.cast("已经到头啦 O.o?");
        } else {
            Integer orderby = teachplan.getOrderby();
            Integer tempOrderby = temp.getOrderby();
            teachplan.setOrderby(tempOrderby);
            temp.setOrderby(orderby);
            teachplanMapper.updateById(teachplan);
            teachplanMapper.updateById(temp);
        }
    }
}
