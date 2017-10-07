package com.nio.ocr.ektp.mobile.model;

public class KTPModel {

    private String nik;
    private String name;
    private String province;
    private String kabupaten;
    private String kecamatan;
    private String kelurahan;
    private String rtRw;
    private String birthPlace;
    private String birthday;
    private String address;
    private String maritalStatus;
    private String occupation;
    private String sex;
    private String bloodType;
    private String religion;
    private String citizenship;
    private String expiryDate;

    public String getCitizenship() {
        return citizenship==null?null:citizenship.toUpperCase();
    }

    public void setCitizenship(String citizenship) {
        if (citizenship!=null)
            this.citizenship = citizenship.trim();
    }

    public String getReligion() {
        return religion==null?null:religion.toUpperCase();
    }

    public void setReligion(String religion) {

        if (religion != null)
            this.religion = religion.trim();
    }

    public String getBloodType() {

        return bloodType==null?null:bloodType.toUpperCase().replaceAll("4","A");
    }

    public void setBloodType(String bloodType) {

        if (bloodType != null)
            this.bloodType = bloodType.trim();
    }

    public String getSex() {
        return sex==null?null:sex.toUpperCase().replaceAll("4","A");
    }

    public void setSex(String sex) {
        if(sex !=null)
            this.sex = sex.trim();
    }

    public String getBirthPlace() {
        return birthPlace==null?null:birthPlace.toUpperCase().replaceAll("0","O");
    }

    public void setBirthPlace(String birthPlace) {

        if(birthPlace!=null)
            this.birthPlace = birthPlace.trim();
    }

    public String getNik() {
        return nik==null?null:nik.toUpperCase().replaceAll("O","0").replaceAll("l","1").replaceAll("I","1");
    }

    public void setNik(String nik) {

        if (nik!=null)
            this.nik = nik.trim();
    }

    public String getName() {
        return name==null?null:name.toUpperCase().replaceAll("0","O");
    }

    public void setName(String name) {
        if(name!=null)
            this.name = name.trim();
    }

    public String getProvince() {
        return province==null?null:province.toUpperCase().replaceAll("0","O");
    }

    public void setProvince(String province) {
        if(province!=null)
            this.province = province.trim();
    }

    public String getKabupaten() {
        return kabupaten==null?null:kabupaten.toUpperCase().replaceAll("0","O");
    }

    public void setKabupaten(String kabupaten) {

        if(kabupaten!=null)
            this.kabupaten = kabupaten.trim();
    }

    public String getKecamatan() {
        return kecamatan==null?null:kecamatan.toUpperCase().replaceAll("0","O");
    }

    public void setKecamatan(String kecamatan) {

        if (kecamatan!=null)
            this.kecamatan = kecamatan.trim();
    }

    public String getKelurahan() {
        return kelurahan==null?null:kelurahan.toUpperCase().replaceAll("0","O");
    }

    public void setKelurahan(String kelurahan) {

        if (kelurahan!=null)
            this.kelurahan = kelurahan.trim();
    }

    public String getRtRw() {
        return rtRw==null?null:rtRw.toUpperCase().replaceAll("O","0").replaceAll("l","1").replaceAll("I","1");
    }

    public void setRtRw(String rtRw) {

        if(rtRw !=null)
            this.rtRw = rtRw.trim();
    }

    public String getBirthday() {
        return birthday==null?null:birthday.toUpperCase().replaceAll("O","0").replaceAll("l","1").replaceAll("I","1");
    }

    public void setBirthday(String birthday) {

        if (birthday!=null)
            this.birthday = birthday.trim();
    }

    public String getAddress() {
        return address==null?null:address.toUpperCase().replaceAll("JLO","JL.").replaceAll("JL0","JL.");
    }

    public void setAddress(String address) {
        if(address!=null)
            this.address = address.trim();
    }

    public String getMaritalStatus() {
        return maritalStatus==null?null:maritalStatus.toUpperCase().replaceAll("4","A");
    }

    public void setMaritalStatus(String maritalStatus) {

        if (maritalStatus!=null)
            this.maritalStatus = maritalStatus.trim();
    }

    public String getOccupation() {
        return occupation==null?null:occupation.toUpperCase().replaceAll("4","A");
    }

    public void setOccupation(String occupation) {

        if (occupation !=null)
            this.occupation = occupation.trim();
    }

    public String getExpiryDate() {
        return expiryDate==null?null:expiryDate.toUpperCase().replaceAll("O","0").replaceAll("l","1").replaceAll("I","1");
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        if (province!=null) sb.append("Province:").append(getProvince()).append("\n");
        if (kabupaten!=null) sb.append("Kabupaten/Kota:").append(getKabupaten()).append("\n");
        if (nik!=null) sb.append("NIK:").append(getNik()).append("\n");
        if (name!=null) sb.append("Name:").append(getName()).append("\n");
        if (birthPlace!=null) sb.append("Birth Place:").append(getBirthPlace()).append("\n");
        if (birthday!=null) sb.append("Birthday:").append(getBirthday()).append("\n");
        if (sex!=null) sb.append("Sex:").append(getSex()).append("\n");
        if (bloodType!=null) sb.append("BloodType:").append(getBloodType()).append("\n");
        if (address!=null) sb.append("Address:").append(getAddress()).append("\n");
        if (rtRw!=null) sb.append("RT/RW:").append(getRtRw()).append("\n");
        if (kelurahan!=null) sb.append("Kelurahan:").append(getKelurahan()).append("\n");
        if (kecamatan!=null) sb.append("Kecamatan:").append(getKecamatan()).append("\n");
        if (religion!=null) sb.append("Religion:").append(getReligion()).append("\n");
        if (maritalStatus!=null) sb.append("Marital Status:").append(getMaritalStatus()).append("\n");
        if (occupation !=null) sb.append("Occupation:").append(getOccupation()).append("\n");
        if (citizenship!=null) sb.append("Citizenship:").append(getCitizenship()).append("\n");
        if (expiryDate!=null) sb.append("Expiry Date:").append(getExpiryDate());

        return sb.toString();
    }
}
