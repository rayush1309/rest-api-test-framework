package com.sdetlabs.api.dummyjson.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Cart {
    private Integer id;
    private Integer userId;
    private List<CartProduct> products;
    private Double total;
    private Double discountedTotal;
    private Integer totalProducts;
    private Integer totalQuantity;
    private Boolean isDeleted;
    private String deletedOn;
    private Boolean merge;

    public Cart() { }

    public Cart(Integer userId, List<CartProduct> products) {
        this.userId = userId;
        this.products = products;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public List<CartProduct> getProducts() { return products; }
    public void setProducts(List<CartProduct> products) { this.products = products; }
    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }
    public Double getDiscountedTotal() { return discountedTotal; }
    public void setDiscountedTotal(Double discountedTotal) { this.discountedTotal = discountedTotal; }
    public Integer getTotalProducts() { return totalProducts; }
    public void setTotalProducts(Integer totalProducts) { this.totalProducts = totalProducts; }
    public Integer getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(Integer totalQuantity) { this.totalQuantity = totalQuantity; }
    public Boolean getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Boolean deleted) { isDeleted = deleted; }
    public String getDeletedOn() { return deletedOn; }
    public void setDeletedOn(String deletedOn) { this.deletedOn = deletedOn; }
    public Boolean getMerge() { return merge; }
    public void setMerge(Boolean merge) { this.merge = merge; }
}
