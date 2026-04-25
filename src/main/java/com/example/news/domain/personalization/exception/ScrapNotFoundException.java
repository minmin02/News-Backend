package com.example.news.domain.personalization.exception;

import com.example.news.domain.personalization.exception.code.PersonalizationErrorCode;
import com.example.news.global.exception.CustomException;

public class ScrapNotFoundException extends CustomException {
    public ScrapNotFoundException() {
        super(PersonalizationErrorCode.SCRAP_NOT_FOUND);
    }
}
