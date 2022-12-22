package com.pobar.entity;

import jakarta.persistence.*;

import java.sql.Date;
import java.util.Objects;

@Entity
@Table(name = "user_detail", schema = "pobar", catalog = "")
public class UserDetailEntity {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id")
    private Integer id;
    @Basic
    @Column(name = "account")
    private String account;
    @Basic
    @Column(name = "name")
    private String name;
    @Basic
    @Column(name = "phone")
    private String phone;
    @Basic
    @Column(name = "birthday")
    private Date birthday;
    @Basic
    @Column(name = "gender")
    private Byte gender;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Date getBirthday() {
        return birthday;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }

    public Byte getGender() {
        return gender;
    }

    public void setGender(Byte gender) {
        this.gender = gender;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserDetailEntity that = (UserDetailEntity) o;
        return Objects.equals(id, that.id) && Objects.equals(account, that.account) && Objects.equals(name, that.name) && Objects.equals(phone, that.phone) && Objects.equals(birthday, that.birthday) && Objects.equals(gender, that.gender);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, account, name, phone, birthday, gender);
    }
}
