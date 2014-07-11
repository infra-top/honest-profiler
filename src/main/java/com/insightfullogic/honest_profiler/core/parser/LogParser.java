package com.insightfullogic.honest_profiler.core.parser;

import java.io.File;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import static com.insightfullogic.honest_profiler.core.parser.LogParser.LogState.END_OF_LOG;
import static com.insightfullogic.honest_profiler.core.parser.LogParser.LogState.NOTHING_READ;
import static com.insightfullogic.honest_profiler.core.parser.LogParser.LogState.READ_RECORD;

public class LogParser {

    private static final int NOT_WRITTEN = 0;
    private static final int TRACE_START = 1;
    private static final int STACK_FRAME = 2;
    private static final int NEW_METHOD = 3;

    private final EventListener listener;

    public static enum LogState { READ_RECORD, NOTHING_READ, END_OF_LOG }

    public LogParser(EventListener listener) {
        this.listener = listener;
    }

    // TODO: delete
    public void parse(File file) {
        throw new UnsupportedOperationException("TODO: remove this");
    }

    // TODO: delete
    public void monitor(File file) {
        throw new UnsupportedOperationException("TODO: remove this");
    }

    public LogState readRecord(ByteBuffer input) throws IOException {
        if (!input.hasRemaining()) {
            listener.endOfLog();
            return END_OF_LOG;
        }

        byte type = input.get();
        try {
            switch (type) {
                case NOT_WRITTEN:
                    // back back one byte since we've just read a 0
                    //System.out.println("retry");
                    input.position(input.position() - 1);
                    return NOTHING_READ;
                case TRACE_START:
                    readTraceStart(input);
                    return READ_RECORD;
                case STACK_FRAME:
                    readStackFrame(input);
                    return READ_RECORD;
                case NEW_METHOD:
                    readNewMethod(input);
                    return READ_RECORD;
            }
        } catch (BufferUnderflowException e) {
            e.printStackTrace();
        }

        listener.endOfLog();
        return END_OF_LOG;
    }

    private void readNewMethod(ByteBuffer input) throws IOException {
        Method newMethod = new Method(input.getLong(), readString(input), readString(input), readString(input));
        newMethod.accept(listener);
    }

    private String readString(ByteBuffer input) throws IOException {
        int size = input.getInt();
        char[] buffer = new char[size];
        // conversion from c style characters to Java.
        for(int i = 0; i < size; i++) {
            buffer[i] = (char) input.get();
        }
        return new String(buffer);
    }

    private void readStackFrame(ByteBuffer input) throws IOException {
        int lineNumber = input.getInt();
        long methodId = input.getLong();
        StackFrame stackFrame = new StackFrame(lineNumber, methodId);
        stackFrame.accept(listener);
    }

    private void readTraceStart(ByteBuffer input) throws IOException {
        int numberOfFrames = input.getInt();
        long threadId = input.getLong();
        TraceStart traceStart = new TraceStart(numberOfFrames, threadId);
        traceStart.accept(listener);
    }

}