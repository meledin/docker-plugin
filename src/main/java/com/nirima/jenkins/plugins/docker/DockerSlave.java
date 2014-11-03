package com.nirima.jenkins.plugins.docker;

import hudson.Extension;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.slaves.AbstractCloudSlave;
import hudson.slaves.NodeProperty;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.RetentionStrategy;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.kohsuke.stapler.DataBoundConstructor;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.DockerException;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.nirima.jenkins.plugins.docker.action.DockerBuildAction;


public class DockerSlave extends AbstractCloudSlave {

    private static final Logger LOGGER = Logger.getLogger(DockerSlave.class.getName());

    public final DockerTemplate dockerTemplate;
    public final String containerId;

    private transient Run theRun;

    @DataBoundConstructor
    public DockerSlave(DockerTemplate dockerTemplate, String containerId, String name, String nodeDescription, String remoteFS, int numExecutors, Mode mode, String labelString, ComputerLauncher launcher, RetentionStrategy retentionStrategy, List<? extends NodeProperty<?>> nodeProperties) throws Descriptor.FormException, IOException {
        super(name, nodeDescription, remoteFS, numExecutors, mode, labelString, launcher, retentionStrategy, nodeProperties);
        Preconditions.checkNotNull(dockerTemplate);
        Preconditions.checkNotNull(containerId);

        this.dockerTemplate = dockerTemplate;
        this.containerId = containerId;
    }

    public DockerCloud getCloud() {
        DockerCloud theCloud = this.dockerTemplate.getParent();

        if( theCloud == null ) {
            throw new RuntimeException("Docker template " + this.dockerTemplate + " has no parent ");
        }

        return theCloud;
    }

    @Override
    public String getDisplayName() {
        return this.name;
    }

    public void setRun(Run run) {
        this.theRun = run;
    }

    @Override
    public DockerComputer createComputer() {
        return new DockerComputer(this);
    }

    public boolean containerExistsInCloud() {
        try {
            DockerClient client = this.getClient();
            client.inspectContainerCmd(this.containerId).exec();
            return true;
        } catch(Exception ex) {
            return false;
        }
    }

    @Override
    protected void _terminate(TaskListener listener) throws IOException, InterruptedException {


        try {
            this.toComputer().disconnect(null);

            try {
                DockerClient client = this.getClient();
            } catch(Exception ex) {
                LOGGER.log(Level.SEVERE, "Failed to stop instance " + this.containerId + " for slave " + this.name + " due to exception", ex);
            }

            // If the run was OK, then do any tagging here
            if( this.theRun != null ) {
                try {
                    this.slaveShutdown(listener);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Failure to slaveShutdown instance " + this.containerId+ " for slave " + this.name , e);
                }
            }

            try {
                DockerClient client = this.getClient();
                client.removeContainerCmd(this.containerId).exec();
            } catch(Exception ex) {
                LOGGER.log(Level.SEVERE, "Failed to remove instance " + this.containerId + " for slave " + this.name + " due to exception",ex);
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failure to terminate instance " + this.containerId + " for slave " + this.name ,e);
        }
    }

    private void slaveShutdown(TaskListener listener) throws DockerException, IOException {

        // The slave has stopped. Should we commit / tag / push ?

        if(!this.getJobProperty().tagOnCompletion) {
            this.addJenkinsAction(null);
            return;
        }

        DockerClient client = this.getClient();


         // Commit
        
        String imgTag = client.commitCmd(this.containerId).withRepository(this.theRun.getParent().getDisplayName()).withTag(this.theRun.getDisplayName()).withAuthor("jenkins").exec();
        
        // Tag it with the jenkins name
        this.addJenkinsAction(imgTag);

        // SHould we add additional tags?
        /*try
        {
            String tagToken = this.getAdditionalTag(listener);

            if( !Strings.isNullOrEmpty(tagToken) ) {
                client.image(tag_image).tag(tagToken, false);
                this.addJenkinsAction(tagToken);

                if( this.getJobProperty().pushOnSuccess ) {
                    client.image(tagToken).push(null);
                }
            }
        }
        catch(Exception ex) {
            LOGGER.log(Level.SEVERE, "Could not add additional tags");
        }*/

        if( this.getJobProperty().cleanImages ) {

            // For some reason, docker delete doesn't delete all tagged
            // versions, despite force = true.
            // So, do it multiple times (protect against infinite looping).

            client.removeImageCmd(imgTag).withForce().exec();
        }

    }

    private String getAdditionalTag(TaskListener listener) {
        // Do a macro expansion on the addJenkinsAction token

        // Job property
        String tagToken = this.getJobProperty().additionalTag;

        // Do any macro expansions
        try {
            if(!Strings.isNullOrEmpty(tagToken)  )
                tagToken = TokenMacro.expandAll((AbstractBuild) this.theRun, listener, tagToken);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tagToken;
    }

    /**
     * Add a built on docker action.
     * @param tag_image
     * @throws IOException
     */
    private void addJenkinsAction(String tag_image) throws IOException {
        this.theRun.addAction( new DockerBuildAction(this.getCloud().serverUrl, this.containerId, tag_image, this.dockerTemplate.remoteFsMapping) );
        this.theRun.save();
    }

    public DockerClient getClient() {
        return this.getCloud().connect();
    }

    /**
     * Called when the slave is connected to Jenkins
     */
    public void onConnected() {

    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("name", this.name)
                .add("containerId", this.containerId)
                .add("template", this.dockerTemplate)
                .toString();
    }

    private DockerJobProperty getJobProperty() {

        try {
            DockerJobProperty p = (DockerJobProperty) ((AbstractBuild) this.theRun).getProject().getProperty(DockerJobProperty.class);

            if (p != null)
                return p;
        } catch(Exception ex) {
            // Don't care.
        }
        // Safe default
        return new DockerJobProperty(false,null,false, true);
    }

    @Extension
	public static final class DescriptorImpl extends SlaveDescriptor {

    	@Override
		public String getDisplayName() {
			return "Docker Slave";
    	};

		@Override
		public boolean isInstantiable() {
			return false;
		}

	}
}
