package com.example.news.domain.personalization.exception;

import com.example.news.domain.personalization.exception.code.PersonalizationErrorCode;
import com.example.news.global.exception.CustomException;

public class DuplicateScrapException extends CustomException {
    public DuplicateScrapException() {
        super(PersonalizationErrorCode.DUPLICATE_SCRAP);
    }
}
