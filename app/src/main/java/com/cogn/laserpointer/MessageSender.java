package com.cogn.laserpointer;


import org.zeromq.ZMQ;
import java.util.concurrent.TimeoutException;

public class MessageSender {
    private static final String TAG = "MESSAGE_SENDER";
    private final MainActivity mainActivity;
    private static volatile boolean busySending;
    private static volatile boolean isRunning;

    private String address;
    private ZMQ.Context context;
    private ZMQ.Socket socket;

    private String message = null;
    private boolean requestStop = false;



    public MessageSender(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void changeAddress(String address){
        stop();
        this.address = address;
        start();
    }

    public void requestMessageSend(String message) throws IllegalStateException{
        if (!isRunning) {
            throw new IllegalStateException("No server is running");
        }
        if (!busySending) {
            this.message = message;
            busySending = true;
        }
    }

    public void sendMessageAndWait(String message) throws TimeoutException, IllegalStateException {
        if (!isRunning) {
            throw new IllegalStateException("No server is running");
        }
        for (int i = 0; i < 40; i++) {
            if (!busySending) {
                this.message = message;
                busySending = true;
                while (busySending && isRunning) {

                }
                if (isRunning) {
                    return;
                } else {
                    throw new TimeoutException("Server stopped running while waiting for a response");
                }
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        throw new TimeoutException("Did not get a chance to send.  Stopped trying.");
    }

    private void sendMsgWhenNotNull() {
        try {
            context = ZMQ.context(1);
            socket = context.socket(ZMQ.REQ);
            socket.setReceiveTimeOut(1000);
            socket.setLinger(1000);
            try {
                socket.connect(address);
                socket.send("z0,0", 0);
                byte[] response = socket.recv(0);
                if (response==null) throw new IllegalArgumentException("No server running");
            } catch (IllegalArgumentException e) {
                isRunning = false;
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mainActivity.showDialogNoResponseError();
                    }
                });
            }
            isRunning = true;
            while (!requestStop) {
                if (message != null) {
                    socket.send(message.getBytes(), 0);
                    byte[] response = socket.recv(0);
                    if (response==null) {
                        socket.close();
                        context.term();
                        context = ZMQ.context(1);
                        socket = context.socket(ZMQ.REQ);
                        socket.setReceiveTimeOut(1000);
                        socket.setLinger(1000);
                        socket.connect(address);
                    }
                    //String result = new String();
                    message = null;
                    busySending = false;
                }
            }
            socket.close();
            context.term();
            isRunning = false;
        } catch (final Exception e) {
            showDialogGeneralErrorOnUi("Unknown", e.getMessage());
            e.printStackTrace();
            isRunning = false;
        }
    }


    public void start()
    {
        new Thread(new Runnable() {
            public void run() {
                sendMsgWhenNotNull();
            }
        }).start();
    }

    public void stop() {
        requestStop = true;
        for (int i = 0; i < 30; i++) {
            if (!isRunning) {
                requestStop = false;
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void showDialogGeneralErrorOnUi(final String title, final String message) {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mainActivity.showDialogGeneralError(title, message);
            }
        });
    }
}
