package com.wei.pojo;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.wei.pojo.enums.Gender;
import com.wei.pojo.enums.UserRole;
import com.wei.pojo.enums.UserStatus;
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
 * 用户实体类
 * 
 * @author wei
 * @since 2025-11-10
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("user")
@Schema(description = "用户实体")
public class User implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @Schema(description = "用户ID")
    private Long id;

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,20}$", message = "用户名必须是4-20位字母、数字或下划线")
    @Schema(description = "用户名", required = true)
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @NotBlank(message = "密码不能为空")
    @JsonIgnore
    @Schema(description = "密码", required = true, hidden = true)
    private String password;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱", required = true)
    private String email;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "性别")
    private Gender sex;

    @Schema(description = "头像URL")
    private String imageUrl;

    @Builder.Default
    @Schema(description = "用户角色")
    private UserRole role = UserRole.USER;

    @Builder.Default
    @Schema(description = "用户状态")
    private UserStatus status = UserStatus.ENABLED;


    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginTime;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    /**
     * 判断用户是否为管理员
     */
    public boolean isAdmin() {
        return UserRole.ADMIN.equals(this.role);
    }

    /**
     * 判断用户是否启用
     */
    public boolean isEnabled() {
        return UserStatus.ENABLED.equals(this.status);
    }
}
