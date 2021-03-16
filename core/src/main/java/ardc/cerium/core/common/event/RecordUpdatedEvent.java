package ardc.cerium.core.common.event;

import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.model.User;

public class RecordUpdatedEvent {

	private final Record record;

	private User user;

	public RecordUpdatedEvent(Record record) {
		this.record = record;
	}

	public RecordUpdatedEvent(Record record, User user) {
		this.record = record;
		this.user = user;
	}

	public Record getRecord() {
		return record;
	}

	public User getUser() {
		return user;
	}

}
