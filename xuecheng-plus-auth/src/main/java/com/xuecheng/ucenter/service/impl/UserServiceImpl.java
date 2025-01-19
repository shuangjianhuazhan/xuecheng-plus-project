package com.xuecheng.ucenter.service.impl;

import com.alibaba.fastjson.JSON;
import com.xuecheng.ucenter.mapper.XcMenuMapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcMenu;
import com.xuecheng.ucenter.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class UserServiceImpl implements UserDetailsService {

    @Autowired
    private XcUserMapper xcUserMapper;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private XcMenuMapper xcMenuMapper;


    /**
     * @param s 请求认证的参数，是AuthParamsDto的字符串
     * @return {@link UserDetails }
     * @doc 重写方法
     * @author sdx
     * @Date 2025/01/17
     */
    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {

        // 将传入的JSON转成AuthParamsDto对象
        AuthParamsDto authParamsDto = null;
        try {
            authParamsDto = JSON.parseObject(s, AuthParamsDto.class);
        } catch (Exception e) {
            log.info("认证请求不符合项目要求:{}", s);
            throw new RuntimeException("认证请求数据格式不对");
        }

        // 认证类型
        String authType = authParamsDto.getAuthType();

        // 根据认证类型从spring容器中取出对应的Bean
        String beanName = authType + "_authservice";
        AuthService authService = applicationContext.getBean(beanName, AuthService.class);

        // 调用统一execute认证方法
        XcUserExt xcUserExt = authService.execute(authParamsDto);

        // 封装xcUserExt用户信息为UserDetails，根据UserDetails生成令牌
        return getUserPrincipal(xcUserExt);
    }


    /**
     * @param xcUserExt
     * @return {@link UserDetails }
     * @doc 查询用户信息
     * @author sdx
     * @Date 2025/01/17
     */
    private UserDetails getUserPrincipal(XcUserExt xcUserExt) {
        String password = xcUserExt.getPassword();

        //根据用户id查询权限
        List<XcMenu> xcMenus = xcMenuMapper.selectPermissionByUserId(xcUserExt.getId());
        String[] authorities = null;
        if (!xcMenus.isEmpty()) {
            List<String> permissions = new ArrayList<>();
            xcMenus.forEach(xcMenu -> {
                // 拿到用户权限
                permissions.add(xcMenu.getCode());
            });
            // 将permissions转成数组
            authorities = permissions.toArray(new String[0]);
        }

        // 先将密码置空，再将user封装成json数据
        xcUserExt.setPassword(null);
        String userJson = JSON.toJSONString(xcUserExt);
        // 返回UserDetails
        return User.withUsername(userJson).password(password).authorities(authorities).build();
    }
}
