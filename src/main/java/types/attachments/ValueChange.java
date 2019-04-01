package types.attachments;

import java.util.ArrayList;

public class ValueChange {

	public enum ChangeType {

		ADD_FLOAT, DIA_FLOAT, SET_FLOAT, ADD_LIST_STRING, REMOVE_LIST_STRING;
		private ChangeType() {

		}
	}
}
