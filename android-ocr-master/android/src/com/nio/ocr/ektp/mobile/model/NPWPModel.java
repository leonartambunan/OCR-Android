package com.nio.ocr.ektp.mobile.model;

public class NPWPModel {

    private String taxId;
    private String taxSubject;
    private String registeredDate;
    private String issuer;

    public String getTaxId() {
        return taxId==null?null:taxId.trim().toUpperCase().replaceAll("O","0");
    }

    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }

    public String getTaxSubject() {
        return taxSubject==null?null: taxSubject.trim().toUpperCase().replaceAll("\n"," ").replaceAll("^NA[MN]A","");
    }

    public void setTaxSubject(String taxSubject) {
        this.taxSubject = taxSubject;
    }

    public String getRegisteredDate() {
        return registeredDate==null?null:registeredDate.trim().toUpperCase().replaceAll("O","0").replaceAll("I","1").replaceAll("l","1");
    }

    public void setRegisteredDate(String registeredDate) {
        this.registeredDate = registeredDate;
    }

    public String getIssuer() {
        return issuer==null?null:issuer.trim().toUpperCase();
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("NPWP Document\n");
        if (taxId !=null) sb.append("NPWP: ").append(getTaxId()).append("\n");
        if (taxSubject !=null) sb.append("Tax Subject: ").append((getTaxSubject())).append("\n");
        if (registeredDate!=null) sb.append("Registered Date: ").append(getRegisteredDate()).append("\n");
        if (issuer!=null) sb.append("Issuer: ").append(getIssuer()).append("\n");

        return sb.toString();
    }
}
