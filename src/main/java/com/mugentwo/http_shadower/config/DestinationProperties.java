package com.mugentwo.http_shadower.config;

public class DestinationProperties {
    private String name;
    private String url;
    private boolean enabled = true;
    private boolean responseSource = false;

    public DestinationProperties() {}

    public DestinationProperties(String name, String url, boolean enabled) {
        this.name = name;
        this.url = url;
        this.enabled = enabled;
    }

    public DestinationProperties(String name, String url, boolean enabled, boolean responseSource) {
        this.name = name;
        this.url = url;
        this.enabled = enabled;
        this.responseSource = responseSource;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isResponseSource() {
        return responseSource;
    }

    public void setResponseSource(boolean responseSource) {
        this.responseSource = responseSource;
    }

    @Override
    public String toString() {
        return "DestinationProperties{" +
                "name='" + name + '\'' +
                ", url='" + url + '\'' +
                ", enabled=" + enabled +
                ", responseSource=" + responseSource +
                '}';
    }
}