package com.example.news.domain.user.exception;

import com.example.news.domain.user.enums.UserErrorCode;
import com.example.news.global.exception.CustomException;

public class InvalidCredentialsException extends CustomException {
  public InvalidCredentialsException() {
    super(UserErrorCode.INVALID_CREDENTIALS);
  }
}