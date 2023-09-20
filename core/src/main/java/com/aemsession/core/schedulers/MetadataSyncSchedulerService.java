package com.aemsession.core.schedulers;

import com.aemsession.core.configs.MetadataSyncScheduler;
import com.aemsession.core.services.ReadJsonAndUpdateMetadataService;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Component(service = Runnable.class, immediate = true)
@Designate(ocd = MetadataSyncScheduler.class)
public class MetadataSyncSchedulerService implements Runnable {

    public static final Logger LOG = LoggerFactory.getLogger(MetadataSyncSchedulerService.class);

    @Reference
    private Scheduler scheduler;

    @Reference
    private ReadJsonAndUpdateMetadataService readJsonAndUpdateMetadataService;

    @Activate
    protected void activate(MetadataSyncScheduler syncScheduler) {
        addScheduler(syncScheduler);
    }

    public void addScheduler(MetadataSyncScheduler syncScheduler) {
        LOG.info("Scheduler added successfully :)");
        if (syncScheduler.enableScheduler()) {
            ScheduleOptions options = scheduler.EXPR(syncScheduler.cronExpression());
            options.name(syncScheduler.schedulerName());
            scheduler.schedule(this, options);
            LOG.info("Scheduler added successfully name='{}'", syncScheduler.schedulerName());
        } else {
            LOG.info("AssetMetadataSyncScheduler disabled");
        }
    }

    @Deactivate
    protected void deactivate(MetadataSyncScheduler config) {
        removeScheduler(config);
    }

    public void removeScheduler(MetadataSyncScheduler config) {
        scheduler.unschedule(config.schedulerName());
    }

    @Modified
    protected void modified(MetadataSyncScheduler config) {
        removeScheduler(config);
        addScheduler(config);
    }

    @Override
    public void run() {
        List<String> updatedMetadata = readJsonAndUpdateMetadataService.readJsonAndUpdateMetadata();
        LOG.info("Updated Metadata From Scheduler : {}", updatedMetadata);
    }
}
