package com.oppshan.securedoc.web;

import com.oppshan.securedoc.bean.SystemConfigBean;
import com.oppshan.securedoc.dto.OrganizationView;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;
import jakarta.inject.Inject;

/**
 * Round-trips {@link OrganizationView} between the JSF component tree and
 * its stored value (the row id, as a string). Required by
 * {@code <p:autoComplete itemValue="#{o}">} on /admin/register.xhtml so the
 * picker can re-render the selected org label after validation failures.
 *
 * <p>{@code managed = true} routes the converter through CDI so
 * {@link SystemConfigBean} can be {@code @Inject}-ed.
 */
@FacesConverter(value = "organizationConverter", managed = true)
public class OrganizationViewConverter implements Converter<OrganizationView> {

    @Inject
    SystemConfigBean system;

    @Override
    public String getAsString(FacesContext context, UIComponent component, OrganizationView value) {
        if (value == null || value.getId() == null) {
            return "";
        }
        return String.valueOf(value.getId());
    }

    @Override
    public OrganizationView getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return system.findOrganizationById(Long.valueOf(value.trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
