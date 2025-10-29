package com.tiki.auth.dto;

public class AddressDto {
    private Long id;
    private String label;
    private String jsonAddress;
    private boolean isDefault;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getJsonAddress() { return jsonAddress; }
    public void setJsonAddress(String jsonAddress) { this.jsonAddress = jsonAddress; }
    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean aDefault) { isDefault = aDefault; }
}


