package com.xuecheng.ucenter.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.mapper.XcUserRoleMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.model.po.XcUserRole;
import com.xuecheng.ucenter.service.AuthService;
import com.xuecheng.ucenter.service.WxAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * @author sdx
 * @version 1.0.0
 * @date 2025/01/19
 * @doc 微信扫码认证
 */
@Slf4j
@Service("wx_authservice")
public class WxAuthServiceImpl implements AuthService, WxAuthService {

    @Autowired
    private XcUserMapper xcUserMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private XcUserRoleMapper xcUserRoleMapper;

    @Autowired
    private WxAuthServiceImpl currentProxy;

    @Value("${weixin.appid}")
    private String appid;

    @Value("${weixin.secret}")
    private String secret;

    @Override
    public XcUserExt execute(AuthParamsDto authParamsDto) { // 统一认证方法

        // 账号
        String username = authParamsDto.getUsername();

        // 查询数据库
        XcUser user = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getUsername, username));
        if (user == null) {
            //返回空表示用户不存在
            throw new RuntimeException("账号不存在");
        }

        XcUserExt xcUserExt = new XcUserExt();
        BeanUtils.copyProperties(user, xcUserExt);
        return xcUserExt;
    }

    @Override
    public XcUser wxAuth(String code) {

        // 微信扫码认证，申请令牌，查询用户信息，保存到数据库

        // 获取 access_token
        Map<String, String> accessToken = getAccess_token(code);

        // 获取用户信息
        Map<String, String> userinfo = getUserinfo(accessToken.get("access_token"), accessToken.get("openid"));

        // 保存用户信息到数据库
        return currentProxy.addWxUser(userinfo);
    }

    /**
     * @param code 授权码
     * @return {@link Map }<{@link String },{@link String }>
     * @doc 携带授权码申请令牌
     * https://api.weixin.qq.com/sns/oauth2/access_token?appid=APPID&secret=SECRET&code=CODE&grant_type=authorization_code
     * {
     * "access_token":"ACCESS_TOKEN",
     * "expires_in":7200,
     * "refresh_token":"REFRESH_TOKEN",
     * "openid":"OPENID",
     * "scope":"SCOPE",
     * "unionid": "UNIONID"
     * }
     * @author sdx
     * @Date 2025/01/19
     */
    private Map<String, String> getAccess_token(String code) {

        String url_temp = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=%s&secret=%s&code=%s&grant_type=authorization_code";

        // 请求令牌的url
        String url = String.format(url_temp, appid, secret, code);
        log.info("调用微信接口申请access_token, url:{}", url);

        // 远程调用此url
        ResponseEntity<String> exchange = restTemplate.exchange(url, HttpMethod.POST, null, String.class);

        // 获取响应的结果
        String result = exchange.getBody();

        // 将结果转成map
        Map<String, String> map = JSON.parseObject(result, Map.class);
        return map;
    }

    /**
     * @param access_token
     * @param openid
     * @return {@link Map }<{@link String },{@link String }>
     * @doc 获取用户信息
     * https://api.weixin.qq.com/sns/userinfo?access_token=ACCESS_TOKEN&openid=OPENID
     * {
     * "openid":"OPENID",
     * "nickname":"NICKNAME",
     * "sex":1,
     * "province":"PROVINCE",
     * "city":"CITY",
     * "country":"COUNTRY",
     * "headimgurl": "https://thirdwx.qlogo.cn/mmopen/g3MonUZtNHkdmzicIlibx6iaFqAc56vxLSUfpb6n5WKSYVY0ChQKkiaJSgQ1dZuTOgvLLrhJbERQQ4eMsv84eavHiaiceqxibJxCfHe/0",
     * "privilege":[
     * "PRIVILEGE1",
     * "PRIVILEGE2"
     * ],
     * "unionid": " o6_bmasdasdsad6_2sgVt7hMZOPfL"
     * }
     * @author sdx
     * @Date 2025/01/19
     */
    private Map<String, String> getUserinfo(String access_token, String openid) {
        String url_temp = "https://api.weixin.qq.com/sns/userinfo?access_token=%s&openid=%s";
        String url = String.format(url_temp, access_token, openid);
        ResponseEntity<String> exchange = restTemplate.exchange(url, HttpMethod.POST, null, String.class);
        //防止乱码进行转码
        String result = new String(Objects.requireNonNull(exchange.getBody()).getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        log.info("调用微信接口申请access_token: 返回值:{}", result);
        Map<String, String> map = JSON.parseObject(result, Map.class);
        return map;
    }

    /**
     * @param userInfo_map
     * @return {@link XcUser }
     * @doc 保存用户信息
     * @author sdx
     * @Date 2025/01/19
     */
    @Transactional
    public XcUser addWxUser(Map<String, String> userInfo_map){

        // 获取unionid
        String unionid = userInfo_map.get("unionid");

        // 根据union查询用户信息是否存在
        XcUser xcUser = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getWxUnionid, unionid));
        if (xcUser != null) {
            return xcUser;
        }

        // 用户数据不存在，新增用户信息
        xcUser = new XcUser();
        String userId = UUID.randomUUID().toString(); // 主键
        xcUser.setId(userId);
        xcUser.setWxUnionid(unionid);
        //记录从微信得到的昵称
        xcUser.setNickname(userInfo_map.get("nickname"));
        xcUser.setUserpic(userInfo_map.get("headimgurl"));
        xcUser.setName(userInfo_map.get("nickname"));
        xcUser.setUsername(unionid);
        xcUser.setPassword(unionid);
        xcUser.setUtype("101001");//学生类型
        xcUser.setStatus("1");//用户状态
        xcUser.setCreateTime(LocalDateTime.now());
        xcUserMapper.insert(xcUser);

        // 向用户角色表添加数据
        XcUserRole xcUserRole = new XcUserRole();
        xcUserRole.setId(UUID.randomUUID().toString());
        xcUserRole.setUserId(userId);
        xcUserRole.setRoleId("17");//学生角色
        xcUserRole.setCreateTime(LocalDateTime.now());
        xcUserRoleMapper.insert(xcUserRole);
        return xcUser;
    }
}
