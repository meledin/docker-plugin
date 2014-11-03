package com.nirima.jenkins.plugins.docker.listener;

import hudson.Extension;
import hudson.model.Run;
import hudson.model.listeners.RunListener;

import java.util.List;
import java.util.logging.Logger;

import com.nirima.jenkins.plugins.docker.action.DockerBuildImageAction;

/**
 * Listen for builds being deleted, and optionally clean up resources
 * (docker images) when this happens.
 *
 */
@Extension
public class DockerRunListener extends RunListener<Run<?,?>> {
    private static final Logger LOGGER = Logger.getLogger(DockerRunListener.class.getName());

    @Override
    public void onDeleted(Run<?, ?> run) {
        super.onDeleted(run);
        List<DockerBuildImageAction> actions = run.getActions(DockerBuildImageAction.class);

        for(DockerBuildImageAction action : actions) {
            if( action.cleanupWithJenkinsJobDelete ) {
                LOGGER.info("Attempting to clean up docker image for " + run);


                if( action.pushOnSuccess ) {
                    
                    // TODO: This was just deleted.

                }
            }
        }

    }
}
