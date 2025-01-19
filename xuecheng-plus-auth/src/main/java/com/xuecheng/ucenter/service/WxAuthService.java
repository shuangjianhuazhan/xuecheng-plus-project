package com.xuecheng.ucenter.service;

import com.xuecheng.ucenter.model.po.XcUser;

/**
 * @author sdx
 * @version 1.0.0
 * @date 2025/01/19
 * @doc 微信认证接口
 */
public interface WxAuthService {
    /**
     * @param code 授权码
     * @return {@link XcUser }
     * @doc 微信扫码认证，申请令牌，查询用户信息，保存到数据库
     * @author sdx
     * @Date 2025/01/19
     */
    public XcUser wxAuth(String code);
}
