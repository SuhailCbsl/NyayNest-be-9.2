package org.dspace.discovery.configuration;

import java.util.List;

public class PhoneticSearchFilter extends DiscoverySearchFilter{
    private boolean enabled = false;

    public PhoneticSearchFilter(){
        super();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<String> transformForPhoneticIndexing(List<String> originalValues) {
        if (!enabled || originalValues == null) {
            return originalValues;
        }

        // Example placeholder: return original values for now.
        // Replace with real phonetic transformation if required.
        return originalValues;
    }

    @Override
    public String toString() {
        return "PhoneticSearchFilter[indexFieldName=" + getIndexFieldName()
                + ", enabled=" + enabled + "]";
    }
}
