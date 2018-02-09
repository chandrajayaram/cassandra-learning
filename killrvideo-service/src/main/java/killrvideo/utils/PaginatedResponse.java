package killrvideo.utils;

import java.util.List;

public class PaginatedResponse<T> {
	private List<T> data;
	private String pagingState;
	private Throwable exeception;
	
	public Throwable getExeception() {
		return exeception;
	}

	public void setExeception(Throwable ex) {
		this.exeception = ex;
	}

	public List<T> getData() {
		return data;
	}

	public void setData(List<T> data) {
		this.data = data;
	}

	public String getPagingState() {
		return pagingState;
	}

	public void setPagingState(String pagingState) {
		this.pagingState = pagingState;
	}
}
