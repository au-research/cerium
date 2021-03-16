package ardc.cerium.drvs.event;

import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.entity.Request;

public class DRVSRecordUpdatedEvent {
    private final Record record;

    private final Request request;

    public DRVSRecordUpdatedEvent(Record record, Request request) {
        this.record = record;
        this.request = request;
    }

    public Record getRecord() {
        return record;
    }

    public Request getRequest() {
        return request;
    }
}
