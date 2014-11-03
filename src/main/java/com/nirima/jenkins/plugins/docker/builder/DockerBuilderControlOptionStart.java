package com.nirima.jenkins.plugins.docker.builder;

import hudson.Extension;
import hudson.model.AbstractBuild;

import org.kohsuke.stapler.DataBoundConstructor;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.DockerException;

/**
 * Created by magnayn on 30/01/2014.
 */
public class DockerBuilderControlOptionStart extends DockerBuilderControlOptionStopStart {

    @DataBoundConstructor
    public DockerBuilderControlOptionStart(String cloudId, String containerId) {
        super(cloudId, containerId);
    }

    @Override
    public void execute(AbstractBuild<?, ?> build) throws DockerException {

        LOGGER.info("Starting container " + this.containerId);
        DockerClient client = this.getClient(build);
        client.startContainerCmd(this.containerId).exec();
        this.getLaunchAction(build).started(client, this.containerId);

    }

    @Extension
    public static final class DescriptorImpl extends DockerBuilderControlOptionDescriptor {
        @Override
        public String getDisplayName() {
            return "Start Container";
        }

    }
}
