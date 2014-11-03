package com.nirima.jenkins.plugins.docker.builder;

import hudson.Extension;
import hudson.model.AbstractBuild;

import org.kohsuke.stapler.DataBoundConstructor;

import com.github.dockerjava.api.DockerException;
import com.nirima.jenkins.plugins.docker.action.DockerLaunchAction;

/**
 * Created by magnayn on 30/01/2014.
 */
public class DockerBuilderControlOptionStopAll extends DockerBuilderControlOption {

    public final boolean remove;

    @DataBoundConstructor
    public DockerBuilderControlOptionStopAll(boolean remove) {

        this.remove = remove;
    }

    @Override
    public void execute(AbstractBuild<?, ?> build) throws DockerException {
        LOGGER.info("Stopping all containers");
        for(DockerLaunchAction.Item containerItem : this.getLaunchAction(build).getRunning()) {
            try {
                LOGGER.info("Stopping container " + containerItem.id);
                containerItem.client.stopContainerCmd(containerItem.id).exec();

                if( this.remove )
                    containerItem.client.removeContainerCmd(containerItem.id).exec();

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Extension
    public static final class DescriptorImpl extends DockerBuilderControlOptionDescriptor {
        @Override
        public String getDisplayName() {
            return "Stop All Containers";
        }

    }
}
