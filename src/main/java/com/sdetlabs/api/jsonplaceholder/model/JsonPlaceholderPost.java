package com.sdetlabs.api.jsonplaceholder.model;

/** JSONPlaceholder post response. */
public record JsonPlaceholderPost(int userId, int id, String title, String body) { }
