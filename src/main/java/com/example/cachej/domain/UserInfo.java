package com.example.cachej.domain;


import java.io.Serializable;

public class UserInfo {
    private Integer id;
    private String username;
    private String product;
    private String department;
    private String token;
    private String qps;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getQps() {
        return qps;
    }

    public void setQps(String qps) {
        this.qps = qps;
    }

    @Override
    public String toString() {
        return "UserInfo{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", product='" + product + '\'' +
                ", department='" + department + '\'' +
                ", token='" + token + '\'' +
                ", qps='" + qps + '\'' +
                '}';
    }
}
