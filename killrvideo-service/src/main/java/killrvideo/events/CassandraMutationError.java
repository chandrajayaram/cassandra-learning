package killrvideo.events;


import killrvideo.utils.ExceptionUtils;

public class CassandraMutationError {

    public final Object request;
    public final Throwable throwable;

    public CassandraMutationError(Object request, Throwable throwable) {
        this.request = request;
        this.throwable = throwable;
    }

    public String buildErrorLog() {
        StringBuilder builder = new StringBuilder();
        builder.append(request.toString()).append("\n");
        builder.append(throwable.getMessage()).append("\n");
        builder.append(ExceptionUtils.mergeStackTrace(throwable));
        return builder.toString();
    }
}
