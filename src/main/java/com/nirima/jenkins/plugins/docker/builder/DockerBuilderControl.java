package com.nirima.jenkins.plugins.docker.builder;

import hudson.DescriptorExtensionList;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;

import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;

import com.github.dockerjava.api.DockerException;

/**
 * Created by magnayn on 29/01/2014.
 */
public class DockerBuilderControl extends Builder implements Serializable {
    private static final Logger LOGGER = Logger.getLogger(DockerBuilderControl.class.getName());

    public final DockerBuilderControlOption option;

    @DataBoundConstructor
    public DockerBuilderControl(DockerBuilderControlOption option) {
        this.option = option;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Start/Stop Docker Containers";
        }

        public static DescriptorExtensionList<DockerBuilderControlOption,DockerBuilderControlOptionDescriptor> getOptionList() {
            return DockerBuilderControlOptionDescriptor.all();
        }
    }


    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {

        try {
            this.option.execute(build);
        } catch (DockerException e) {
            throw new RuntimeException(e);
        }

        // Save the actions
        build.save();
        return true;
    }
}
