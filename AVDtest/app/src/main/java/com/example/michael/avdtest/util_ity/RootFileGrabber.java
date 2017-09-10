package com.example.michael.avdtest.util_ity;

/**
 * Created by Michael on 4/28/2017.
 */

import android.os.Handler;
import android.os.HandlerThread;

import java.util.ArrayList;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;

public class RootFileGrabber {

    public static Shell.Interactive shellInteractive;
    public static Handler handler;
    private static HandlerThread handlerThread;

    /**
     * Initializes an interactive shell, which will stay throughout the app lifecycle
     * The shell is associated with a handler thread which maintain the message queue from the
     * callbacks of shell as we certainly cannot allow the callbacks to run on same thread because
     * of possible deadlock situation and the asynchronous behaviour of LibSuperSU
     */
    public void initializeInteractiveShell() {

        // only one looper can be associated to a thread. So we're making sure not to create new
        // handler threads every time the code relaunch.

        handlerThread = new HandlerThread("handler");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        shellInteractive = (new Shell.Builder()).useSU().setHandler(handler).open();

    }

    /**
     * Runs a command line prompt in shell script
     *
     * @param cmd
     *   Command line arguments
     * @return
     *   Arraylist of the output(s) of the command
     */
    public static ArrayList<String> runShellCommand(String cmd){

        final ArrayList<String> result = new ArrayList<>();

        // callback being called on a background handler thread
        shellInteractive.addCommand(cmd, 0, new Shell.OnCommandResultListener() {
            @Override
            public void onCommandResult(int commandCode, int exitCode, List<String> output) {
                if(output != null) {
                    for (String line : output) {
                        result.add(line);
                    }
                }
            }
        });
        shellInteractive.waitForIdle();

        return result;
    }

    /**
     * Closes the API, must be called before destroy()
     */
    public static void closeInteractiveShell(){
        shellInteractive.close();
    }

}
