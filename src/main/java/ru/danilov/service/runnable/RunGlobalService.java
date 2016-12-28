package ru.danilov.service.runnable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Danilov on 01.05.2016.
 */
public class RunGlobalService {
	private static String urlPathServer = null;
    public static void main(String[] args) throws InterruptedException, ExecutionException {
    	if(args.length > 0) {
    		urlPathServer = args[0];
    	}
        final ExecutorService screenServiceThread = Executors.newSingleThreadExecutor();
        CompletableFuture<Void> rezult = CompletableFuture.runAsync(new ScreenService(urlPathServer), screenServiceThread).exceptionally(t-> {
            t.printStackTrace();
            return null;
        });

        final ExecutorService userKeyThread = Executors.newSingleThreadExecutor();
        CompletableFuture.runAsync(new UserWindowsKeyListenerService(urlPathServer), userKeyThread).exceptionally(t-> {
            t.printStackTrace();
            return null;
        });

        rezult.get();
    }
}
