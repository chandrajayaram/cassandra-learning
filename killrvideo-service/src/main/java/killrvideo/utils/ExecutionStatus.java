package killrvideo.utils;

public class ExecutionStatus {
	public enum STATUS{ 
	SUCCESS, FAIL
	}
	private STATUS status;
	private Throwable exception;
	private Object data;
	
	public STATUS getStatus() {
		return status;
	}
	public Throwable getException() {
		return exception;
	}
	public void setStatus(STATUS status) {
		this.status = status;
	}
	public void setException(Throwable exception) {
		this.exception = exception;
	}
	public Object getData() {
		return data;
	}
	public void setData(Object data) {
		this.data = data;
	}
	
	
	
}
