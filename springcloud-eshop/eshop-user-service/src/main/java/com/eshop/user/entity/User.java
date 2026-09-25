package com.eshop.user.entity;

/**
 * 用户实体
 */
public class User {

    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String phone;
    private String email;
    private String avatar;
    private Integer gender;   // 0-未知 1-男 2-女
    private String role;      // USER / ADMIN
    private Integer status;   // 0-禁用 1-正常

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public Integer getGender() { return gender; }
    public void setGender(Integer gender) { this.gender = gender; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
