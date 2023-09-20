package com.aemsession.core.models.impl;

import com.aemsession.core.models.SlingModel;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@Model(adaptables = Resource.class,
       adapters = SlingModel.class,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class SlingModelImpl implements SlingModel {

    private static final Logger log = LoggerFactory.getLogger(SlingModelImpl.class);

    @Inject
    String slingModelText;

    @SlingObject
    Resource resource;

    @Override
    public String getSlingModelText() {
        return slingModelText;
    }

    @Override
    public List<String> getSlingAnnotations() {
        List<String> annotationDetails = new ArrayList<>();

        Resource annotationChild = resource.getChild("slingAnnoation");
        if (null == annotationChild) {
            return annotationDetails;
        }
        log.info("\n annotationChild = " + annotationChild);
        Iterable<Resource> children = annotationChild.getChildren();

        for (Resource childResource : children) {
            ValueMap resourceValueMap = childResource.adaptTo(ValueMap.class);
            String slingAnnotation = resourceValueMap.get("slingAnnoation", String.class);
            annotationDetails.add(slingAnnotation);
        }
        log.info("\n annotationDetails = " + annotationDetails);
        return annotationDetails;
    }
}
