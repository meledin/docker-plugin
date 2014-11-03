package com.nirima.jenkins.plugins.docker.ws;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.DockerClientConfig.DockerClientConfigBuilder;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.nirima.jenkins.plugins.docker.MDockerClientBuilder;

public class MTEst
{
    public static void main(String[] args) throws IOException
    {
        DockerClientConfigBuilder b = new DockerClientConfigBuilder();
        b.withVersion("1.5");
        b.withUri("http://172.17.11.4:2375");
        final DockerClient dockerClient = MDockerClientBuilder.getInstance(b.build()).build();
        
        List<Container> containers = dockerClient.listContainersCmd().withShowAll(false).exec();
        
        String ami = "jenkins-1";
        
        List<Image> images = dockerClient.listImagesCmd().withShowAll(true).withFilter(ami).exec();
        
        if (images.size() == 0)
        {
            
            InputStream imageStream = dockerClient.pullImageCmd("ami").exec();
            
            int streamValue = 0;
            while (streamValue != -1)
            {
                streamValue = imageStream.read();
            }
            imageStream.close(); 
        }
        
        final InspectImageResponse ir = dockerClient.inspectImageCmd(ami).exec();
        
        Collection<Container> matching = Collections2.filter(containers, new Predicate<Container>() {
            public boolean apply(@Nullable Container container)
            {
                
                InspectContainerResponse cis = dockerClient.inspectContainerCmd(container.getId()).exec();
                return (cis.getImageId().equalsIgnoreCase(ir.getId()));
            }
        });
        System.out.println(matching.size());
    }
}
