package com.nirima.jenkins.plugins.docker.utils;

import hudson.model.TaskListener;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.DelegatingComputerLauncher;
import hudson.slaves.SlaveComputer;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RetryingComputerLauncher extends DelegatingComputerLauncher {

    private static final Logger log = LoggerFactory.getLogger(RetryingComputerLauncher.class);

    /**
     * time (ms) to back off between retries?
     */
    private final int pause   = 5000;

    /**
     * Let us know when to pause the launch.
     */
    private boolean hasTried = false;

    public RetryingComputerLauncher(ComputerLauncher delegate) {
        super(delegate);
    }

    @Override
    public void launch(SlaveComputer computer, TaskListener listener) throws IOException, InterruptedException {
        if (this.hasTried) {
            log.info("Launch failed, pausing before retry.");
            Thread.sleep(this.pause);
        }
        super.launch(computer, listener);
        this.hasTried = true;
    }
}
