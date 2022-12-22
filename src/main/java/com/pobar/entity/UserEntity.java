package com.pobar.entity;

import jakarta.persistence.*;

import java.sql.Timestamp;
import java.util.Objects;

@Entity
@Table(name = "user", schema = "pobar", catalog = "")
public class UserEntity {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id")
    private Integer id;
    @Basic
    @Column(name = "account")
    private String account;
    @Basic
    @Column(name = "salt")
    private String salt;
    @Basic
    @Column(name = "password")
    private String password;
    @Basic
    @Column(name = "email")
    private String email;
    @Basic
    @Column(name = "register_date")
    private Timestamp registerDate;
    @Basic
    @Column(name = "update_date")
    private Timestamp updateDate;
    @Basic
    @Column(name = "role")
    private Byte role;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Timestamp getRegisterDate() {
        return registerDate;
    }

    public void setRegisterDate(Timestamp registerDate) {
        this.registerDate = registerDate;
    }

    public Timestamp getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(Timestamp updateDate) {
        this.updateDate = updateDate;
    }

    public Byte getRole() {
        return role;
    }

    public void setRole(Byte role) {
        this.role = role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserEntity that = (UserEntity) o;
        return Objects.equals(id, that.id) && Objects.equals(account, that.account) && Objects.equals(salt, that.salt) && Objects.equals(password, that.password) && Objects.equals(email, that.email) && Objects.equals(registerDate, that.registerDate) && Objects.equals(updateDate, that.updateDate) && Objects.equals(role, that.role);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, account, salt, password, email, registerDate, updateDate, role);
    }
}
