package com.aemsession.core.configs;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Metadata Sync Scheduler", description = "Scheduler to sync metadata from json to AEM Assets.")
public @interface MetadataSyncScheduler {

    @AttributeDefinition(name = "Scheduler Name", description = "Enter Scheduler Name")
    public String schedulerName() default "Scheduler for Metadata Sync";

    @AttributeDefinition(name = "Cron Expression", description = "Enter Cron Expression")
    public String cronExpression();

    @AttributeDefinition(name = "Enable Scheduler", description = "Check to Enable this Scheduler")
    public boolean enableScheduler() default false;

}
