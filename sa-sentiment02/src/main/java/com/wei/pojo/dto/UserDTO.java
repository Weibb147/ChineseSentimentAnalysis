package com.wei.pojo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户数据传输对象
 * 用于在不同层之间传输用户数据
 * 
 * @author wei
 * @since 2025-11-10
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "用户数据传输对象")
public class UserDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    private Long id;

    @NotBlank(message = "用户名不能为空", groups = {LoginGroup.class, RegisterGroup.class})
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,20}$", message = "用户名必须是4-20位字母、数字或下划线", groups = {RegisterGroup.class})
    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @NotBlank(message = "邮箱不能为空", groups = {RegisterGroup.class})
    @Email(message = "邮箱格式不正确", groups = {RegisterGroup.class})
    @Schema(description = "邮箱")
    private String email;

    @NotBlank(message = "密码不能为空", groups = {LoginGroup.class, RegisterGroup.class})
    @Schema(description = "密码")
    private String password;

    @Schema(description = "头像URL")
    private String imageUrl;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "性别")
    private String sex;

    @Schema(description = "用户角色")
    private String role;

    @Schema(description = "用户状态")
    private Integer status;

    @Schema(description = "验证码")
    private String code;

    @Schema(description = "验证码UUID")
    private String uuid;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    /**
     * 登录校验组
     */
    public interface LoginGroup {}

    /**
     * 注册校验组
     */
    public interface RegisterGroup {}
}
