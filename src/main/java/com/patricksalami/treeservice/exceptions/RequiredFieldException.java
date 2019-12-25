package com.patricksalami.treeservice.exceptions;

public class RequiredFieldException extends RuntimeException {

    private String fieldName;

    public RequiredFieldException(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return this.fieldName;
    }
}
