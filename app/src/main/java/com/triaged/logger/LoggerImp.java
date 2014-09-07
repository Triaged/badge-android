package com.triaged.logger;

import android.util.Log;

/**
 * Created by Sadegh Kazemy on 9/6/14.
 */

public class LoggerImp implements ILogger {

    private boolean isEnable;

    private final static int INFO_LOG = 1;
    private final static int WARNING_LOG = 2;
    private final static int ERROR_LOG = 3;

    public LoggerImp(boolean isEnable) {
        this.isEnable = isEnable;
    }


    @Override
    public void i(String msg) {
        out(msg, INFO_LOG);
    }

    @Override
    public void e(String msg) {
        out(msg, ERROR_LOG);
    }

    @Override
    public void w(String msg) {
        out(msg, WARNING_LOG);
    }

    @Override
    public void w(String msg, Throwable throwable) {
        out(msg, WARNING_LOG);
        e(throwable);
    }

    @Override
    public void e(Throwable throwable) {
        if (isEnable) {
            throwable.printStackTrace();
        }
    }

    @Override
    public void e(String msg, Throwable throwable) {
        if (isEnable) {
            e(msg);
            e(throwable);
        }
    }

    public void out(String msg, int logType) {
        if (isEnable) {
            Thread thread = Thread.currentThread();
            StackTraceElement[] stack = thread.getStackTrace();
            StackTraceElement element = stack[4];

            int start = element.getClassName().lastIndexOf(".")+1;
            int end = element.getClassName().lastIndexOf("$");
            if (end == -1)
                end = element.getClassName().length();
            String tag = element.getClassName().substring(start, end);

            switch (logType) {
                case INFO_LOG:
                    Log.i(tag, msg + " ____ @ " + element.toString()+"\n");
                    break;
                case WARNING_LOG:
                    Log.w(tag, msg + " ____ @ " + element.toString() + "\n");
                    break;
                case ERROR_LOG:
                    Log.e(tag, msg + " ____ @ " + element.toString() + "\n");
                    break;

            }
        }
    }

}
