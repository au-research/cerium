package ardc.cerium.core.exception;

public class TransformerNotFoundException extends APIException {

	private final String fromSchema;

	private final String toSchema;

	public TransformerNotFoundException(String fromSchema, String toSchema) {
		super();
		this.fromSchema = fromSchema;
		this.toSchema = toSchema;

	}

	@Override
	public String getMessageID() {
		return "api.error.transformer_exception";
	}

	@Override
	public String[] getArgs() {
		return new String[] { this.fromSchema, this.toSchema };
	}

}
