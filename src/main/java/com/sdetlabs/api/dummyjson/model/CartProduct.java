package com.sdetlabs.api.dummyjson.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CartProduct {
    private Integer id;
    private String title;
    private Double price;
    private Integer quantity;
    private Double total;
    private Double discountPercentage;
    private Double discountedPrice;
    private Double discountedTotal;
    private String thumbnail;

    public CartProduct() { }

    public CartProduct(Integer id, Integer quantity) {
        this.id = id;
        this.quantity = quantity;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }
    public Double getDiscountPercentage() { return discountPercentage; }
    public void setDiscountPercentage(Double discountPercentage) { this.discountPercentage = discountPercentage; }
    public Double getDiscountedPrice() { return discountedPrice; }
    public void setDiscountedPrice(Double discountedPrice) { this.discountedPrice = discountedPrice; }
    public Double getDiscountedTotal() { return discountedTotal; }
    public void setDiscountedTotal(Double discountedTotal) { this.discountedTotal = discountedTotal; }
    public String getThumbnail() { return thumbnail; }
    public void setThumbnail(String thumbnail) { this.thumbnail = thumbnail; }
}
