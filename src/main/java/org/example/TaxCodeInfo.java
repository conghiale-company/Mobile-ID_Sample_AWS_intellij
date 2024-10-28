package org.example;

public class TaxCodeInfo {
    private int index;
    private String taxCode;
    private String status;
    private String actionDay;

    public TaxCodeInfo(int index, String taxCode, String status, String actionDay) {
        this.index = index;
        this.taxCode = taxCode;
        this.status = status;
        this.actionDay = actionDay;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getTaxCode() {
        return taxCode;
    }

    public void setTaxCode(String taxCode) {
        this.taxCode = taxCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getActionDay() {
        return actionDay;
    }

    public void setActionDay(String actionDay) {
        this.actionDay = actionDay;
    }
}
