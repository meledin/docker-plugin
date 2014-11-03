package com.nirima.jenkins.plugins.docker.builder;

import hudson.Extension;
import hudson.model.AbstractBuild;

import org.kohsuke.stapler.DataBoundConstructor;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.DockerException;

/**
 * Created by magnayn on 30/01/2014.
 */
public class DockerBuilderControlOptionStop extends DockerBuilderControlOptionStopStart {

    public final boolean remove;

    @DataBoundConstructor
    public DockerBuilderControlOptionStop(String cloudId, String containerId, boolean remove) {
        super(cloudId, containerId);
        this.remove = remove;
    }

    @Override
    public void execute(AbstractBuild<?, ?> build) throws DockerException {
        LOGGER.info("Stopping container " + this.containerId);
        DockerClient client = this.getClient(build);
        client.stopContainerCmd(this.containerId).exec();
        this.getLaunchAction(build).stopped(client, this.containerId);
        if( this.remove )
            client.removeContainerCmd(this.containerId).exec();
    }


    @Extension
    public static final class DescriptorImpl extends DockerBuilderControlOptionDescriptor {
        @Override
        public String getDisplayName() {
            return "Stop Container";
        }

    }
}
