package com.example.news.domain.user.exception;

import com.example.news.domain.user.enums.UserErrorCode;
import com.example.news.global.exception.CustomException;

public class UserNotFoundException extends CustomException {
  public UserNotFoundException() {
    super(UserErrorCode.USER_NOT_FOUND);
  }
}