package com.triaged.logger;

/**
 * Created by Sadegh Kazemy on 9/6/14.
 */
public interface ILogger {

    public void i(String msg);

    public void w(String msg);

    public void w(String msg, Throwable throwable);

    public void e(String msg);

    public void e(Throwable throwable);

    public void e(String msg, Throwable throwable);
}
