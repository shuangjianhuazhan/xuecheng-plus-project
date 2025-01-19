package com.xuecheng.ucenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.feignclient.CheckCodeClient;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.service.AuthService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * @author sdx
 * @version 1.0.0
 * @date 2025/01/17
 * @doc 账号密码方式认证
 */
@Service("password_authservice")
public class PasswordAuthServiceImpl implements AuthService {

    @Autowired
    private XcUserMapper xcUserMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CheckCodeClient checkCodeClient;

    @Override
    public XcUserExt execute(AuthParamsDto authParamsDto) {

        // 1. 远程调用验证码服务校验验证码
        String checkCode = authParamsDto.getCheckcode();

        // 验证码对应的key
        String checkCodeKey = authParamsDto.getCheckcodekey();

        // 非空校验
        if(StringUtils.isBlank(checkCodeKey) || StringUtils.isBlank(checkCode)){
            throw new RuntimeException("未输入验证码");
        }

        // 校验验证码
        Boolean verify = checkCodeClient.verify(checkCodeKey, checkCode);
        if (verify == null || !verify) {
            throw new RuntimeException("验证码输入错误");
        }

        // 2. 账号是否存在
        String username = authParamsDto.getUsername();

        // 根据username账号查询数据库
        LambdaQueryWrapper<XcUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(XcUser::getUsername, username);
        XcUser user = xcUserMapper.selectOne(queryWrapper);

        // 用户不存在
        if (user == null) {
            throw new RuntimeException("账号不存在");
        }

        // 3. 密码是否正确
        // 取出用户正确的密码
        String password = user.getPassword();

        // 拿到输入的密码
        String passwordIn = authParamsDto.getPassword();

        // 校验密码
        boolean matches = passwordEncoder.matches(passwordIn, password);
        if (!matches) {
            throw new RuntimeException("账号或密码错误");
        }

        // 4. 封装数据 + 权限并返回
        XcUserExt xcUserExt = new XcUserExt();
        BeanUtils.copyProperties(user, xcUserExt);
        return xcUserExt;
    }
}
