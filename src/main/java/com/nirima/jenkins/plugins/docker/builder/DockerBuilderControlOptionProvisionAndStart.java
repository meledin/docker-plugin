package com.nirima.jenkins.plugins.docker.builder;

import hudson.Extension;
import hudson.model.AbstractBuild;

import org.kohsuke.stapler.DataBoundConstructor;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.DockerException;
import com.nirima.jenkins.plugins.docker.DockerTemplate;

/**
 * Created by magnayn on 30/01/2014.
 */
public class DockerBuilderControlOptionProvisionAndStart extends DockerBuilderControlCloudOption {
    private final String templateId;

    @DataBoundConstructor
    public DockerBuilderControlOptionProvisionAndStart(String cloudName, String templateId) {
        super(cloudName);
        this.templateId = templateId;
    }

    public String getTemplateId() {
        return this.templateId;
    }

    @Override
    public void execute(AbstractBuild<?, ?> build) throws DockerException {

        DockerTemplate template = this.getCloud(build).getTemplate(this.templateId);

        String containerId = template.provisionNew().getId();

        LOGGER.info("Starting container " + containerId);
        DockerClient client = this.getClient(build);
        client.startContainerCmd(containerId).exec();
        this.getLaunchAction(build).started(client, containerId);
    }

    @Extension
    public static final class DescriptorImpl extends DockerBuilderControlOptionDescriptor {
        @Override
        public String getDisplayName() {
            return "Provision & Start Container";
        }

    }
}
