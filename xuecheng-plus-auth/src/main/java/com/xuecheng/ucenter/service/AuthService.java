package com.xuecheng.ucenter.service;

import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;


/**
 * @author sdx
 * @version 1.0.0
 * @date 2025/01/17
 * @doc 统一的认证接口
 */
public interface AuthService {


    /**
     * @param authParamsDto
     * @return {@link XcUserExt }
     * @doc 认证方法
     * @author sdx
     * @Date 2025/01/17
     */
    XcUserExt execute(AuthParamsDto authParamsDto);
}
