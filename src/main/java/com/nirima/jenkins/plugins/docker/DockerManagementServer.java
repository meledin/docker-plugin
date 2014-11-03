package com.nirima.jenkins.plugins.docker;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;

import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;
import com.nirima.jenkins.plugins.docker.utils.Consts;

/**
 * Created by magnayn on 22/02/2014.
 */
public class DockerManagementServer implements Describable<DockerManagementServer>
{
    final String      name;
    final DockerCloud theCloud;
    
    public Descriptor<DockerManagementServer> getDescriptor()
    {
        return Jenkins.getInstance().getDescriptorByType(DescriptorImpl.class);
    }
    
    public String getUrl()
    {
        return DockerManagement.get().getUrlName() + "/server/" + this.name;
    }
    
    public DockerManagementServer(String name)
    {
        this.name = name;
        this.theCloud = PluginImpl.getInstance().getServer(name);
    }
    
    public Collection<Image> getImages()
    {
        return this.theCloud.connect().listImagesCmd().withShowAll(false).exec();
    }
    
    public Collection<Container> getProcesses()
    {
        return this.theCloud.connect().listContainersCmd().withShowAll(false).exec();
    }
    
    public String asTime(Long time)
    {
        if (time == null)
            return "";
        
        long when = System.currentTimeMillis() - time;
        
        Date dt = new Date(when);
        return dt.toString();
    }
    
    public String getJsUrl(String jsName)
    {
        return Consts.PLUGIN_JS_URL + jsName;
    }
    
    public void doControlSubmit(@QueryParameter("stopId") String stopId, StaplerRequest req, StaplerResponse rsp) throws ServletException, IOException, InterruptedException
    {
        
        this.theCloud.connect().stopContainerCmd(stopId).exec();
        
        rsp.sendRedirect(".");
    }
    
    @Extension
    public static final class DescriptorImpl extends Descriptor<DockerManagementServer>
    {
        
        @Override
        public String getDisplayName()
        {
            return "server ";
        }
        
    }
}
