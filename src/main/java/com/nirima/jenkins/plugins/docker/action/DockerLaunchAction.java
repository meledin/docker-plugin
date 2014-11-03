package com.nirima.jenkins.plugins.docker.action;

import hudson.model.Action;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.github.dockerjava.api.DockerClient;

/**
 * Action to record launching of a slave.
 */
public class DockerLaunchAction implements Action, Serializable, Cloneable{

    public static class Item {
        public final DockerClient client;
        public final String id;

        public Item(DockerClient client, String id) {
            this.client = client;
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || this.getClass() != o.getClass()) return false;

            Item item = (Item) o;

            if (!this.client.equals(item.client)) return false;
            if (!this.id.equals(item.id)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = this.client.hashCode();
            result = 31 * result + this.id.hashCode();
            return result;
        }
    }

    private transient List<Item> running = new ArrayList<Item>();

    public String getIconFileName() {
        return null;
    }

    public String getDisplayName() {
        return null;
    }

    public String getUrlName() {
        return null;
    }

    public void started(DockerClient client, String containerName) {
        this.running.add( new Item(client, containerName) );
    }

    public void stopped(DockerClient client, String containerName) {
        this.running.remove( new Item(client, containerName) );
    }

    public Iterable<Item> getRunning() {
        return this.running;
    }
}
